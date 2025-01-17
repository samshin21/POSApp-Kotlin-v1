package com.example.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var topBarTextView: TextView
    private lateinit var journalListView: ListView
    private lateinit var journalAdapter: ArrayAdapter<String>
    private lateinit var paymentBuilder: PaymentBuilder // Add PaymentManager instance
    private var currentInput: String = ""
    private val journalEntries = mutableListOf<ReceiptItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topBarTextView = findViewById(R.id.topBarTextView)
        journalListView = findViewById(R.id.journalListView)
        val buttonCash = findViewById<Button>(R.id.button_cash)
        val buttonCard = findViewById<Button>(R.id.button_card)

        // Initialize PaymentManager
        paymentBuilder = PaymentBuilder(this)

        //Initialize Journal
        journalAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        journalListView.adapter = journalAdapter

        //Initializing buttons
        setupDigitButtons()
        setupActionButtons()

        // Refactor Cash Button Functionality
        buttonCash.setOnClickListener {
            paymentBuilder.processCashPayment(journalEntries) { orderNumber ->
                if (orderNumber == null) {
                    Toast.makeText(this, "Failed to process cash payment", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    clearJournalAfterPayment()
                }
            }
        }

        // Refactor Card Button Functionality
        buttonCard.setOnClickListener {
            paymentBuilder.processCardPayment(journalEntries) { orderNumber ->
                if (orderNumber == null) {
                    Toast.makeText(this, "Failed to process card payment", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    clearJournalAfterPayment()
                }
            }
        }
    }

    private fun setupDigitButtons() {
        val digitButtons = listOf(
            R.id.button_0,
            R.id.button_1,
            R.id.button_2,
            R.id.button_3,
            R.id.button_4,
            R.id.button_5,
            R.id.button_6,
            R.id.button_7,
            R.id.button_8,
            R.id.button_9
        )
        digitButtons.forEach { btnId ->
            findViewById<Button>(btnId).setOnClickListener {
                currentInput += (it as Button).text
                updateTopBar()
            }
        }
        findViewById<Button>(R.id.button_00).setOnClickListener {
            currentInput += "00"
            updateTopBar()
        }
        findViewById<Button>(R.id.button_clear).setOnClickListener {
            currentInput = ""
            updateTopBar()
        }
    }

    private fun setupActionButtons() {
        findViewById<Button>(R.id.button_no_tax).setOnClickListener {
            addItemToJournal(noTax = true)
        }
        findViewById<Button>(R.id.button_tax).setOnClickListener {
            addItemToJournal(noTax = false)
        }
    }

    private fun updateTopBar() {
        topBarTextView.text = if (currentInput.isEmpty()) {
            getString(R.string.enter_a_number)
        } else {
            formatPrice(currentInput)
        }
    }

    private fun formatPrice(input: String): String {
        val cents = input.toIntOrNull() ?: 0
        val dollars = cents / 100
        val remainderCents = cents % 100
        return String.format(Locale.US, "$%d.%02d", dollars, remainderCents)
    }

    private fun addItemToJournal(noTax: Boolean) {
        if (currentInput.isEmpty()) {
            Toast.makeText(this, "No price entered", Toast.LENGTH_SHORT).show()
            return
        }

        val cents = currentInput.toIntOrNull() ?: 0
        val price = cents / 100.0 // Convert cents to dollars as a Double
        val taxAmount = if (noTax) 0.0 else price * 0.06 // Calculate tax based on price

        // Format price and tax to two decimal places
        val formattedPrice = String.format(Locale.US, "%.2f", price)
        val formattedTax = String.format(Locale.US, "%.2f", taxAmount)

        val itemName = "Item ${journalEntries.size + 1}"
        val item = ReceiptItem(
            itemName = itemName,
            price = formattedPrice,
            tax = formattedTax
        )
        journalEntries.add(item)

        // Display the formatted item details
        journalAdapter.add("$itemName: Price = $$formattedPrice, Tax = $$formattedTax")
        currentInput = ""
        updateTopBar()
    }

    private fun clearJournalAfterPayment() {
        journalEntries.clear()
        journalAdapter.clear()
        currentInput = ""
        updateTopBar()
    }
}
