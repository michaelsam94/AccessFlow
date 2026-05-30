package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.MacroEntity
import com.example.data.MacroRepository
import com.example.data.PhraseRuleRepository
import com.example.domain.usecase.EvaluatePhraseRulesUseCase
import com.example.domain.usecase.ExecuteMacroUseCase
import com.example.domain.usecase.GestureDispatcher
import com.example.domain.usecase.ParseWindowContentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class AccessFlowService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val TAG = "AccessFlowService"

    // Custom view lifecycle management for Compose inside dynamic window managers
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false

    private lateinit var macroRepository: MacroRepository
    private lateinit var phraseRuleRepository: PhraseRuleRepository

    private val parseWindowContentUseCase = ParseWindowContentUseCase()
    private val evaluatePhraseRulesUseCase = EvaluatePhraseRulesUseCase()
    private val executeMacroUseCase = ExecuteMacroUseCase()

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null

    // Concrete implementation of gesture dispatcher that invokes raw Accessibility gestures
    private val gestureDispatcher = object : GestureDispatcher {
        override fun click(xPercent: Float, yPercent: Float): Boolean {
            val displayMetrics = resources.displayMetrics
            val x = displayMetrics.widthPixels * xPercent
            val y = displayMetrics.heightPixels * yPercent
            return dispatchClickGesture(x, y)
        }

        override fun longClick(xPercent: Float, yPercent: Float): Boolean {
            val displayMetrics = resources.displayMetrics
            val x = displayMetrics.widthPixels * xPercent
            val y = displayMetrics.heightPixels * yPercent
            return dispatchLongClickGesture(x, y)
        }

        override fun scrollUp(): Boolean {
            val displayMetrics = resources.displayMetrics
            val startY = displayMetrics.heightPixels * 0.2f
            val endY = displayMetrics.heightPixels * 0.8f
            val centerX = displayMetrics.widthPixels * 0.5f
            return dispatchScrollGesture(centerX, startY, centerX, endY)
        }

        override fun scrollDown(): Boolean {
            val displayMetrics = resources.displayMetrics
            val startY = displayMetrics.heightPixels * 0.8f
            val endY = displayMetrics.heightPixels * 0.2f
            val centerX = displayMetrics.widthPixels * 0.5f
            return dispatchScrollGesture(centerX, startY, centerX, endY)
        }
    }

    companion object {
        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var activeInstance: AccessFlowService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        val db = AppDatabase.getDatabase(this)
        macroRepository = MacroRepository(db.macroDao())
        phraseRuleRepository = PhraseRuleRepository(db.phraseRuleDao())

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                ttsInitialized = true
                Log.d(TAG, "TTS Engine Initialized Connected.")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        activeInstance = this
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // Show floating overlay if permitted
        showOverlayWidget()
        speakText("Access Flow accessibility tracking configured successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Scan updates in window structures
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            serviceScope.launch {
                val rootNode = try {
                    rootInActiveWindow
                } catch (e: Exception) {
                    null
                } ?: return@launch

                val detectedTexts = parseWindowContentUseCase.execute(rootNode)
                val enabledRules = phraseRuleRepository.getEnabledRulesOneShot()

                val matchedRule = evaluatePhraseRulesUseCase.execute(enabledRules, detectedTexts)
                if (matchedRule != null) {
                    executeMacroById(matchedRule.macroId, "Matched automated keyword event: ${matchedRule.phrase}")
                }
            }
        }
    }

    override fun onInterrupt() {
        speakText("Access Flow was interrupted.")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        isServiceRunning = false
        activeInstance = null
        hideOverlayWidget()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try {
            tts.shutdown()
        } catch (e: Exception) {}
        serviceJob.cancel()
        return super.onUnbind(intent)
    }

    /**
     * Splits target text into clean sentences and enqueues split blocks into the TTS queue using QUEUE_ADD,
     * so parsing/pacing and pause mechanisms work reliably at natural sentence boundaries.
     */
    fun speakText(text: String) {
        if (!ttsInitialized) return
        // Split on literal text endpoints
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        for (sentence in sentences) {
            val sentenceSegment = sentence.trim()
            if (sentenceSegment.isNotEmpty()) {
                tts.speak(sentenceSegment, TextToSpeech.QUEUE_ADD, null, "sentence_id_${System.nanoTime()}")
            }
        }
    }

    /**
     * Dispatches macro trigger sequence with logs and feedback text
     */
    fun executeMacroById(macroId: String, reason: String = "Manual interaction") {
        serviceScope.launch {
            val macro = macroRepository.getMacroByIdOneShot(macroId)
            if (macro == null) {
                Log.e(TAG, "Macro key target not found: $macroId")
                return@launch
            }
            if (!macro.isEnabled) {
                Log.d(TAG, "Macro associated with rule is disabled: ${macro.name}")
                return@launch
            }

            val steps = macroRepository.getStepsForMacroOneShot(macroId)
            speakText("Launching Macro Automation ${macro.name}. $reason.")
            executeMacroUseCase.execute(steps, gestureDispatcher)
        }
    }

    fun isOverlayVisible(): Boolean = overlayView != null

    /**
     * Spawns floating macro triggering controller using accessibility overlays parameter set
     */
    fun showOverlayWidget() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay BIND permission missing, can't display panel.")
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = -100
        }

        val container = FrameLayout(this)
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AccessFlowService)
            setViewTreeViewModelStoreOwner(this@AccessFlowService)
            setViewTreeSavedStateRegistryOwner(this@AccessFlowService)
            setContent {
                OverlayContent(
                    onTriggerMacro = { macroId ->
                        executeMacroById(macroId, "Direct overlay tap")
                    },
                    macroRepository = macroRepository
                )
            }
        }

        container.addView(composeView)
        overlayView = container

        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed placing overlay content into WindowManager hierarchy", e)
        }
    }

    fun hideOverlayWidget() {
        overlayView?.let { ViewContainer ->
            try {
                windowManager?.removeView(ViewContainer)
            } catch (e: Exception) {}
            overlayView = null
        }
    }

    // Gesture Dispatch Implementation Mechanics
    private fun dispatchClickGesture(x: Float, y: Float): Boolean {
        val clickPath = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun dispatchLongClickGesture(x: Float, y: Float): Boolean {
        val clickPath = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 750)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun dispatchScrollGesture(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val scrollPath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(scrollPath, 0, 480)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }
}

