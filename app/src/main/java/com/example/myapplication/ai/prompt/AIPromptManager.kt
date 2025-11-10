package com.example.myapplication.ai.prompt

/**
 * AI提示词管理器 - 修复版本
 */
class AIPromptManager {

    /**
     * 构建AI提示词
     */
    fun buildPrompt(content: String, fileName: String, chunkIndex: Int): String {
        val template = selectTemplate(fileName)
        return replaceTemplatePlaceholders(template, content, fileName, chunkIndex)
    }

    /**
     * 根据文件名选择模板
     */
    private fun selectTemplate(fileName: String): String {
        return when {
            fileName.contains("数学", ignoreCase = true) -> MATH_TEMPLATE
            fileName.contains("英语", ignoreCase = true) -> ENGLISH_TEMPLATE
            fileName.contains("历史", ignoreCase = true) -> HISTORY_TEMPLATE
            fileName.contains("物理", ignoreCase = true) -> PHYSICS_TEMPLATE
            fileName.contains("化学", ignoreCase = true) -> CHEMISTRY_TEMPLATE
            else -> DEFAULT_TEMPLATE
        }
    }

    /**
     * 替换模板占位符 - 使用简单的字符串替换
     */
    private fun replaceTemplatePlaceholders(
        template: String,
        content: String,
        fileName: String,
        chunkIndex: Int
    ): String {
        val processedContent = if (content.length > 3800) {
            content.substring(0, 3800) + "...[内容过长已截断]"
        } else {
            content
        }

        val startId = chunkIndex * 100 + 1

        return template
            .replace("{content}", processedContent)
            .replace("{fileName}", fileName)
            .replace("{chunkIndex}", chunkIndex.toString())
            .replace("{startId}", startId.toString())
    }

    companion object {
        // 模板使用简单的 {placeholder} 格式
        private const val DEFAULT_TEMPLATE = """
请将以下学习内容结构化提取为知识卡片：

文件：{fileName}
当前块：第{chunkIndex}部分
起始编号：{startId}

要求：
1. 识别主要知识点（概念、定义、原理）
2. 为每个知识点提取关键词（2-5个词）
3. 提供简洁准确的解释（50-200字）
4. 按逻辑顺序编号

内容：
{content}

请以JSON格式回复，确保结构清晰。
"""

        private const val MATH_TEMPLATE = """
请从以下数学内容中提取公式、定理和解题方法：

文件：{fileName}
当前块：第{chunkIndex}部分

要求：
1. 提取数学公式和定理
2. 标注适用场景和条件
3. 提供简单示例
4. 编号从{startId}开始

内容：
{content}
"""

        private const val ENGLISH_TEMPLATE = """
请从以下英语内容中提取词汇、语法和表达方式：

文件：{fileName}
当前块：第{chunkIndex}部分

要求：
1. 提取重要词汇和短语
2. 标注词性和用法
3. 提供例句和翻译
4. 编号从{startId}开始

内容：
{content}
"""

        private const val HISTORY_TEMPLATE = """
请从以下历史内容中提取事件、人物和时间线：

文件：{fileName}
当前块：第{chunkIndex}部分

要求：
1. 提取历史事件和人物
2. 标注时间和地点
3. 提供背景和影响
4. 编号从{startId}开始

内容：
{content}
"""

        private const val PHYSICS_TEMPLATE = """
请从以下物理内容中提取定律、公式和实验方法：

文件：{fileName}
当前块：第{chunkIndex}部分

要求：
1. 提取物理定律和公式
2. 标注单位和量纲
3. 提供应用场景
4. 编号从{startId}开始

内容：
{content}
"""

        private const val CHEMISTRY_TEMPLATE = """
请从以下化学内容中提取反应、元素和实验方法：

文件：{fileName}
当前块：第{chunkIndex}部分

要求：
1. 提取化学反应和元素性质
2. 标注反应条件和产物
3. 提供实验步骤
4. 编号从{startId}开始

内容：
{content}
"""
    }
}