package com.example.upload10.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [KnowledgeCard::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeCardDao(): KnowledgeCardDao
}