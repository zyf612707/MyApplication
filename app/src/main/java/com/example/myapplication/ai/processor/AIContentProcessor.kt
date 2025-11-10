package com.example.myapplication.ai.processor

import com.example.myapplication.data.KnowledgeCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.myapplication.ai.validation.ContentValidator
import com.example.myapplication.data.model.ProcessingResult
import com.example.myapplication.ai.validation.DefaultContentValidator
import com.example.myapplication.ai.cost.ProcessingCache

/**
 * AI内容处理主入口
 */
class AIContentProcessor(
    private val preprocessor: ContentPreprocessor = ContentPreprocessor(),
    private val chunkStrategy: ChunkStrategy = SmartChunkStrategy(),
    private val validator: ContentValidator = DefaultContentValidator(),
    private val cache: ProcessingCache = ProcessingCache()
) {

    suspend fun processContent(
        content: String,
        fileName: String,
        enableValidation: Boolean = true
    ): Flow<ProcessingResult> = flow {
        try {
            emit(ProcessingResult.Processing("开始处理文件: $fileName"))

            // 1. 检查缓存
            val cachedResult = cache.getCachedResult(content.hashCode().toString())
            if (cachedResult != null) {
                emit(ProcessingResult.Success(cachedResult, true))
                return@flow
            }

            // 2. 预处理
            emit(ProcessingResult.Processing("预处理内容..."))
            val processedContent = preprocessor.preprocess(content, fileName)

            // 3. 智能分块
            emit(ProcessingResult.Processing("分析内容结构..."))
            val chunks = chunkStrategy.chunkContent(processedContent, 2000)
            emit(ProcessingResult.Processing("将内容分为${chunks.size}个块进行处理"))

            // 4. 分批处理
            val allCards = mutableListOf<KnowledgeCard>()
            chunks.forEachIndexed { index, chunk ->
                emit(ProcessingResult.Processing("处理第${index + 1}/${chunks.size}个块..."))

                val chunkCards = processSingleChunk(chunk, fileName, index)
                if (enableValidation) {
                    val validatedCards = validator.validateCards(chunkCards)
                    allCards.addAll(validatedCards.validCards)

                    if (validatedCards.invalidCards.isNotEmpty()) {
                        emit(ProcessingResult.PartialSuccess(
                            message = "第${index + 1}块有${validatedCards.invalidCards.size}张卡片需要优化"
                        ))
                    }
                } else {
                    allCards.addAll(chunkCards)
                }
            }

            // 5. 后处理
            val finalCards = validator.deduplicateCards(allCards)
            cache.cacheResult(content.hashCode().toString(), finalCards)

            emit(ProcessingResult.Success(
                cards = finalCards,
                fromCache = false,
                qualityScore = validator.calculateQualityScore(finalCards)
            ))

        } catch (e: Exception) {
            emit(ProcessingResult.Error("处理失败: ${e.message}"))
        }
    }

    private suspend fun processSingleChunk(
        chunk: String,
        fileName: String,
        chunkIndex: Int
    ): List<KnowledgeCard> {
        // 这里调用实际的AI API
        // 暂时使用模拟处理
        return simulateAIProcessing(chunk, fileName, chunkIndex)
    }

    private fun simulateAIProcessing(content: String, fileName: String, chunkIndex: Int): List<KnowledgeCard> {
        // 模拟AI处理逻辑
        val lines = content.split("\n").filter { it.isNotBlank() }
        return lines.mapIndexed { index, line ->
            KnowledgeCard(
                title = "知识点 ${chunkIndex * 100 + index + 1}",
                content = line,
                fileTag = "AI处理",
                section = "第${chunkIndex + 1}部分",
                itemId = "${chunkIndex * 100 + index + 1}",
                keyword = line.take(20),
                explanation = line,
                sourceFile = fileName
            )
        }
    }
}