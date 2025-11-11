package com.example.f1telemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL

class TelemetryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TelemetryScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen() {
    var selectedDriver by remember { mutableStateOf("Verstappen") }
    var selectedMetric by remember { mutableStateOf("Speed") }
    var lapData by remember { mutableStateOf<List<Pair<Int, Double>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val drivers = mapOf(
        "Verstappen" to 1,
        "Leclerc" to 16,
        "Norris" to 4,
        "Hamilton" to 44,
        "Alonso" to 14
    )

    val metrics = listOf("Speed", "Throttle", "Brake", "RPM", "Gear")

    fun loadTelemetry() {
        fetchTelemetryData(
            driverNum = drivers[selectedDriver] ?: 1,
            sessionKey = 9158,
            metric = selectedMetric.lowercase(),
            onResult = { data ->
                lapData = data
                isLoading = false
                errorMessage = null
            },
            onError = { err ->
                isLoading = false
                errorMessage = err
            }
        )
    }

    LaunchedEffect(Unit) { loadTelemetry() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = { Text("OpenF1 Telemetry Dashboard") })
        Spacer(modifier = Modifier.height(16.dp))

        var expandedDriver by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { expandedDriver = true }) {
                Text("Driver: $selectedDriver")
            }
            DropdownMenu(
                expanded = expandedDriver,
                onDismissRequest = { expandedDriver = false }
            ) {
                drivers.keys.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedDriver = name
                            expandedDriver = false
                            isLoading = true
                            loadTelemetry()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        var expandedMetric by remember { mutableStateOf(false) }
        Box {
            Button(onClick = { expandedMetric = true }) {
                Text("Metric: $selectedMetric")
            }
            DropdownMenu(
                expanded = expandedMetric,
                onDismissRequest = { expandedMetric = false }
            ) {
                metrics.forEach { metric ->
                    DropdownMenuItem(
                        text = { Text(metric) },
                        onClick = {
                            selectedMetric = metric
                            expandedMetric = false
                            isLoading = true
                            loadTelemetry()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when {
            isLoading -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage ?: "")
            lapData.isNotEmpty() -> LapChart(lapData, selectedMetric)
        }
    }
}

fun fetchTelemetryData(
    driverNum: Int,
    sessionKey: Int,
    metric: String,
    onResult: (List<Pair<Int, Double>>) -> Unit,
    onError: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val apiUrl = "https://api.openf1.org/v1/car_data?session_key=$sessionKey&driver_number=$driverNum"
            val response = URL(apiUrl).readText()
            val jsonArray = JSONArray(response)
            val data = mutableListOf<Pair<Int, Double>>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (!obj.isNull(metric)) {
                    val rawValue = obj.getDouble(metric)
                    val valid = when (metric) {
                        "speed" -> rawValue in 0.0..400.0
                        "rpm" -> rawValue in 0.0..13000.0
                        "throttle", "brake" -> rawValue in 0.0..100.0
                        "n_gear" -> rawValue in 0.0..9.0
                        else -> true
                    }
                    if (valid) data.add(Pair(i, rawValue))
                }
            }

            if (data.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onError("No valid $metric data available.")
                }
                return@launch
            }

            val lapAverages = data.groupBy { it.first / 400 }
                .map { (lap, values) -> Pair(lap + 1, values.map { it.second }.average()) }

            withContext(Dispatchers.Main) { onResult(lapAverages) }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Error fetching $metric: ${e.localizedMessage}") }
        }
    }
}

@Composable
fun LapChart(laps: List<Pair<Int, Double>>, metricLabel: String) {
    val fullLabel = when (metricLabel.lowercase()) {
        "speed" -> "Speed (km/h)"
        "rpm" -> "Engine RPM"
        "throttle" -> "Throttle (%)"
        "brake" -> "Brake (%)"
        "gear" -> "Gear"
        else -> metricLabel
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$fullLabel vs Time",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            factory = { context ->
                val chart = LineChart(context)

                val entries = laps.map { Entry(it.first.toFloat(), it.second.toFloat()) }
                val dataSet = LineDataSet(entries, fullLabel).apply {
                    color = android.graphics.Color.RED
                    lineWidth = 2f
                    setCircleColor(android.graphics.Color.BLACK)
                    circleRadius = 3f
                    valueTextSize = 9f
                }

                chart.data = LineData(dataSet)
                chart.description.isEnabled = false
                chart.setTouchEnabled(true)
                chart.setPinchZoom(true)
                chart.axisRight.isEnabled = false

                chart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textSize = 12f
                    granularity = 1f
                }

                chart.axisLeft.apply {
                    textSize = 12f
                    axisMinimum = 0f
                    when (metricLabel.lowercase()) {
                        "speed" -> axisMaximum = 400f
                        "rpm" -> axisMaximum = 13000f
                        "throttle", "brake" -> axisMaximum = 100f
                        "gear" -> axisMaximum = 9f
                        else -> axisMaximum = laps.maxOfOrNull { it.second }?.toFloat() ?: 100f
                    }
                    setDrawGridLines(true)
                }

                chart.legend.isEnabled = true
                chart.invalidate()
                chart
            }
        )
        Text("Lap Number", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
