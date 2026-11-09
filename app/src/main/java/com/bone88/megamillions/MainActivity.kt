package com.bone88.megamillions

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WHITE_MIN = 1
private const val WHITE_MAX = 70
private const val WHITE_COUNT = 5
private const val MEGA_MIN = 1
private const val MEGA_MAX = 24
private const val MAX_ALLOWED_ATTEMPTS = 250_000

internal fun isValidDrawNumbers(white: List<Int>, mega: Int): Boolean =
    white.size == WHITE_COUNT && white.distinct().size == WHITE_COUNT &&
        white.all { it in WHITE_MIN..WHITE_MAX } && mega in MEGA_MIN..MEGA_MAX

internal data class Draw(val date: String, val white: List<Int>, val mega: Int) {
    val key: String get() = white.sorted().joinToString(",") + "|" + mega
}

// White-ball matching is positional: only equal values in the same ascending column count.
// Mega Ball never contributes.
internal fun hasWhiteColumnConflict(candidate: Draw, other: Draw): Boolean =
    candidate.white.zip(other.white).count { (candidateNumber, otherNumber) ->
        candidateNumber == otherNumber
    } > 1

internal fun hasHistoricalWhiteConflict(candidate: Draw, history: List<Draw>): Boolean =
    history.any { old -> hasWhiteColumnConflict(candidate, old) }

internal fun hasWhiteBallDuplicate(candidate: Draw, history: List<Draw>): Boolean =
    history.any { old -> old.white == candidate.white }

internal data class GenerationRecord(
    val timestamp: Long,
    val attempt: Int,
    val white: List<Int>,
    val mega: Int,
    val accepted: Boolean,
    val reason: String
) {
    val key: String get() = white.sorted().joinToString(",") + "|" + mega
}

private data class GenerationResult(
    val candidate: Draw?,
    val attempts: Int
)

private data class SimulatorInitialData(
    val history: List<Draw>,
    val acceptedRecords: List<GenerationRecord>,
    val rejectedRecords: List<GenerationRecord>,
    val generatedCount: Int,
    val acceptedCount: Int,
    val rejectedCount: Int
)

internal object SecureLotteryRandom {
    private val source = SecureRandom()
    private const val UINT32_RANGE = 4_294_967_296L
    private const val UINT32_MASK = 0xffff_ffffL

    // Uniform bounded sampling: reject the incomplete tail of the 32-bit range.
    internal fun nextInt(bound: Int, random: SecureRandom = source): Int {
        require(bound > 0)
        val limit = UINT32_RANGE - (UINT32_RANGE % bound.toLong())
        while (true) {
            val unsigned = random.nextInt().toLong() and UINT32_MASK
            val mapped = mapUnsignedToBound(unsigned, bound, limit)
            if (mapped != null) return mapped
        }
    }

    internal fun mapUnsignedToBound(value: Long, bound: Int): Int? {
        require(bound > 0)
        require(value in 0 until UINT32_RANGE)
        val limit = UINT32_RANGE - (UINT32_RANGE % bound.toLong())
        return mapUnsignedToBound(value, bound, limit)
    }

    private fun mapUnsignedToBound(value: Long, bound: Int, limit: Long): Int? {
        if (value >= limit) return null
        return (value % bound.toLong()).toInt()
    }

    // Fisher-Yates selects five distinct values without replacement.
    internal fun sampleWhite(random: SecureRandom = source): List<Int> {
        val pool = (WHITE_MIN..WHITE_MAX).toMutableList()
        for (index in pool.lastIndex downTo 1) {
            val other = nextInt(index + 1, random)
            val temp = pool[index]
            pool[index] = pool[other]
            pool[other] = temp
        }
        return pool.take(WHITE_COUNT).sorted()
    }

