package com.example.domain.usecase

import com.example.data.PhraseRuleEntity

class EvaluatePhraseRulesUseCase {
    /**
     * Checks if any given enabled rules match the list of text blocks retrieved from screen.
     * Evaluates in priorities order (highest priority rule matched first).
     * Modes: EXACT, CONTAINS, REGEX.
     */
    fun execute(rules: List<PhraseRuleEntity>, detectedTexts: List<String>): PhraseRuleEntity? {
        if (rules.isEmpty() || detectedTexts.isEmpty()) return null
        
        // Sort by priority descending (priority ordering as requested)
        val sortedRules = rules.sortedByDescending { it.priority }
        
        for (rule in sortedRules) {
            val targetPhrase = rule.phrase.trim()
            if (targetPhrase.isEmpty()) continue
            
            for (screenText in detectedTexts) {
                val isMatch = when (rule.matchingMode.uppercase()) {
                    "EXACT" -> screenText.equals(targetPhrase, ignoreCase = true)
                    "CONTAINS" -> screenText.contains(targetPhrase, ignoreCase = true)
                    "REGEX" -> {
                        try {
                            val regex = Regex(targetPhrase, RegexOption.IGNORE_CASE)
                            regex.containsMatchIn(screenText)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    else -> false
                }
                
                if (isMatch) {
                    return rule
                }
            }
        }
        return null
    }
}
