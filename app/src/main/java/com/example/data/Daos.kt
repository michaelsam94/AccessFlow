package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllMacros(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    fun getMacroById(id: String): Flow<MacroEntity?>

    @Query("SELECT * FROM macros WHERE id = :id LIMIT 1")
    suspend fun getMacroByIdOneShot(id: String): MacroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity)

    @Delete
    suspend fun deleteMacro(macro: MacroEntity)

    // For Macro Steps
    @Query("SELECT * FROM macro_steps WHERE macroId = :macroId ORDER BY sequenceNumber ASC")
    fun getStepsForMacro(macroId: String): Flow<List<MacroStepEntity>>

    @Query("SELECT * FROM macro_steps WHERE macroId = :macroId ORDER BY sequenceNumber ASC")
    suspend fun getStepsForMacroOneShot(macroId: String): List<MacroStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: MacroStepEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<MacroStepEntity>)

    @Query("DELETE FROM macro_steps WHERE macroId = :macroId")
    suspend fun deleteStepsForMacro(macroId: String)
}

@Dao
interface PhraseRuleDao {
    @Query("SELECT * FROM phrase_rules ORDER BY priority DESC, createdAt DESC")
    fun getAllRules(): Flow<List<PhraseRuleEntity>>

    @Query("SELECT * FROM phrase_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRulesOneShot(): List<PhraseRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: PhraseRuleEntity)

    @Delete
    suspend fun deleteRule(rule: PhraseRuleEntity)

    @Query("UPDATE phrase_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateRuleStatus(id: String, isEnabled: Boolean)
}
