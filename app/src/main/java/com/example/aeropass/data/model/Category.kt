package com.example.aeropass.data.model

enum class CredentialCategory(val displayName: String, val icon: Int) {
    GENERAL("General", android.R.drawable.ic_menu_info_details),
    SOCIAL("Social Media", android.R.drawable.ic_menu_share),
    BANKING("Banking", android.R.drawable.ic_menu_save),
    WORK("Work", android.R.drawable.ic_menu_agenda),
    SHOPPING("Shopping", android.R.drawable.ic_menu_add),
    ENTERTAINMENT("Entertainment", android.R.drawable.ic_menu_slideshow),
    EDUCATION("Education", android.R.drawable.ic_menu_edit),
    HEALTH("Health", android.R.drawable.ic_menu_help),
    TRAVEL("Travel", android.R.drawable.ic_menu_mapmode),
    OTHER("Other", android.R.drawable.ic_menu_sort_by_size);
    
    companion object {
        fun fromString(category: String): CredentialCategory {
            return values().find { it.displayName == category } ?: GENERAL
        }
        
        fun getAllCategories(): List<String> {
            return values().map { it.displayName }
        }
    }
}