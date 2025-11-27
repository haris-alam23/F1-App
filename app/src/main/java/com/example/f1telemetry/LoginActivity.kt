package com.example.f1telemetry

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.f1telemetry.ui.theme.F1Red
import com.example.f1telemetry.ui.theme.F1TelemetryTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.TextFieldDefaults



class LoginActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(R.drawable.f1_logo),
                contentDescription = null,
                modifier = Modifier.size(175.dp)
            )


            Text(
                text = "F1 Telemetry",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)

            )

            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.login_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.7f)
            )

            Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(0.15f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {


                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (rememberMe) {
                            prefs.edit().putString("email", it).apply()
                        }
                    },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color(0xFF444444),
                    )

                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (rememberMe) {
                            prefs.edit().putString("password", it).apply()
                        }
                    },
                    label = { Text(stringResource(R.string.password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color(0xFF444444),
                    )
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

                            prefs.edit().apply() {
                                putBoolean("remember", checked)

                                if (checked) {
                                    putString("email", email)
                                    putString("password", password)
                                } else {
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors= listOf(
                            Color(0xFF8d0000),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {


        }
    }
}

