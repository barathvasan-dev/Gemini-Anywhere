package com.geminianywhere.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Button
import com.geminianywhere.app.R
import com.geminianywhere.app.data.CommandHistory
import com.geminianywhere.app.data.FavoritePrompts
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var history: CommandHistory
    private lateinit var favorites: FavoritePrompts
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var tvStats: TextView
    private lateinit var btnClearAll: Button
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize data managers
        history = CommandHistory(this)
        favorites = FavoritePrompts(this)

        // Find views
        recyclerView = findViewById(R.id.recyclerView)
        tvStats = findViewById(R.id.tvStats)
        btnClearAll = findViewById(R.id.btnClearAll)
        emptyState = findViewById(R.id.emptyState)

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.history_title)

        // Setup RecyclerView
        adapter = HistoryAdapter(
            items = history.getAll().toMutableList(),
            onView = { item ->
                showHistoryDetailDialog(item)
            },
            onAddToFavorites = { item ->
                favorites.add(
                    title = item.prompt.take(50),
                    prompt = item.prompt,
                    category = "General",
                    tags = listOf(item.context)
                )
                Snackbar.make(recyclerView, "Added to favorites", Snackbar.LENGTH_SHORT).show()
            },
            onDelete = { item ->
                history.delete(item.id)
                updateUI()
                Snackbar.make(recyclerView, "Deleted", Snackbar.LENGTH_SHORT).show()
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup clear all button
        btnClearAll.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all history?")
                .setPositiveButton("Clear") { _, _ ->
                    history.clear()
                    updateUI()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        updateUI()
    }

    private fun showHistoryDetailDialog(item: CommandHistory.HistoryItem) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        val message = buildString {
            append("📝 Prompt:\n")
            append(item.prompt)
            append("\n\n")
            append("💬 Response:\n")
            append(item.response)
            append("\n\n")
            append("📍 Context: ${item.context}\n")
            append("🕐 Time: ${dateFormat.format(Date(item.timestamp))}")
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_history_detail, null)
        val tvDetails = dialogView.findViewById<TextView>(R.id.tvHistoryDetails)
        val btnCopyPrompt = dialogView.findViewById<Button>(R.id.btnCopyPrompt)
        val btnCopyResponse = dialogView.findViewById<Button>(R.id.btnCopyResponse)
        val btnCopyBoth = dialogView.findViewById<Button>(R.id.btnCopyBoth)
        
        tvDetails.text = message
        
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("History Details")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
        
        btnCopyPrompt.setOnClickListener {
            val clip = ClipData.newPlainText("Prompt", item.prompt)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(recyclerView, "✓ Prompt copied", Snackbar.LENGTH_SHORT).show()
        }
        
        btnCopyResponse.setOnClickListener {
            val clip = ClipData.newPlainText("Response", item.response)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(recyclerView, "✓ Response copied", Snackbar.LENGTH_SHORT).show()
        }
        
        btnCopyBoth.setOnClickListener {
            val bothText = "Prompt:\n${item.prompt}\n\nResponse:\n${item.response}"
            val clip = ClipData.newPlainText("Prompt & Response", bothText)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(recyclerView, "✓ Both copied", Snackbar.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }

    private fun updateUI() {
        val items = history.getAll()
        adapter.items = items.toMutableList()
        adapter.notifyDataSetChanged()

        // Update stats
        val stats = history.getStats()
        tvStats.text = "Total: ${stats["total"]} commands | Today: ${stats["today"]}"

        // Show/hide empty state
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class HistoryAdapter(
    var items: MutableList<CommandHistory.HistoryItem>,
    private val onView: (CommandHistory.HistoryItem) -> Unit,
    private val onAddToFavorites: (CommandHistory.HistoryItem) -> Unit,
    private val onDelete: (CommandHistory.HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPrompt: TextView = view.findViewById(R.id.tvPrompt)
        val tvResponse: TextView = view.findViewById(R.id.tvResponse)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val chipContext: TextView = view.findViewById(R.id.chipContext)
        val btnAddToFavorites: Button = view.findViewById(R.id.btnAddToFavorites)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvPrompt.text = item.prompt
        holder.tvResponse.text = item.response.take(100) + if (item.response.length > 100) "..." else ""
        holder.tvDate.text = dateFormat.format(Date(item.timestamp))
        holder.chipContext.text = item.context

        // Click on entire item to view full details
        holder.itemView.setOnClickListener { onView(item) }

        holder.btnAddToFavorites.setOnClickListener { onAddToFavorites(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}
