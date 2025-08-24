package com.example.aeropass.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aeropass.R
import com.example.aeropass.databinding.ActivityCredentialTypeSelectionBinding
import com.example.aeropass.data.model.CredentialType
import com.example.aeropass.ui.credential.AddEditLoginCredentialActivity
import com.example.aeropass.ui.credential.AddEditPaymentCredentialActivity
import com.example.aeropass.ui.credential.AddEditIdentityCredentialActivity
import com.example.aeropass.ui.credential.AddEditSecureNotesCredentialActivity

class CredentialTypeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCredentialTypeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCredentialTypeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.cardLogin.setOnClickListener {
            navigateToCredentialForm(CredentialType.LOGIN)
        }

        binding.cardPayment.setOnClickListener {
            navigateToCredentialForm(CredentialType.PAYMENT)
        }

        binding.cardIdentity.setOnClickListener {
            navigateToCredentialForm(CredentialType.IDENTITY)
        }

        binding.cardSecureNotes.setOnClickListener {
            navigateToCredentialForm(CredentialType.SECURE_NOTES)
        }
    }

    private fun navigateToCredentialForm(credentialType: CredentialType) {
        val intent = when (credentialType) {
            CredentialType.LOGIN -> Intent(this, AddEditLoginCredentialActivity::class.java)
            CredentialType.PAYMENT -> Intent(this, AddEditPaymentCredentialActivity::class.java)
            CredentialType.IDENTITY -> Intent(this, AddEditIdentityCredentialActivity::class.java)
            CredentialType.SECURE_NOTES -> Intent(this, AddEditSecureNotesCredentialActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}