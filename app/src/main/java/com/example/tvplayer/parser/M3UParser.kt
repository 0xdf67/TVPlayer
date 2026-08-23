package com.example.tvplayer.parser

import com.example.tvplayer.data.Channel
import java.net.URL

object M3UParser {

    fun parse(content: String): List<Channel> {
        val lines = content.lines()
        if (lines.isEmpty() || !lines[0].trim().uppercase().startsWith("#EXTM3U")) {
            return emptyList()
        }

        val channels = mutableListOf<Channel>()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup = ""
        var currentEpgId: String? = null
        var number = 1

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXTINF:") -> {
                    val info = trimmed.removePrefix("#EXTINF:").trim()
                    currentName = extractAttribute(info, "tvg-name")
                        ?: extractAttribute(info, "title")
                        ?: info.substringAfterLast(",", "Canal $number").trim()
                    currentLogo = extractAttribute(info, "tvg-logo")
                    currentGroup = extractAttribute(info, "group-title") ?: ""
                    currentEpgId = extractAttribute(info, "tvg-id")
                }
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    val streamUrl = trimmed
                    if (isValidUrl(streamUrl)) {
                        channels.add(
                            Channel(
                                id = number,
                                number = number,
                                name = currentName.ifEmpty { "Canal $number" },
                                logoUrl = currentLogo,
                                streamUrl = streamUrl,
                                group = currentGroup,
                                epgId = currentEpgId
                            )
                        )
                        number++
                    }
                    currentName = ""
                    currentLogo = null
                    currentGroup = ""
                    currentEpgId = null
                }
            }
        }

        return channels
    }

    private fun extractAttribute(info: String, key: String): String? {
        val regex = "$key=\"([^\"]*)\"".toRegex()
        return regex.find(info)?.groupValues?.get(1)
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            url.startsWith("http://") || url.startsWith("https://") || url.startsWith("rtmp://") || url.startsWith("udp://")
        } catch (e: Exception) {
            false
        }
    }
}
