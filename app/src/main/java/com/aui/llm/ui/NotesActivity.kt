package com.aui.llm.ui
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aui.llm.AuiApp
import com.aui.llm.R
import com.aui.llm.data.db.entities.NoteEntity
import com.aui.llm.data.repo.NoteRepository
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

class NotesActivity : AppCompatActivity() {
    private lateinit var repo: NoteRepository
    private val notes = mutableListOf<NoteEntity>()
    private lateinit var adapter: NoteAdapter
    private lateinit var markwon: Markwon

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_notes)
        repo = NoteRepository((application as AuiApp).db)
        markwon = Markwon.create(this)

        val rv = findViewById<RecyclerView>(R.id.rvNotes)
        adapter = NoteAdapter(notes, markwon, onDelete = { note ->
            lifecycleScope.launch { repo.delete(note); loadNotes() }
        })
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAddNote).setOnClickListener {
            val t = findViewById<EditText>(R.id.etNote).text.toString().trim()
            if (t.isNotEmpty()) lifecycleScope.launch {
                repo.insert(NoteEntity(title = t.take(30), content = t))
                findViewById<EditText>(R.id.etNote).text.clear()
                loadNotes()
            }
        }
        loadNotes()
    }

    private fun loadNotes() {
        lifecycleScope.launch {
            repo.all().collect { list -> notes.clear(); notes.addAll(list); adapter.notifyDataSetChanged() }
        }
    }
}

class NoteAdapter(private val items: List<NoteEntity>, private val markwon: Markwon, val onDelete: (NoteEntity) -> Unit) :
    RecyclerView.Adapter<NoteAdapter.VH>() {
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvNoteTitle)
        val content: TextView = v.findViewById(R.id.tvNoteContent)
        val del: ImageButton = v.findViewById(R.id.btnNoteDelete)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_note, p, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val n = items[pos]
        h.title.text = n.title
        markwon.setMarkdown(h.content, n.content)
        h.del.setOnClickListener { onDelete(n) }
    }
    override fun getItemCount() = items.size
}
