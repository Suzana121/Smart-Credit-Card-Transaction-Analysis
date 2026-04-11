package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single credit card transaction returned from the transactions API.
 *
 * @property id Unique server-assigned transaction identifier.
 * @property businessName Name of the merchant or business where the charge occurred.
 * @property amount Transaction amount in NIS (₪).
 * @property date Date of the transaction as returned by the server (ISO-style string).
 * @property category Spending category (e.g. `"Food"`, `"Shopping"`, `"Transport"`).
 * @property status ML-assigned classification: `"REGULAR"` for expected charges,
 *   `"IRREGULAR"` for suspicious or anomalous charges.
 */
data class Transaction(
    @SerializedName("id") val id: String = "",
    @SerializedName("businessName") val businessName: String = "Unknown",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("date") val date: String = "",
    @SerializedName("category") val category: String = "General",
    @SerializedName("status") val status: String = "REGULAR"
)
