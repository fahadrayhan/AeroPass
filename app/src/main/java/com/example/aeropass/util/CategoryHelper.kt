package com.example.aeropass.util

import com.example.aeropass.R

object CategoryHelper {
    
    /**
     * Get the appropriate icon resource for a given category
     */
    fun getCategoryIcon(category: String): Int {
        return when (category.lowercase()) {
            "general" -> R.drawable.ic_category_general
            "social media" -> R.drawable.ic_category_social
            "banking & finance", "banking", "finance" -> R.drawable.ic_category_banking
            "e-commerce & shopping", "shopping" -> R.drawable.ic_category_shopping
            "work & business", "work", "business" -> R.drawable.ic_category_work
            "entertainment & media", "entertainment" -> R.drawable.ic_category_entertainment
            "travel & transportation", "travel" -> R.drawable.ic_category_travel
            "education & learning", "education" -> R.drawable.ic_category_education
            "health & fitness", "health" -> R.drawable.ic_category_health
            "technology & development", "technology" -> R.drawable.ic_category_technology
            "government & legal", "government" -> R.drawable.ic_category_government
            "utilities & services", "utilities" -> R.drawable.ic_category_utilities
            else -> R.drawable.ic_category_other
        }
    }
    
    /**
     * Get category color based on category type
     */
    fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "general" -> R.color.category_general
            "social media" -> R.color.category_social
            "banking & finance", "banking", "finance" -> R.color.category_banking
            "e-commerce & shopping", "shopping" -> R.color.category_shopping
            "work & business", "work", "business" -> R.color.category_work
            "entertainment & media", "entertainment" -> R.color.category_entertainment
            "travel & transportation", "travel" -> R.color.category_travel
            "education & learning", "education" -> R.color.category_education
            "health & fitness", "health" -> R.color.category_health
            "technology & development", "technology" -> R.color.category_technology
            "government & legal", "government" -> R.color.category_government
            "utilities & services", "utilities" -> R.color.category_utilities
            else -> R.color.category_other
        }
    }
    
    /**
     * Get suggested categories based on credential content
     */
    fun getSuggestedCategory(title: String, url: String = "", notes: String = ""): String {
        val content = "$title $url $notes".lowercase()
        
        return when {
            content.contains("bank") || content.contains("finance") || content.contains("investment") -> "Banking & Finance"
            content.contains("facebook") || content.contains("twitter") || content.contains("instagram") || 
            content.contains("linkedin") || content.contains("social") -> "Social Media"
            content.contains("amazon") || content.contains("ebay") || content.contains("shop") || 
            content.contains("store") || content.contains("buy") -> "E-commerce & Shopping"
            content.contains("work") || content.contains("office") || content.contains("company") || 
            content.contains("business") -> "Work & Business"
            content.contains("netflix") || content.contains("youtube") || content.contains("spotify") || 
            content.contains("entertainment") || content.contains("media") -> "Entertainment & Media"
            content.contains("travel") || content.contains("flight") || content.contains("hotel") || 
            content.contains("booking") -> "Travel & Transportation"
            content.contains("school") || content.contains("university") || content.contains("education") || 
            content.contains("course") || content.contains("learning") -> "Education & Learning"
            content.contains("health") || content.contains("medical") || content.contains("fitness") || 
            content.contains("doctor") || content.contains("hospital") -> "Health & Fitness"
            content.contains("github") || content.contains("code") || content.contains("dev") || 
            content.contains("api") || content.contains("tech") -> "Technology & Development"
            content.contains("government") || content.contains("legal") || content.contains("tax") || 
            content.contains("official") -> "Government & Legal"
            content.contains("utility") || content.contains("service") || content.contains("electric") || 
            content.contains("gas") || content.contains("water") -> "Utilities & Services"
            else -> "General"
        }
    }
}