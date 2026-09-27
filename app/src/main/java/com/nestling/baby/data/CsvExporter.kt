package com.nestling.baby.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.nestling.baby.R
import com.nestling.baby.domain.BabyEvent
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The whole log, as a file the parent owns. No account needed to get your data out —
 * that is the entire point.
 */
object CsvExporter {

    const val HEADER = "id,type,start_local,end_local,duration_seconds,side,amount_ml,diaper,note"

    private val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /** Pure function so a unit test can prove every event ever logged is in the file. */
    fun toCsv(events: List<BabyEvent>, zone: ZoneId = ZoneId.systemDefault()): String {
        val builder = StringBuilder(HEADER).append('\n')
        events.forEach { event ->
            builder
                .append(event.id).append(',')
                .append(event.type.name.lowercase()).append(',')
                .append(timestamp(event.startedAt, zone)).append(',')
                .append(event.endedAt?.let { timestamp(it, zone) }.orEmpty()).append(',')
                .append(event.durationMillis?.let { it / 1000 }?.toString().orEmpty()).append(',')
                .append(event.side?.name?.lowercase().orEmpty()).append(',')
                .append(event.amountMl?.toString().orEmpty()).append(',')
                .append(event.diaper?.name?.lowercase().orEmpty()).append(',')
                .append(escape(event.note.orEmpty()))
                .append('\n')
        }
        return builder.toString()
    }

    fun fileName(today: LocalDate = LocalDate.now()): String = "nestling-log-$today.csv"

    /** Writes into cacheDir/exports, the only directory the FileProvider exposes. */
    fun write(context: Context, csv: String, fileName: String = fileName()): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(csv)
        return file
    }

    suspend fun export(
        context: Context,
        repository: NestlingRepository,
        zone: ZoneId = ZoneId.systemDefault(),
    ): File = write(context, toCsv(repository.allEventsAscending(), zone))

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_share_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, context.getString(R.string.export_share_title)).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun timestamp(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(TIMESTAMP)

    private fun escape(raw: String): String =
        if (raw.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + raw.replace("\"", "\"\"").replace("\r\n", " ").replace('\n', ' ') + "\""
        } else {
            raw
        }
}
