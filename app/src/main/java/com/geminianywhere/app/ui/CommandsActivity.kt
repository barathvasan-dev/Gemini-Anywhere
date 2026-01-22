package com.geminianywhere.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.geminianywhere.app.databinding.ActivityCommandsBinding
import com.geminianywhere.app.databinding.DialogAddCommandBinding
import com.geminianywhere.app.databinding.ItemCommandBinding
import com.geminianywhere.app.utils.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class CommandsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommandsBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var adapter: CommandAdapter
    private var commands = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommandsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        setupToolbar()
        loadCommands()
        setupRecyclerView()
        setupFab()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadCommands() {
        commands = prefManager.getCommands().toMutableMap()
    }

    private fun setupRecyclerView() {
        adapter = CommandAdapter(
            commands = commands,
            onEdit = { command -> showEditDialog(command) },
            onDelete = { command -> deleteCommand(command) }
        )
        binding.rvCommands.layoutManager = LinearLayoutManager(this)
        binding.rvCommands.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddCommand.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogBinding = DialogAddCommandBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Add Command"

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val command = dialogBinding.etCommandName.text.toString().trim()
                val prompt = dialogBinding.etPromptTemplate.text.toString().trim()

                if (command.isNotEmpty() && prompt.isNotEmpty()) {
                    if (!command.startsWith("/")) {
                        Snackbar.make(binding.root, "Command must start with /", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    commands[command] = prompt
                    prefManager.saveCommands(commands)
                    adapter.updateCommands(commands)
                    Snackbar.make(binding.root, "✓ Command added: $command", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(command: String) {
        val dialogBinding = DialogAddCommandBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Edit Command"
        dialogBinding.etCommandName.setText(command)
        dialogBinding.etPromptTemplate.setText(commands[command])

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newCommand = dialogBinding.etCommandName.text.toString().trim()
                val newPrompt = dialogBinding.etPromptTemplate.text.toString().trim()

                if (newCommand.isNotEmpty() && newPrompt.isNotEmpty()) {
                    if (!newCommand.startsWith("/")) {
                        Snackbar.make(binding.root, "Command must start with /", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    // Remove old command if name changed
                    if (command != newCommand) {
                        commands.remove(command)
                    }
                    commands[newCommand] = newPrompt
                    prefManager.saveCommands(commands)
                    adapter.updateCommands(commands)
                    Snackbar.make(binding.root, "✓ Command updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCommand(command: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Command")
            .setMessage("Delete $command?")
            .setPositiveButton("Delete") { _, _ ->
                commands.remove(command)
                prefManager.saveCommands(commands)
                adapter.updateCommands(commands)
                Snackbar.make(binding.root, "✓ Command deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class CommandAdapter(
    private var commands: Map<String, String>,
    private val onEdit: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CommandAdapter.CommandViewHolder>() {

    private val commandList = commands.keys.toMutableList()

    fun updateCommands(newCommands: Map<String, String>) {
        commands = newCommands
        commandList.clear()
        commandList.addAll(newCommands.keys)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CommandViewHolder {
        val binding = ItemCommandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommandViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        val command = commandList[position]
        holder.bind(command, commands[command] ?: "")
    }

    override fun getItemCount() = commandList.size

    inner class CommandViewHolder(private val binding: ItemCommandBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(command: String, prompt: String) {
            binding.tvCommand.text = command
            binding.tvPrompt.text = prompt
            binding.btnEdit.setOnClickListener { onEdit(command) }
            binding.btnDelete.setOnClickListener { onDelete(command) }
        }
    }
}
