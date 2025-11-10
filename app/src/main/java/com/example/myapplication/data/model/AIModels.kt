package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

// AI处理请求
data class AIProcessRequest(
    @SerializedName("content")
    val content: String,

    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("structure_type")
    val structureType: String = "knowledge_cards"
)

// AI处理响应
data class AIProcessResponse(
    @SerializedName("file_tag")
    val fileTag: String,

    @SerializedName("sections")
    val sections: List<AISection>
)

data class AISection(
    @SerializedName("section_title")
    val sectionTitle: String,

    @SerializedName("items")
    val items: List<AIItem>
)

data class AIItem(
    @SerializedName("item_id")
    val itemId: String,

    @SerializedName("keyword")
    val keyword: String,

    @SerializedName("explanation")
    val explanation: String
)