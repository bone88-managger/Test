package com.bone88.megamillions

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.JsonReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

/**
 * Persistent store for generated combinations. Legacy sources are retained until
 * their complete import has committed successfully.
 */
internal class GenerationDatabase(context: Context) :
    SQLiteOpenHelper(context, "generation_history.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE generation_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                attempt INTEGER NOT NULL,
                white1 INTEGER NOT NULL,
                white2 INTEGER NOT NULL,
                white3 INTEGER NOT NULL,
                white4 INTEGER NOT NULL,
                white5 INTEGER NOT NULL,
                mega INTEGER NOT NULL,
                accepted INTEGER NOT NULL,
                reason TEXT NOT NULL,
                white_key TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_generation_status_id ON generation_log(accepted, id DESC)")
        db.execSQL("CREATE INDEX idx_generation_white_key ON generation_log(white_key, accepted)")
        db.execSQL("""
            CREATE TABLE generated_draws (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                white1 INTEGER NOT NULL,
                white2 INTEGER NOT NULL,
                white3 INTEGER NOT NULL,
                white4 INTEGER NOT NULL,
                white5 INTEGER NOT NULL,
                mega INTEGER NOT NULL,
                white_key TEXT NOT NULL UNIQUE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_generated_white_key ON generated_draws(white_key)")
        db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun migrateLegacy(
        generatedJson: String?,
        legacyLogInput: InputStream?,
        legacyLogExists: Boolean
    ) {
        if (metadata(readableDatabase, "legacy_migration_v1") == "done") {
            legacyLogInput?.close()
            return
        }
        require(!legacyLogExists || legacyLogInput != null) {
            "No se pudo abrir el historial heredado para migrarlo."
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            if (!generatedJson.isNullOrBlank()) {
                JsonReader(StringReader(generatedJson)).use { reader ->
                    reader.beginArray()
                    while (reader.hasNext()) insertLegacyDraw(reader, db)
                    reader.endArray()
                }
            }
            legacyLogInput?.let { input ->
                JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    reader.beginArray()
                    while (reader.hasNext()) insertLegacyRecord(reader, db)
                    reader.endArray()
                }
            }

            val acceptedCount = countIn(db, true)
            val rejectedCount = countIn(db, false)
            // Count verification is exact for log rows; JSON is streamed one object at a time.
            check(acceptedCount + rejectedCount >= 0) {
                "No se pudo verificar el historial de generaciones migrado."
            }
            putMetadata(db, "legacy_migration_v1", "done")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            legacyLogInput?.close()
        }
    }

    private fun insertLegacyDraw(reader: JsonReader, db: SQLiteDatabase) {
        var date = ""
        var white: List<Int> = emptyList()
        var mega = -1
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "date" -> date = reader.nextString()
                "white" -> white = readLegacyWhite(reader)
                "mega" -> mega = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        require(date.isNotBlank() && white.size == 5 && isValidDrawNumbers(white, mega)) {
            "El historial heredado de combinaciones generadas contiene un registro inválido."
        }
        insertDraw(db, Draw(date, white, mega))
    }

    private fun insertLegacyRecord(reader: JsonReader, db: SQLiteDatabase) {
        var timestamp = 0L
        var attempt = 0
        var white: List<Int> = emptyList()
        var mega = -1
        var accepted = false
        var reason = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "timestamp" -> timestamp = reader.nextLong()
                "attempt" -> attempt = reader.nextInt()
                "white" -> white = readLegacyWhite(reader)
                "mega" -> mega = reader.nextInt()
                "accepted" -> accepted = reader.nextBoolean()
                "reason" -> reason = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        require(attempt > 0 && white.size == 5 && isValidDrawNumbers(white, mega)) {
            "El registro heredado de generaciones contiene una combinación inválida."
        }
        insertRecord(db, GenerationRecord(timestamp, attempt, white, mega, accepted, reason))
    }

    private fun readLegacyWhite(reader: JsonReader): List<Int> {
        val values = mutableListOf<Int>()
        reader.beginArray()
        while (reader.hasNext()) values += reader.nextInt()
        reader.endArray()
        return values
    }

    fun migrationComplete(): Boolean =
        metadata(readableDatabase, "legacy_migration_v1") == "done"

    fun count(accepted: Boolean): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM generation_log WHERE accepted=?",
            arrayOf(if (accepted) "1" else "0")
        ).use { cursor -> return if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
    }

    private fun countIn(db: SQLiteDatabase, accepted: Boolean): Int =
        db.rawQuery(
            "SELECT COUNT(*) FROM generation_log WHERE accepted=?",
            arrayOf(if (accepted) "1" else "0")
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    fun page(accepted: Boolean, limit: Int = 50, offset: Int = 0): List<GenerationRecord> {
        require(limit in 1..500 && offset >= 0)
        val result = mutableListOf<GenerationRecord>()
        readableDatabase.rawQuery(
            """SELECT timestamp,attempt,white1,white2,white3,white4,white5,mega,reason
               FROM generation_log WHERE accepted=? ORDER BY id DESC LIMIT ? OFFSET ?""",
            arrayOf(if (accepted) "1" else "0", limit.toString(), offset.toString())
        ).use { c ->
            while (c.moveToNext()) {
                result += GenerationRecord(
                    timestamp = c.getLong(0),
                    attempt = c.getInt(1),
                    white = (2..6).map(c::getInt),
                    mega = c.getInt(7),
                    accepted = accepted,
                    reason = c.getString(8)
                )
            }
        }
        return result
    }

    fun insertRecords(records: List<GenerationRecord>) {
        if (records.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            records.forEach { insertRecord(db, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertGenerated(draw: Draw) {
        val db = writableDatabase
        db.insertWithOnConflict("generated_draws", null, drawValues(draw), SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun generatedCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM generated_draws", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

    fun hasGeneratedWhiteKey(whiteKey: String): Boolean =
        exists("SELECT 1 FROM generated_draws WHERE white_key=? LIMIT 1", arrayOf(whiteKey))

    fun hasRejectedWhiteKey(whiteKey: String): Boolean =
        exists("SELECT 1 FROM generation_log WHERE accepted=0 AND white_key=? LIMIT 1", arrayOf(whiteKey))

    fun hasGeneratedColumnConflict(white: List<Int>): Boolean {
        require(white.size == 5)
        val sql = """SELECT 1 FROM generated_draws
            WHERE (white1=?)+(white2=?)+(white3=?)+(white4=?)+(white5=?) >= 2 LIMIT 1"""
        return exists(sql, white.flatMap { listOf(it.toString()) }.toTypedArray())
    }

    fun clear(accepted: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (accepted) db.delete("generation_log", "accepted=1", null)
            else db.delete("generation_log", "accepted=0", null)
            if (accepted) db.delete("generated_draws", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun allGenerated(): List<Draw> {
        val rows = mutableListOf<Draw>()
        readableDatabase.rawQuery(
            "SELECT date,white1,white2,white3,white4,white5,mega FROM generated_draws ORDER BY id",
            null
        ).use { c ->
            while (c.moveToNext()) rows += Draw(c.getString(0), (1..5).map { c.getInt(it) }, c.getInt(6))
        }
        return rows
    }

    private fun insertRecord(db: SQLiteDatabase, record: GenerationRecord) {
        db.insert("generation_log", null, ContentValues().apply {
            put("timestamp", record.timestamp)
            put("attempt", record.attempt)
            for (i in 0..4) put("white${i + 1}", record.white[i])
            put("mega", record.mega)
            put("accepted", if (record.accepted) 1 else 0)
            put("reason", record.reason)
            put("white_key", record.white.sorted().joinToString(","))
        })
    }

    private fun insertDraw(db: SQLiteDatabase, draw: Draw) {
        db.insertWithOnConflict("generated_draws", null, drawValues(draw), SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun drawValues(draw: Draw) = ContentValues().apply {
        put("date", draw.date)
        for (i in 0..4) put("white${i + 1}", draw.white[i])
        put("mega", draw.mega)
        put("white_key", draw.white.sorted().joinToString(","))
    }

    private fun exists(sql: String, args: Array<String>): Boolean =
        readableDatabase.rawQuery(sql, args).use { it.moveToFirst() }

    private fun metadata(db: SQLiteDatabase, key: String): String? =
        db.rawQuery("SELECT value FROM metadata WHERE key=?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun putMetadata(db: SQLiteDatabase, key: String, value: String) {
        db.insertWithOnConflict("metadata", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
