package com.aui.llm.ui
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aui.llm.AuiApp
import com.aui.llm.R
import com.aui.llm.api.AiClient
import com.aui.llm.data.AiProvider
import com.aui.llm.data.ApiKeyManager
import com.aui.llm.data.db.entities.MessageEntity
import com.aui.llm.data.repo.ChatRepository
import kotlinx.coroutines.launch
import java.util.UUID

data class UiMsg(val text: String, val isUser: Boolean, val isError: Boolean = false, val isLoading: Boolean = false)

class ChatActivity : AppCompatActivity() {
    private lateinit var km: ApiKeyManager
    private lateinit var repo: ChatRepository
    private val client = AiClient()
    private val msgs = mutableListOf<UiMsg>()
    private lateinit var adapter: MsgAdapter
    private var provider = AiProvider.GEMINI
    private var model = "gemini-2.5-flash"
    private var conversationId = ""

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_chat)
        km = ApiKeyManager(this)
        repo = ChatRepository((application as AuiApp).db)

        val rv = findViewById<RecyclerView>(R.id.rvMessages)
        adapter = MsgAdapter(msgs)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        val providers = AiProvider.values().toList()
        val sp = findViewById<Spinner>(R.id.spinnerProvider)
        val spModel = findViewById<Spinner>(R.id.spinnerModel)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, providers.map { it.emoji + " " + it.displayName })

        fun loadModels(p: AiProvider) {
            spModel.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, p.models.map { it.label })
            if (p.models.isNotEmpty()) model = p.models[0].id
        }
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { provider = providers[pos]; loadModels(provider) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (provider.models.isNotEmpty() && pos < provider.models.size) model = provider.models[pos].id
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        loadModels(provider)

        lifecycleScope.launch { conversationId = repo.newConversation("محادثة جديدة", provider.name, model) }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val input = findViewById<EditText>(R.id.etMessage)
            val t = input.text.toString().trim()
            if (t.isEmpty()) return@setOnClickListener
            input.text.clear()
            send(t)
        }

        msgs.add(UiMsg("🔥 AUI LLM v2.0 جاهز", false))
        adapter.notifyDataSetChanged()
    }

    private fun send(text: String) {
        msgs.add(UiMsg(text, true))
        adapter.notifyDataSetChanged()
        findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(msgs.size - 1)

        val key = km.get(provider)
        if (key.isNullOrEmpty()) {
            msgs.add(UiMsg("⚠️ مفيش مفتاح", false, isError = true))
            adapter.notifyDataSetChanged()
            return
        }

        val idx = msgs.size
        msgs.add(UiMsg("⏳ ...", false, isLoading = true))
        adapter.notifyDataSetChanged()

        lifecycleScope.launch {
            if (conversationId.isEmpty()) conversationId = repo.newConversation(text.take(30), provider.name, model)
            repo.addMessage(MessageEntity(conversationId = conversationId, role = "user", content = text))
            val history = repo.recentContext(conversationId, 10)
            val r = client.send(provider, key, model, history, text)
            val isErr = r.startsWith("❌") || r.startsWith("⚠️")
            msgs[idx] = UiMsg(r, false, isError = isErr)
            adapter.notifyDataSetChanged()
            findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(msgs.size - 1)
            if (!isErr) repo.addMessage(MessageEntity(conversationId = conversationId, role = "assistant", content = r, provider = provider.name))
        }
    }
}

class MsgAdapter(private val items: List<UiMsg>) : RecyclerView.Adapter<MsgAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txt: TextView = v.findViewById(R.id.tvMessage)
        val con: View = v.findViewById(R.id.container)
        val who: TextView = v.findViewById(R.id.tvWho)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        h.txt.text = m.text
        when {
            m.isUser -> { h.con.setBackgroundColor(0xFFFFD60A.toInt()); h.txt.setTextColor(0xFF0A0A0A.toInt()); h.who.text = "أنا"; h.who.setTextColor(0xFF0A0A0A.toInt()) }
            m.isError -> { h.con.setBackgroundColor(0xFF3A1A1A.toInt()); h.txt.setTextColor(0xFFFF453A.toInt()); h.who.text = "خطأ"; h.who.setTextColor(0xFFFF453A.toInt()) }
            m.isLoading -> { h.con.setBackgroundColor(0xFF2C2C2E.toInt()); h.txt.setTextColor(0xFFFFD60A.toInt()); h.who.text = "⏳"; h.who.setTextColor(0xFFFFD60A.toInt()) }
            else -> { h.con.setBackgroundColor(0xFF1C1C1E.toInt()); h.txt.setTextColor(0xFFFFFFFF.toInt()); h.who.text = "AUI"; h.who.setTextColor(0xFFBF5AF2.toInt()) }
        }
    }
    override fun getItemCount() = items.size
}
