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
import com.example.aeropass.databinding.ActivityAddEditLoginCredentialBinding
import com.example.aeropass.ui.credential.CredentialViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditLoginCredentialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditLoginCredentialBinding
    private val viewModel: CredentialViewModel by viewModels()
    private var credentialId: Long = -1
    private var isEditMode = false

    companion object {
        const val EXTRA_CREDENTIAL_ID = "credential_id"
        
        fun newIntent(context: Context, credentialId: Long = -1L): Intent {
            return Intent(context, AddEditLoginCredentialActivity::class.java).apply {
                if (credentialId != -1L) putExtra(EXTRA_CREDENTIAL_ID, credentialId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditLoginCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        handleIntent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Login Credential" else "Add Login Credential"
    }

    private fun setupViews() {
        setupCategorySpinner()
        setupCopyPasswordButton()
        setupSaveButton()
        // Autofill functionality removed
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Personal", "Work", "Finance", "Social", "Shopping", "Entertainment", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupCopyPasswordButton() {
        binding.btnCopyPassword.setOnClickListener {
            val password = binding.etPassword.text.toString()
            if (password.isNotEmpty()) {
                copyToClipboard("Password", password)
                Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No password to copy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveLoginCredential()
        }
    }

    private fun setupObservers() {
        viewModel.operationResult.observe(this) { result ->
            result.onSuccess { 
                Toast.makeText(this, "Login credential saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
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
            supportActionBar?.title = "Edit Login Credential"
            viewModel.loadCredential(credentialId)
        } else {
            supportActionBar?.title = "Add Login Credential"
        }
    }

    private fun loadCredential() {
        viewModel.loadCredential(credentialId)
    }

    private fun populateFields(credential: DecryptedCredential) {
        binding.etTitle.setText(credential.title)
        binding.etUsername.setText(credential.username)
        binding.etEmail.setText(credential.email)
        binding.etPassword.setText(credential.password)
        binding.etUrl.setText(credential.url)
        binding.etRecoveryPhone.setText(credential.recoveryPhone)
        // Use notes field for recovery info for backward compatibility

        
        // Set category
        val categoryAdapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
        val categoryPosition = categoryAdapter.getPosition(credential.category)
        if (categoryPosition >= 0) {
            binding.spinnerCategory.setSelection(categoryPosition)
        }
    }

    private fun saveLoginCredential() {
        val title = binding.etTitle.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()
        val recoveryPhone = binding.etRecoveryPhone.text.toString().trim()
        val recoveryInfo = binding.etRecoveryInfo.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()


        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        }
        
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }

        // Clear any previous errors
        binding.tilTitle.error = null
        binding.tilUsername.error = null
        binding.tilPassword.error = null

        val currentTime = System.currentTimeMillis()
        val existingCredential = if (isEditMode) viewModel.credential.value else null

        val credential = DecryptedCredential(
            id = if (isEditMode) credentialId else 0,
            title = title,
            username = username,
            email = email,
            password = password,
            url = url,
            recoveryPhone = recoveryPhone,
            category = category,
            notes = recoveryInfo, // Use recovery info as notes for backward compatibility
            isFavorite = false,
            tags = emptyList(), // No tags for login credentials
            type = CredentialType.LOGIN,
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
                saveLoginCredential()
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

    // Autofill functionality removed
    
    // Autofill text watcher removed
    
    // Autofill suggestions popup removed
    
    private fun fillFormWithSuggestion(credential: DecryptedCredential) {
        binding.etTitle.setText(credential.title)
        binding.etUsername.setText(credential.username)
        binding.etEmail.setText(credential.email)
        binding.etPassword.setText(credential.password)
        binding.etUrl.setText(credential.url)
        
        // Set category if it matches
        val categories = resources.getStringArray(R.array.credential_categories)
        val categoryIndex = categories.indexOf(credential.category)
        if (categoryIndex >= 0) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }
    }

    private fun shareCredential() {
        val title = binding.etTitle.text.toString()
        val username = binding.etUsername.text.toString()
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        val url = binding.etUrl.text.toString()
        val notes = binding.etRecoveryInfo.text.toString()
        val category = binding.spinnerCategory.selectedItem.toString()

        val shareText = buildString {
            appendLine("🔐 AeroPass Credentials")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Type: Login")
            if (title.isNotEmpty()) appendLine("Title: $title")
            if (username.isNotEmpty()) appendLine("Username: $username")
            if (email.isNotEmpty()) appendLine("Email: $email")
            if (password.isNotEmpty()) appendLine("Password: $password")
            if (url.isNotEmpty()) appendLine("URL: $url")
            if (category.isNotEmpty()) appendLine("Category: $category")
            if (notes.isNotEmpty()) {
                appendLine("Notes:")
                appendLine(notes)
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠ Keep this information secure!")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Login Credential: $title")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Login Credential"))
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Credential")
            .setMessage("Are you sure you want to delete this login credential?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCredential(credentialId)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}