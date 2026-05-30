package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "macro_steps",
    foreignKeys = [
        ForeignKey(
            entity = MacroEntity::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macroId"])]
)
data class MacroStepEntity(
    @PrimaryKey val id: String,
    val macroId: String,
    val sequenceNumber: Int,
    val actionType: String, // "CLICK", "LONG_CLICK", "SCROLL_UP", "SCROLL_DOWN", "WAIT"
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f,
    val delayMs: Long = 500L,
    val textArgument: String? = null
)

@Entity(
    tableName = "phrase_rules",
    foreignKeys = [
        ForeignKey(
            entity = MacroEntity::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macroId"])]
)
data class PhraseRuleEntity(
    @PrimaryKey val id: String,
    val phrase: String,
    val matchingMode: String, // "EXACT", "CONTAINS", "REGEX"
    val macroId: String,
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
