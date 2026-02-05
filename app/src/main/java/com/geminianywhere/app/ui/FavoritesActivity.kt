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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import com.geminianywhere.app.R
import com.geminianywhere.app.data.FavoritePrompts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favorites: FavoritePrompts
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoritesAdapter
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var fabAdd: ExtendedFloatingActionButton
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Initialize data manager
        favorites = FavoritePrompts(this)

        // Find views
        recyclerView = findViewById(R.id.recyclerView)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        fabAdd = findViewById(R.id.fabAdd)
        emptyState = findViewById(R.id.emptyState)

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.favorites_title)

        // Setup RecyclerView
        adapter = FavoritesAdapter(
            items = favorites.getAll().toMutableList(),
            onUse = { item ->
                // Copy prompt to clipboard for reuse
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Prompt", item.prompt)
                clipboard.setPrimaryClip(clip)
                
                favorites.incrementUsage(item.id)
                updateUI()
                Snackbar.make(recyclerView, "✓ Prompt copied to clipboard", Snackbar.LENGTH_SHORT).show()
            },
            onEdit = { item ->
                showAddEditDialog(item)
            },
            onDelete = { item ->
                favorites.delete(item.id)
                updateUI()
                Snackbar.make(recyclerView, "Deleted", Snackbar.LENGTH_SHORT).show()
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup category filter
        setupCategoryFilter()

        // Setup FAB
        fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }

        updateUI()
    }

    private fun setupCategoryFilter() {
        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                adapter.items = favorites.getAll().toMutableList()
            } else {
                val chip = findViewById<Chip>(checkedIds[0])
                val category = chip?.text.toString()
                adapter.items = when (category) {
                    "All" -> favorites.getAll().toMutableList()
                    else -> favorites.getByCategory(category).toMutableList()
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showAddEditDialog(item: FavoritePrompts.FavoriteItem?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etPrompt = view.findViewById<EditText>(R.id.etPrompt)
        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val etTags = view.findViewById<EditText>(R.id.etTags)

        // Setup category dropdown
        val categories = arrayOf("General", "Work", "Personal", "Creative", "Technical", "Social")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        actvCategory.setAdapter(categoryAdapter)

        // Pre-fill if editing
        item?.let {
            etTitle.setText(it.title)
            etPrompt.setText(it.prompt)
            actvCategory.setText(it.category, false) // false = don't filter
            etTags.setText(it.tags.joinToString(", "))
        } ?: run {
            // Set default category for new items
            actvCategory.setText("General", false)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (item == null) "Add Favorite" else "Edit Favorite")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString()
                val prompt = etPrompt.text.toString()
                val category = actvCategory.text.toString().ifEmpty { "General" }
                val tags = etTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (item == null) {
                    favorites.add(title, prompt, category, tags)
                } else {
                    favorites.update(item.id, title, prompt, category, tags)
                }
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUI() {
        val items = favorites.getAll()
        adapter.items = items.toMutableList()
        adapter.notifyDataSetChanged()

        // Show/hide empty state
        emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class FavoritesAdapter(
    var items: MutableList<FavoritePrompts.FavoriteItem>,
    private val onUse: (FavoritePrompts.FavoriteItem) -> Unit,
    private val onEdit: (FavoritePrompts.FavoriteItem) -> Unit,
    private val onDelete: (FavoritePrompts.FavoriteItem) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvPrompt: TextView = view.findViewById(R.id.tvPrompt)
        val chipCategory: Chip = view.findViewById(R.id.chipCategory)
        val chipGroupTags: ChipGroup = view.findViewById(R.id.chipGroupTags)
        val tvUsageCount: TextView = view.findViewById(R.id.tvUsageCount)
        val btnUse: Button = view.findViewById(R.id.btnUse)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.tvTitle.text = item.title
        holder.tvPrompt.text = item.prompt
        holder.chipCategory.text = item.category
        holder.tvUsageCount.text = "Used ${item.usageCount} times"

        // Add tags as chips
        holder.chipGroupTags.removeAllViews()
        item.tags.forEach { tag ->
            val chip = Chip(holder.itemView.context).apply {
                text = tag
                isClickable = false
            }
            holder.chipGroupTags.addView(chip)
        }

        holder.btnUse.setOnClickListener { onUse(item) }
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}
