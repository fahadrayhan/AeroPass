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
import android.view.View
import android.widget.AdapterView
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
import com.example.aeropass.databinding.ActivityAddEditPaymentCredentialBinding
import com.example.aeropass.ui.credential.CredentialViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditPaymentCredentialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPaymentCredentialBinding
    private val viewModel: CredentialViewModel by viewModels()
    private var credentialId: Long = -1
    private var isEditMode = false

    companion object {
        const val EXTRA_CREDENTIAL_ID = "credential_id"
        const val PAYMENT_TYPE_BANK_ACCOUNT = "Bank Account"
        const val PAYMENT_TYPE_CREDIT_CARD = "Credit/Debit Card"
        
        fun newIntent(context: Context, credentialId: Long = -1L): Intent {
            return Intent(context, AddEditPaymentCredentialActivity::class.java).apply {
                if (credentialId != -1L) putExtra(EXTRA_CREDENTIAL_ID, credentialId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPaymentCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        handleIntent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Banking Credential" else "Add Banking Credential"
    }

    private fun setupViews() {
        setupPaymentTypeSpinner()
        setupCardTypeSpinner()
        setupCategorySpinner()
        setupSaveButton()
        setupExpirationDateFormatting()
        // Autofill functionality removed
    }
    
    // Autofill functionality removed
    
    // Autofill text watcher removed
    
    // Autofill suggestions popup removed
    
    private fun fillFormWithSuggestion(credential: DecryptedCredential) {
        // Note: Payment credentials don't have a separate title field
        binding.etCardholderName.setText(credential.cardholderName)
        binding.etBankName.setText(credential.bankName)
        binding.etCardBankName.setText(credential.bankName)
        binding.etAccountNumber.setText(credential.accountNumber)
        binding.etCardNumber.setText(credential.cardNumber)
        binding.etExpirationDate.setText(credential.expirationDate)
        binding.etCvv.setText(credential.cvv)
        binding.etRoutingNumber.setText(credential.routingNumber)
        binding.etBranch.setText(credential.branch)
    }

    private fun setupPaymentTypeSpinner() {
        val paymentTypes = arrayOf(PAYMENT_TYPE_BANK_ACCOUNT, PAYMENT_TYPE_CREDIT_CARD)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, paymentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaymentType.adapter = adapter
        
        binding.spinnerPaymentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = paymentTypes[position]
                toggleFormFields(selectedType)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupCardTypeSpinner() {
        val cardTypes = resources.getStringArray(R.array.card_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cardTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCardType.adapter = adapter
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.payment_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }
    
    private fun toggleFormFields(paymentType: String) {
        when (paymentType) {
            PAYMENT_TYPE_BANK_ACCOUNT -> {
                binding.layoutBankAccount.visibility = View.VISIBLE
                binding.layoutCreditCard.visibility = View.GONE
                clearCreditCardFields()
            }
            PAYMENT_TYPE_CREDIT_CARD -> {
                binding.layoutBankAccount.visibility = View.GONE
                binding.layoutCreditCard.visibility = View.VISIBLE
                clearBankAccountFields()
            }
        }
    }
    
    private fun clearBankAccountFields() {
        binding.etBankName.setText("")
        binding.etAccountHolderName.setText("")
        binding.etAccountNumber.setText("")
        binding.etBranch.setText("")
        binding.etRoutingNumber.setText("")
    }
    
    private fun clearCreditCardFields() {
        binding.etCardholderName.setText("")
        binding.etCardNumber.setText("")
        binding.etExpirationDate.setText("")
        binding.etCvv.setText("")
        binding.spinnerCardType.setSelection(0)
        binding.etCardBankName.setText("")
    }

    private fun setupExpirationDateFormatting() {
        binding.etExpirationDate.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private val maxLength = 5

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true

                val str = s.toString().replace("/", "")
                val digits = str.filter { it.isDigit() }
                val formatted = StringBuilder()

                for (i in digits.indices) {
                    if (i >= 4) break
                    if (i == 2) formatted.append("/")
                    formatted.append(digits[i])
                }

                s.replace(0, s.length, formatted.toString())
                isFormatting = false
            }
        })
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            savePaymentCredential()
        }
    }

    private fun setupObservers() {
        viewModel.operationResult.observe(this) { result ->
            result.onSuccess { 
                Toast.makeText(this, "Banking credential saved successfully", Toast.LENGTH_SHORT).show()
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
            supportActionBar?.title = "Edit Banking Credential"
            viewModel.loadCredential(credentialId)
        } else {
            supportActionBar?.title = "Add Banking Credential"
            // Default to bank account
            binding.spinnerPaymentType.setSelection(0)
        }
    }

    private fun populateFields(credential: DecryptedCredential) {
        // Set payment type based on existing data
        val paymentType = credential.paymentType ?: PAYMENT_TYPE_BANK_ACCOUNT
        val paymentTypeAdapter = binding.spinnerPaymentType.adapter as ArrayAdapter<String>
        val paymentTypePosition = paymentTypeAdapter.getPosition(paymentType)
        if (paymentTypePosition >= 0) {
            binding.spinnerPaymentType.setSelection(paymentTypePosition)
        }
        
        // Populate fields based on payment type
        when (paymentType) {
            PAYMENT_TYPE_BANK_ACCOUNT -> {
                binding.etBankName.setText(credential.bankName)
                binding.etAccountHolderName.setText(credential.cardholderName) // Using cardholderName for account holder
                binding.etAccountNumber.setText(credential.accountNumber)
                binding.etBranch.setText(credential.branch)
                binding.etRoutingNumber.setText(credential.routingNumber)
            }
            PAYMENT_TYPE_CREDIT_CARD -> {
                binding.etCardholderName.setText(credential.cardholderName)
                binding.etCardNumber.setText(credential.cardNumber)
                binding.etExpirationDate.setText(credential.expirationDate)
                binding.etCvv.setText(credential.cvv)
                binding.etCardBankName.setText(credential.bankName)
                
                // Set card type
                val cardType = credential.cardType ?: ""
                val cardTypeAdapter = binding.spinnerCardType.adapter as ArrayAdapter<String>
                val cardTypePosition = cardTypeAdapter.getPosition(cardType)
                if (cardTypePosition >= 0) {
                    binding.spinnerCardType.setSelection(cardTypePosition)
                }
            }
        }
        
        // Common fields
        binding.etInternetBankingUsername.setText(credential.internetBankingUsername)
        binding.etInternetBankingPassword.setText(credential.internetBankingPassword)
        binding.etNotes.setText(credential.notes)

        
        // Set category
        val categoryAdapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
        val categoryPosition = categoryAdapter.getPosition(credential.category)
        if (categoryPosition >= 0) {
            binding.spinnerCategory.setSelection(categoryPosition)
        }
    }

    private fun savePaymentCredential() {
        val paymentType = binding.spinnerPaymentType.selectedItem.toString()
        val internetBankingUsername = binding.etInternetBankingUsername.text.toString().trim()
        val internetBankingPassword = binding.etInternetBankingPassword.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()


        var hasError = false
        var title = ""
        var bankName = ""
        var cardholderName = ""
        var accountNumber = ""
        var cardNumber = ""
        var expirationDate = ""
        var cvv = ""
        var routingNumber = ""
        var branch = ""
        var cardType = ""

        when (paymentType) {
            PAYMENT_TYPE_BANK_ACCOUNT -> {
                bankName = binding.etBankName.text.toString().trim()
                cardholderName = binding.etAccountHolderName.text.toString().trim() // Account holder name
                accountNumber = binding.etAccountNumber.text.toString().trim()
                branch = binding.etBranch.text.toString().trim()
                routingNumber = binding.etRoutingNumber.text.toString().trim()
                title = "$bankName - Account"
                
                // Validation for bank account
                if (bankName.isEmpty()) {
                    binding.tilBankName.error = "Bank name is required"
                    hasError = true
                } else {
                    binding.tilBankName.error = null
                }
                
                if (cardholderName.isEmpty()) {
                    binding.tilAccountHolderName.error = "Account holder name is required"
                    hasError = true
                } else {
                    binding.tilAccountHolderName.error = null
                }
                
                if (accountNumber.isEmpty()) {
                    binding.tilAccountNumber.error = "Account number is required"
                    hasError = true
                } else {
                    binding.tilAccountNumber.error = null
                }
            }
            
            PAYMENT_TYPE_CREDIT_CARD -> {
                cardholderName = binding.etCardholderName.text.toString().trim()
                cardNumber = binding.etCardNumber.text.toString().trim()
                expirationDate = binding.etExpirationDate.text.toString().trim()
                cvv = binding.etCvv.text.toString().trim()
                cardType = binding.spinnerCardType.selectedItem.toString()
                bankName = binding.etCardBankName.text.toString().trim()
                title = "$cardType Card"
                
                // Validation for credit card
                if (cardholderName.isEmpty()) {
                    binding.tilCardholderName.error = "Cardholder name is required"
                    hasError = true
                } else {
                    binding.tilCardholderName.error = null
                }
                
                if (cardNumber.isEmpty()) {
                    binding.tilCardNumber.error = "Card number is required"
                    hasError = true
                } else if (cardNumber.length < 13) {
                    binding.tilCardNumber.error = "Invalid card number"
                    hasError = true
                } else {
                    binding.tilCardNumber.error = null
                }
                
                if (expirationDate.isEmpty()) {
                    binding.tilExpirationDate.error = "Expiration date is required"
                    hasError = true
                } else {
                    binding.tilExpirationDate.error = null
                }
                
                if (cvv.isEmpty()) {
                    binding.tilCvv.error = "CVV is required"
                    hasError = true
                } else if (cvv.length < 3 || cvv.length > 4) {
                    binding.tilCvv.error = "Invalid CVV"
                    hasError = true
                } else {
                    binding.tilCvv.error = null
                }
            }
        }
        
        if (hasError) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val existingCredential = if (isEditMode) viewModel.credential.value else null

        val credential = DecryptedCredential(
            id = if (isEditMode) credentialId else 0,
            title = title,
            paymentType = paymentType,
            bankName = bankName,
            cardholderName = cardholderName,
            accountNumber = accountNumber,
            cardNumber = cardNumber,
            expirationDate = expirationDate,
            cvv = cvv,
            routingNumber = routingNumber,
            branch = branch,
            cardType = cardType,
            internetBankingUsername = internetBankingUsername,
            internetBankingPassword = internetBankingPassword,
            notes = notes,
            category = category,
            isFavorite = false,
            tags = emptyList(),
            type = CredentialType.PAYMENT,
            createdAt = existingCredential?.createdAt ?: currentTime,
            updatedAt = currentTime
        )

        if (isEditMode) {
            viewModel.updateCredential(credential)
        } else {
            viewModel.addCredential(credential)
        }
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
                savePaymentCredential()
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

    private fun shareCredential() {
        val paymentType = binding.spinnerPaymentType.selectedItem.toString()
        val category = binding.spinnerCategory.selectedItem.toString()
        val notes = binding.etNotes.text.toString()

        val shareText = buildString {
            appendLine("🔐 AeroPass Credentials")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            
            when (paymentType) {
                PAYMENT_TYPE_BANK_ACCOUNT -> {
                    val bankName = binding.etBankName.text.toString()
                    val accountHolder = binding.etAccountHolderName.text.toString()
                    appendLine("Type: Payment")
                    if (accountHolder.isNotEmpty()) appendLine("Title: $accountHolder")
                    if (paymentType.isNotEmpty()) appendLine("Payment Type: $paymentType")
                    val accountNumber = binding.etAccountNumber.text.toString()
                    val branch = binding.etBranch.text.toString()
                    val routingNumber = binding.etRoutingNumber.text.toString()
                    
                    if (bankName.isNotEmpty()) appendLine("Bank Name: $bankName")
                    if (accountHolder.isNotEmpty()) appendLine("Account Holder: $accountHolder")
                    if (accountNumber.isNotEmpty()) appendLine("Account Number: $accountNumber")
                    if (branch.isNotEmpty()) appendLine("Branch: $branch")
                    if (routingNumber.isNotEmpty()) appendLine("Routing Number: $routingNumber")
                }
                PAYMENT_TYPE_CREDIT_CARD -> {
                    val cardholderName = binding.etCardholderName.text.toString()
                    val cardNumber = binding.etCardNumber.text.toString()
                    val expirationDate = binding.etExpirationDate.text.toString()
                    val cvv = binding.etCvv.text.toString()
                    val cardBankName = binding.etCardBankName.text.toString()
                    val cardType = binding.spinnerCardType.selectedItem.toString()
                    
                    appendLine("Type: Payment")
                    if (cardholderName.isNotEmpty()) appendLine("Title: $cardholderName")
                    if (paymentType.isNotEmpty()) appendLine("Payment Type: $paymentType")
                    if (cardholderName.isNotEmpty()) appendLine("Cardholder Name: $cardholderName")
                    if (cardNumber.isNotEmpty()) appendLine("Card Number: $cardNumber")
                    if (expirationDate.isNotEmpty()) appendLine("Expiration Date: $expirationDate")
                    if (cvv.isNotEmpty()) appendLine("CVV: $cvv")
                    if (cardBankName.isNotEmpty()) appendLine("Bank Name: $cardBankName")
                    if (cardType.isNotEmpty()) appendLine("Card Type: $cardType")
                }
            }
            
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
            putExtra(Intent.EXTRA_SUBJECT, "Payment Credential: $paymentType")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Payment Credential"))
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Banking Credential")
            .setMessage("Are you sure you want to delete this Banking credential? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCredential(credentialId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}