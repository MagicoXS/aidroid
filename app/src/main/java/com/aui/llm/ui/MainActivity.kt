package com.aui.llm.ui
import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aui.llm.R
import com.aui.llm.api.AiClient
import com.aui.llm.data.AiProvider
import com.aui.llm.data.ApiKeyManager
import com.aui.llm.security.AuiAdmin
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var km: ApiKeyManager
    private val client = AiClient()
    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName

    private val perms = arrayOf(
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        Toast.makeText(this, "✅ ${r.count { it.value }} صلاحية", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        km = ApiKeyManager(this)
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        admin = ComponentName(this, AuiAdmin::class.java)
        askPerms()

        findViewById<RecyclerView>(R.id.rvKeys).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = KeysAdapter(AiProvider.values().toList(), km, client, lifecycleScope)
        }
        val tvAdmin = findViewById<TextView>(R.id.tvAdminStatus)
        val btnAdmin = findViewById<Button>(R.id.btnAdmin)
        fun refresh() {
            if (dpm.isAdminActive(admin)) {
                tvAdmin.text = "🛡️ الحماية مفعلة ✅"
                tvAdmin.setTextColor(0xFF30D158.toInt())
                btnAdmin.text = "🛡️ أدوات الحماية"
                btnAdmin.setBackgroundColor(0xFF30D158.toInt())
            } else {
                tvAdmin.text = "⚠️ غير مفعلة"
                tvAdmin.setTextColor(0xFFFF453A.toInt())
                btnAdmin.text = "🛡️ تفعيل"
                btnAdmin.setBackgroundColor(0xFFFF453A.toInt())
            }
        }
        refresh()
        btnAdmin.setOnClickListener {
            if (dpm.isAdminActive(admin)) {
                AlertDialog.Builder(this).setTitle("أدوات").setItems(arrayOf("🔒 قفل", "📷 تعطيل كاميرا", "📷 تفعيل كاميرا", "💣 مسح", "❌ إلغاء")) { _, w ->
                    when (w) {
                        0 -> dpm.lockNow()
                        1 -> dpm.setCameraDisabled(admin, true)
                        2 -> dpm.setCameraDisabled(admin, false)
                        3 -> AlertDialog.Builder(this).setTitle("⚠️").setMessage("مسح كل البيانات؟").setPositiveButton("امسح") { _, _ -> dpm.wipeData(0) }.setNegativeButton("إلغاء", null).show()
                        4 -> { dpm.removeActiveAdmin(admin); refresh() }
                    }
                }.show()
            } else startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            })
        }
        findViewById<Button>(R.id.btnChat).setOnClickListener { startActivity(Intent(this, ChatActivity::class.java)) }
        findViewById<Button>(R.id.btnNotes).setOnClickListener { startActivity(Intent(this, NotesActivity::class.java)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun askPerms() {
        val req = mutableListOf<String>()
        for (p in perms) if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) req.add(p)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            req.add(Manifest.permission.POST_NOTIFICATIONS)
        if (req.isNotEmpty()) launcher.launch(req.toTypedArray())
    }
}

class KeysAdapter(
    private val items: List<AiProvider>, private val km: ApiKeyManager,
    private val client: AiClient, private val scope: kotlinx.coroutines.CoroutineScope
) : RecyclerView.Adapter<KeysAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val emoji: TextView = v.findViewById(R.id.tvEmoji)
        val name: TextView = v.findViewById(R.id.tvName)
        val input: EditText = v.findViewById(R.id.etKey)
        val test: Button = v.findViewById(R.id.btnTest)
        val save: Button = v.findViewById(R.id.btnSave)
        val del: Button = v.findViewById(R.id.btnDelete)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_api_key, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.emoji.text = p.emoji; h.name.text = p.displayName; h.input.hint = p.hint
        h.input.setText(km.get(p) ?: "")
        h.save.setOnClickListener { val k = h.input.text.toString().trim(); if (k.isNotEmpty()) { km.save(p, k); Toast.makeText(h.itemView.context, "✅", Toast.LENGTH_SHORT).show() } }
        h.test.setOnClickListener {
            h.test.isEnabled = false
            scope.launch {
                val r = client.test(p, h.input.text.toString().trim())
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(h.itemView.context, r.message, Toast.LENGTH_LONG).show(); h.test.isEnabled = true
                }
            }
        }
        h.del.setOnClickListener { km.delete(p); h.input.setText("") }
    }
    override fun getItemCount() = items.size
}
