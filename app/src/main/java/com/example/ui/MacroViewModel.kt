package com.example.ui

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MacroEntity
import com.example.data.MacroRepository
import com.example.data.MacroStepEntity
import com.example.data.PhraseRuleEntity
import com.example.data.PhraseRuleRepository
import com.example.service.AccessFlowService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

class MacroViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val macroRepository = MacroRepository(db.macroDao())
    private val phraseRuleRepository = PhraseRuleRepository(db.phraseRuleDao())

    val macros: StateFlow<List<MacroEntity>> = macroRepository.allMacros
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val rules: StateFlow<List<PhraseRuleEntity>> = phraseRuleRepository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isServiceActive = MutableStateFlow(false)
    val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    init {
        checkPermissions()
        // Pre-seed default templates if Room database is empty
        viewModelScope.launch {
            macroRepository.allMacros.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultTemplates()
                }
            }
        }
    }

    fun checkPermissions() {
        _isServiceActive.value = AccessFlowService.isServiceRunning
        val context = getApplication<Application>().applicationContext
        _isOverlayPermissionGranted.value = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        Log.d("MacroViewModel", "Check Permissions: ServiceRunning=${_isServiceActive.value}, OverlayEnabled=${_isOverlayPermissionGranted.value}")
    }

    private suspend fun seedDefaultTemplates() {
        val m1Id = UUID.randomUUID().toString()
        val m1 = MacroEntity(
            id = m1Id,
            name = "Scroll Down gesture",
            description = "Triggers a smooth vertical scroll down sweep",
            isEnabled = true,
            isFavorite = true
        )
        val m1Steps = listOf(
            MacroStepEntity(UUID.randomUUID().toString(), m1Id, 1, "SCROLL_DOWN", 0.5f, 0.8f, 1000L)
        )
        macroRepository.insertMacro(m1)
        macroRepository.insertSteps(m1Steps)

        val m2Id = UUID.randomUUID().toString()
        val m2 = MacroEntity(
            id = m2Id,
            name = "Double-Tap Action",
            description = "Simulates double clicks sequentially in core mid screen",
            isEnabled = true,
            isFavorite = false
        )
        val m2Steps = listOf(
            MacroStepEntity(UUID.randomUUID().toString(), m2Id, 1, "CLICK", 0.5f, 0.5f, 150L),
            MacroStepEntity(UUID.randomUUID().toString(), m2Id, 2, "CLICK", 0.5f, 0.5f, 400L)
        )
        macroRepository.insertMacro(m2)
        macroRepository.insertSteps(m2Steps)

        // Seed default priority-based Phrase Rule matching EXACT phrase "scroll"
        val rule1 = PhraseRuleEntity(
            id = UUID.randomUUID().toString(),
            phrase = "scroll screen",
            matchingMode = "CONTAINS",
            macroId = m1Id,
            isEnabled = true,
            priority = 5
        )
        phraseRuleRepository.insertRule(rule1)
    }

    fun addMacro(name: String, description: String, steps: List<MacroStepEntity>) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val macro = MacroEntity(
                id = newId,
                name = name,
                description = description,
                isEnabled = true
            )
            macroRepository.insertMacro(macro)
            val mappingSteps = steps.mapIndexed { idx, step ->
                step.copy(id = UUID.randomUUID().toString(), macroId = newId, sequenceNumber = idx + 1)
            }
            macroRepository.insertSteps(mappingSteps)
        }
    }

    fun deleteMacro(macro: MacroEntity) {
        viewModelScope.launch {
            macroRepository.deleteMacro(macro)
        }
    }

    fun toggleMacroStatus(macro: MacroEntity) {
        viewModelScope.launch {
            macroRepository.insertMacro(macro.copy(isEnabled = !macro.isEnabled))
        }
    }

    fun toggleMacroFavorite(macro: MacroEntity) {
        viewModelScope.launch {
            macroRepository.insertMacro(macro.copy(isFavorite = !macro.isFavorite))
        }
    }

    fun insertPhraseRule(phrase: String, matchingMode: String, macroId: String, priority: Int) {
        viewModelScope.launch {
            val rule = PhraseRuleEntity(
                id = UUID.randomUUID().toString(),
                phrase = phrase,
                matchingMode = matchingMode,
                macroId = macroId,
                priority = priority,
                isEnabled = true
            )
            phraseRuleRepository.insertRule(rule)
        }
    }

    fun deletePhraseRule(rule: PhraseRuleEntity) {
        viewModelScope.launch {
            phraseRuleRepository.deleteRule(rule)
        }
    }

    fun toggleRuleStatus(rule: PhraseRuleEntity) {
        viewModelScope.launch {
            phraseRuleRepository.updateRuleStatus(rule.id, !rule.isEnabled)
        }
    }

    fun simulateTtsSpeak(text: String) {
        AccessFlowService.activeInstance?.speakText(text)
    }

    fun runMacroDirectly(macroId: String) {
        AccessFlowService.activeInstance?.executeMacroById(macroId, "Manual execution via Dashboard Controls")
    }

    fun toggleActiveOverlayState() {
        val instance = AccessFlowService.activeInstance ?: return
        if (!instance.isOverlayVisible()) {
            instance.showOverlayWidget()
        } else {
            instance.hideOverlayWidget()
        }
    }
}
