package com.example.aeropass.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aeropass.AeroPassApplication
import com.example.aeropass.R
import com.example.aeropass.data.model.Credential
import com.example.aeropass.databinding.ActivityMainBinding
import com.example.aeropass.ui.auth.AuthActivity
import com.example.aeropass.ui.credential.AddEditLoginCredentialActivity
import com.example.aeropass.ui.credential.AddEditPaymentCredentialActivity
import com.example.aeropass.ui.credential.AddEditIdentityCredentialActivity
import com.example.aeropass.ui.credential.AddEditSecureNotesCredentialActivity
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.ui.CredentialTypeSelectionActivity
import com.example.aeropass.ui.settings.SettingsActivity
import com.example.aeropass.utils.SecureClipboardManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application)
    }
    private lateinit var credentialAdapter: CredentialAdapter
    private lateinit var clipboardManager: SecureClipboardManager
    private var actionMode: ActionMode? = null
    private var currentCategories: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        clipboardManager = SecureClipboardManager(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeViewModel()
        observeSession()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "AeroPass"
    }

    private fun setupRecyclerView() {
        credentialAdapter = CredentialAdapter(
            onItemClick = { credential ->
                val intent = when (credential.credentialType) {
                    CredentialType.LOGIN -> AddEditLoginCredentialActivity.newIntent(this, credential.id)
                    CredentialType.PAYMENT -> AddEditPaymentCredentialActivity.newIntent(this, credential.id)
                    CredentialType.IDENTITY -> AddEditIdentityCredentialActivity.newIntent(this, credential.id)
                    CredentialType.SECURE_NOTES -> AddEditSecureNotesCredentialActivity.newIntent(this, credential.id)
                }
                startActivity(intent)
            },
            onCopyPassword = { credential ->
                val decrypted = viewModel.decryptCredential(credential)
                decrypted?.let {
                    val app = application as AeroPassApplication
                    val textToCopy = when (credential.credentialType) {
                        CredentialType.LOGIN -> it.password
                        CredentialType.PAYMENT -> it.cardNumber.ifEmpty { it.accountNumber }
                        CredentialType.IDENTITY -> it.documentNumber
                        CredentialType.SECURE_NOTES -> it.secretKey.ifEmpty { it.noteContent }
                    }
                    if (textToCopy.isNotEmpty()) {
                        clipboardManager.copyToClipboard(
                            text = textToCopy,
                            autoClearSeconds = app.sessionManager.clipboardTimeout / 1000
                        )
                    }
                }
            },
            onStartActionMode = {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(actionModeCallback)
                }
            },
            decryptCredential = { credential ->
                viewModel.decryptCredential(credential)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = credentialAdapter
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, CredentialTypeSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun shareCredentials(selectedIds: Set<Long>) {
        lifecycleScope.launch {
            val credentialsToShare = mutableListOf<Credential>()
            selectedIds.forEach { id ->
                viewModel.credentials.value?.find { it.id == id }?.let {
                    credentialsToShare.add(it)
                }
            }

            if (credentialsToShare.isNotEmpty()) {
                val shareText = buildString {
                    appendLine("🔐 AeroPass Credentials")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━")
                    credentialsToShare.forEach { credential ->
                        val decrypted = viewModel.decryptCredential(credential)
                        decrypted?.let {
                            appendLine("Title: ${it.title}")
                            appendLine("Type: ${it.type.displayName}")
                            
                            when (it.type) {
                                CredentialType.LOGIN -> {
                                    if (it.username.isNotEmpty()) appendLine("Username: ${it.username}")
                                    if (it.email.isNotEmpty()) appendLine("Email: ${it.email}")
                                    if (it.password.isNotEmpty()) appendLine("Password: ${it.password}")
                                    if (it.url.isNotEmpty()) appendLine("URL: ${it.url}")
                                    if (it.recoveryInfo.isNotEmpty()) appendLine("Recovery Info: ${it.recoveryInfo}")
                                    if (it.notes.isNotEmpty()) appendLine("Notes: ${it.notes}")
                                }
                                CredentialType.PAYMENT -> {
                                    if (it.cardholderName.isNotEmpty()) appendLine("Cardholder: ${it.cardholderName}")
                                    if (it.cardNumber.isNotEmpty()) appendLine("Card Number: ${it.cardNumber}")
                                    if (it.expirationDate.isNotEmpty()) appendLine("Expiration: ${it.expirationDate}")
                                    if (it.cvv.isNotEmpty()) appendLine("CVV: ${it.cvv}")
                                    if (it.bankName.isNotEmpty()) appendLine("Bank: ${it.bankName}")
                                    if (it.accountNumber.isNotEmpty()) appendLine("Account Number: ${it.accountNumber}")
                                    if (it.routingNumber.isNotEmpty()) appendLine("Routing Number: ${it.routingNumber}")
                                    if (it.branch.isNotEmpty()) appendLine("Branch: ${it.branch}")
                                    if (it.internetBankingUsername.isNotEmpty()) appendLine("Internet Banking Username: ${it.internetBankingUsername}")
                                    if (it.internetBankingPassword.isNotEmpty()) appendLine("Internet Banking Password: ${it.internetBankingPassword}")
                                    if (it.billingAddress.isNotEmpty()) appendLine("Billing Address: ${it.billingAddress}")
                                }
                                CredentialType.IDENTITY -> {
                                    if (it.fullName.isNotEmpty()) appendLine("Full Name: ${it.fullName}")
                                    if (it.fathersName.isNotEmpty()) appendLine("Father's Name: ${it.fathersName}")
                                    if (it.mothersName.isNotEmpty()) appendLine("Mother's Name: ${it.mothersName}")
                                    if (it.dateOfBirth.isNotEmpty()) appendLine("Date of Birth: ${it.dateOfBirth}")
                                    if (it.documentType.isNotEmpty()) appendLine("Document Type: ${it.documentType}")
                                    if (it.documentNumber.isNotEmpty()) appendLine("Document Number: ${it.documentNumber}")

                                    if (it.webPortal.isNotEmpty()) appendLine("Web Portal: ${it.webPortal}")
                                }
                                CredentialType.SECURE_NOTES -> {
                                    if (it.secretKey.isNotEmpty()) appendLine("Secret Key: ${it.secretKey}")
                                    if (it.qrCodeData.isNotEmpty()) appendLine("QR Code Data: ${it.qrCodeData}")
                                    if (it.noteContent.isNotEmpty()) appendLine("Note Content: ${it.noteContent}")
                                }
                            }
                            
                            if (it.category.isNotEmpty()) appendLine("Category: ${it.category}")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
                        }
                    }
                    appendLine("⚠️ Keep this information secure!")
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "AeroPass Credentials")
                }
                startActivity(Intent.createChooser(shareIntent, "Share Credentials"))
            }
        }
    }

    private fun deleteCredentials(selectedIds: Set<Long>) {
        AlertDialog.Builder(this)
            .setTitle("Delete Credentials")
            .setMessage("Are you sure you want to delete the selected credentials? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    selectedIds.forEach { id ->
                        viewModel.credentials.value?.find { it.id == id }?.let {
                            viewModel.deleteCredential(it)
                        }
                    }
                    actionMode?.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.credentials.observe(this) { credentials ->
            credentialAdapter.submitList(credentials)
            binding.emptyView.visibility = if (credentials.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainViewModel.UiState.Success -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is MainViewModel.UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.categories.observe(this) { categories ->
            currentCategories = categories
        }
    }

    private fun observeSession() {
        val app = application as AeroPassApplication
        app.sessionManager.isAuthenticated.observe(this) { isAuthenticated ->
            if (!isAuthenticated) {
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchCredentials(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                true
            }
            R.id.action_filter -> {
                showCategoryFilterDialog()
                true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCategoryFilterDialog() {
        val categoryList = mutableListOf("All")
        categoryList.addAll(currentCategories)

        AlertDialog.Builder(this)
            .setTitle("Filter by Category")
            .setItems(categoryList.toTypedArray()) { _, which ->
                val selectedCategory = categoryList[which]
                viewModel.filterByCategory(selectedCategory)
            }
            .show()
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_credential_selection, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedIds = credentialAdapter.getSelectedItems()
            return when (item.itemId) {
                R.id.action_share -> {
                    shareCredentials(selectedIds)
                    true
                }
                R.id.action_delete -> {
                    deleteCredentials(selectedIds)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            credentialAdapter.clearSelection()
            actionMode = null
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as AeroPassApplication
        app.sessionManager.resetAutoLockTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        val app = application as AeroPassApplication
        app.sessionManager.resetAutoLockTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager.cleanup()
    }
}