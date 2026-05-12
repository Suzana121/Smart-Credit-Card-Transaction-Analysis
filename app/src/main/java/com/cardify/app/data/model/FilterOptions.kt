package com.cardify.app.data.model

import com.google.gson.annotations.SerializedName

val STATIC_CATEGORIES = listOf(
    "Food",
    "Shopping",
    "Transport",
    "Finance",
    "Other",
    "Health",
    "Education",
)

data class FilterOptions(
    @SerializedName("categories") val categories: List<String> = STATIC_CATEGORIES,
    @SerializedName("minAmount")  val minAmount: Double = 0.0,
    @SerializedName("maxAmount")  val maxAmount: Double = 5000.0
)

data class ActiveFilters(
    val selectedCategories: Set<String> = emptySet(),
    val transactionType: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null
) {
    val isEmpty: Boolean get() =
        selectedCategories.isEmpty() && transactionType == null &&
                minAmount == null && maxAmount == null &&
                dateFrom == null && dateTo == null
}