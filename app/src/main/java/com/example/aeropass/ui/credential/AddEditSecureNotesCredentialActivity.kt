package com.example.aeropass.ui.credential

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aeropass.R
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.data.model.DecryptedCredential
import com.example.aeropass.databinding.ActivityAddEditSecureNotesCredentialBinding
import com.example.aeropass.ui.credential.CredentialViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditSecureNotesCredentialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditSecureNotesCredentialBinding
    private val viewModel: CredentialViewModel by viewModels()
    private var credentialId: Long = -1
    private var isEditMode = false

    companion object {
        const val EXTRA_CREDENTIAL_ID = "credential_id"
        
        fun newIntent(context: Context, credentialId: Long = -1L): Intent {
            return Intent(context, AddEditSecureNotesCredentialActivity::class.java).apply {
                if (credentialId != -1L) putExtra(EXTRA_CREDENTIAL_ID, credentialId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditSecureNotesCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        handleIntent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Secure Notes" else "Add Secure Notes"
    }

    private fun setupViews() {
        setupCategorySpinner()
        setupCopySecretButton()
        setupSaveButton()
        // Autofill functionality removed
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.secure_notes_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupCopySecretButton() {
        binding.btnCopySecret.setOnClickListener {
            val secretKey = binding.etSecretKey.text.toString()
            if (secretKey.isNotEmpty()) {
                copyToClipboard("Secret Key", secretKey)
                Toast.makeText(this, "Secret key copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No secret key to copy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSecureNotesCredential()
        }
    }

    private fun setupObservers() {
        viewModel.operationResult.observe(this) { result ->
            result.onSuccess { 
                Toast.makeText(this, "Secure notes saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { error ->
                if (error is com.example.aeropass.data.repository.DuplicateCredentialException) {
                    error.message?.toLongOrNull()?.let {
                        showOverwriteConfirmationDialog(it)
                    }
                } else {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.credential.observe(this) { credential ->
            credential?.let { populateFields(it) }
        }
    }

    private fun handleIntent() {
        credentialId = intent.getLongExtra("credential_id", -1)
        isEditMode = credentialId != -1L
        
        if (isEditMode) {
            supportActionBar?.title = "Edit Secure Notes"
            viewModel.loadCredential(credentialId)
        } else {
            supportActionBar?.title = "Add Secure Notes"
        }
    }

    private fun loadCredential() {
        viewModel.loadCredential(credentialId)
    }

    private fun populateFields(credential: DecryptedCredential) {
        binding.etTitle.setText(credential.title)
        binding.etSecretKey.setText(credential.secretKey)
        binding.etNoteContent.setText(credential.noteContent)

        
        // Set category
        val categoryAdapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
        val categoryPosition = categoryAdapter.getPosition(credential.category)
        if (categoryPosition >= 0) {
            binding.spinnerCategory.setSelection(categoryPosition)
        }
    }

    private fun saveSecureNotesCredential() {
        val title = binding.etTitle.text.toString().trim()
        val secretKey = binding.etSecretKey.text.toString().trim()
        val noteContent = binding.etNoteContent.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()


        // Validation
        var hasError = false
        
        if (title.isEmpty()) {
            binding.tilTitle.error = "Title/Service name is required"
            hasError = true
        } else {
            binding.tilTitle.error = null
        }
        
        if (secretKey.isEmpty() && noteContent.isEmpty()) {
            Toast.makeText(this, "At least one field (Secret Key or Notes) must be filled", Toast.LENGTH_LONG).show()
            hasError = true
        }
        
        if (hasError) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val existingCredential = if (isEditMode) viewModel.credential.value else null

        val credential = DecryptedCredential(
            id = if (isEditMode) credentialId else 0,
            title = title,
            secretKey = secretKey,
            noteContent = noteContent,
            category = category,
            notes = noteContent, // Use noteContent as notes for backward compatibility
            isFavorite = false,
            tags = emptyList(), // Tags removed
            type = CredentialType.SECURE_NOTES,
            createdAt = existingCredential?.createdAt ?: currentTime,
            updatedAt = currentTime
        )

        if (isEditMode) {
            viewModel.updateCredential(credential)
        } else {
            viewModel.addCredential(credential)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isEditMode) {
            menuInflater.inflate(R.menu.menu_credential, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveSecureNotesCredential()
                true
            }
            R.id.action_share -> {
                shareCredential()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    
    private fun fillFormWithSuggestion(credential: DecryptedCredential) {
        binding.etTitle.setText(credential.title)
        binding.etSecretKey.setText(credential.secretKey)
        binding.etNoteContent.setText(credential.noteContent)
        
        // Set category if it matches
        val categories = resources.getStringArray(R.array.secure_notes_categories)
        val categoryIndex = categories.indexOf(credential.category)
        if (categoryIndex >= 0) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }
    }

    private fun shareCredential() {
        val title = binding.etTitle.text.toString()
        val secretKey = binding.etSecretKey.text.toString()
        val noteContent = binding.etNoteContent.text.toString()
        val category = binding.spinnerCategory.selectedItem.toString()

        val shareText = buildString {
            appendLine("🔐 AeroPass Credentials")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Type: Secure Notes")
            if (title.isNotEmpty()) appendLine("Title: $title")
            if (secretKey.isNotEmpty()) appendLine("Secret Key: $secretKey")
            if (category.isNotEmpty()) appendLine("Category: $category")
            if (noteContent.isNotEmpty()) {
                appendLine("Notes:")
                appendLine(noteContent)
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠ Keep this information secure!")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Secure Notes: $title")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Secure Notes"))
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Secure Notes")
            .setMessage("Are you sure you want to delete this secure note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCredential(credentialId)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOverwriteConfirmationDialog(existingCredentialId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Duplicate Credential")
            .setMessage("A credential with the same title already exists. Do you want to overwrite it?")
            .setPositiveButton("Overwrite") { _, _ ->
                val title = binding.etTitle.text.toString().trim()
                val secretKey = binding.etSecretKey.text.toString().trim()
                val noteContent = binding.etNoteContent.text.toString().trim()
                val category = binding.spinnerCategory.selectedItem.toString()
                val isFavorite = false

                val currentTime = System.currentTimeMillis()

                val credential = DecryptedCredential(
                    id = existingCredentialId,
                    title = title,
                    secretKey = secretKey,
                    noteContent = noteContent,
                    category = category,
                    notes = noteContent, // Use noteContent as notes for backward compatibility
                    isFavorite = isFavorite,
                    tags = emptyList(), // Tags removed
                    type = CredentialType.SECURE_NOTES,
                    createdAt = viewModel.credential.value?.createdAt ?: currentTime,
                    updatedAt = currentTime
                )
                viewModel.updateCredential(credential)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}