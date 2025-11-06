package com.example.upload10.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_cards")
data class KnowledgeCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String
)