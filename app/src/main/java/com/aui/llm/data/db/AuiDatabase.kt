package com.aui.llm.data.db
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aui.llm.data.db.daos.ChatDao
import com.aui.llm.data.db.daos.NoteDao
import com.aui.llm.data.db.entities.*
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, NoteEntity::class],
    version = 1, exportSchema = false
)
abstract class AuiDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun noteDao(): NoteDao
    companion object {
        @Volatile private var INSTANCE: AuiDatabase? = null
        fun get(c: Context): AuiDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                val key = SecureRandom().generateSeed(32)
                val factory = SupportFactory(key)
                Room.databaseBuilder(c.applicationContext, AuiDatabase::class.java, "aui_v2.db")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
