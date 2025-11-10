package com.example.myapplication.ai.validation

import com.example.myapplication.data.KnowledgeCard

/**
 * 内容验证器
 */
interface ContentValidator {
    fun validateCards(cards: List<KnowledgeCard>): ValidationResult
    fun calculateQualityScore(cards: List<KnowledgeCard>): Double
    fun deduplicateCards(cards: List<KnowledgeCard>): List<KnowledgeCard>
}

class DefaultContentValidator : ContentValidator {

    override fun validateCards(cards: List<KnowledgeCard>): ValidationResult {
        val validCards = mutableListOf<KnowledgeCard>()
        val invalidCards = mutableListOf<InvalidCard>()

        cards.forEach { card ->
            val issues = validateSingleCard(card)
            if (issues.isEmpty()) {
                validCards.add(card)
            } else {
                invalidCards.add(InvalidCard(card, issues))
            }
        }

        return ValidationResult(validCards, invalidCards)
    }

    override fun calculateQualityScore(cards: List<KnowledgeCard>): Double {
        if (cards.isEmpty()) return 0.0

        var totalScore = 0.0
        cards.forEach { card ->
            totalScore += calculateCardScore(card)
        }

        return totalScore / cards.size
    }

    override fun deduplicateCards(cards: List<KnowledgeCard>): List<KnowledgeCard> {
        return cards.distinctBy { card ->
            "${card.keyword}-${card.explanation.take(50)}"
        }
    }

    private fun validateSingleCard(card: KnowledgeCard): List<String> {
        val issues = mutableListOf<String>()

        if (card.keyword.isBlank()) issues.add("关键词为空")
        if (card.keyword.length > 50) issues.add("关键词过长")
        if (card.explanation.length < 10) issues.add("解释过短")
        if (card.explanation.length > 500) issues.add("解释过长")
        if (card.keyword == card.explanation) issues.add("关键词与解释重复")

        return issues
    }

    private fun calculateCardScore(card: KnowledgeCard): Double {
        var score = 0.0

        // 关键词质量（0-40分）
        score += when {
            card.keyword.length in 2..5 -> 40.0
            card.keyword.length in 6..10 -> 30.0
            else -> 20.0
        }

        // 解释质量（0-40分）
        score += when {
            card.explanation.length in 50..200 -> 40.0
            card.explanation.length in 30..49 -> 30.0
            card.explanation.length in 201..300 -> 30.0
            else -> 20.0
        }

        // 内容独特性（0-20分）
        score += if (card.keyword != card.explanation.take(20)) 20.0 else 10.0

        return score / 100.0
    }
}

data class ValidationResult(
    val validCards: List<KnowledgeCard>,
    val invalidCards: List<InvalidCard>
)

data class InvalidCard(
    val card: KnowledgeCard,
    val issues: List<String>
)