package com.khairul.stockkereta

data class Car(
    val id: String = "",
    val model: String = "",
    val year: String = "",
    val plate: String = "",
    val runner: String = "",
    val capital: Double = 0.0,
    val costs: Double = 0.0,
    val salePrice: Double = 0.0,
    val status: String = "STOK"
) {
    val totalCost: Double get() = capital + costs
    val profit: Double get() = if (status == "TERJUAL") salePrice - totalCost else 0.0
}
