package com.example.app

data class ReceiptItem(
    val itemName: String, // Name of the item
    val price: String,    // Price as a formatted string (e.g., "10.00")
    val tax: String       // Tax as a formatted string (e.g., "0.60")
)
