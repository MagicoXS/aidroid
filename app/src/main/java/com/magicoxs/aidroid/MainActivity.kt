package com.magicoxs.aidroid
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "JAMDAN AIDROID V10\nPRO BUILD ✅"
            textSize = 26f
            setPadding(60, 400, 60, 60)
        })
    }
}
