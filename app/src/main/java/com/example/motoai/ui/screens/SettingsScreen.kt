package com.example.motoai.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.motoai.ui.AppScreen
import com.example.motoai.ui.MainViewModel
import com.example.ui.theme.MotoBackground
import com.example.ui.theme.MotoBorder
import com.example.ui.theme.MotoCyan
import com.example.ui.theme.MotoGreen
import com.example.ui.theme.MotoRed
import com.example.ui.theme.MotoSurface
import com.example.ui.theme.MotoSurfaceElevated
import com.example.ui.theme.MotoTextMuted
import com.example.ui.theme.MotoTextPrimary
import com.example.ui.theme.MotoTextSecondary
import com.example.ui.theme.MotoViolet

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val prefs = viewModel.securePrefs

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var currentStoredKey by remember { mutableStateOf(prefs.getGeminiApiKey()) }

    var selectedImageProvider by remember { mutableStateOf(prefs.getImageProvider()) }
    var selectedVideoProvider by remember { mutableStateOf(prefs.getVideoProvider()) }

    var selectedVoiceLanguage by remember { mutableStateOf(prefs.getVoiceLanguage()) }
    var voicePitch by remember { mutableFloatStateOf(prefs.getVoicePitch()) }
    var voiceSpeed by remember { mutableFloatStateOf(prefs.getVoiceSpeed()) }

    var wakeWordEnabled by remember { mutableStateOf(prefs.isWakeWordEnabled()) }
    var confirmSensitiveActions by remember { mutableStateOf(prefs.isConfirmSensitiveActions()) }
    var uiLanguage by remember { mutableStateOf(prefs.getUiLanguage()) }

    val maskedKey = remember(currentStoredKey) {
        if (currentStoredKey.length > 8) {
            "${currentStoredKey.take(4)}••••••••${currentStoredKey.takeLast(4)}"
        } else if (currentStoredKey.isNotEmpty()) {
            "••••••••••••"
        } else {
            "Not configured (Using local intelligence fallback)"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MotoBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MotoSurfaceElevated)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MotoCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MOTO AI Control Center",
                        color = MotoTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. Gemini API Configuration (Keystore Protected)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MotoCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI API CONFIGURATION",
                                color = MotoCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Current Key (Android Keystore Encrypted):",
                            color = MotoTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = maskedKey,
                            color = if (currentStoredKey.isNotEmpty()) MotoGreen else MotoTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = { Text("Paste new Gemini API key here...", color = MotoTextMuted) },
                            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                    Icon(
                                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = MotoCyan
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotoCyan,
                                unfocusedBorderColor = Color(0xFF253350),
                                focusedTextColor = MotoTextPrimary,
                                unfocusedTextColor = MotoTextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        prefs.saveGeminiApiKey(apiKeyInput)
                                        currentStoredKey = prefs.getGeminiApiKey()
                                        apiKeyInput = ""
                                        Toast.makeText(context, "API Key saved securely into Android Keystore", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = apiKeyInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MotoCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("save_api_key_button")
                            ) {
                                Text("Save Key", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                            }

                            if (currentStoredKey.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        prefs.clearGeminiApiKey()
                                        currentStoredKey = ""
                                        Toast.makeText(context, "Cleared API Key", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MotoRed.copy(alpha = 0.2f)),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Clear", color = MotoRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keys are securely encrypted on-device with AES-GCM via Android Keystore. They are never sent to external servers or logged in plain text.",
                            color = MotoTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 2. Image & Video Providers
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "GENERATION PROVIDERS",
                            color = MotoCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Image Generation Engine:", color = MotoTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("gemini" to "Gemini Flash Image", "web_ai" to "Neural Diffusion").forEach { (id, name) ->
                                val selected = selectedImageProvider == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MotoCyan.copy(alpha = 0.2f) else MotoSurfaceElevated)
                                        .border(1.dp, if (selected) MotoCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedImageProvider = id
                                            prefs.saveImageProvider(id)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (selected) MotoCyan else MotoTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Video Generation Engine:", color = MotoTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("veo" to "Google Veo Fast", "custom_video" to "Neural Motion").forEach { (id, name) ->
                                val selected = selectedVideoProvider == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MotoViolet.copy(alpha = 0.2f) else MotoSurfaceElevated)
                                        .border(1.dp, if (selected) MotoViolet else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedVideoProvider = id
                                            prefs.saveVideoProvider(id)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (selected) MotoViolet else MotoTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Voice & Speech Recognition Settings
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MotoCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VOICE & SPEECH SYNTHESIS",
                                color = MotoCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Assistant Voice Language:", color = MotoTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "en-US" to "English (US)",
                                "en-IN" to "English (IN)",
                                "hi-IN" to "Hindi (हिन्दी)",
                                "ur-PK" to "Urdu (اردو)"
                            ).forEach { (lang, label) ->
                                val selected = selectedVoiceLanguage == lang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MotoCyan.copy(alpha = 0.2f) else MotoSurfaceElevated)
                                        .border(1.dp, if (selected) MotoCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedVoiceLanguage = lang
                                            prefs.saveVoiceLanguage(lang)
                                            viewModel.applyVoiceSettings()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) MotoCyan else MotoTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Voice Pitch: ${String.format("%.2f", voicePitch)}", color = MotoTextSecondary, fontSize = 12.sp)
                        Slider(
                            value = voicePitch,
                            onValueChange = {
                                voicePitch = it
                                prefs.saveVoicePitch(it)
                                viewModel.applyVoiceSettings()
                            },
                            valueRange = 0.7f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = MotoCyan, activeTrackColor = MotoCyan)
                        )

                        Text(text = "Voice Speed: ${String.format("%.2f", voiceSpeed)}", color = MotoTextSecondary, fontSize = 12.sp)
                        Slider(
                            value = voiceSpeed,
                            onValueChange = {
                                voiceSpeed = it
                                prefs.saveVoiceSpeed(it)
                                viewModel.applyVoiceSettings()
                            },
                            valueRange = 0.7f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = MotoCyan, activeTrackColor = MotoCyan)
                        )
                    }
                }
            }

            // 4. Security & Confirmation Preferences
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MotoCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SECURITY & ACTIONS",
                                color = MotoCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Wake Name “MOTO”", color = MotoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Responds to spoken wake commands", color = MotoTextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = wakeWordEnabled,
                                onCheckedChange = {
                                    wakeWordEnabled = it
                                    prefs.saveWakeWordEnabled(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = MotoCyan, checkedTrackColor = MotoCyan.copy(alpha = 0.3f))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Confirm Sensitive Actions", color = MotoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Show confirmation dialog before making phone calls or sending SMS", color = MotoTextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = confirmSensitiveActions,
                                onCheckedChange = {
                                    confirmSensitiveActions = it
                                    prefs.saveConfirmSensitiveActions(it)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = MotoCyan, checkedTrackColor = MotoCyan.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            // 5. Data Privacy & Local History
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MotoRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PRIVACY & LOCAL DATA",
                                color = MotoRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MOTO AI respects your privacy. Speech audio is never saved or recorded secretly. You can wipe all local sessions and cached previews anytime.",
                            color = MotoTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.clearLocalHistory()
                                Toast.makeText(context, "Local activity and generated cache cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MotoRed.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("clear_history_button")
                        ) {
                            Text("Clear Local History & Cache", color = MotoRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 6. About MOTO AI
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface)
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_moto_logo),
                            contentDescription = "MOTO Logo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MotoCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "MOTO AI v1.0",
                                color = MotoTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "“Your Voice. Your AI. Your MOTO.”",
                                color = MotoCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Powered by Google Gemini 3.5 & Veo architectures",
                                color = MotoTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
