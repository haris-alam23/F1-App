package com.example.f1telemetry

import android.R
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeScreenActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            F1TelemetryTheme {
                HomeScreen()

            }
        }
    }

    @Composable
    fun HomeScreen() {

        val user = firebaseAuth.currentUser
        val userId = user?.uid
        var database = FirebaseDatabase.getInstance().getReference("users/$userId")
        var firstName by remember { mutableStateOf("Guest") }



        LaunchedEffect(true) {
            database.get().addOnSuccessListener { snapshot ->
                val first = snapshot.child("firstName").value.toString()
                firstName = first
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)

        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF8d0000),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Text(
                        "Welcome, $firstName",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Your Personalized F1 Analytics Hub",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                FeatureCard("Telemetry", "View real-time telemetry data",) {
                    startActivity(Intent(this@HomeScreenActivity, TelemetryActivity::class.java))

                }

                Spacer(Modifier.height(16.dp))

                FeatureCard(
                    title = "Driver Comparison",
                    description = "Compare two drivers side-by-side"
                ) {
                    startActivity(
                        Intent(
                            this@HomeScreenActivity,
                            DriverComparisonActivity::class.java
                        )
                    )

                }

                Spacer(Modifier.height(16.dp))

                FeatureCard(
                    title = "Race Results",
                    description = "Browse races, practice, quali, and race data"
                ) {
                    startActivity(Intent(this@HomeScreenActivity, ResultsActivity::class.java))

                }

                Spacer(Modifier.height(16.dp))

                Button(onClick = {
                    firebaseAuth.signOut()
                    startActivity(Intent(this@HomeScreenActivity, LoginActivity::class.java))
                    finish()
                }) {
                    Text("Logout")
                }

            }
        }

    }

    @Composable
    fun FeatureCard(title: String, description: String, onClick: () -> Unit) {
        ElevatedCard(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
            ),
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors= listOf(
                                Color(0xFF8d0000),
                                Color(0xFF5A0A0A),
                                Color(0xFF1d1d1d)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFFFFF)

                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFFFFF)
                )
            }
        }

        }
    }
}
