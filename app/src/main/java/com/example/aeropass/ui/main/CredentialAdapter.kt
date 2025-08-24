package com.example.aeropass.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.aeropass.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aeropass.data.model.Credential
import com.example.aeropass.data.model.DecryptedCredential
import com.example.aeropass.databinding.ItemCredentialBinding
import java.text.SimpleDateFormat
import java.util.*

class CredentialAdapter(
    private val onItemClick: (Credential) -> Unit,
    private val onCopyPassword: (Credential) -> Unit,
    private val onStartActionMode: () -> Unit,
    private val decryptCredential: (Credential) -> DecryptedCredential?
) : ListAdapter<Credential, CredentialAdapter.CredentialViewHolder>(CredentialDiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    var isSelectionMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val binding = ItemCredentialBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CredentialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedItems(): Set<Long> {
        return selectedItems
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    inner class CredentialViewHolder(private val binding: ItemCredentialBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(credential: Credential) {
            binding.apply {
                tvWebsiteName.text = credential.title
                
                // Decrypt credential to show proper details
                val decrypted = decryptCredential(credential)
                
                when (credential.credentialType) {
                    com.example.aeropass.data.model.CredentialType.LOGIN -> {
                        val usernameText = if (decrypted?.username?.isNotEmpty() == true && decrypted?.email?.isNotEmpty() == true) {
                            "${decrypted.username} | ${decrypted.email}"
                        } else if (decrypted?.username?.isNotEmpty() == true) {
                            decrypted.username
                        } else if (decrypted?.email?.isNotEmpty() == true) {
                            decrypted.email
                        } else {
                            "No username/email"
                        }
                        tvUsername.text = usernameText
                    }
                    com.example.aeropass.data.model.CredentialType.PAYMENT -> {
                        val displayText = if (decrypted?.internetBankingUsername?.isNotEmpty() == true) {
                            "${decrypted.cardholderName ?: "No name"} | ${decrypted.internetBankingUsername}"
                        } else {
                            decrypted?.cardholderName ?: "No cardholder name"
                        }
                        tvUsername.text = displayText
                    }
                    com.example.aeropass.data.model.CredentialType.IDENTITY -> {
                        val nameText = buildString {
                            append(decrypted?.fullName ?: "No name")
                            if (decrypted?.fathersName?.isNotEmpty() == true) {
                                append(" | Father: ${decrypted.fathersName}")
                            }
                            if (decrypted?.mothersName?.isNotEmpty() == true) {
                                append(" | Mother: ${decrypted.mothersName}")
                            }
                        }
                        tvUsername.text = nameText
                    }
                    com.example.aeropass.data.model.CredentialType.SECURE_NOTES -> {
                        val displayText = buildString {
                            if (decrypted?.secretKey?.isNotEmpty() == true) {
                                append("Secret: ${decrypted.secretKey.take(20)}${if (decrypted.secretKey.length > 20) "..." else ""}")
                            }
                            if (decrypted?.noteContent?.isNotEmpty() == true) {
                                if (isNotEmpty()) append(" | ")
                                append("Notes: ${decrypted.noteContent.take(30)}${if (decrypted.noteContent.length > 30) "..." else ""}")
                            }
                            if (isEmpty()) {
                                append("No content")
                            }
                        }
                        tvUsername.text = displayText
                    }
                }

                tvCategory.text = credential.category

                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvLastUpdated.text = "Updated: ${dateFormat.format(Date(credential.lastUpdated))}"

                val category = com.example.aeropass.data.model.CredentialCategory.fromString(credential.category)
                ivCategoryIcon.setImageResource(category.icon)

                // Favorite indicator removed from layout

                if (selectedItems.contains(credential.id)) {
                    root.setBackgroundColor(ContextCompat.getColor(root.context, R.color.colorSelectedItemBackground))
                } else {
                    root.setBackgroundColor(Color.TRANSPARENT)
                }

                root.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(credential.id)
                    } else {
                        onItemClick(credential)
                    }
                }

                root.setOnLongClickListener {
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        onStartActionMode()
                    }
                    toggleSelection(credential.id)
                    true
                }

                btnCopyPassword.setOnClickListener {
                    onCopyPassword(credential)
                }
            }
        }

        private fun toggleSelection(credentialId: Long) {
            if (selectedItems.contains(credentialId)) {
                selectedItems.remove(credentialId)
            } else {
                selectedItems.add(credentialId)
            }
            notifyItemChanged(adapterPosition)
        }
    }

    class CredentialDiffCallback : DiffUtil.ItemCallback<Credential>() {
        override fun areItemsTheSame(oldItem: Credential, newItem: Credential): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Credential, newItem: Credential): Boolean {
            return oldItem == newItem
        }
    }
}