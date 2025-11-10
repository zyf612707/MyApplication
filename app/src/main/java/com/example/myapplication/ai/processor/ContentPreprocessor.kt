package com.example.myapplication.ai.processor

/**
 * 内容预处理
 */
class ContentPreprocessor {

    fun preprocess(content: String, fileName: String): String {
        return content
            .normalizeSpacing()
            .removeExcessiveNewlines()
            .cleanFormatting()
            .addFileNameContext(fileName)
    }

    private fun String.normalizeSpacing(): String {
        return this.replace("\\s+".toRegex(), " ")
    }

    private fun String.removeExcessiveNewlines(): String {
        return this.replace("\n{3,}".toRegex(), "\n\n")
    }

    private fun String.cleanFormatting(): String {
        return this.replace("【.*?】".toRegex(), "") // 移除方括号标注
            .replace("\\*\\*".toRegex(), "") // 移除粗体标记
            .trim()
    }

    private fun String.addFileNameContext(fileName: String): String {
        val fileContext = when {
            fileName.contains("数学", ignoreCase = true) -> "数学知识点"
            fileName.contains("英语", ignoreCase = true) -> "英语词汇"
            fileName.contains("历史", ignoreCase = true) -> "历史事件"
            else -> "学习内容"
        }
        return "[$fileContext]\n$this"
    }
}