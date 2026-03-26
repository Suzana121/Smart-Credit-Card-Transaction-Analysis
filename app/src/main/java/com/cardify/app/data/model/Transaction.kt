package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("id") val id: String = "",
    @SerializedName("businessName") val businessName: String = "Unknown",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("date") val date: String = "",
    @SerializedName("category") val category: String = "General",
    @SerializedName("status") val status: String = "REGULAR"
)