package com.sublearn.platform

import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.sublearn.domain.TranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/** Official ML Kit translation. Model files are managed by ML Kit and never copied into the app. */
class MlKitTranslationProvider : TranslationProvider {
    override val providerId: String = "mlkit"

    override suspend fun isModelReady(sourceLanguage: String, targetLanguage: String): Boolean = withContext(Dispatchers.IO) {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage) ?: return@withContext false
        val target = TranslateLanguage.fromLanguageTag(targetLanguage) ?: return@withContext false
        val manager = RemoteModelManager.getInstance()
        manager.isModelDownloaded(TranslateRemoteModel.Builder(source).build()).await() &&
            manager.isModelDownloaded(TranslateRemoteModel.Builder(target).build()).await()
    }

    override suspend fun downloadModel(sourceLanguage: String, targetLanguage: String) {
        val translator = createTranslator(sourceLanguage, targetLanguage)
        try {
            translator.downloadModelIfNeeded().await()
        } finally {
            translator.close()
        }
    }

    override suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String {
        if (text.isBlank()) return text
        val translator = createTranslator(sourceLanguage, targetLanguage)
        return try {
            translator.translate(text).await()
        } finally {
            translator.close()
        }
    }

    private fun createTranslator(sourceLanguage: String, targetLanguage: String): Translator {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage)
            ?: throw IllegalArgumentException("ML Kit does not support source language $sourceLanguage")
        val target = TranslateLanguage.fromLanguageTag(targetLanguage)
            ?: throw IllegalArgumentException("ML Kit does not support target language $targetLanguage")
        return Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build(),
        )
    }
}
