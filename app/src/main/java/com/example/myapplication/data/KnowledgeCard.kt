package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "knowledge_cards")
data class KnowledgeCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,

    // 新增AI处理字段
    @ColumnInfo(name = "file_tag")
    val fileTag: String = "",           // 文件标签

    @ColumnInfo(name = "section")
    val section: String = "",           // 二级章节

    @ColumnInfo(name = "item_id")
    val itemId: String = "",           // 编号

    @ColumnInfo(name = "keyword")
    val keyword: String = "",          // 关键词（卡片正面）

    @ColumnInfo(name = "explanation")
    val explanation: String = "",      // 详细释义（卡片背面）

    @ColumnInfo(name = "source_file")
    val sourceFile: String = "",       // 源文件名

    @ColumnInfo(name = "processed_time")
    val processedTime: Long = System.currentTimeMillis()  // 处理时间
)