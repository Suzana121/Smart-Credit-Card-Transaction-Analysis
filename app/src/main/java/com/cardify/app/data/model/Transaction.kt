package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

data class Transaction(
    val id: String? = null,

    // הוספנו את "merchant" לרשימת השמות האפשריים!
    @SerializedName("businessName", alternate = ["merchant", "Description", "business_name", "BusinessName"])
    val businessName: String? = "Unknown Business",

    @SerializedName("amount", alternate = ["Amount"])
    val amount: Double? = 0.0,

    @SerializedName("date", alternate = ["Date"])
    val date: String? = "",

    @SerializedName("status", alternate = ["Status"])
    val status: String? = "REGULAR"

)