    internal fun sampleMega(random: SecureRandom = source): Int =
        MEGA_MIN + nextInt(MEGA_MAX - MEGA_MIN + 1, random)
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("mega_millions", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text("Preparando y verificando los historiales...")
                    }
                }
            }
        }
        val database = GenerationDatabase(applicationContext)
        lifecycleScope.launch {
            try {
                val initialData = withContext(Dispatchers.IO) {
                    val legacyLogExists = fileList().contains("generation_log.json")
                    val legacyLogInput = if (legacyLogExists) openFileInput("generation_log.json") else null
                    database.migrateLegacy(
                        generatedJson = prefs.getString("generated", null),
                        legacyLogInput = legacyLogInput,
                        legacyLogExists = legacyLogExists
                    )
                    SimulatorInitialData(
                        history = readDraws("history"),
                        acceptedRecords = database.page(true, 50, 0),
                        rejectedRecords = database.page(false, 50, 0),
                        generatedCount = database.generatedCount(),
                        acceptedCount = database.count(true),
                        rejectedCount = database.count(false)
                    )
                }
                setContent {
                    MaterialTheme {
                        SimulatorScreen(
                            database = database,
                            initialData = initialData,
                            onSaveHistory = { history -> saveDraws("history", history) },
                            onImport = { uri -> importHistory(uri) }
                        )
                    }
                }
            } catch (e: Exception) {
                setContent {
                    MaterialTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("No se pudo verificar la migración a SQLite.", fontWeight = FontWeight.Bold)
                                Text("Los datos originales se conservaron. Cierra y vuelve a abrir la aplicación después de revisar el problema.")
                                Text(e.message ?: "Error de lectura o almacenamiento.")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readDraws(key: String): List<Draw> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                val nums = item.getJSONArray("white")
                Draw(item.getString("date"), (0 until nums.length()).map { nums.getInt(it) }, item.getInt("mega"))
            }
        } catch (_: Exception) { emptyList() }
    }    

    private fun saveDraws(key: String, draws: List<Draw>) {
        val array = JSONArray()
        draws.forEach { draw ->
            val whites = JSONArray()
            draw.white.forEach { whites.put(it) }
            array.put(JSONObject().put("date", draw.date).put("white", whites).put("mega", draw.mega))
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun readGenerationLog(): List<GenerationRecord> {
        return try {
            val raw = openFileInput("generation_log.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val numbers = item.getJSONArray("white")
                GenerationRecord(
                    timestamp = item.getLong("timestamp"),
                    attempt = item.getInt("attempt"),
                    white = (0 until numbers.length()).map { numbers.getInt(it) },
                    mega = item.getInt("mega"),
                    accepted = item.getBoolean("accepted"),
                    reason = item.getString("reason")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveGenerationLog(records: List<GenerationRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val whites = JSONArray()
            record.white.forEach { whites.put(it) }
            array.put(
                JSONObject()
                    .put("timestamp", record.timestamp)
                    .put("attempt", record.attempt)
                    .put("white", whites)
                    .put("mega", record.mega)
                    .put("accepted", record.accepted)
                    .put("reason", record.reason)
            )
        }
        openFileOutput("generation_log.json", MODE_PRIVATE).bufferedWriter(Charsets.UTF_8).use {
            it.write(array.toString())
        }
    }

    private fun importHistory(uri: Uri): List<Draw> {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo abrir el archivo.")
        val mime = contentResolver.getType(uri).orEmpty()
        val isPdf = mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")
        val text = if (isPdf) {
            PDDocument.load(ByteArrayInputStream(bytes)).use { PDFTextStripper().getText(it) }
        } else {
            bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }
        val parsed = if (isPdf) parsePdfText(text) else parseCsv(text)
        require(parsed.isNotEmpty()) { "No encontré sorteos reconocibles en el archivo." }
        return parsed
    }

    private fun parsePdfText(text: String): List<Draw> {
        val dateLine = Regex("""^\s*(\d{1,2}/\d{1,2}/\d{2,4})\s+(.+)$""")
        val number = Regex("""\b\d{1,2}\b""")
        val rows = linkedMapOf<String, Draw>()
        text.lineSequence().forEach { line ->
            val match = dateLine.find(line) ?: return@forEach
            val nums = number.findAll(match.groupValues[2]).map { it.value.toInt() }.toList()
            if (nums.size < 6) return@forEach
            val white = nums.take(5).sorted()
            if (!isValidDrawNumbers(white, nums[5])) return@forEach
            val draw = Draw(match.groupValues[1], white, nums[5])
            rows[draw.date + "|" + draw.key] = draw
        }
        return rows.values.toList()
    }

    private fun parseCsv(text: String): List<Draw> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size >= 2) { "El CSV está vacío." }
        val header = lines.first().split(",").map { it.trim().lowercase() }
        val required = listOf("date", "white1", "white2", "white3", "white4", "white5", "mega_ball")
        val indexes = required.map { header.indexOf(it) }
        require(indexes.all { it >= 0 }) { "El CSV necesita date, white1..white5 y mega_ball." }
        return lines.drop(1).mapIndexedNotNull { index, line ->
            val cells = line.split(",").map { it.trim().trim('"') }
            try {
                val white = (1..5).map { cells[indexes[it]].toInt() }
                val mega = cells[indexes[6]].toInt()
                if (!isValidDrawNumbers(white, mega)) null
                else Draw(cells[indexes[0]], white.sorted(), mega)
            } catch (_: Exception) {
                throw IllegalArgumentException("Error en la fila CSV " + (index + 2) + ".")
            }
        }
    }
}

@Composable
private fun SimulatorScreen(
    database: GenerationDatabase,
    initialData: SimulatorInitialData,
    onSaveHistory: (List<Draw>) -> Unit,
    onImport: (Uri) -> List<Draw>
) {
    var history by remember { mutableStateOf(initialData.history) }
    var acceptedRecords by remember { mutableStateOf(initialData.acceptedRecords) }
    var rejectedRecords by remember { mutableStateOf(initialData.rejectedRecords) }
    var generatedCount by remember { mutableIntStateOf(initialData.generatedCount) }
    var acceptedCount by remember { mutableIntStateOf(initialData.acceptedCount) }
    var rejectedCount by remember { mutableIntStateOf(initialData.rejectedCount) }
    var showRejectedHistory by remember { mutableStateOf(false) }
    var confirmClearAccepted by remember { mutableStateOf(false) }
    var confirmClearRejected by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<Draw?>(null) }
    var attemptLimit by remember { mutableStateOf("100000") }
    var message by remember { mutableStateOf(if (history.isEmpty()) "Importa el PDF histórico para comenzar." else "Historial listo.") }
    var error by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !isImporting) {
            scope.launch {
                isImporting = true
                message = "Leyendo el archivo. Puedes esperar aquí..."
                error = false
                try {
                    // PDF parsing and file reading run off the UI thread to prevent a frozen/black screen.
                    val rows = withContext(Dispatchers.IO) { onImport(uri) }
                    history = rows
                    // Keep the permanent generated-combination registry and log when history is re-imported.
                    onSaveHistory(history)
                    message = "Se importaron " + rows.size + " sorteos correctamente. Las generaciones anteriores se conservaron."
                } catch (e: Exception) {
                    message = e.message ?: "No se pudo leer el archivo."
                    error = true
                } finally {
                    isImporting = false
                }
            }
        }
    }

    if (confirmClearAccepted) {
        AlertDialog(
            onDismissRequest = { confirmClearAccepted = false },
            title = { Text("Borrar historial de aceptados") },
            text = { Text("Se eliminarán las combinaciones aceptadas guardadas. Podrán volver a generarse y aceptarse.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { database.clear(true) }
                            acceptedRecords = emptyList()
                            acceptedCount = 0
                            generatedCount = 0
                            latest = null
                            message = "Se borró el historial de aceptados."
                        } catch (e: Exception) {
                            message = e.message ?: "No se pudo borrar el historial de aceptados."
                            error = true
                        } finally {
                            confirmClearAccepted = false
                        }
                    }
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAccepted = false }) { Text("Cancelar") }
            }
        )
    }
    if (confirmClearRejected) {
        AlertDialog(
            onDismissRequest = { confirmClearRejected = false },
            title = { Text("Borrar historial de rechazados") },
            text = { Text("Se eliminarán los rechazos guardados. La aplicación volverá a evaluar esas combinaciones si aparecen otra vez.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { database.clear(false) }
                            rejectedRecords = emptyList()
                            rejectedCount = 0
                            message = "Se borró el historial de rechazados."
                        } catch (e: Exception) {
                            message = e.message ?: "No se pudo borrar el historial de rechazados."
                            error = true
                        } finally {
                            confirmClearRejected = false
                        }
                    }
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearRejected = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (showRejectedHistory) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showRejectedHistory = false }) { Text("VOLVER") }
                        Text("Historial de rechazos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rechazos registrados: " + rejectedCount, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { confirmClearRejected = true }, enabled = !isGenerating) { Text("Borrar rechazos") }
                    }
                }
                items(rejectedRecords) { record ->
                    Card {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("RECHAZADA · Intento " + record.attempt, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text(record.white.joinToString(" · ") + " | Mega Ball: " + record.mega)
                            Text(record.reason, style = MaterialTheme.typography.bodySmall)
                            Text(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp)), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (rejectedCount == 0) item { Text("Todavía no hay rechazos registrados.") }
                if (rejectedRecords.size < rejectedCount) item {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val next = withContext(Dispatchers.IO) {
                                database.page(false, 50, rejectedRecords.size)
                            }
                            rejectedRecords = rejectedRecords + next
                        }
                    }) { Text("Cargar más rechazos") }
                }
            }
        } else {
        // One scrolling list for the entire screen avoids nesting a LazyColumn inside a Column.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Mega Millions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            item { Text("Simulador Android · generación criptográfica y filtros históricos") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Aviso", fontWeight = FontWeight.Bold)
                        Text("No predice sorteos ni puede garantizar que nadie gane. Los filtros solo limitan las combinaciones que acepta esta app.")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("application/pdf", "text/*", "application/octet-stream")) },
                        enabled = !isImporting && !isGenerating
                    ) {
                        Text("Cargar PDF / CSV")
                    }
                    Text(history.size.toString() + " sorteos", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (isImporting) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Cargando y leyendo el PDF...")
                    }
                }
            }
            item {
                Text("Reglas: 5 blancas de 1–70 y Mega Ball independiente de 1–24. Se rechaza si 2 o más bolas blancas coinciden en las mismas columnas, comparando el historial y las combinaciones generadas anteriormente. La Mega Ball no cuenta como coincidencia.")
            }
            item {
                OutlinedTextField(
                    value = attemptLimit,
                    onValueChange = { value -> if (value.all { it.isDigit() }) attemptLimit = value },
                    label = { Text("Máximo de intentos (1–" + MAX_ALLOWED_ATTEMPTS + ")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = {
                        val maxAttempts = attemptLimit.toIntOrNull()
                        if (maxAttempts == null || maxAttempts !in 1..MAX_ALLOWED_ATTEMPTS) {
                            message = "El límite debe estar entre 1 y " + MAX_ALLOWED_ATTEMPTS + "."
                            error = true
                        } else {
                            scope.launch {
                                isGenerating = true
                                try {
                                    require(history.isNotEmpty()) { "Primero carga el PDF histórico." }
                                    val result = withContext(Dispatchers.IO) {
                                        generateCandidate(history, database, maxAttempts)
                                    }
                                    val refreshed = withContext(Dispatchers.IO) {
                                        SimulatorInitialData(
                                            history = history,
                                            acceptedRecords = database.page(true, 50, 0),
                                            rejectedRecords = database.page(false, 50, 0),
                                            generatedCount = database.generatedCount(),
                                            acceptedCount = database.count(true),
                                            rejectedCount = database.count(false)
                                        )
                                    }
                                    acceptedRecords = refreshed.acceptedRecords
                                    rejectedRecords = refreshed.rejectedRecords
                                    generatedCount = refreshed.generatedCount
                                    acceptedCount = refreshed.acceptedCount
                                    rejectedCount = refreshed.rejectedCount
                                    if (result.candidate != null) {
                                        latest = result.candidate
                                        message = "Combinación aceptada en " + result.attempts + " intento(s)."
                                        error = false
                                    } else {
                                        message = "No se encontró una combinación válida tras " + maxAttempts + " intentos. Se registraron todos los rechazos."
                                        error = true
                                    }
                                } catch (e: Exception) {
                                    message = e.message ?: "No se pudo generar."
                                    error = true
                                } finally {
                                    isGenerating = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = history.isNotEmpty() && !isImporting
                ) { Text("GENERAR COMBINACIÓN") }
            }
            latest?.let { draw ->
                item {
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Tu combinación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                draw.white.forEach { NumberBall(it.toString()) }
                                NumberBall(draw.mega.toString(), mega = true)
                            }
                            Text("Aleatoria; no tiene ventaja predictiva sobre otra combinación legal.")
                        }
                    }
                }
            }
            item { Text(message, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            item { Divider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Combinaciones aceptadas y protegidas contra duplicados: " + generatedCount, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { confirmClearAccepted = true }, enabled = !isGenerating) { Text("Borrar aceptados") }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registro de generaciones aceptadas: " + acceptedCount, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { showRejectedHistory = true }) {
                        Text("Ver rechazos (" + rejectedCount + ")")
                    }
                }
            }
            items(acceptedRecords) { record ->
                Card {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            (if (record.accepted) "ACEPTADA" else "RECHAZADA") +
                                " · Intento " + record.attempt,
                            color = if (record.accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(record.white.joinToString(" · ") + " | Mega Ball: " + record.mega)
                        Text(record.reason, style = MaterialTheme.typography.bodySmall)
                        Text(
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp)),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            if (acceptedRecords.size < acceptedCount) item {
                OutlinedButton(onClick = {
                    scope.launch {
                        val next = withContext(Dispatchers.IO) {
                            database.page(true, 50, acceptedRecords.size)
                        }
                        acceptedRecords = acceptedRecords + next
                    }
                }) { Text("Cargar más aceptados") }
            }
            item { Text("El historial oficial, las combinaciones aceptadas y el registro se guardan localmente.") }
        }
        }
    }
}