@Stable
data class HotkeyGridItem(
    val id: String,
    val name: String,
    val isEnabled: Boolean
)

@Composable
fun OverlayContent(
    onTriggerMacro: (String) -> Unit,
    macroRepository: MacroRepository
) {
    var isExpanded by remember { mutableStateOf(false) }
    var macroItems by remember { mutableStateOf(emptyList<HotkeyGridItem>()) }

    LaunchedEffect(Unit) {
        macroRepository.allMacros.collect { list ->
            macroItems = list.map { HotkeyGridItem(it.id, it.name, it.isEnabled) }
        }
    }

    Surface(
        color = Color(0xFF1E1E2C), // Cosmic dark theme base tone
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(10.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .semantics { role = Role.Button }
    ) {
        if (!isExpanded) {
            // Trigger overlay circle widget (exceeds 72dp requirement for comfortable precision touch target size)
            IconButton(
                onClick = { isExpanded = true },
                modifier = Modifier
                    .size(80.dp) // Exceeding 72dp target
                    .testTag("overlay_trigger_btn")
                    .semantics { 
                        role = Role.Button
                    },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF8257E5) // Vibrant Cosmic Grape Violet Accent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Expand AccessFlow Hotkey palette",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // Expanded hotkey selections board
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AccessFlow",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier
                            .size(72.dp) // Minimum touch target 72dp
                            .testTag("overlay_close_btn")
                            .semantics { role = Role.Button }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse the panel list details",
                            tint = Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0x32FFFFFF))
                Spacer(modifier = Modifier.height(8.dp))

                val activeList = macroItems.filter { it.isEnabled }
                if (activeList.isEmpty()) {
                    Text(
                        text = "No active macros found.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeList.forEach { item ->
                            Button(
                                onClick = { onTriggerMacro(item.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp) // Minimum 72dp touch target as requested in section 10
                                    .testTag("hotkey_${item.id}")
                                    .semantics { role = Role.Button },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E244B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Launch ${item.name} macro controls",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
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
