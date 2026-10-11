package com.aui.llm.ui
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aui.llm.R
class LockActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); setContentView(R.layout.activity_lock)
        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            if (findViewById<EditText>(R.id.etPin).text.toString() == "0000") { startActivity(Intent(this, MainActivity::class.java)); finish() }
            else Toast.makeText(this, "❌ PIN غلط", Toast.LENGTH_SHORT).show()
        }
    }
}
