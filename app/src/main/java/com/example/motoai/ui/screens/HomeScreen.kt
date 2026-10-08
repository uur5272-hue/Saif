package com.example.motoai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.motoai.ui.AppScreen
import com.example.motoai.ui.MainViewModel
import com.example.motoai.ui.components.FuturisticAiOrb
import com.example.motoai.ui.components.StatusIndicatorBar
import com.example.ui.theme.MotoBackground
import com.example.ui.theme.MotoBorder
import com.example.ui.theme.MotoCyan
import com.example.ui.theme.MotoGreen
import com.example.ui.theme.MotoSurface
import com.example.ui.theme.MotoSurfaceElevated
import com.example.ui.theme.MotoTextMuted
import com.example.ui.theme.MotoTextPrimary
import com.example.ui.theme.MotoTextSecondary
import com.example.ui.theme.MotoViolet
import com.example.motoai.voice.MicState

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val micState by viewModel.micState.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val queryInput by viewModel.queryInput.collectAsState()
    val latestResponse by viewModel.latestResponse.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()

    // Permission launcher for speech recognition
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val onMicClicked = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (micState == MicState.LISTENING) {
            viewModel.stopListening()
        } else {
            if (hasPermission) {
                viewModel.startListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MotoBackground)
    ) {
        // Futuristic Cyber Background
        Image(
            painter = painterResource(id = R.drawable.ic_moto_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        // Dark gradient scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MotoBackground.copy(alpha = 0.85f),
                            MotoBackground.copy(alpha = 0.70f),
                            MotoBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_moto_logo),
                            contentDescription = "MOTO AI Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MotoCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MOTO AI",
                                color = MotoTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Your Voice. Your AI. Your MOTO.",
                                color = MotoCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Settings Shortcut
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MotoSurfaceElevated.copy(alpha = 0.8f))
                            .border(1.dp, MotoBorder, CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MotoCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Status bar
            item {
                StatusIndicatorBar(
                    micState = micState,
                    isThinking = isThinking,
                    isSpeaking = isSpeaking,
                    errorMessage = errorMessage,
                    onCancelSpeaking = { viewModel.cancelVoice() },
                    onDismissError = { viewModel.clearError() }
                )
            }

            // Animated AI Orb Centerpiece
            item {
                Spacer(modifier = Modifier.height(10.dp))
                FuturisticAiOrb(
                    micState = micState,
                    soundLevel = soundLevel,
                    isThinking = isThinking,
                    isSpeaking = isSpeaking,
                    onClick = onMicClicked
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (micState == MicState.LISTENING) "Listening to your voice..." else "Tap Orb or Mic to Command",
                    color = if (micState == MicState.LISTENING) MotoGreen else MotoTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // AI Response Area Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MotoSurface.copy(alpha = 0.9f))
                        .border(1.dp, MotoBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                        .testTag("ai_response_area")
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MotoCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MOTO INTELLIGENCE",
                                color = MotoCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = latestResponse,
                            color = MotoTextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Quick Feature Shortcuts Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Image Gen shortcut
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.IMAGE_GEN) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("shortcut_image_gen"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoSurfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MotoBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MotoCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate Image",
                            color = MotoTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Video Gen shortcut
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.VIDEO_GEN) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("shortcut_video_gen"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoSurfaceElevated
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MotoBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MotoViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Video",
                            color = MotoTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Voice Command Suggester Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TRY SAYING:",
                        color = MotoTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prompts = listOf(
                            "“MOTO, open YouTube”",
                            "“MOTO, search for Free Fire”",
                            "“MOTO, what is the weather?”",
                            "“MOTO, open Chrome”",
                            "“MOTO, call Rahul”",
                            "“MOTO, generate an image”",
                            "“MOTO, open Settings”"
                        )
                        items(prompts) { p ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MotoSurfaceElevated.copy(alpha = 0.5f))
                                    .border(1.dp, Color(0xFF202C44), RoundedCornerShape(12.dp))
                                    .clickable {
                                        val clean = p.replace("“", "").replace("”", "")
                                        viewModel.processUserCommand(clean)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = p,
                                    color = MotoTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Text Input & Send Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MotoSurface.copy(alpha = 0.95f))
                        .border(1.dp, MotoBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { viewModel.setQueryInput(it) },
                        placeholder = {
                            Text(
                                "Type command or query...",
                                color = MotoTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MotoTextPrimary,
                            unfocusedTextColor = MotoTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.submitCurrentTextQuery() })
                    )

                    // Mic toggle
                    IconButton(
                        onClick = onMicClicked,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (micState == MicState.LISTENING) MotoGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .testTag("microphone_button")
                    ) {
                        Icon(
                            imageVector = if (micState == MicState.LISTENING) Icons.Default.Mic else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (micState == MicState.LISTENING) MotoGreen else MotoCyan
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = { viewModel.submitCurrentTextQuery() },
                        enabled = queryInput.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (queryInput.isNotBlank()) MotoCyan else Color.Transparent)
                            .testTag("submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (queryInput.isNotBlank()) Color(0xFF090D16) else MotoTextMuted
                        )
                    }
                }
            }

            // Recent Commands History Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MotoTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECENT ACTIVITY",
                                color = MotoTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (commandHistory.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                color = MotoCyan,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { viewModel.clearLocalHistory() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (commandHistory.isEmpty()) {
                        Text(
                            text = "No recent commands yet. Try saying 'MOTO, open YouTube'.",
                            color = MotoTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        commandHistory.take(5).forEach { history ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MotoSurface.copy(alpha = 0.6f))
                                    .border(1.dp, Color(0xFF1B263B), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.processUserCommand(history.query) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = history.query,
                                            color = MotoTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = history.response,
                                            color = MotoTextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MotoCyan.copy(alpha = 0.1f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = history.intent,
                                            color = MotoCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
