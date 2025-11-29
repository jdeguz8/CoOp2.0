package com.jdeguzman.coop2demo

data class LocationNote(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Float = 100f,
    val userId: String = ""
)