@Composable
private fun NumberBall(value: String, mega: Boolean = false) {
    Surface(shape = CircleShape, color = if (mega) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

private fun generateCandidate(
    history: List<Draw>,
    database: GenerationDatabase,
    maxAttempts: Int
): GenerationResult {
    val records = mutableListOf<GenerationRecord>()
    val bufferedRejectedKeys = HashSet<String>()
    val batchSize = 250

    fun flush(acceptedDraw: Draw? = null) {
        database.insertGenerationBatch(records, acceptedDraw)
        records.clear()
        bufferedRejectedKeys.clear()
    }

    for (attempt in 1..maxAttempts) {
        val white = SecureLotteryRandom.sampleWhite()
        val whiteKey = white.joinToString(",")
        if (whiteKey in bufferedRejectedKeys || database.hasRejectedWhiteKey(whiteKey)) continue

        val candidate = Draw("SIMULADO", white, SecureLotteryRandom.sampleMega())
        val duplicate = hasWhiteBallDuplicate(candidate, history) ||
            database.hasGeneratedWhiteKey(whiteKey) ||
            database.hasAcceptedWhiteKey(whiteKey)
        val conflictingDraw = if (duplicate) false else
            hasHistoricalWhiteConflict(candidate, history) ||
                database.hasGeneratedColumnConflict(white) ||
                database.hasAcceptedColumnConflict(white)

        val accepted = !duplicate && !conflictingDraw
        val reason = when {
            duplicate -> "Combinación de bolas blancas ya registrada anteriormente."
            conflictingDraw -> "Tiene 2 o más bolas blancas iguales en las mismas columnas que una combinación registrada anteriormente."
            else -> "Aceptada: cumple las reglas y no está duplicada."
        }
        records += GenerationRecord(
            timestamp = System.currentTimeMillis(),
            attempt = attempt,
            white = white,
            mega = candidate.mega,
            accepted = accepted,
            reason = reason
        )

        if (accepted) {
            flush(candidate)
            return GenerationResult(candidate, attempt)
        }

        bufferedRejectedKeys += whiteKey
        if (records.size >= batchSize) flush()
    }

    flush()
    return GenerationResult(null, maxAttempts)
}
