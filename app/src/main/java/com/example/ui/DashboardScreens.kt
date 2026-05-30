package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MacroEntity
import com.example.data.MacroStepEntity
import com.example.data.PhraseRuleEntity

// Styling constants for cosmic elegance
val SpaceDarkBg = Color(0xFF0F0F1A)
val SpaceCardBg = Color(0xFF1B1B2F)
val NeonPurple = Color(0xFF9E00FF)
val NeonTeal = Color(0xFF00FFCC)
val MutedSlate = Color(0xFF7F7F9F)

@Composable
fun OnboardingScreen(
    viewModel: MacroViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.checkPermissions()
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SpaceDarkBg, Color(0xFF1E1035))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "AccessFlow Brand logo icon",
                    tint = NeonTeal,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Welcome to AccessFlow",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A modern gesture and speech automated accessibility assistant. Enable systems permissions below to launch utility overlays.",
                    fontSize = 14.sp,
                    color = MutedSlate,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 20.sp
                )
            }

            // Permissions Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Permission Card 1: Accessibility Tracker
                PermissionCard(
                    title = "Accessibility Tracking Service",
                    description = "Required to scan words displayed on your active window layout screens and simulate clicks/scrolls.",
                    isActive = isServiceActive,
                    onEnableClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                            Toast.makeText(context, "Locate 'AccessFlow' in list and activate it.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to locate Accessibility settings.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    testTagSuffix = "accessibility"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Card 2: Draw Overlays
                PermissionCard(
                    title = "System Alert Helper Overlays",
                    description = "Required to spawn the floating Hotkey triggers buttons palette panel on top of other utilities apps.",
                    isActive = isOverlayActive,
                    onEnableClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "System alert window setting not available.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    testTagSuffix = "overlay"
                )
            }

            // CTA Bottom
            Button(
                onClick = {
                    if (isServiceActive) {
                        onComplete()
                    } else {
                        Toast.makeText(context, "Please configure AccessFlow service to proceed.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("onboarding_continue_btn")
                    .semantics { 
                        role = Role.Button 
                        contentDescription = "Continue into the dashboard screen"
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceActive) NeonPurple else Color(0x339E00FF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isServiceActive) "Enter Dashboard" else "Service Activation Required",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isActive: Boolean,
    onEnableClick: () -> Unit,
    testTagSuffix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isActive) NeonTeal else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MutedSlate,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0x3200FFCC) else NeonPurple
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .testTag("grant_permission_$testTagSuffix")
                    .semantics { role = Role.Button }
            ) {
                Text(
                    text = if (isActive) "Enabled" else "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MacrosListScreen(
    viewModel: MacroViewModel,
    highlightMacroId: String? = null
) {
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(highlightMacroId) {
        if (!highlightMacroId.isNullOrEmpty()) {
            Log.d("MacrosListScreen", "Highlighting specific macro ID from deeplink: $highlightMacroId")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonPurple,
                contentColor = Color.White,
                modifier = Modifier
                    .testTag("create_macro_fab")
                    .semantics { 
                        role = Role.Button
                        contentDescription = "Create a new custom gesture automation Macro"
                    }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        },
        containerColor = SpaceDarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (macros.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Empty macros configuration status helper illustration description",
                        tint = MutedSlate,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No Automation Macros Yet",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Customize automated sequences using clicks/scrolls, then assign voice or phrase keywords matching tags to activate them hands-free.",
                        color = MutedSlate,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Macro Pipelines",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Keyed list traversal matching section 11 requirements
                    items(macros, key = { it.id }) { macro ->
                        val isHighlighted = macro.id == highlightMacroId
                        MacroItemRow(
                            macro = macro,
                            onToggleActive = { viewModel.toggleMacroStatus(macro) },
                            onToggleFavorite = { viewModel.toggleMacroFavorite(macro) },
                            onDelete = { viewModel.deleteMacro(macro) },
                            onExecute = { viewModel.runMacroDirectly(macro.id) },
                            isHighlighted = isHighlighted
                        )
                    }
                }
            }

            if (showCreateDialog) {
                CreateMacroWizard(
                    onDismiss = { showCreateDialog = false },
                    onSave = { name, desc, steps ->
                        viewModel.addMacro(name, desc, steps)
                        showCreateDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun MacroItemRow(
    macro: MacroEntity,
    onToggleActive: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onExecute: () -> Unit,
    isHighlighted: Boolean
) {
    var recordingStateText by remember { mutableStateOf("Ready") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) Modifier.background(
                    Brush.horizontalGradient(listOf(NeonTeal, Color.Transparent)),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF26194A) else SpaceCardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = macro.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHighlighted) NeonTeal else Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite macro button indicator",
                                tint = if (macro.isFavorite) Color.Yellow else MutedSlate
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = macro.description,
                        fontSize = 12.sp,
                        color = MutedSlate
                    )
                }

                // Switch status control
                Switch(
                    checked = macro.isEnabled,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonTeal,
                        checkedTrackColor = NeonPurple
                    ),
                    modifier = Modifier
                        .testTag("toggle_macro_${macro.id}")
                        .semantics {
                            stateDescription = if (macro.isEnabled) "Enabled" else "Disabled"
                        }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x22FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Testing Run CTA
                Button(
                    onClick = onExecute,
                    enabled = macro.isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D2D44),
                        disabledContainerColor = Color(0x11FFFFFF)
                    ),
                    modifier = Modifier
                        .testTag("execute_${macro.id}")
                        .semantics { role = Role.Button }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play icon trigger",
                        tint = if (macro.isEnabled) NeonTeal else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Flow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (macro.isEnabled) Color.White else Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (recordingStateText == "Ready") {
                                recordingStateText = "Recording active"
                            } else {
                                recordingStateText = "Ready"
                            }
                        },
                        modifier = Modifier
                            .testTag("record_macro_${macro.id}")
                            .semantics { 
                                stateDescription = recordingStateText
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Macro recording toggles status control button",
                            tint = if (recordingStateText == "Recording active") Color.Red else MutedSlate
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .testTag("delete_${macro.id}")
                            .semantics { 
                                role = Role.Button
                                contentDescription = "Delete this Macro completely"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5555)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhraseRulesScreen(
    viewModel: MacroViewModel
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (macros.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = NeonPurple,
                    contentColor = Color.White,
                    modifier = Modifier
                        .testTag("create_rule_fab")
                        .semantics { 
                            role = Role.Button
                            contentDescription = "Create a new phrase-matching trigger Rule"
                        }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }
        },
        containerColor = SpaceDarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (rules.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Phrase trigger empty placeholder visual screen reader indicator",
                        tint = MutedSlate,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No Text-Matching Rules",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect automated macros to specific text strings detected on your active window (EXACT strings, CONTAINS words, or REGEX pattern matches).",
                        color = MutedSlate,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    if (macros.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please record or create at least one Macro first before building trigger rules.",
                            color = NeonTeal,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Speech & Parsing Triggers",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(rules, key = { it.id }) { rule ->
                        val linkedMacroName = macros.firstOrNull { it.id == rule.macroId }?.name ?: "Unknown Pipeline"
                        RuleItemCard(
                            rule = rule,
                            macroName = linkedMacroName,
                            onToggleRule = { viewModel.toggleRuleStatus(rule) },
                            onDelete = { viewModel.deletePhraseRule(rule) }
                        )
                    }
                }
            }

            if (showCreateDialog) {
                CreateRuleDialog(
                    macros = macros,
                    onDismiss = { showCreateDialog = false },
                    onSave = { phrase, mode, macroId, priority ->
                        viewModel.insertPhraseRule(phrase, mode, macroId, priority)
                        showCreateDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun RuleItemCard(
    rule: PhraseRuleEntity,
    macroName: String,
    onToggleRule: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\"${rule.phrase}\"",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(rule.matchingMode, fontSize = 10.sp, color = Color.White) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF2D2D44))
                        )
                        Text(
                            text = "Priority: ${rule.priority}",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggleRule() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonTeal,
                        checkedTrackColor = NeonPurple
                    ),
                    modifier = Modifier.testTag("toggle_rule_${rule.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x18FFFFFF))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Triggers: $macroName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .testTag("delete_rule_${rule.id}")
                        .semantics { role = Role.Button }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete prompt phrase rule button",
                        tint = Color(0xFFFF5555),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MacroViewModel
) {
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isOverlayActive by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()

    var testMessage by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        viewModel.checkPermissions()
        onDispose {}
    }

    Scaffold(
        containerColor = SpaceDarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "System Diagnostics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Hardware & Engine Status Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Permission Matrix",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        StatusLabel(
                            label = "Accessibility Tracker Services Engine",
                            status = if (isServiceActive) "Active Connection" else "Offline (Action Required)",
                            active = isServiceActive,
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        StatusLabel(
                            label = "Floating Canvas Widget Canvas",
                            status = if (isOverlayActive) "Permitted draw" else "Disabled",
                            active = isOverlayActive,
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }

            // Interactive TTS Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Speech Playback Auditing (TTS)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Test our advanced sentence splitter mechanism. Enqueued lines read and output vocal segments comfortably at natural system boundaries.",
                            fontSize = 12.sp,
                            color = MutedSlate
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = testMessage,
                            onValueChange = { testMessage = it },
                            placeholder = { Text("Type sentences split by punctuation to check queuing...", fontSize = 13.sp, color = MutedSlate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tts_text_input")
                                .background(Color(0xFF131326), RoundedCornerShape(8.dp)),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonTeal,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (testMessage.text.isNotBlank()) {
                                    viewModel.simulateTtsSpeak(testMessage.text)
                                    Toast.makeText(context, "Speaking chunks via engine...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Input diagnostic speech text first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isServiceActive,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_tts_btn")
                                .semantics { role = Role.Button }
                        ) {
                            Text("Speak Diagnostic Sentences", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Controller Overlays toggles
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Floating Controls Overlay Launcher",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toggle the hotkey launch palette on screen. Note: Overlay permission must be granted.",
                            fontSize = 12.sp,
                            color = MutedSlate
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.toggleActiveOverlayState()
                            },
                            enabled = isServiceActive && isOverlayActive,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("toggle_overlay_status_btn")
                                .semantics { role = Role.Button }
                        ) {
                            Text("Toggle Floating Bubble On-Screen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusLabel(
    label: String,
    status: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = Color.White)
            Text(status, fontSize = 11.sp, color = if (active) NeonTeal else Color(0xFFFF5555))
        }
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Launch systemic permission settings panel icon",
            tint = MutedSlate,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun CreateRuleDialog(
    macros: List<MacroEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var matchingMode by remember { mutableStateOf("CONTAINS") }
    var macroId by remember { mutableStateOf(macros.firstOrNull()?.id ?: "") }
    var priorityString by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Matching Rule Trigger", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Key Word/Phrase Pattern") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_phrase_input"),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Matching Mode Selection
                Text("Matching Evaluation Logic", fontSize = 12.sp, color = MutedSlate)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXACT", "CONTAINS", "REGEX").forEach { mode ->
                        val isSel = matchingMode == mode
                        Button(
                            onClick = { matchingMode = mode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) NeonPurple else Color(0xFF2D2D44)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(mode, fontSize = 10.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = priorityString,
                    onValueChange = { priorityString = it },
                    label = { Text("Priority Level Rank (Int)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rule_priority_input"),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                // Select Trigger Pipeline
                Text("Triggers Macro pipeline", fontSize = 12.sp, color = MutedSlate)
                macros.forEach { macro ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { macroId = macro.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = macroId == macro.id, onClick = { macroId = macro.id })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(macro.name, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priority = priorityString.toIntOrNull() ?: 0
                    if (phrase.trim().isNotEmpty() && macroId.isNotEmpty()) {
                        onSave(phrase, matchingMode, macroId, priority)
                    }
                },
                modifier = Modifier.testTag("save_rule_btn")
            ) {
                Text("Save Trigger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_rule_btn")) {
                Text("Cancel")
            }
        },
        containerColor = SpaceCardBg
    )
}

@Composable
fun CreateMacroWizard(
    onDismiss: () -> Unit,
    onSave: (String, String, List<MacroStepEntity>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val currentSteps = remember { mutableStateListOf<MacroStepEntity>() }

    var actionType by remember { mutableStateOf("CLICK") }
    var xPctStr by remember { mutableStateOf("0.5") }
    var yPctStr by remember { mutableStateOf("0.5") }
    var delayStr by remember { mutableStateOf("800") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Automation pipeline", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Pipeline Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_name_input"),
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Pipeline Function Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("macro_desc_input"),
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal)
                    )
                }

                item {
                    HorizontalDivider(color = Color(0x32FFFFFF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Sequence Stack (${currentSteps.size})", fontSize = 13.sp, color = NeonTeal, fontWeight = FontWeight.Bold)
                }

                if (currentSteps.isEmpty()) {
                    item {
                        Text("No steps added yet. Model standard workflows below.", color = MutedSlate, fontSize = 11.sp)
                    }
                } else {
                    items(currentSteps) { step ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Step: ${step.actionType} (x:${step.xPercent}, y:${step.yPercent}) delay: ${step.delayMs}ms",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete step button",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { currentSteps.remove(step) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Build Step Settings", fontSize = 12.sp, color = MutedSlate)
                    
                    // Action types
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("CLICK", "LONG_CLICK", "SCROLL_UP", "SCROLL_DOWN", "WAIT").forEach { act ->
                            val isSel = actionType == act
                            Button(
                                onClick = { actionType = act },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) NeonPurple else Color(0xFF1B1B2F)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            ) {
                                Text(act, fontSize = 8.sp, maxLines = 1)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = xPctStr,
                            onValueChange = { xPctStr = it },
                            label = { Text("X-ratio", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("step_x_input"),
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal)
                        )
                        OutlinedTextField(
                            value = yPctStr,
                            onValueChange = { yPctStr = it },
                            label = { Text("Y-ratio", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("step_y_input"),
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal)
                        )
                        OutlinedTextField(
                            value = delayStr,
                            onValueChange = { delayStr = it },
                            label = { Text("Delay (Ms)", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("step_delay_input"),
                            textStyle = TextStyle(color = Color.White, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonTeal)
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            val x = xPctStr.toFloatOrNull() ?: 0.5f
                            val y = yPctStr.toFloatOrNull() ?: 0.5f
                            val delay = delayStr.toLongOrNull() ?: 500L
                            
                            currentSteps.add(
                                MacroStepEntity(
                                    id = "",
                                    macroId = "",
                                    sequenceNumber = currentSteps.size + 1,
                                    actionType = actionType,
                                    xPercent = x,
                                    yPercent = y,
                                    delayMs = delay
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3200FFCC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("add_step_to_sequence_btn")
                    ) {
                        Text("Add Step Component", fontSize = 12.sp, color = NeonTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && currentSteps.isNotEmpty()) {
                        onSave(name, description, currentSteps.toList())
                    }
                },
                modifier = Modifier.testTag("save_macro_btn")
            ) {
                Text("Compile Pipeline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_macro_btn")) {
                Text("Cancel")
            }
        },
        containerColor = SpaceCardBg
    )
}
