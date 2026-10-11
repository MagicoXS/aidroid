package com.aui.llm.data.repo
import com.aui.llm.data.db.AuiDatabase
import com.aui.llm.data.db.entities.ConversationEntity
import com.aui.llm.data.db.entities.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val db: AuiDatabase) {
    fun conversations(): Flow<List<ConversationEntity>> = db.chatDao().allConversations()
    fun messages(cid: String): Flow<List<MessageEntity>> = db.chatDao().messages(cid)
    suspend fun recentContext(cid: String, limit: Int = 10): List<MessageEntity> =
        db.chatDao().recentMessages(cid, limit).reversed()
    suspend fun newConversation(title: String, provider: String, model: String): String {
        val id = UUID.randomUUID().toString()
        db.chatDao().upsertConversation(ConversationEntity(id, title, provider, model))
        return id
    }
    suspend fun touchConversation(cid: String, provider: String, model: String) {
        val existing = db.chatDao().allConversations()
        db.chatDao().upsertConversation(ConversationEntity(cid, "", provider, model))
    }
    suspend fun addMessage(m: MessageEntity) = db.chatDao().insertMessage(m)
    suspend fun clear(cid: String) = db.chatDao().clearMessages(cid)
    suspend fun delete(cid: String) = db.chatDao().deleteConversation(cid)
}
