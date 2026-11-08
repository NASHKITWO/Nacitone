/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.nacitone.music.utils.dataStore
import com.nacitone.music.constants.ListenTogetherSessionTokenKey
import com.nacitone.music.constants.ProxyEnabledKey
import com.nacitone.music.constants.ProxyUrlKey
import com.nacitone.music.constants.ProxyTypeKey
import com.nacitone.music.constants.ProxyUsernameKey
import com.nacitone.music.constants.OpenRouterBaseUrlKey
import com.nacitone.music.constants.ListenTogetherServerUrlKey
import com.nacitone.music.constants.ProxyPasswordKey
import com.nacitone.music.constants.LastFMSessionKey
import com.nacitone.music.constants.DeeplApiKey
import com.nacitone.music.constants.OpenRouterApiKey
import okio.Buffer
import okio.buffer
import okio.sink
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.nacitone.innertube.utils.parseCookieString
import com.nacitone.innertube.utils.sha1
import com.nacitone.music.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.nacitone.music.constants.DataSyncIdKey
import com.nacitone.music.constants.InnerTubeAuthUserKey
import com.nacitone.music.constants.InnerTubeCookieKey
import com.nacitone.music.constants.VisitorDataKey
import com.nacitone.music.db.InternalDatabase
import com.nacitone.music.db.MusicDatabase
import com.nacitone.music.db.entities.ArtistEntity
import com.nacitone.music.db.entities.Song
import com.nacitone.music.db.entities.SongEntity
import com.nacitone.music.extensions.div
import com.nacitone.music.extensions.tryOrNull
import com.nacitone.music.extensions.zipInputStream
import com.nacitone.music.extensions.zipOutputStream
import com.nacitone.music.playback.MusicService
import com.nacitone.music.playback.MusicService.Companion.PERSISTENT_AUTOMIX_FILE
import com.nacitone.music.playback.MusicService.Companion.PERSISTENT_PLAYER_STATE_FILE
import com.nacitone.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.nacitone.music.utils.reportException
import com.nacitone.music.utils.ArtistNameAliases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject

data class CsvImportState(
    val previewRows: List<List<String>> = emptyList(),
    val artistColumnIndex: Int = 0,
    val titleColumnIndex: Int = 1,
    val urlColumnIndex: Int = -1,
    val hasHeader: Boolean = true,
)

