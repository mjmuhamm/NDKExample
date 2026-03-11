package com.example.ndkexample.domain

data class BenckmarkStats(
    val mode: String = "",
    val firstRun: Long = 0L,
    val warmAvg: Long = 0L,
    val isNative : Boolean = false

)
