package com.jdeguzman.coop2demo

data class StoreLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "" // optional, if using auth
)
