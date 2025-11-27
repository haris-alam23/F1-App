package com.example.f1telemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.example.f1telemetry.ui.theme.F1White
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import androidx.core.graphics.toColorInt

class TelemetryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F1TelemetryTheme {
                TelemetryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen() {
    var selectedYear by remember { mutableStateOf(2025) }


    val metricOptions = listOf(
        "Speed (km/h)" to "speed",
        "Throttle (%)" to "throttle",
        "Brake" to "brake",
        "RPM" to "rpm",
        "Gear" to "n_gear",
        "DRS State" to "drs"
    )
    var selectedMetric by remember { mutableStateOf(metricOptions[0]) }

    var selectedMeeting by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var selectedSession by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var selectedDriver by remember { mutableStateOf<Pair<Int, String>?>(null) }

    var carData by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF171717),
                        Color(0xFF1d1d1d),
                        Color(0xFF242424)

                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Telemetry Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )


        SettingCard {
            Text(
                "Year",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            YearDropdown(
                selectedYear = selectedYear,
                onSelected = { year ->
                    selectedYear = year
                    selectedMeeting = null
                    selectedSession = null
                    selectedDriver = null
                    carData = emptyList()
                    errorMessage = null
                }
            )
        }


        SettingCard {
            Text(
                "Race",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MeetingDropdown(
                year = selectedYear,
                selected = selectedMeeting,
                onSelected = {
                    selectedMeeting = it
                    selectedSession = null
                    selectedDriver = null
                    carData = emptyList()
                    errorMessage = null
                }
            )
        }

        SettingCard {
            Text(
                "Session",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SessionDropdown(
                meetingKey = selectedMeeting?.first,
                selected = selectedSession,
                onSelected = { session ->
                    selectedSession = session
                    selectedDriver = null
                    carData = emptyList()
                    errorMessage = null
                }
            )
        }

        SettingCard {
            Text(
                "Driver",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            DriverDropdown(
                sessionKey = selectedSession?.first,
                selected = selectedDriver,
                onSelected = { driver ->
                    selectedDriver = driver
                    carData = emptyList()
                    errorMessage = null
                }
            )
        }

        SettingCard {
            Text(
                "Metric",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MetricDropdown(
                options = metricOptions,
                selected = selectedMetric,
                onSelected = { metric ->
                    selectedMetric = metric
                    carData = emptyList()
                    errorMessage = null
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            onClick = {
                errorMessage = null

                if (selectedMeeting == null || selectedSession == null || selectedDriver == null) {
                    errorMessage = "Please select a year, race, session, and driver."
                    return@Button
                }

                val driverNumber = selectedDriver!!.first
                val sessionKey = selectedSession!!.first
                val metricField = selectedMetric.second

                isLoading = true
                scope.launch {
                    try {
                        carData = fetchCarData(driverNumber, sessionKey, metricField)
                        if (carData.isEmpty()) {
                            errorMessage =
                                "No telemetry available for this session/metric. Try Practice/Qualifying or another metric."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            Text("Load Telemetry")
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (carData.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp)
                ) {
                    Text(
                        selectedMetric.first,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TelemetryChart(carData, selectedMetric.first)
                }
            }
        }
    }
}


@Composable
fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),

    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8d0000),
                            Color(0xFF5A0A0A),
                            Color(0xFF1d1d1d)

                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}


@Composable
fun YearDropdown(
    selectedYear: Int,
    onSelected: (Int) -> Unit
) {
    val years = listOf(2025,2024, 2023, 2022, 2021)
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.width(260.dp)
    ) {
        Text(selectedYear.toString())
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.width(260.dp)
    ) {
        years.forEach { year ->
            DropdownMenuItem(
                text = { Text(year.toString()) },
                onClick = {
                    onSelected(year)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun MeetingDropdown(
    year: Int,
    selected: Pair<Int, String>?,
    onSelected: (Pair<Int, String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var meetings by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }

    LaunchedEffect(year) {
        meetings = fetchMeetings(year)
    }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.width(260.dp)
    ) {
        Text(selected?.second ?: "Select Race")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.width(260.dp)
    ) {
        if (meetings.isEmpty()) {
            DropdownMenuItem(text = { Text("Loading...") }, onClick = {})
        } else {
            meetings.forEach { meeting ->
                DropdownMenuItem(
                    text = { Text(meeting.second) },
                    onClick = {
                        onSelected(meeting)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SessionDropdown(
    meetingKey: Int?,
    selected: Pair<Int, String>?,
    onSelected: (Pair<Int, String>) -> Unit
) {
    if (meetingKey == null) {
        Text("Select a race first")
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }

    LaunchedEffect(meetingKey) {
        sessions = fetchSessions(meetingKey)
    }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.width(260.dp)
    ) {
        Text(selected?.second ?: "Select Session")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.width(260.dp)
    ) {
        if (sessions.isEmpty()) {
            DropdownMenuItem(text = { Text("Loading sessions...") }, onClick = {})
        } else {
            sessions.forEach { session ->
                DropdownMenuItem(
                    text = { Text(session.second) },
                    onClick = {
                        onSelected(session)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DriverDropdown(
    sessionKey: Int?,
    selected: Pair<Int, String>?,
    onSelected: (Pair<Int, String>) -> Unit
) {
    if (sessionKey == null) {
        Text("Select a session first")
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var drivers by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }

    LaunchedEffect(sessionKey) {
        drivers = fetchDrivers(sessionKey)
    }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.width(260.dp)
    ) {
        Text(selected?.second ?: "Select Driver")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.width(260.dp)
    ) {
        if (drivers.isEmpty()) {
            DropdownMenuItem(text = { Text("Loading drivers...") }, onClick = {})
        } else {
            drivers.forEach { driver ->
                DropdownMenuItem(
                    text = { Text("${driver.first} - ${driver.second}") },
                    onClick = {
                        onSelected(driver)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MetricDropdown(
    options: List<Pair<String, String>>,
    selected: Pair<String, String>,
    onSelected: (Pair<String, String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        modifier = Modifier.width(260.dp)
    ) {
        Text(selected.first)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.width(260.dp)
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.first) },
                onClick = {
                    onSelected(option)
                    expanded = false
                }
            )
        }
    }
}


suspend fun fetchMeetings(year: Int): List<Pair<Int, String>> =
    withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.openf1.org/v1/meetings?year=$year").readText()
            val array = JSONArray(json)

            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                obj.getInt("meeting_key") to obj.getString("meeting_name")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

suspend fun fetchSessions(meetingKey: Int): List<Pair<Int, String>> =
    withContext(Dispatchers.IO) {
        try {
            val json =
                URL("https://api.openf1.org/v1/sessions?meeting_key=$meetingKey").readText()
            val array = JSONArray(json)

            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                obj.getInt("session_key") to obj.getString("session_name")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

suspend fun fetchDrivers(sessionKey: Int): List<Pair<Int, String>> =
    withContext(Dispatchers.IO) {
        try {
            val json =
                URL("https://api.openf1.org/v1/drivers?session_key=$sessionKey").readText()
            val array = JSONArray(json)

            (0 until array.length()).map {
                val obj = array.getJSONObject(it)
                obj.getInt("driver_number") to obj.getString("full_name")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

suspend fun fetchCarData(
    driverNumber: Int,
    sessionKey: Int,
    metricField: String
): List<Entry> =
    withContext(Dispatchers.IO) {
        val url =
            "https://api.openf1.org/v1/car_data?driver_number=$driverNumber&session_key=$sessionKey"

        try {
            println("DEBUG CAR_DATA URL = $url")

            val json = URL(url).readText()
            val array = JSONArray(json)
            println("DEBUG CAR_DATA COUNT = ${array.length()}")

            val entries = mutableListOf<Entry>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val value = obj.optDouble(metricField, Double.NaN)
                if (!value.isNaN()) {
                    entries.add(Entry(i.toFloat(), value.toFloat()))
                }
            }

            entries
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


@Composable
fun TelemetryChart(data: List<Entry>, label: String) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(data, label)
            dataSet.color = android.graphics.Color.RED
            dataSet.setDrawCircles(false)
            dataSet.lineWidth = 2f

            chart.data = LineData(dataSet)
            chart.invalidate()
            chart.setBackgroundColor("#1d1d1d".toColorInt())
            chart.axisLeft.textColor = android.graphics.Color.WHITE
            chart.axisLeft.axisLineColor = android.graphics.Color.WHITE
            chart.xAxis.textColor = android.graphics.Color.WHITE
            chart.xAxis.axisLineColor = android.graphics.Color.WHITE
            chart.axisRight.isEnabled = false
            chart.legend.textColor = android.graphics.Color.WHITE
            chart.xAxis.setDrawGridLines(false)

        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
