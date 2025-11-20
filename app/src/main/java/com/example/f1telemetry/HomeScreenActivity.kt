package com.example.f1telemetry

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeScreenActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(true) {
                database.get().addOnSuccessListener { snapshot ->
                    val first = snapshot.child("firstName").value.toString()
                    firstName = first
                }
            }

            Text("Welcome, $firstName")

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
