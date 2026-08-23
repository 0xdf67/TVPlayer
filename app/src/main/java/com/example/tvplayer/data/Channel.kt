package com.example.tvplayer.data

import java.io.Serializable

data class Channel(
    val id: Int,
    val number: Int,
    val name: String,
    val logoUrl: String?,
    val streamUrl: String,
    val group: String = "",
    val epgId: String? = null
) : Serializable
