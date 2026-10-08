package com.sublearn.feature.player

import com.sublearn.domain.Cue

internal enum class TranslationProblem { MODEL_MISSING, FAILED }

internal data class LookupUiState(
    val selectedText: String,
    val contextCue: Cue,
    val sourceLanguageTag: String = "en",
    val targetLanguageTag: String = "fa",
    val translation: String = "",
    val loading: Boolean = true,
    val problem: TranslationProblem? = null,
    val saved: Boolean = false,
)
