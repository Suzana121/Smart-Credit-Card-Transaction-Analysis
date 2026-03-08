package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("userID", alternate = ["id", "ID"])
    val id: String? = null,

    @SerializedName("businessName", alternate = ["merchant", "Description", "business_name", "BusinessName", "Business Name"])
    val businessName: String? = "Unknown Business",

    @SerializedName("amount", alternate = ["Amount", "Value", "Price"])
    val amount: Double? = 0.0,

    @SerializedName("date", alternate = ["Date", "Transaction Date"])
    val date: String? = "",

    @SerializedName("status", alternate = ["Status", "state"])
    val status: String? = "REGULAR"
)