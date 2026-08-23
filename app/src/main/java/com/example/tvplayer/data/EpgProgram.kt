package com.example.tvplayer.data

import java.io.Serializable

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val iconUrl: String? = null
) : Serializable
