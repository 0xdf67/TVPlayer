package com.example.tvplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class EpgRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val xmlTvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    suspend fun loadEpg(url: String): Result<EpgData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
            val (programs, channelIcons) = parseXmlTv(body)
            Result.success(EpgData(programs, channelIcons))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateSampleEpg(channels: List<Channel>): EpgData {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val programs = mutableMapOf<String, List<EpgProgram>>()
        val channelIcons = mutableMapOf<String, String?>()

        channels.forEachIndexed { index, channel ->
            val key = channel.epgId ?: channel.name
            val channelPrograms = mutableListOf<EpgProgram>()
            val baseOffset = (index % 4) * 15 * 60 * 1000L
            val titles = listOf(
                "Morning News",
                "Documentary Special",
                "Live Sports",
                "Evening Show",
                "Movie Premiere",
                "Late Night Talk"
            )

            for (i in -2..4) {
                val start = now + baseOffset + (i * hour)
                val end = start + hour
                channelPrograms.add(
                    EpgProgram(
                        channelId = key,
                        title = titles[(index + i).mod(titles.size)],
                        description = "Programa em ${channel.name}",
                        startTime = start,
                        endTime = end
                    )
                )
            }
            programs[key] = channelPrograms
            channelIcons[key] = channel.logoUrl
        }

        return EpgData(programs, channelIcons)
    }

    private fun parseXmlTv(xml: String): Pair<Map<String, List<EpgProgram>>, Map<String, String?>> {
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()
        val channelIcons = mutableMapOf<String, String?>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentChannelId = ""
        var currentTitle = ""
        var currentDesc: String? = null
        var currentStart = 0L
        var currentEnd = 0L
        var currentIcon: String? = null
        var currentTag = ""
        var insideChannel = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    when (currentTag) {
                        "channel" -> {
                            insideChannel = true
                            currentChannelId = parser.getAttributeValue(null, "id") ?: ""
                            currentIcon = null
                        }
                        "programme" -> {
                            insideChannel = false
                            currentChannelId = parser.getAttributeValue(null, "channel") ?: ""
                            currentStart = parseXmlTvDate(parser.getAttributeValue(null, "start") ?: "")
                            currentEnd = parseXmlTvDate(parser.getAttributeValue(null, "stop") ?: "")
                            currentTitle = ""
                            currentDesc = null
                            currentIcon = null
                        }
                        "icon" -> {
                            currentIcon = parser.getAttributeValue(null, "src")
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    when (currentTag) {
                        "title" -> if (!insideChannel) currentTitle = parser.text ?: ""
                        "desc" -> if (!insideChannel) currentDesc = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            if (currentChannelId.isNotEmpty()) {
                                channelIcons[currentChannelId] = currentIcon
                            }
                            insideChannel = false
                            currentChannelId = ""
                        }
                        "programme" -> {
                            if (currentChannelId.isNotEmpty()) {
                                programs.getOrPut(currentChannelId) { mutableListOf() }.add(
                                    EpgProgram(
                                        channelId = currentChannelId,
                                        title = currentTitle,
                                        description = currentDesc,
                                        startTime = currentStart,
                                        endTime = currentEnd,
                                        iconUrl = currentIcon
                                    )
                                )
                            }
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return programs to channelIcons
    }

    private fun parseXmlTvDate(value: String): Long {
        return try {
            xmlTvDateFormat.parse(value)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
