package com.geminianywhere.app.ui

import android.os.Bundle
import android.view.LayoutInflater
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

    private companion object {
        const val COMMAND_PREFIX = "/"
    }

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
        showCommandDialog(isEdit = false)
    }

    private fun showEditDialog(command: String) {
        showCommandDialog(isEdit = true, existingCommand = command)
    }

    private fun showCommandDialog(isEdit: Boolean, existingCommand: String? = null) {
        val dialogBinding = DialogAddCommandBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = if (isEdit) "Edit Command" else "Add Command"
        
        if (isEdit && existingCommand != null) {
            dialogBinding.etCommandName.setText(existingCommand)
            dialogBinding.etPromptTemplate.setText(commands[existingCommand])
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val command = dialogBinding.etCommandName.text.toString().trim()
                val prompt = dialogBinding.etPromptTemplate.text.toString().trim()

                when {
                    command.isEmpty() || prompt.isEmpty() -> {
                        showSnackbar("Please fill all fields", false)
                    }
                    !command.startsWith(COMMAND_PREFIX) -> {
                        showSnackbar("Command must start with $COMMAND_PREFIX", false)
                    }
                    else -> {
                        if (isEdit && existingCommand != command) {
                            commands.remove(existingCommand)
                        }
                        commands[command] = prompt
                        prefManager.saveCommands(commands)
                        adapter.updateCommands(commands)
                        val message = if (isEdit) "✓ Command updated" else "✓ Command added: $command"
                        showSnackbar(message)
                    }
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
                showSnackbar("✓ Command deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSnackbar(message: String, isSuccess: Boolean = true) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
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