data class ConvertedSongLog(
    val title: String,
    val artists: String,
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    // Kunci API, sesi, dan kata sandi tidak ikut diekspor agar berkas backup aman dibagikan.
                    val settingsSnapshot =
                        runBlocking(Dispatchers.IO) { context.applicationContext.dataStore.data.first() }
                            .toMutablePreferences()
                    SECRET_PREFERENCE_KEYS.forEach { settingsSnapshot.remove(it) }
                    outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                    runBlocking(Dispatchers.IO) {
                        val sink = outputStream.sink().buffer()
                        PreferencesSerializer.writeTo(settingsSnapshot, sink)
                        sink.flush()
                    }
                    outputStream.putNextEntry(ZipEntry(ArtistNameAliases.BACKUP_FILENAME))
                    outputStream.write(ArtistNameAliases.serialize().encodeToByteArray())
                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    val dbPath = database.openHelper.writableDatabase.path
                    if (dbPath != null) {
                        FileInputStream(dbPath).use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                            inputStream.copyTo(outputStream)
                        }
                        val walFile = File("$dbPath-wal")
                        if (walFile.exists()) {
                            FileInputStream(walFile).use { inputStream ->
                                outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-wal"))
                                inputStream.copyTo(outputStream)
                            }
                        }
                        val shmFile = File("$dbPath-shm")
                        if (shmFile.exists()) {
                            FileInputStream(shmFile).use { inputStream ->
                                outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-shm"))
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                }
            }
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.backup_create_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        // Run in viewModelScope to allow suspending
        viewModelScope.launch(Dispatchers.IO) {
            val restoreDbName = "restored_${InternalDatabase.DB_NAME}"
            val restoreDbPath = context.getDatabasePath(restoreDbName).absolutePath
            val tempSettings = File(context.filesDir, "datastore/$SETTINGS_FILENAME.restore")

            runCatching {
                Timber.tag("RESTORE").i("Starting restore from URI: $uri")

                val currentDbPath = database.openHelper.writableDatabase.path
                if (currentDbPath == null) {
                    Timber.tag("RESTORE").e("Database path is null, cannot restore")
                    return@launch
                }

                File(restoreDbPath).delete()
                File("$restoreDbPath-wal").delete()
                File("$restoreDbPath-shm").delete()
                tempSettings.delete()

                var foundDb = false
                var foundSettings = false
                var restoredArtistNameAliases: Map<String, String>? = null

                context.applicationContext.contentResolver.openInputStream(uri)?.use { raw ->
                    raw.zipInputStream().use { inputStream ->
                        var entry = tryOrNull { inputStream.nextEntry }
                        while (entry != null) {
                            Timber.tag("RESTORE").i("Found zip entry: ${entry.name}")
                            when (entry.name) {
                                SETTINGS_FILENAME -> {
                                    foundSettings = true
                                    // Batasi ukuran dan buang kunci/endpoint sensitif dari backup yang tidak tepercaya.
                                    val raw = inputStream.readBytesLimited(MAX_SETTINGS_BYTES)
                                    val parsed = PreferencesSerializer.readFrom(Buffer().write(raw)).toMutablePreferences()
                                    RESTORE_BLOCKED_KEYS.forEach { parsed.remove(it) }
                                    tempSettings.outputStream().use { out ->
                                        val sink = out.sink().buffer()
                                        PreferencesSerializer.writeTo(parsed, sink)
                                        sink.flush()
                                    }
                                }
                                ArtistNameAliases.BACKUP_FILENAME -> {
                                    restoredArtistNameAliases =
                                        ArtistNameAliases.deserialize(inputStream.readBytesLimited(MAX_SETTINGS_BYTES).decodeToString())
                                }
                                InternalDatabase.DB_NAME -> {
                                    foundDb = true
                                    FileOutputStream(restoreDbPath).use { out ->
                                        val buf = ByteArray(64 * 1024)
                                        var total = 0L
                                        while (true) {
                                            val n = inputStream.read(buf)
                                            if (n < 0) break
                                            total += n
                                            if (total > MAX_DB_BYTES) throw java.io.IOException("Database backup terlalu besar")
                                            out.write(buf, 0, n)
                                        }
                                    }
                                }
                                "${InternalDatabase.DB_NAME}-wal" -> {
                                    // Skip WAL — we'll open cleanly
                                }
                                "${InternalDatabase.DB_NAME}-shm" -> {
                                    // Skip SHM — we'll open cleanly
                                }
                                else -> Timber.tag("RESTORE").i("Skipping unexpected entry: ${entry.name}")
                            }
                            entry = tryOrNull { inputStream.nextEntry }
                        }
                    }
                } ?: run {
                    Timber.tag("RESTORE").e("Could not open input stream for uri: $uri")
                    return@launch
                }

                if (!foundDb && !foundSettings) {
                    Timber.tag("RESTORE").w("No expected entries found in archive")
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                var backupDbVersion = -1
                var currentDbVersion = -1
                if (foundDb) {
                    // Read backup DB version using raw SQLite (no Room involvement)
                    backupDbVersion = InternalDatabase.readDatabaseVersion(restoreDbPath)
                    if (backupDbVersion <= 0) {
                        Timber.tag("RESTORE").e("Cannot read backup database version")
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            Toast.makeText(context, R.string.restore_database_incompatible, Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    // Read current database version dynamically — this matches whatever Room
                    // annotation says, even when the schema version changes in future builds.
                    currentDbVersion = database.openHelper.writableDatabase.version
                    if (backupDbVersion > currentDbVersion) {
                        Timber.tag("RESTORE").w(
                            "Backup DB version $backupDbVersion > current $currentDbVersion, incompatible"
                        )
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            Toast.makeText(context, R.string.restore_database_incompatible, Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                }

                // === Proceed with restore ===

                // 1. Stop service first — ensures no pending DB writes during swap
                context.stopService(Intent(context, MusicService::class.java))
                try {
                    kotlinx.coroutines.withTimeout(5000) {
                        MusicService.shutdownDeferred.await()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Timeout waiting for MusicService to shutdown")
                    File(restoreDbPath).delete()
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 2. Close the database — all operations should be done by now
                database.close()

                // 3. Swap DB files — staged copy to avoid corrupting the live DB
                var dbSwapSucceeded = true
                if (foundDb) {
                    try {
                        val currentFile = File(currentDbPath)
                        val stagedFile = File("$currentDbPath.restore_staged")
                        val backupFile = File("$currentDbPath.restore_backup")

                        stagedFile.delete()
                        backupFile.delete()

                        File(restoreDbPath).copyTo(stagedFile, overwrite = true)

                        // Preserve current DB, promote staged, remove backup
                        if (!currentFile.renameTo(backupFile)) {
                            error("Failed to preserve current DB before restore")
                        }
                        if (!stagedFile.renameTo(currentFile)) {
                            // Rollback: restore the original
                            backupFile.renameTo(currentFile)
                            error("Failed to promote restored DB")
                        }
                        backupFile.delete()

                        // Delete stale WAL/SHM so the DB opens clean
                        File("$currentDbPath-wal").delete()
                        File("$currentDbPath-shm").delete()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to swap DB files")
                        dbSwapSucceeded = false
                    }
                }

                if (dbSwapSucceeded) {
                    if (foundSettings) {
                        val actualSettings = File(
                            context.filesDir,
                            "datastore/$SETTINGS_FILENAME",
                        )
                        tempSettings.copyTo(actualSettings, overwrite = true)
                    }

                    context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
                    context.filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
                    context.filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
                    ArtistNameAliases.restore(context, restoredArtistNameAliases.orEmpty())

                    // 4. Restart — Room will open the swapped DB and run migrations if needed
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    intent?.let { context.startActivity(it) }
                    Runtime.getRuntime().exit(0)
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                reportException(it)
                Timber.tag("RESTORE").e(it, "Restore failed unexpectedly")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
                }
            }

            File(restoreDbPath).delete()
            File("$restoreDbPath-wal").delete()
            File("$restoreDbPath-shm").delete()
            tempSettings.delete()
        }
    }

    fun previewCsvFile(context: Context, uri: Uri): CsvImportState {
        val previewRows = mutableListOf<List<String>>()
        val csvState: CsvImportState
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val rowsToPreview = lines.take(6).map { parseCsvLine(it) }
                previewRows.addAll(rowsToPreview)

                val hasHeader = lines.isNotEmpty() && lines[0].contains(",")
                // Deteksi otomatis kolom dari ekspor Spotify (Exportify) atau CSV serupa.
                val header = previewRows.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
                val detectedTitle = header.indexOfFirst {
                    it == "track name" || it == "title" || it == "judul" || it == "song" || it == "name"
                }
                val detectedArtist = header.indexOfFirst { it.startsWith("artist") || it == "artis" }
                val detected = detectedTitle >= 0 && detectedArtist >= 0
                csvState = if (detected) {
                    CsvImportState(
                        previewRows = previewRows,
                        artistColumnIndex = detectedArtist,
                        titleColumnIndex = detectedTitle,
                        hasHeader = true,
                    )
                } else {
                    CsvImportState(
                        previewRows = previewRows,
                        hasHeader = hasHeader,
                    )
                }
                return csvState
            }
        }.onFailure {
            reportException(it)
            Toast.makeText(context, "Failed to preview CSV file", Toast.LENGTH_SHORT).show()
        }
        return CsvImportState()
    }

    suspend fun importPlaylistFromCsv(
        context: Context,
        uri: Uri,
        columnMapping: CsvImportState,
        onProgress: (Int) -> Unit = {},
        onLogUpdate: (List<ConvertedSongLog>) -> Unit = {},
    ): ArrayList<Song> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val songs = arrayListOf<Song>()
        val recentLogs = mutableListOf<ConvertedSongLog>()

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                val startIndex = if (columnMapping.hasHeader) 1 else 0
                val totalLines = lines.size - startIndex

                lines.drop(startIndex).forEachIndexed { index, line ->
                    val parts = parseCsvLine(line)

                    if (parts.isNotEmpty()) {
                        if (columnMapping.artistColumnIndex < parts.size && columnMapping.titleColumnIndex < parts.size) {
                            val title = parts[columnMapping.titleColumnIndex].trim()
                            val artistStr = parts[columnMapping.artistColumnIndex].trim()

                            if (title.isNotEmpty() && artistStr.isNotEmpty()) {
                                val artists = artistStr.split(";", ",").map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { ArtistEntity(id = "", name = it) }

                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = title,
                                    ),
                                    artists = artists,
                                )
                                songs.add(mockSong)

                                val logEntry = ConvertedSongLog(
                                    title = title,
                                    artists = artists.joinToString(", ") { it.name },
                                )
                                recentLogs.add(0, logEntry)
                                if (recentLogs.size > 3) {
                                    recentLogs.removeAt(recentLogs.size - 1)
                                }
                                onLogUpdate(recentLogs.toList())
                            }
                        }
                    }

                    val progress = ((index + 1) * 100) / totalLines
                    onProgress(progress)
                }
            }
        }.onFailure {
            reportException(it)
        }

        songs
    }

    suspend fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        return importPlaylistFromCsv(context, uri, CsvImportState())
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result.map { it.trim().trim('"') }
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.isNotEmpty() && lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)
                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
        private const val MAX_DB_BYTES = 2L * 1024 * 1024 * 1024
        private const val MAX_SETTINGS_BYTES = 2L * 1024 * 1024

        private fun java.io.InputStream.readBytesLimited(max: Long): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0L
            while (true) {
                val n = read(buf)
                if (n < 0) break
                total += n
                require(total <= max) { "Berkas pengaturan terlalu besar" }
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }

        // Pengaturan yang bisa mengalihkan lalu lintas/kunci ke server penyerang tidak dipulihkan dari backup.
        private val RESTORE_BLOCKED_KEYS: List<androidx.datastore.preferences.core.Preferences.Key<*>> =
            listOf(
                ProxyEnabledKey,
                ProxyUrlKey,
                ProxyTypeKey,
                ProxyUsernameKey,
                ProxyPasswordKey,
                OpenRouterBaseUrlKey,
                ListenTogetherServerUrlKey,
                OpenRouterApiKey,
                DeeplApiKey,
                LastFMSessionKey,
                ListenTogetherSessionTokenKey,
                InnerTubeCookieKey,
                VisitorDataKey,
                DataSyncIdKey,
            )

        private val SECRET_PREFERENCE_KEYS: List<androidx.datastore.preferences.core.Preferences.Key<String>> =
            listOf(
                OpenRouterApiKey,
                DeeplApiKey,
                LastFMSessionKey,
                ProxyPasswordKey,
                ListenTogetherSessionTokenKey,
                InnerTubeCookieKey,
                VisitorDataKey,
                DataSyncIdKey,
            )
    }
}
