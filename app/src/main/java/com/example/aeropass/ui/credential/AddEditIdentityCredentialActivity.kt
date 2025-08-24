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
import com.example.aeropass.databinding.ActivityAddEditIdentityCredentialBinding
import com.example.aeropass.ui.credential.CredentialViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditIdentityCredentialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditIdentityCredentialBinding
    private val viewModel: CredentialViewModel by viewModels()
    private var credentialId: Long = -1
    private var isEditMode = false

    companion object {
        const val EXTRA_CREDENTIAL_ID = "credential_id"
        const val DOCUMENT_TYPE_NATIONAL_ID = "National ID"
        const val DOCUMENT_TYPE_PASSPORT = "Passport"
        const val DOCUMENT_TYPE_DRIVING_LICENSE = "Driving License"
        const val DOCUMENT_TYPE_BIRTH_CERTIFICATE = "Birth Certificate"
        
        fun newIntent(context: Context, credentialId: Long = -1L): Intent {
            return Intent(context, AddEditIdentityCredentialActivity::class.java).apply {
                if (credentialId != -1L) putExtra(EXTRA_CREDENTIAL_ID, credentialId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditIdentityCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        handleIntent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Identity Information" else "Add Identity Information"
    }

    private fun setupViews() {
        setupDocumentTypeSpinner()
        setupCategorySpinners()
        setupCopyDocumentButton()
        setupSaveButton()
        // Autofill functionality removed
    }

    private fun setupDocumentTypeSpinner() {
        val documentTypes = resources.getStringArray(R.array.document_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, documentTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDocumentType.adapter = adapter
        
        binding.spinnerDocumentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = documentTypes[position]
                toggleFormFields(selectedType)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCategorySpinners() {
        val categories = resources.getStringArray(R.array.credential_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        // Set up all category spinners
        binding.spinnerNationalIdCategory.adapter = adapter
        binding.spinnerPassportCategory.adapter = adapter
        binding.spinnerDlCategory.adapter = adapter
        binding.spinnerBcCategory.adapter = adapter
    }

    private fun toggleFormFields(documentType: String) {
        // Hide all conditional layouts first
        binding.layoutNationalId.visibility = View.GONE
        binding.layoutPassport.visibility = View.GONE
        binding.layoutDrivingLicense.visibility = View.GONE
        binding.layoutBirthCertificate.visibility = View.GONE
        
        // Clear all conditional fields
        clearNationalIdFields()
        clearPassportFields()
        clearDrivingLicenseFields()
        clearBirthCertificateFields()
        
        // Show relevant layout based on document type
        when (documentType) {
            DOCUMENT_TYPE_NATIONAL_ID -> {
                binding.layoutNationalId.visibility = View.VISIBLE
            }
            DOCUMENT_TYPE_PASSPORT -> {
                binding.layoutPassport.visibility = View.VISIBLE
            }
            DOCUMENT_TYPE_DRIVING_LICENSE -> {
                binding.layoutDrivingLicense.visibility = View.VISIBLE
            }
            DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                binding.layoutBirthCertificate.visibility = View.VISIBLE
            }
        }
    }
    
    private fun clearNationalIdFields() {
        binding.etNationalId.setText("")
        binding.etNidAddress.setText("")
    }
    
    private fun clearPassportFields() {
        binding.etPassportNumber.setText("")
        binding.etDateOfIssue.setText("")
        binding.etDateOfExpiry.setText("")
    }
    
    private fun clearDrivingLicenseFields() {
        binding.etDrivingLicenseNumber.setText("")
        binding.etDlDateOfIssue.setText("")
        binding.etValidity.setText("")
        binding.etIssuingAuthority.setText("")
    }
    
    private fun clearBirthCertificateFields() {
        binding.etBirthCertificateNumber.setText("")
        binding.etBcAddress.setText("")
    }



    private fun setupCopyDocumentButton() {
        // Setup copy button for National ID
        binding.btnCopyNationalId.setOnClickListener {
            val nationalId = binding.etNationalId.text.toString()
            if (nationalId.isNotEmpty()) {
                copyToClipboard("National ID Number", nationalId)
                Toast.makeText(this, "National ID number copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No National ID number to copy", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup copy button for Passport
        binding.btnCopyPassportNumber.setOnClickListener {
            val passportNumber = binding.etPassportNumber.text.toString()
            if (passportNumber.isNotEmpty()) {
                copyToClipboard("Passport Number", passportNumber)
                Toast.makeText(this, "Passport number copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No passport number to copy", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup copy button for Driving License
        binding.btnCopyDrivingLicenseNumber.setOnClickListener {
            val drivingLicenseNumber = binding.etDrivingLicenseNumber.text.toString()
            if (drivingLicenseNumber.isNotEmpty()) {
                copyToClipboard("Driving License Number", drivingLicenseNumber)
                Toast.makeText(this, "Driving license number copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No driving license number to copy", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup copy button for Birth Certificate
        binding.btnCopyBirthCertificateNumber.setOnClickListener {
            val birthCertificateNumber = binding.etBirthCertificateNumber.text.toString()
            if (birthCertificateNumber.isNotEmpty()) {
                copyToClipboard("Birth Certificate Number", birthCertificateNumber)
                Toast.makeText(this, "Birth certificate number copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No birth certificate number to copy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveIdentityCredential()
        }
    }

    private fun setupObservers() {
        viewModel.operationResult.observe(this) { result ->
            result.onSuccess { 
                Toast.makeText(this, "Identity information saved successfully", Toast.LENGTH_SHORT).show()
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
            supportActionBar?.title = "Edit Identity Information"
            viewModel.loadCredential(credentialId)
        } else {
            supportActionBar?.title = "Add Identity Information"
        }
    }

    private fun loadCredential() {
        viewModel.loadCredential(credentialId)
    }

    private fun populateFields(credential: DecryptedCredential) {
        binding.etWebPortal.setText(credential.webPortal)

        
        // Set document type
        val documentTypeAdapter = binding.spinnerDocumentType.adapter as ArrayAdapter<String>
        val documentTypePosition = documentTypeAdapter.getPosition(credential.documentType)
        if (documentTypePosition >= 0) {
            binding.spinnerDocumentType.setSelection(documentTypePosition)
        }
        
        // Populate document-specific fields based on document type
        when (credential.documentType) {
            DOCUMENT_TYPE_NATIONAL_ID -> {
                binding.etFullName.setText(credential.fullName)
                binding.etFathersName.setText(credential.fathersName)
                binding.etMothersName.setText(credential.mothersName)
                binding.etNidDateOfBirth.setText(credential.dateOfBirth)
                binding.etNationalId.setText(credential.nationalId)
                binding.etNidAddress.setText(credential.address)
                binding.etNidNotes.setText(credential.notes)
            }
            DOCUMENT_TYPE_PASSPORT -> {
                binding.etPassportFullName.setText(credential.fullName)
                binding.etPassportFathersName.setText(credential.fathersName)
                binding.etPassportMothersName.setText(credential.mothersName)
                binding.etPassportDateOfBirth.setText(credential.dateOfBirth)
                binding.etPassportNumber.setText(credential.passportNumber)
                binding.etDateOfIssue.setText(credential.dateOfIssue)
                binding.etDateOfExpiry.setText(credential.dateOfExpiry)
                binding.etPassportNotes.setText(credential.notes)
            }
            DOCUMENT_TYPE_DRIVING_LICENSE -> {
                binding.etDlFullName.setText(credential.fullName)
                binding.etDlFathersName.setText(credential.fathersName)
                binding.etDlDateOfBirth.setText(credential.dateOfBirth)
                binding.etDrivingLicenseNumber.setText(credential.drivingLicenseNumber)
                binding.etDlDateOfIssue.setText(credential.dateOfIssue)
                binding.etValidity.setText(credential.validity)
                binding.etIssuingAuthority.setText(credential.issuingAuthority)
                binding.etDlNotes.setText(credential.notes)
            }
            DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                binding.etBcFullName.setText(credential.fullName)
                binding.etBcFathersName.setText(credential.fathersName)
                binding.etBcMothersName.setText(credential.mothersName)
                binding.etBcDateOfBirth.setText(credential.dateOfBirth)
                binding.etBirthCertificateNumber.setText(credential.birthCertificateNumber)
                binding.etBcAddress.setText(credential.address)
                binding.etBcNotes.setText(credential.notes)
            }
        }
        
        // Set category based on document type
         when (credential.documentType) {
             DOCUMENT_TYPE_NATIONAL_ID -> {
                 val categoryAdapter = binding.spinnerNationalIdCategory.adapter as ArrayAdapter<String>
                 val categoryPosition = categoryAdapter.getPosition(credential.category)
                 if (categoryPosition >= 0) {
                     binding.spinnerNationalIdCategory.setSelection(categoryPosition)
                 }
             }
             DOCUMENT_TYPE_PASSPORT -> {
                 val categoryAdapter = binding.spinnerPassportCategory.adapter as ArrayAdapter<String>
                 val categoryPosition = categoryAdapter.getPosition(credential.category)
                 if (categoryPosition >= 0) {
                     binding.spinnerPassportCategory.setSelection(categoryPosition)
                 }
             }
             DOCUMENT_TYPE_DRIVING_LICENSE -> {
                 val categoryAdapter = binding.spinnerDlCategory.adapter as ArrayAdapter<String>
                 val categoryPosition = categoryAdapter.getPosition(credential.category)
                 if (categoryPosition >= 0) {
                     binding.spinnerDlCategory.setSelection(categoryPosition)
                 }
             }
             DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                 val categoryAdapter = binding.spinnerBcCategory.adapter as ArrayAdapter<String>
                 val categoryPosition = categoryAdapter.getPosition(credential.category)
                 if (categoryPosition >= 0) {
                     binding.spinnerBcCategory.setSelection(categoryPosition)
                 }
             }
         }
    }

    private fun saveIdentityCredential() {
        val documentType = binding.spinnerDocumentType.selectedItem.toString()
        val webPortal = binding.etWebPortal.text.toString().trim()
                val notes = ""


        // Get document-type-specific field values
        var fullName = ""
        var fathersName = ""
        var mothersName = ""
        var dateOfBirth = ""
        var category = ""
        var documentSpecificNotes = ""

        when (documentType) {
                     DOCUMENT_TYPE_NATIONAL_ID -> {
                         fullName = binding.etFullName.text.toString().trim()
                         fathersName = binding.etFathersName.text.toString().trim()
                         mothersName = binding.etMothersName.text.toString().trim()
                 dateOfBirth = binding.etNidDateOfBirth.text.toString().trim()
                 category = binding.spinnerNationalIdCategory.selectedItem.toString()
                 documentSpecificNotes = binding.etNidNotes.text.toString().trim()
             }
             DOCUMENT_TYPE_PASSPORT -> {
                 fullName = binding.etPassportFullName.text.toString().trim()
                 fathersName = binding.etPassportFathersName.text.toString().trim()
                 mothersName = binding.etPassportMothersName.text.toString().trim()
                 dateOfBirth = binding.etPassportDateOfBirth.text.toString().trim()
                 category = binding.spinnerPassportCategory.selectedItem.toString()
                 documentSpecificNotes = binding.etPassportNotes.text.toString().trim()
             }
             DOCUMENT_TYPE_DRIVING_LICENSE -> {
                 fullName = binding.etDlFullName.text.toString().trim()
                 fathersName = binding.etDlFathersName.text.toString().trim()
                 dateOfBirth = binding.etDlDateOfBirth.text.toString().trim()
                 category = binding.spinnerDlCategory.selectedItem.toString()
                 documentSpecificNotes = binding.etDlNotes.text.toString().trim()
             }
             DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                 fullName = binding.etBcFullName.text.toString().trim()
                 fathersName = binding.etBcFathersName.text.toString().trim()
                 mothersName = binding.etBcMothersName.text.toString().trim()
                 dateOfBirth = binding.etBcDateOfBirth.text.toString().trim()
                 category = binding.spinnerBcCategory.selectedItem.toString()
                 documentSpecificNotes = binding.etBcNotes.text.toString().trim()
             }
         }

        // Validation
        var hasError = false
        
        if (fullName.isEmpty()) {
            // Set error on the appropriate full name field based on document type
            when (documentType) {
                DOCUMENT_TYPE_NATIONAL_ID -> {
                    binding.tilFullName.error = "Full name is required"
                }
                DOCUMENT_TYPE_PASSPORT -> {
                    binding.tilPassportFullName.error = "Full name is required"
                }
                DOCUMENT_TYPE_DRIVING_LICENSE -> {
                    binding.tilDlFullName.error = "Full name is required"
                }
                DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                    binding.tilBcFullName.error = "Full name is required"
                }
            }
            hasError = true
        } else {
            // Clear error on all full name fields
            binding.tilFullName.error = null
            binding.tilPassportFullName.error = null
            binding.tilDlFullName.error = null
            binding.tilBcFullName.error = null
        }
        
        // Validate document-specific required fields
        when (documentType) {
            DOCUMENT_TYPE_NATIONAL_ID -> {
                val nationalId = binding.etNationalId.text.toString().trim()
                if (nationalId.isEmpty()) {
                    binding.tilNationalId.error = "National ID number is required"
                    hasError = true
                } else {
                    binding.tilNationalId.error = null
                }
            }
            DOCUMENT_TYPE_PASSPORT -> {
                val passportNumber = binding.etPassportNumber.text.toString().trim()
                if (passportNumber.isEmpty()) {
                    binding.tilPassportNumber.error = "Passport number is required"
                    hasError = true
                } else {
                    binding.tilPassportNumber.error = null
                }
            }
            DOCUMENT_TYPE_DRIVING_LICENSE -> {
                val drivingLicenseNumber = binding.etDrivingLicenseNumber.text.toString().trim()
                if (drivingLicenseNumber.isEmpty()) {
                    binding.tilDrivingLicenseNumber.error = "Driving license number is required"
                    hasError = true
                } else {
                    binding.tilDrivingLicenseNumber.error = null
                }
            }
            DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                val birthCertificateNumber = binding.etBirthCertificateNumber.text.toString().trim()
                if (birthCertificateNumber.isEmpty()) {
                    binding.tilBirthCertificateNumber.error = "Birth certificate number is required"
                    hasError = true
                } else {
                    binding.tilBirthCertificateNumber.error = null
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
            title = fullName, // Use fullName for backward compatibility
            fullName = fullName,
            fathersName = fathersName,
            mothersName = mothersName,
            dateOfBirth = dateOfBirth,
            documentType = documentType,
            // Document-specific fields
            nationalId = if (documentType == DOCUMENT_TYPE_NATIONAL_ID) binding.etNationalId.text.toString().trim() else "",
            passportNumber = if (documentType == DOCUMENT_TYPE_PASSPORT) binding.etPassportNumber.text.toString().trim() else "",
            drivingLicenseNumber = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etDrivingLicenseNumber.text.toString().trim() else "",
            birthCertificateNumber = if (documentType == DOCUMENT_TYPE_BIRTH_CERTIFICATE) binding.etBirthCertificateNumber.text.toString().trim() else "",
            dateOfIssue = when (documentType) {
                DOCUMENT_TYPE_PASSPORT -> binding.etDateOfIssue.text.toString().trim()
                DOCUMENT_TYPE_DRIVING_LICENSE -> binding.etDlDateOfIssue.text.toString().trim()
                else -> ""
            },
            dateOfExpiry = if (documentType == DOCUMENT_TYPE_PASSPORT) binding.etDateOfExpiry.text.toString().trim() else "",
            validity = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etValidity.text.toString().trim() else "",
            issuingAuthority = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etIssuingAuthority.text.toString().trim() else "",
            address = when (documentType) {
                DOCUMENT_TYPE_NATIONAL_ID -> binding.etNidAddress.text.toString().trim()
                DOCUMENT_TYPE_BIRTH_CERTIFICATE -> binding.etBcAddress.text.toString().trim()
                else -> ""
            },
            webPortal = webPortal,
            category = category,
            notes = notes,
            isFavorite = false,
            tags = emptyList(), // Tags removed
            type = CredentialType.IDENTITY,
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
                saveIdentityCredential()
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
    
    // Autofill field suggestion removed
    
    private fun fillFormWithSuggestion(credential: DecryptedCredential) {
        // Set document type in spinner
        val documentTypes = resources.getStringArray(R.array.document_types)
        val documentTypeIndex = documentTypes.indexOf(credential.documentType)
        if (documentTypeIndex >= 0) {
            binding.spinnerDocumentType.setSelection(documentTypeIndex)
        }
        
        // Populate document-specific fields
        when (credential.documentType) {
            DOCUMENT_TYPE_NATIONAL_ID -> {
                binding.etFullName.setText(credential.fullName)
                binding.etFathersName.setText(credential.fathersName)
                binding.etMothersName.setText(credential.mothersName)
                binding.etNidDateOfBirth.setText(credential.dateOfBirth)
                binding.etNationalId.setText(credential.nationalId)
                binding.etNidAddress.setText(credential.address)
                binding.etNidNotes.setText(credential.notes)
                
                val categories = resources.getStringArray(R.array.credential_categories)
                val categoryIndex = categories.indexOf(credential.category)
                if (categoryIndex >= 0) {
                    binding.spinnerNationalIdCategory.setSelection(categoryIndex)
                }
            }
            DOCUMENT_TYPE_PASSPORT -> {
                binding.etPassportFullName.setText(credential.fullName)
                binding.etPassportFathersName.setText(credential.fathersName)
                binding.etPassportMothersName.setText(credential.mothersName)
                binding.etPassportDateOfBirth.setText(credential.dateOfBirth)
                binding.etPassportNumber.setText(credential.passportNumber)
                binding.etDateOfIssue.setText(credential.dateOfIssue)
                binding.etDateOfExpiry.setText(credential.dateOfExpiry)
                binding.etPassportNotes.setText(credential.notes)
                
                val categories = resources.getStringArray(R.array.credential_categories)
                val categoryIndex = categories.indexOf(credential.category)
                if (categoryIndex >= 0) {
                    binding.spinnerPassportCategory.setSelection(categoryIndex)
                }
            }
            DOCUMENT_TYPE_DRIVING_LICENSE -> {
                binding.etDlFullName.setText(credential.fullName)
                binding.etDlFathersName.setText(credential.fathersName)
                binding.etDlDateOfBirth.setText(credential.dateOfBirth)
                binding.etDrivingLicenseNumber.setText(credential.drivingLicenseNumber)
                binding.etDlDateOfIssue.setText(credential.dateOfIssue)
                binding.etValidity.setText(credential.validity)
                binding.etIssuingAuthority.setText(credential.issuingAuthority)
                binding.etDlNotes.setText(credential.notes)
                
                val categories = resources.getStringArray(R.array.credential_categories)
                val categoryIndex = categories.indexOf(credential.category)
                if (categoryIndex >= 0) {
                    binding.spinnerDlCategory.setSelection(categoryIndex)
                }
            }
            DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                binding.etBcFullName.setText(credential.fullName)
                binding.etBcFathersName.setText(credential.fathersName)
                binding.etBcMothersName.setText(credential.mothersName)
                binding.etBcDateOfBirth.setText(credential.dateOfBirth)
                binding.etBirthCertificateNumber.setText(credential.birthCertificateNumber)
                binding.etBcAddress.setText(credential.address)
                binding.etBcNotes.setText(credential.notes)
                
                val categories = resources.getStringArray(R.array.credential_categories)
                val categoryIndex = categories.indexOf(credential.category)
                if (categoryIndex >= 0) {
                    binding.spinnerBcCategory.setSelection(categoryIndex)
                }
            }
        }

        binding.etWebPortal.setText(credential.webPortal)
    }

    private fun shareCredential() {
        val documentType = binding.spinnerDocumentType.selectedItem.toString()
        val webPortal = binding.etWebPortal.text.toString()

        val shareText = buildString {
            appendLine("🔐 AeroPass Credentials")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            
            when (documentType) {
                DOCUMENT_TYPE_NATIONAL_ID -> {
                    val fullName = binding.etFullName.text.toString()
                    appendLine("Type: Identity Document")
                    if (documentType.isNotEmpty()) appendLine("Document Type: $documentType")
                    if (webPortal.isNotEmpty()) appendLine("Web Portal: $webPortal")
                    val fathersName = binding.etFathersName.text.toString()
                    val mothersName = binding.etMothersName.text.toString()
                    val dateOfBirth = binding.etNidDateOfBirth.text.toString()
                    val documentNumber = binding.etNationalId.text.toString()
                    val address = binding.etNidAddress.text.toString()
                    val notes = binding.etNidNotes.text.toString()
                    
                    if (fullName.isNotEmpty()) appendLine("Full Name: $fullName")
                    if (fathersName.isNotEmpty()) appendLine("Father's Name: $fathersName")
                    if (mothersName.isNotEmpty()) appendLine("Mother's Name: $mothersName")
                    if (dateOfBirth.isNotEmpty()) appendLine("Date of Birth: $dateOfBirth")
                    if (documentNumber.isNotEmpty()) appendLine("National ID Number: $documentNumber")
                    if (address.isNotEmpty()) appendLine("Address: $address")
                    if (notes.isNotEmpty()) {
                        appendLine("Notes:")
                        appendLine(notes)
                    }
                }
                DOCUMENT_TYPE_PASSPORT -> {
                    val fullName = binding.etPassportFullName.text.toString()
                    appendLine("Type: Identity Document")
                    if (fullName.isNotEmpty()) appendLine("Title: $fullName")
                    if (documentType.isNotEmpty()) appendLine("Document Type: $documentType")
                    if (webPortal.isNotEmpty()) appendLine("Web Portal: $webPortal")
                    val fathersName = binding.etPassportFathersName.text.toString()
                    val mothersName = binding.etPassportMothersName.text.toString()
                    val passportNumber = binding.etPassportNumber.text.toString()
                    val dateOfBirth = binding.etPassportDateOfBirth.text.toString()
                    val dateOfIssue = binding.etDateOfIssue.text.toString()
                    val expiryDate = binding.etDateOfExpiry.text.toString()
                    val category = binding.spinnerPassportCategory.selectedItem.toString()
                    val notes = binding.etPassportNotes.text.toString()
                    
                    if (fullName.isNotEmpty()) appendLine("Full Name: $fullName")
                    if (fathersName.isNotEmpty()) appendLine("Father's Name: $fathersName")
                    if (mothersName.isNotEmpty()) appendLine("Mother's Name: $mothersName")
                    if (passportNumber.isNotEmpty()) appendLine("Passport Number: $passportNumber")
                    if (dateOfBirth.isNotEmpty()) appendLine("Date of Birth: $dateOfBirth")
                    if (dateOfIssue.isNotEmpty()) appendLine("Date of Issue: $dateOfIssue")
                    if (expiryDate.isNotEmpty()) appendLine("Date of Expiry: $expiryDate")
                    if (category.isNotEmpty()) appendLine("Category: $category")
                    if (notes.isNotEmpty()) {
                        appendLine("Notes:")
                        appendLine(notes)
                    }
                }
                DOCUMENT_TYPE_DRIVING_LICENSE -> {
                    val fullName = binding.etDlFullName.text.toString()
                    appendLine("Type: Identity Document")
                    if (documentType.isNotEmpty()) appendLine("Document Type: $documentType")
                    if (webPortal.isNotEmpty()) appendLine("Web Portal: $webPortal")
                    val fathersName = binding.etDlFathersName.text.toString()
                    val licenseNumber = binding.etDrivingLicenseNumber.text.toString()
                    val dateOfBirth = binding.etDlDateOfBirth.text.toString()
                    val dateOfIssue = binding.etDlDateOfIssue.text.toString()
                    val validity = binding.etValidity.text.toString()
                    val issuingAuthority = binding.etIssuingAuthority.text.toString()
                    val category = binding.spinnerDlCategory.selectedItem.toString()
                    val notes = binding.etDlNotes.text.toString()
                    
                    if (fullName.isNotEmpty()) appendLine("Full Name: $fullName")
                    if (fathersName.isNotEmpty()) appendLine("Father's Name: $fathersName")
                    if (licenseNumber.isNotEmpty()) appendLine("License Number: $licenseNumber")
                    if (dateOfBirth.isNotEmpty()) appendLine("Date of Birth: $dateOfBirth")
                    if (dateOfIssue.isNotEmpty()) appendLine("Date of Issue: $dateOfIssue")
                    if (validity.isNotEmpty()) appendLine("Validity: $validity")
                    if (issuingAuthority.isNotEmpty()) appendLine("Issuing Authority: $issuingAuthority")
                    if (category.isNotEmpty()) appendLine("Category: $category")
                    if (notes.isNotEmpty()) {
                        appendLine("Notes:")
                        appendLine(notes)
                    }
                }
                DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                    val fullName = binding.etBcFullName.text.toString()
                    appendLine("Type: Identity Document")
                    if (documentType.isNotEmpty()) appendLine("Document Type: $documentType")
                    if (webPortal.isNotEmpty()) appendLine("Web Portal: $webPortal")
                    val fathersName = binding.etBcFathersName.text.toString()
                    val mothersName = binding.etBcMothersName.text.toString()
                    val certificateNumber = binding.etBirthCertificateNumber.text.toString()
                    val dateOfBirth = binding.etBcDateOfBirth.text.toString()
                    val address = binding.etBcAddress.text.toString()
                    val category = binding.spinnerBcCategory.selectedItem.toString()
                    val notes = binding.etBcNotes.text.toString()
                    
                    if (fullName.isNotEmpty()) appendLine("Full Name: $fullName")
                    if (fathersName.isNotEmpty()) appendLine("Father's Name: $fathersName")
                    if (mothersName.isNotEmpty()) appendLine("Mother's Name: $mothersName")
                    if (certificateNumber.isNotEmpty()) appendLine("Certificate Number: $certificateNumber")
                    if (dateOfBirth.isNotEmpty()) appendLine("Date of Birth: $dateOfBirth")
                    if (address.isNotEmpty()) appendLine("Address: $address")
                    if (category.isNotEmpty()) appendLine("Category: $category")
                    if (notes.isNotEmpty()) {
                        appendLine("Notes:")
                        appendLine(notes)
                    }
                }
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠ Keep this information secure!")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Identity Credential: $documentType")
        }

        startActivity(Intent.createChooser(shareIntent, "Share Identity Credential"))
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Identity Information")
            .setMessage("Are you sure you want to delete this identity information?")
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
                val documentType = binding.spinnerDocumentType.selectedItem.toString()
                val webPortal = binding.etWebPortal.text.toString().trim()
                val notes = ""
                val isFavorite = false

                // Get document-type-specific field values
                var fullName = ""
                var fathersName = ""
                var mothersName = ""
                var dateOfBirth = ""
                var category = ""
                var documentSpecificNotes = ""

                when (documentType) {
                     DOCUMENT_TYPE_NATIONAL_ID -> {
                         fullName = binding.etFullName.text.toString().trim()
                         fathersName = binding.etFathersName.text.toString().trim()
                         mothersName = binding.etMothersName.text.toString().trim()
                         dateOfBirth = binding.etNidDateOfBirth.text.toString().trim()
                         category = binding.spinnerNationalIdCategory.selectedItem.toString()
                         documentSpecificNotes = binding.etNidNotes.text.toString().trim()
                     }
                     DOCUMENT_TYPE_PASSPORT -> {
                         fullName = binding.etPassportFullName.text.toString().trim()
                         fathersName = binding.etPassportFathersName.text.toString().trim()
                         mothersName = binding.etPassportMothersName.text.toString().trim()
                         dateOfBirth = binding.etPassportDateOfBirth.text.toString().trim()
                         category = binding.spinnerPassportCategory.selectedItem.toString()
                         documentSpecificNotes = binding.etPassportNotes.text.toString().trim()
                     }
                     DOCUMENT_TYPE_DRIVING_LICENSE -> {
                         fullName = binding.etDlFullName.text.toString().trim()
                         fathersName = binding.etDlFathersName.text.toString().trim()
                         dateOfBirth = binding.etDlDateOfBirth.text.toString().trim()
                         category = binding.spinnerDlCategory.selectedItem.toString()
                         documentSpecificNotes = binding.etDlNotes.text.toString().trim()
                     }
                     DOCUMENT_TYPE_BIRTH_CERTIFICATE -> {
                         fullName = binding.etBcFullName.text.toString().trim()
                         fathersName = binding.etBcFathersName.text.toString().trim()
                         mothersName = binding.etBcMothersName.text.toString().trim()
                         dateOfBirth = binding.etBcDateOfBirth.text.toString().trim()
                         category = binding.spinnerBcCategory.selectedItem.toString()
                         documentSpecificNotes = binding.etBcNotes.text.toString().trim()
                     }
                 }

                val currentTime = System.currentTimeMillis()

                val credential = DecryptedCredential(
                    id = existingCredentialId,
                    title = fullName, // Use fullName for backward compatibility
                    fullName = fullName,
                    fathersName = fathersName,
                    mothersName = mothersName,
                    dateOfBirth = dateOfBirth,
                    documentType = documentType,
                    // Document-specific fields
                    nationalId = if (documentType == DOCUMENT_TYPE_NATIONAL_ID) binding.etNationalId.text.toString().trim() else "",
                    passportNumber = if (documentType == DOCUMENT_TYPE_PASSPORT) binding.etPassportNumber.text.toString().trim() else "",
                    drivingLicenseNumber = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etDrivingLicenseNumber.text.toString().trim() else "",
                    birthCertificateNumber = if (documentType == DOCUMENT_TYPE_BIRTH_CERTIFICATE) binding.etBirthCertificateNumber.text.toString().trim() else "",
                    dateOfIssue = when (documentType) {
                        DOCUMENT_TYPE_PASSPORT -> binding.etDateOfIssue.text.toString().trim()
                        DOCUMENT_TYPE_DRIVING_LICENSE -> binding.etDlDateOfIssue.text.toString().trim()
                        else -> ""
                    },
                    dateOfExpiry = if (documentType == DOCUMENT_TYPE_PASSPORT) binding.etDateOfExpiry.text.toString().trim() else "",
                    validity = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etValidity.text.toString().trim() else "",
                    issuingAuthority = if (documentType == DOCUMENT_TYPE_DRIVING_LICENSE) binding.etIssuingAuthority.text.toString().trim() else "",
                    address = when (documentType) {
                        DOCUMENT_TYPE_NATIONAL_ID -> binding.etNidAddress.text.toString().trim()
                        DOCUMENT_TYPE_BIRTH_CERTIFICATE -> binding.etBcAddress.text.toString().trim()
                        else -> ""
                    },
                    webPortal = webPortal,
                    category = category,
                    notes = notes,
                    isFavorite = isFavorite,
                    tags = emptyList(), // Tags removed
                    type = CredentialType.IDENTITY,
                    createdAt = viewModel.credential.value?.createdAt ?: currentTime,
                    updatedAt = currentTime
                )
                viewModel.updateCredential(credential)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}