package com.example.myapplication.ai.cost
import com.example.myapplication.data.KnowledgeCard
/**
 * Token估算和成本控制
 */
class TokenEstimator {

    fun estimateTokens(text: String): Int {
        // 简单估算：中文字符算1.5个token，英文字符算0.25个token
        val chineseChars = text.count { it in '\u4e00'..'\u9fff' }
        val otherChars = text.length - chineseChars

        return (chineseChars * 1.5 + otherChars * 0.25).toInt()
    }

    fun calculateCost(tokens: Int, model: String = "gpt-3.5-turbo"): Double {
        val costPerToken = when (model) {
            "gpt-3.5-turbo" -> 0.000002 // $0.002 per 1K tokens
            "gpt-4" -> 0.00003 // $0.03 per 1K tokens
            else -> 0.000002
        }
        return tokens * costPerToken
    }
}

class RateLimiter(private val requestsPerMinute: Int = 60) {
    private val requestTimestamps = mutableListOf<Long>()

    suspend fun acquire() {
        while (true) {
            cleanupOldRequests()
            if (requestTimestamps.size < requestsPerMinute) {
                requestTimestamps.add(System.currentTimeMillis())
                return
            }
            kotlinx.coroutines.delay(1000) // 等待1秒后重试
        }
    }

    private fun cleanupOldRequests() {
        val oneMinuteAgo = System.currentTimeMillis() - 60000
        requestTimestamps.removeAll { it < oneMinuteAgo }
    }
}

class ProcessingCache {
    private val cache = mutableMapOf<String, List<KnowledgeCard>>()

    fun getCachedResult(contentHash: String): List<KnowledgeCard>? {
        return cache[contentHash]
    }

    fun cacheResult(contentHash: String, cards: List<KnowledgeCard>) {
        cache[contentHash] = cards
        // 可添加缓存清理逻辑
    }
}