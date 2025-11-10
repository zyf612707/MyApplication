package com.example.myapplication.data.model

import com.example.myapplication.data.KnowledgeCard

/**
 * 处理结果密封类
 */
sealed class ProcessingResult {
    data class Processing(val message: String) : ProcessingResult()
    data class Success(
        val cards: List<KnowledgeCard>,
        val fromCache: Boolean,
        val qualityScore: Double = 0.0
    ) : ProcessingResult()

    data class PartialSuccess(val message: String) : ProcessingResult()
    data class Error(val message: String) : ProcessingResult()
}