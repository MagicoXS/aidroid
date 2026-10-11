package com.aui.llm.data.repo
import com.aui.llm.data.db.AuiDatabase
import com.aui.llm.data.db.entities.NoteEntity
import kotlinx.coroutines.flow.Flow
class NoteRepository(private val db: AuiDatabase) {
    fun all(): Flow<List<NoteEntity>> = db.noteDao().all()
    fun search(q: String): Flow<List<NoteEntity>> = db.noteDao().search(q)
    suspend fun insert(n: NoteEntity) = db.noteDao().insert(n)
    suspend fun update(n: NoteEntity) = db.noteDao().update(n)
    suspend fun delete(n: NoteEntity) = db.noteDao().delete(n)
}
