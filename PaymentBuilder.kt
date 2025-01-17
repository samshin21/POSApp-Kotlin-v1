package com.example.app

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.app.printer.*
import com.example.app.terminal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class PaymentBuilder(private val context: Context) {
    private val client = OkHttpClient()
    private val url = "https://215sales.com/samshin/test.php"

    fun processCashPayment(journalEntries: List<ReceiptItem>, callback: (String?) -> Unit) {
        //Initialize HttpClient
        makeHttpCall("cash") { orderNumber ->
            if (orderNumber != null) {
                printReceipt(orderNumber, "CASH", journalEntries)
            } else {
                callback(null)
            }
        }
    }

    fun processCardPayment(journalEntries: List<ReceiptItem>, callback: (String?) -> Unit) {
        //Initialize PosLinkClient
        val posLinkClient = PosLinkClient(
            ipAddress = "192.168.1.191",
            port = "10009"
        )
        // Use the IO dispatcher for network/IO-heavy operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1) Calculate the total (including tax) and convert to cents
                val totalAmount = journalEntries.sumOf { it.price.toDouble() + it.tax.toDouble() }
                val formattedAmount = (totalAmount * 100).toInt().toString()

                // 3) Set up the PosLink & PaymentRequest
                val posLink = posLinkClient.setupPosLink()
                val paymentRequest = posLinkClient.createPaymentRequest(formattedAmount)
                posLink.PaymentRequest = paymentRequest

                // 4) Process the transaction
                //    (Note: make sure PosLinkClient.processTransaction()
                //     uses 'posLink' internally or accepts 'posLink' as a parameter)
                posLinkClient.processTransaction(posLink)

                // 5) If transaction is successful, call your HTTP function and print the receipt
                makeHttpCall("card") { orderNumber ->
                    if (orderNumber != null) {
                        printReceipt(orderNumber, "CARD", journalEntries)
                    } else {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                // Handle errors on the main thread (e.g., show Toast, log, etc.)
                withContext(Dispatchers.Main) {
                    println("Error processing transaction: ${e.message}")
                }
            }
        }
    }

    private fun makeHttpCall(paymentType: String, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val formBody = FormBody.Builder()
                .add("instruction", "getOrderNumber")
                .add("paymentType", paymentType)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    withContext(Dispatchers.Main) {
                        Log.d("PaymentManager", "Order number received: $responseBody")
                        callback(responseBody)
                    }
                }
            } catch (e: Exception) {
                Log.e("PaymentManager", "HTTP call failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "HTTP error: ${e.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            }
        }
    }

    private fun printReceipt(orderNumber: String, paymentType: String, journalEntries: List<ReceiptItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val printerClient = PrinterClient("192.168.1.251", 9100)
                printerClient.connect()

                val receiptBuilder = ReceiptBuilder(journalEntries)
                receiptBuilder.printReceipt(printerClient, orderNumber, paymentType)

                printerClient.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Receipt printed successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PaymentManager", "Error printing receipt: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error printing receipt: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
