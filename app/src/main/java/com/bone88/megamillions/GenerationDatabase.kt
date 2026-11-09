package com.bone88.megamillions

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.JsonReader
import java.io.InputStreamReader

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
        generated: List<Draw>,
        log: List<GenerationRecord>,
        legacyLogExists: Boolean
    ) {
        val db = writableDatabase
        if (metadata(db, "legacy_migration_v1") == "done") return
        db.beginTransaction()
        try {
            generated.forEach { insertDraw(db, it) }
            log.forEach { insertRecord(db, it) }

            val acceptedCount = countIn(db, true)
            val rejectedCount = countIn(db, false)
            check(acceptedCount == log.count { it.accepted }) {
                "No se pudo verificar el total de registros aceptados migrados."
            }
            check(rejectedCount == log.count { !it.accepted }) {
                "No se pudo verificar el total de registros rechazados migrados."
            }
            val expectedGenerated = generated.map { it.white.sorted().joinToString(",") }.distinct().size
            val actualGenerated = db.rawQuery("SELECT COUNT(*) FROM generated_draws", null).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
            check(actualGenerated == expectedGenerated) {
                "No se pudo verificar el total de combinaciones generadas migradas."
            }

            putMetadata(db, "legacy_migration_v1", "done")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        // The caller removes the legacy file only after this method returns.
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
