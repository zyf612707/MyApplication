package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<KnowledgeCard>)

    // 新增查询方法
    @Query("SELECT * FROM knowledge_cards ORDER BY processed_time DESC")
    fun getAllCards(): Flow<List<KnowledgeCard>>

    @Query("SELECT * FROM knowledge_cards WHERE source_file = :fileName ORDER BY item_id")
    fun getCardsByFile(fileName: String): Flow<List<KnowledgeCard>>

    @Query("SELECT DISTINCT source_file FROM knowledge_cards")
    fun getProcessedFiles(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM knowledge_cards WHERE source_file = :fileName")
    suspend fun getCardCountByFile(fileName: String): Int
}