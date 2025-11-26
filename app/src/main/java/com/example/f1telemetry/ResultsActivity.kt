package com.example.f1telemetry

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.*

class ResultsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            F1TelemetryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ResultsScreen()
                }
            }
        }
    }
}

data class SessionResultItem(
    val position: Int,
    val driverNumber: Int,
    val name: String,
    val team: String?,
    val duration: Double?,
    val gap: Double?,
    val laps: Int,
    val status: String,
    val headshotUrl: String?
)

data class SimpleSessionInfo(
    val sessionKey: Int,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen() {

    val seasons = listOf("2023", "2024","2025")
    var selectedSeason by remember { mutableStateOf("2025") }
    var meetings by remember { mutableStateOf<List<MeetingInfo>>(emptyList()) }
    var selectedMeeting by remember { mutableStateOf<MeetingInfo?>(null) }
    var sessions by remember { mutableStateOf<List<SimpleSessionInfo>>(emptyList()) }
    var selectedSession by remember { mutableStateOf<SimpleSessionInfo?>(null) }
    var results by remember { mutableStateOf<List<SessionResultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedSeason) {
        isLoading = true
        error = null
        selectedMeeting = null
        selectedSession = null
        results = emptyList()
        try {
            meetings = getRaceWeekends(selectedSeason.toInt())
        } catch (e: Exception) {
            error = "Failed to load race weekends."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedMeeting) {
        val meeting = selectedMeeting ?: return@LaunchedEffect
        isLoading = true
        error = null
        selectedSession = null
        results = emptyList()
        try {
            sessions = getSessionsForMeetings(meeting.meetingKey)
        } catch (e: Exception) {
            error = "Failed to load sessions."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSession) {
        val session = selectedSession ?: return@LaunchedEffect
        isLoading = true
        error = null
        results = emptyList()

        scope.launch {
            try {
                results = getSessionResults(session.sessionKey)
            } catch (e: Exception) {
                error = "Failed to load results."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Session Results", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        DropdownBox(
            label = "Season",
            options = seasons,
            selected = selectedSeason,
            onSelectedChange = { selectedSeason = it },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(Modifier.height(8.dp))

        DropdownBox(
            label = "Race Weekend",
            options = meetings.map { it.displayName },
            selected = selectedMeeting?.displayName,
            onSelectedChange = { name ->
                selectedMeeting = meetings.find { it.displayName == name }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(Modifier.height(8.dp))

        DropdownBox(
            label = "Session",
            options = sessions.map { it.name },
            selected = selectedSession?.name,
            onSelectedChange = { label ->
                selectedSession = sessions.find { it.name == label }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        if (isLoading) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return
        }
        LazyColumn {
            items(results) { item ->
                ResultsRow(item)
            }
        }
     }
}

@Composable
fun ResultsRow(item: SessionResultItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        AsyncImage(
            model = item.headshotUrl,
            contentDescription = item.name,
            modifier = Modifier
                .size(100.dp)
                .padding(end =12.dp)
        )

        Column(Modifier.padding(12.dp)) {

            Text("P${item.position}: ${item.name}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            Text("Team: ${item.team ?: "N/A"}")
            Text("Laps: ${item.laps}")
            Text("Gap: ${item.gap?.let { "${it}s" } ?: "—"}")
            Text("Status: ${item.status}")
            }
        }
}

suspend fun getRaceWeekends(year: Int): List<MeetingInfo> =
    withContext(Dispatchers.IO) {
        val arr = JSONArray(URL("https://api.openf1.org/v1/meetings?year=$year").readText())
        val list = mutableListOf<MeetingInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                MeetingInfo(
                    meetingKey = o.getInt("meeting_key"),
                    name = o.getString("meeting_name"),
                    location = o.optString("location"),
                    country = o.optString("country_name")
                )
            )
        }
        list.sortedBy { it.meetingKey }
    }

suspend fun getSessionsForMeetings(meetingKey: Int): List<SimpleSessionInfo> =
    withContext(Dispatchers.IO) {
        val arr = JSONArray(URL("https://api.openf1.org/v1/sessions?meeting_key=$meetingKey").readText())
        val list = mutableListOf<SimpleSessionInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val rawName = o.optString("session_name").lowercase()

            val name = when {
                "practice 1" in rawName -> "FP1"
                "practice 2" in rawName -> "FP2"
                "practice 3" in rawName -> "FP3"
                "qualifying" in rawName -> "Qualifying"
                "sprint" in rawName -> "Sprint"
                "race" in rawName -> "Race"
                else -> o.optString("session_name")
                }
            list.add(
                SimpleSessionInfo(
                    sessionKey = o.getInt("session_key"),
                    name = name
                    )
                )
        }
        list
    }

suspend fun getSessionResults(sessionKey: Int): List<SessionResultItem> =
    withContext(Dispatchers.IO) {

        try {
            val resultsUrl = "https://api.openf1.org/v1/session_result?session_key=$sessionKey"
            val resultsText = URL(resultsUrl).readText()
            val resultsArr = JSONArray(resultsText)

            if (resultsArr.length() == 0) {
                return@withContext emptyList()
            }

            val driversUrl = "https://api.openf1.org/v1/drivers?session_key=$sessionKey"
            val driversText = URL(driversUrl).readText()
            val driversArr = JSONArray(driversText)

            val driverMap = mutableMapOf<Int, DriverInfo>()

            for (i in 0 until driversArr.length()) {
                val o = driversArr.getJSONObject(i)
                driverMap[o.getInt("driver_number")] = DriverInfo(
                    driverNumber = o.getInt("driver_number"),
                    name = o.getString("full_name"),
                    code = o.optString("name_acronym"),
                    team = o.optString("team_name"),
                    headshotUrl = o.optString("headshot_url")
                )
            }

            val list = mutableListOf<SessionResultItem>()

            for (i in 0 until resultsArr.length()) {
                val o = resultsArr.getJSONObject(i)
                val dn = o.getInt("driver_number")
                val driver = driverMap[dn] ?: continue

                val status = when {
                    o.optBoolean("dsq") -> "DSQ"
                    o.optBoolean("dns") -> "DNS"
                    o.optBoolean("dnf") -> "DNF"
                    else -> "Finished"
                }

                list.add(
                    SessionResultItem(
                        position = o.optInt("position"),
                        driverNumber = dn,
                        name = driver.name,
                        team = driver.team,
                        headshotUrl = driver.headshotUrl,
                        duration = o.optDouble("duration", -1.0),
                        gap = o.optDouble("gap_to_leader", -1.0),
                        laps = o.optInt("number_of_laps", 0),
                        status = status
                        )
                    )
            }

            list.sortedBy { if (it.position == 0) 999 else it.position }
        }
        catch (e: Exception) {
            Log.e("OpenF1", "Error fetching session results: ${e.message}")
            return@withContext emptyList()
                }
    }

fun formatTime(seconds: Double?): String {
    if (seconds == null || seconds <= 0) return "—"
    val mins = (seconds / 60).toInt()
    val secs = seconds - mins * 60
    return if (mins > 0) "%d:%06.3f".format(mins, secs) else "%.3f s".format(seconds)
}
