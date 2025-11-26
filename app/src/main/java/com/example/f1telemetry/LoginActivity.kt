package com.example.f1telemetry

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.savedstate.serialization.saved
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.google.firebase.auth.FirebaseAuth
import java.util.prefs.Preferences
import kotlin.math.log

class LoginActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        firebaseAuth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        setContent {
            F1TelemetryTheme {
                LoginScreen(prefs)
            }
        }
    }
    @Composable
    fun LoginScreen(prefs: SharedPreferences) {
        val savedEmail = prefs.getString("email", "") ?: ""
        val savedPassword = prefs.getString("password", "") ?: ""
        val remember = prefs.getBoolean("remember", false)

        var email by remember { mutableStateOf(savedEmail) }
        var password by remember { mutableStateOf(savedPassword) }
        var rememberMe by remember {mutableStateOf(remember)}
        var error by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if(rememberMe){
                        prefs.edit().putString("email",it).apply()
                        }
                    },
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if(rememberMe){
                        prefs.edit().putString("password",it).apply()
                                    }
                                },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center

            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { checked ->
                        rememberMe = checked

                        prefs.edit().apply(){
                            putBoolean("remember", checked)

                            if(checked){
                                putString("email", email)
                                putString("password", password)
                            }else{
                                remove("email")
                                remove("password")
                            }
                            apply()
                        }

                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remember Me",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true

                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                startActivity(Intent(this@LoginActivity, HomeScreenActivity::class.java))
                                finish()
                            } else {
                                error = task.exception?.message ?: "Login failed."
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isLoading) Text(stringResource(R.string.login_button))
                else CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            error.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = {
                startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
            }) {
                Text(stringResource(R.string.go_to_signup))
            }
        }




    }
}

