package com.example.tvplayer.data

data class EpgData(
    val programs: Map<String, List<EpgProgram>> = emptyMap(),
    val channelIcons: Map<String, String?> = emptyMap()
)
