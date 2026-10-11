package com.aui.llm
import android.app.Application
import com.aui.llm.data.db.AuiDatabase
class AuiApp : Application() {
    val db by lazy { AuiDatabase.get(this) }
}
