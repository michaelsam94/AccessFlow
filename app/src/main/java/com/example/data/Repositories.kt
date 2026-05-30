package com.example.data

import kotlinx.coroutines.flow.Flow

class MacroRepository(private val macroDao: MacroDao) {
    val allMacros: Flow<List<MacroEntity>> = macroDao.getAllMacros()

    fun getMacroById(id: String): Flow<MacroEntity?> = macroDao.getMacroById(id)

    suspend fun getMacroByIdOneShot(id: String): MacroEntity? = macroDao.getMacroByIdOneShot(id)

    suspend fun insertMacro(macro: MacroEntity) = macroDao.insertMacro(macro)

    suspend fun deleteMacro(macro: MacroEntity) = macroDao.deleteMacro(macro)

    fun getStepsForMacro(macroId: String): Flow<List<MacroStepEntity>> = macroDao.getStepsForMacro(macroId)

    suspend fun getStepsForMacroOneShot(macroId: String): List<MacroStepEntity> = macroDao.getStepsForMacroOneShot(macroId)

    suspend fun insertStep(step: MacroStepEntity) = macroDao.insertStep(step)

    suspend fun insertSteps(steps: List<MacroStepEntity>) = macroDao.insertSteps(steps)

    suspend fun deleteStepsForMacro(macroId: String) = macroDao.deleteStepsForMacro(macroId)
}

class PhraseRuleRepository(private val phraseRuleDao: PhraseRuleDao) {
    val allRules: Flow<List<PhraseRuleEntity>> = phraseRuleDao.getAllRules()

    suspend fun getEnabledRulesOneShot(): List<PhraseRuleEntity> = phraseRuleDao.getEnabledRulesOneShot()

    suspend fun insertRule(rule: PhraseRuleEntity) = phraseRuleDao.insertRule(rule)

    suspend fun deleteRule(rule: PhraseRuleEntity) = phraseRuleDao.deleteRule(rule)

    suspend fun updateRuleStatus(id: String, isEnabled: Boolean) = phraseRuleDao.updateRuleStatus(id, isEnabled)
}
