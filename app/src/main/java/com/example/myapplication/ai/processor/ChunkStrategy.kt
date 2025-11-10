package com.example.myapplication.ai.processor

/**
 * 内容分块策略
 */
interface ChunkStrategy {
    fun chunkContent(content: String, maxChunkSize: Int): List<String>
}

class SmartChunkStrategy : ChunkStrategy {
    override fun chunkContent(content: String, maxChunkSize: Int): List<String> {
        return when {
            hasClearChapters(content) -> splitByChapters(content, maxChunkSize)
            hasParagraphs(content) -> splitByParagraphs(content, maxChunkSize)
            else -> splitByLength(content, maxChunkSize)
        }
    }

    private fun hasClearChapters(content: String): Boolean {
        val chapterPatterns = listOf(
            "第[一二三四五六七八九十]+章",
            "Chapter\\s+\\d+",
            "Section\\s+\\d+"
        )
        return chapterPatterns.any { pattern -> pattern.toRegex().containsMatchIn(content) }
    }

    private fun hasParagraphs(content: String): Boolean {
        return content.split("\n\n").size > 5
    }

    private fun splitByChapters(content: String, maxChunkSize: Int): List<String> {
        // 按章节分割的逻辑
        val chapters = mutableListOf<String>()
        // 实现章节分割...
        return chapters.ifEmpty { splitByLength(content, maxChunkSize) }
    }

    private fun splitByParagraphs(content: String, maxChunkSize: Int): List<String> {
        val paragraphs = content.split("\n\n")
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        paragraphs.forEach { paragraph ->
            if (currentChunk.length + paragraph.length > maxChunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
            }
            currentChunk.append(paragraph).append("\n\n")
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        return chunks
    }

    private fun splitByLength(content: String, maxChunkSize: Int): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < content.length) {
            var end = (start + maxChunkSize).coerceAtMost(content.length)

            // 尝试在句子边界分割
            if (end < content.length) {
                val lastPeriod = content.lastIndexOf('.', end)
                val lastNewline = content.lastIndexOf('\n', end)

                end = when {
                    lastNewline > start + maxChunkSize * 0.7 -> lastNewline
                    lastPeriod > start + maxChunkSize * 0.7 -> lastPeriod + 1
                    else -> end
                }
            }

            chunks.add(content.substring(start, end))
            start = end
        }

        return chunks
    }
}