package com.example.f1telemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Locale

class DriverComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            F1TelemetryTheme {
                    DriverComparisonScreen()
            }
        }
    }
}

data class MeetingInfo(
    val meetingKey: Int,
    val name: String,
    val location: String?,
    val country: String?
) {
    val displayName: String
        get() = buildString {
            append(name)
            val extra = location ?: country
            if (!extra.isNullOrBlank()) {
                append(" (")
                append(extra)
                append(")")
            }
        }
    }

data class SessionInfo(
    val sessionKey: Int,
    val rawName: String,
    val type: String?
) {
    val displayName: String
        get() {
            val n = rawName.lowercase()
            return when {
                "practice 1" in n -> "FP1"
                "practice 2" in n -> "FP2"
                "practice 3" in n -> "FP3"
                "qualifying" in n -> "Qualifying"
                "sprint" in n -> "Sprint"
                "race" in n -> "Race"
                else -> rawName
            }
        }
}

data class DriverInfo(
    val driverNumber: Int,
    val name: String,
    val code: String?,
    val team: String?,
    val headshotUrl: String?
)

data class LapStats(
    val lapCount: Int,
    val bestLap: Double?,
    val bestS1: Double?,
    val bestS2: Double?,
    val bestS3: Double?,
    val avgLap: Double?,
    val bestSpeedTrap: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverComparisonScreen() {
    val seasons = listOf("2023", "2024","2025")
    var selectedSeason by remember { mutableStateOf(seasons.last()) }
    var meetings by remember { mutableStateOf<List<MeetingInfo>>(emptyList()) }
    var selectedMeeting by remember { mutableStateOf<MeetingInfo?>(null) }
    var sessions by remember { mutableStateOf<List<SessionInfo>>(emptyList()) }
    var selectedSession by remember { mutableStateOf<SessionInfo?>(null) }

    var drivers by remember { mutableStateOf<List<DriverInfo>>(emptyList()) }
    var selectedDriverAName by remember { mutableStateOf<String?>(null) }
    var selectedDriverBName by remember { mutableStateOf<String?>(null) }
    var driverAStats by remember { mutableStateOf<Pair<DriverInfo, LapStats>?>(null) }
    var driverBStats by remember { mutableStateOf<Pair<DriverInfo, LapStats>?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedSeason) {
        val year = selectedSeason.toIntOrNull() ?: 2025
        isLoading = true
        errorMessage = null
        meetings = emptyList()
        selectedMeeting = null
        sessions = emptyList()
        selectedSession = null
        drivers = emptyList()
        selectedDriverAName = null
        selectedDriverBName = null
        driverAStats = null
        driverBStats = null

        try {
            meetings = getRaceMeetings(year)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load race weekends."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedMeeting?.meetingKey) {
        val meeting = selectedMeeting ?: return@LaunchedEffect
        isLoading = true
        errorMessage = null
        sessions = emptyList()
        selectedSession = null
        drivers = emptyList()
        selectedDriverAName = null
        selectedDriverBName = null
        driverAStats = null
        driverBStats = null

        try {
            sessions = getSessionsForMeeting(meeting.meetingKey)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load sessions."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSession?.sessionKey) {
        val session = selectedSession ?: return@LaunchedEffect
        isLoading = true
        errorMessage = null
        drivers = emptyList()
        selectedDriverAName = null
        selectedDriverBName = null
        driverAStats = null
        driverBStats = null

        try {
            drivers = getDriversForSession(session.sessionKey)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load drivers."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Driver Comparison",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        DropdownBox(
            label = "Season",
            options = seasons,
            selected = selectedSeason,
            onSelectedChange = { selectedSeason = it },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        DropdownBox(
            label = "Race Weekend",
            options = meetings.map { it.displayName },
            selected = selectedMeeting?.displayName,
            onSelectedChange = { label ->
                selectedMeeting = meetings.find { it.displayName == label }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        DropdownBox(
            label = "Session (FP1, Quali, Race...)",
            options = sessions.map { it.displayName },
            selected = selectedSession?.displayName,
            onSelectedChange = { label ->
                selectedSession = sessions.find { it.displayName == label }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (drivers.isNotEmpty()) {
            DropdownBox(
                label = "Driver A",
                options = drivers.map { it.name },
                selected = selectedDriverAName,
                onSelectedChange = { selectedDriverAName = it },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DropdownBox(
                label = "Driver B",
                options = drivers.map { it.name },
                selected = selectedDriverBName,
                onSelectedChange = { selectedDriverBName = it },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val session = selectedSession
                    if (session == null) {
                        errorMessage = "Please select a session."
                        return@Button
                    }

                    if (selectedDriverAName == null || selectedDriverBName == null) {
                        errorMessage = "Please select two drivers."
                        return@Button
                    }

                    if (selectedDriverAName == selectedDriverBName) {
                        errorMessage = "Please select two different drivers."
                        return@Button
                    }

                    val driverA = drivers.find { it.name == selectedDriverAName }
                    val driverB = drivers.find { it.name == selectedDriverBName }

                    if (driverA == null || driverB == null) {
                        errorMessage = "Could not find selected drivers."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val statsA = getLapStats(session.sessionKey, driverA.driverNumber)
                            val statsB = getLapStats(session.sessionKey, driverB.driverNumber)
                            driverAStats = driverA to statsA
                            driverBStats = driverB to statsB
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Failed to load lap stats."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text("Compare")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
        }

        driverAStats?.let { (info, stats) ->
            DriverStatsCard(title = "Driver A", driver = info, stats = stats)
            Spacer(modifier = Modifier.height(20.dp))
        }

        driverBStats?.let { (info, stats) ->
            DriverStatsCard(title = "Driver B", driver = info, stats = stats)
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (driverAStats == null && driverBStats == null && !isLoading) {
            Text(
                text = "Pick a season, race weekend, session and two drivers to compare.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownBox(
    label: String,
    options: List<String>,
    selected: String?,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelectedChange(option)
                    }
                )
            }
        }
    }
}

@Composable
fun DriverStatsCard(
    title: String,
    driver: DriverInfo,
    stats: LapStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = driver.headshotUrl,
                    contentDescription = driver.name,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(driver.name, fontWeight = FontWeight.Bold)
                    Text("No. ${driver.driverNumber} ${driver.code?.let { "• $it" } ?: ""}")
                    Text(driver.team ?: "Unknown Team")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(12.dp))

            StatsRow("Laps completed", stats.lapCount.toString())
            StatsRow("Best lap", formatSeconds(stats.bestLap))
            StatsRow("Average lap", formatSeconds(stats.avgLap))
            StatsRow("Best Sector 1", formatSeconds(stats.bestS1))
            StatsRow("Best Sector 2", formatSeconds(stats.bestS2))
            StatsRow("Best Sector 3", formatSeconds(stats.bestS3))
            StatsRow("Best speed trap",stats.bestSpeedTrap?.let { "$it km/h" } ?: "N/A"
            )
        }
    }
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}


suspend fun getRaceMeetings(year: Int): List<MeetingInfo> = withContext(Dispatchers.IO) {
    val url =
        "https://api.openf1.org/v1/meetings?year=$year"

    val text = URL(url).readText()
    val arr = JSONArray(text)
    val list = mutableListOf<MeetingInfo>()

    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val meetingKey = o.optInt("meeting_key", -1)
        if (meetingKey <= 0) continue

        list.add(
            MeetingInfo(meetingKey = meetingKey,
                name = o.optString("meeting_name"),
                location = o.optString("location", null),
                country = o.optString("country_name", null)
            )
        )
    }
    list.sortedBy { it.name }
}

suspend fun getSessionsForMeeting(meetingKey: Int): List<SessionInfo> =
    withContext(Dispatchers.IO) {
        val url =
            "https://api.openf1.org/v1/sessions?meeting_key=$meetingKey"
        val text = URL(url).readText()
        val arr = JSONArray(text)
        val list = mutableListOf<SessionInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sessionKey = o.optInt("session_key", -1)
            if (sessionKey <= 0) continue

            list.add(
                SessionInfo(
                    sessionKey = sessionKey,
                    rawName = o.optString("session_name"),
                    type = o.optString("session_type", null)
                    )
                )
        }
        list
    }

suspend fun getDriversForSession(sessionKey: Int): List<DriverInfo> =
    withContext(Dispatchers.IO) {
        val url =
            "https://api.openf1.org/v1/drivers?session_key=$sessionKey"

        val text = URL(url).readText()
        val arr = JSONArray(text)
        val list = mutableListOf<DriverInfo>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val number = o.optInt("driver_number", -1)
            if (number <= 0) continue
            val fullName = o.optString("full_name")
                .ifBlank { o.optString("broadcast_name") }
                .ifBlank {
                    val first = o.optString("first_name")
                    val last = o.optString("last_name")
                    listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
                }

            list.add(
                DriverInfo(
                    driverNumber = number,
                    name = fullName,
                    code = o.optString("name_acronym", null),
                    team = o.optString("team_name", null),
                    headshotUrl = o.optString("headshot_url", null)
                  )
                )
            }

        list.sortedBy { it.name }
    }

suspend fun getLapStats(sessionKey: Int, driverNumber: Int): LapStats =
    withContext(Dispatchers.IO) {
        val url =
            "https://api.openf1.org/v1/laps?session_key=$sessionKey&driver_number=$driverNumber"

        val text = URL(url).readText()
        val arr = JSONArray(text)

        if (arr.length() == 0) {
            return@withContext LapStats(
                lapCount = 0,
                bestLap = null,
                bestS1 = null,
                bestS2 = null,
                bestS3 = null,
                avgLap = null,
                bestSpeedTrap = null
            )
        }
        var lapCount = 0
        var totalLapDuration = 0.0
        var bestLap = Double.POSITIVE_INFINITY
        var bestS1 = Double.POSITIVE_INFINITY
        var bestS2 = Double.POSITIVE_INFINITY
        var bestS3 = Double.POSITIVE_INFINITY
        var bestSpeedTrap: Int? = null

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val lapDuration = o.optDouble("lap_duration", Double.NaN)
            if (!lapDuration.isNaN() && lapDuration > 0.0) {
                lapCount++
                totalLapDuration += lapDuration
                if (lapDuration < bestLap) bestLap = lapDuration
            }
            val s1 = o.optDouble("duration_sector_1", Double.NaN)
            if (!s1.isNaN() && s1 > 0.0 && s1 < bestS1) bestS1 = s1

            val s2 = o.optDouble("duration_sector_2", Double.NaN)
            if (!s2.isNaN() && s2 > 0.0 && s2 < bestS2) bestS2 = s2

            val s3 = o.optDouble("duration_sector_3", Double.NaN)
            if (!s3.isNaN() && s3 > 0.0 && s3 < bestS3) bestS3 = s3

            val stSpeed = o.optInt("st_speed", -1)
            if (stSpeed > 0) {
                if (bestSpeedTrap == null || stSpeed > bestSpeedTrap) {
                    bestSpeedTrap = stSpeed
                }
            }
        }

        val avgLap = if (lapCount > 0) totalLapDuration / lapCount else null
        LapStats(
            lapCount = lapCount,
            bestLap = if (bestLap.isInfinite()) null else bestLap,
            bestS1 = if (bestS1.isInfinite()) null else bestS1,
            bestS2 = if (bestS2.isInfinite()) null else bestS2,
            bestS3 = if (bestS3.isInfinite()) null else bestS3,
            avgLap = avgLap,
            bestSpeedTrap = bestSpeedTrap
        )
    }
fun formatSeconds(value: Double?): String {
    return if (value == null) {
        "N/A"
    } else {
        String.format(Locale.US, "%.3f s", value)
    }
}
