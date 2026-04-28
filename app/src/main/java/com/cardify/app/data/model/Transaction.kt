package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

data class TransactionPage(
    @SerializedName("transactions") val transactions: List<Transaction> = emptyList(),
    @SerializedName("nextCursor")   val nextCursor: String? = null,
    @SerializedName("hasMore")      val hasMore: Boolean = false
)

data class Transaction(
    @SerializedName("id")                val id: String = "",
    @SerializedName("businessName")      val businessName: String = "Unknown",
    @SerializedName("amount")            val amount: Double = 0.0,
    @SerializedName("date")              val date: String = "",
    @SerializedName("category")          val category: String = "General",
    @SerializedName("status")            val status: String = "REGULAR",
    @SerializedName("explanation")       val explanation: String = "",
    @SerializedName("anomaly_score")     val anomalyScore: Double = 0.0,
    @SerializedName("currency")          val currency: String = "ILS",
    @SerializedName("original_amount")   val originalAmount: Double = 0.0,
    @SerializedName("original_currency") val originalCurrency: String = "ILS",
    @SerializedName("file_id")           val fileId: String? = null   // ← חדש
)

data class UploadedFile(
    @SerializedName("id")               val id: String = "",
    @SerializedName("fileName")         val fileName: String = "",
    @SerializedName("uploadedAt")       val uploadedAt: String = "",
    @SerializedName("transactionCount") val transactionCount: Int = 0,
    @SerializedName("irregularCount")   val irregularCount: Int = 0
)