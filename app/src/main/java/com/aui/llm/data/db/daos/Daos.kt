package com.aui.llm.data.db.daos
import androidx.room.*
import com.aui.llm.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun allConversations(): Flow<List<ConversationEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(c: ConversationEntity)
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY timestamp ASC")
    fun messages(cid: String): Flow<List<MessageEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :cid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentMessages(cid: String, limit: Int): List<MessageEntity>
    @Insert
    suspend fun insertMessage(m: MessageEntity): Long
    @Query("DELETE FROM messages WHERE conversationId = :cid")
    suspend fun clearMessages(cid: String)
    @Query("DELETE FROM conversations WHERE id = :cid")
    suspend fun deleteConversation(cid: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun all(): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE title LIKE '%'||:q||'%' OR content LIKE '%'||:q||'%' ORDER BY updatedAt DESC")
    fun search(q: String): Flow<List<NoteEntity>>
    @Insert
    suspend fun insert(n: NoteEntity): Long
    @Update
    suspend fun update(n: NoteEntity)
    @Delete
    suspend fun delete(n: NoteEntity)
}
