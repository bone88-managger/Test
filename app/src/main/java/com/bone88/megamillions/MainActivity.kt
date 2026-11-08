package com.bone88.megamillions

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

private data class Draw(val date: String, val white: List<Int>, val mega: Int) {
    val values: Set<Int> get() = (white + mega).toSet()
    val key: String get() = white.joinToString(",") + "|" + mega
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("mega_millions", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MaterialTheme {
                SimulatorScreen(
                    initialHistory = readDraws("history"),
                    initialGenerated = readDraws("generated"),
                    onSave = { history, generated ->
                        saveDraws("history", history)
                        saveDraws("generated", generated)
                    },
                    onImport = { uri -> importHistory(uri) }
                )
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

    private fun importHistory(uri: Uri): List<Draw> {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo abrir el archivo.")
        val mime = contentResolver.getType(uri).orEmpty()
        val text = if (mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")) {
            PDDocument.load(ByteArrayInputStream(bytes)).use { PDFTextStripper().getText(it) }
        } else {
            bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }
        val parsed = if (mime.contains("pdf") || uri.toString().lowercase().endsWith(".pdf")) {
            parsePdfText(text)
        } else {
            parseCsv(text)
        }
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
            if (white.distinct().size != 5 || (white + nums[5]).any { it !in 1..99 }) return@forEach
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
                if (white.size != 5 || white.distinct().size != 5 || (white + mega).any { it !in 1..99 }) null
                else Draw(cells[indexes[0]], white.sorted(), mega)
            } catch (_: Exception) {
                throw IllegalArgumentException("Error en la fila CSV ${index + 2}.")
            }
        }
    }
}

@Composable
private fun SimulatorScreen(
    initialHistory: List<Draw>,
    initialGenerated: List<Draw>,
    onSave: (List<Draw>, List<Draw>) -> Unit,
    onImport: (Uri) -> List<Draw>
) {
    var history by remember { mutableStateOf(initialHistory) }
    var generated by remember { mutableStateOf(initialGenerated) }
    var latest by remember { mutableStateOf<Draw?>(null) }
    var message by remember { mutableStateOf(if (history.isEmpty()) "Importa el PDF histórico para comenzar." else "Historial listo.") }
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val rows = onImport(uri)
                history = rows
                generated = emptyList()
                latest = null
                onSave(history, generated)
                message = "Se importaron ${rows.size} sorteos correctamente."
                error = false
            } catch (e: Exception) {
                message = e.message ?: "No se pudo leer el archivo."
                error = true
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mega Millions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Simulador Android · historial histórico y filtros de coincidencia")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Aviso", fontWeight = FontWeight.Bold)
                    Text("No predice sorteos ni puede garantizar que nadie gane. Los filtros solo limitan las combinaciones que acepta esta app.")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf", "text/*", "application/octet-stream")) }) {
                    Text("Cargar PDF / CSV")
                }
                Text("${history.size} sorteos", style = MaterialTheme.typography.labelLarge)
            }
            Text("Reglas: 5 blancas de 1–70 y Mega Ball independiente de 1–24. Se rechazan combinaciones con más de 2 valores numéricos compartidos con cualquier fila histórica.")
            Button(
                onClick = {
                    try {
                        require(history.isNotEmpty()) { "Primero carga el PDF histórico." }
                        val candidate = generateCandidate(history + generated)
                        generated = generated + candidate
                        latest = candidate
                        onSave(history, generated)
                        message = "Combinación aceptada: cumple los filtros configurados."
                        error = false
                    } catch (e: Exception) {
                        message = e.message ?: "No se pudo generar."
                        error = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = history.isNotEmpty()
            ) { Text("GENERAR COMBINACIÓN") }

            latest?.let { draw ->
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
            Text(message, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Divider()
            Text("Combinaciones generadas en este dispositivo: ${generated.size}", fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(generated.asReversed()) { draw ->
                    Text("${draw.white.joinToString(" · ")}  |  Mega Ball: ${draw.mega}")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("La app guarda el historial y las combinaciones localmente en este teléfono.")
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

private fun generateCandidate(history: List<Draw>): Draw {
    val random = SecureRandom()
    val existing = history.map { it.key }.toSet()
    repeat(1_000_000) {
        val whiteSet = linkedSetOf<Int>()
        while (whiteSet.size < 5) whiteSet.add(random.nextInt(70) + 1)
        val white = whiteSet.sorted()
        val mega = random.nextInt(24) + 1
        val candidate = Draw("SIMULADO", white, mega)
        if (candidate.key in existing) return@repeat
        val candidateValues = candidate.values
        val allowed = history.none { old -> candidateValues.intersect(old.values).size > 2 }
        if (allowed) return candidate
    }
    error("No se encontró combinación válida tras 1.000.000 de intentos. El filtro puede ser demasiado estricto.")
}
