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

    suspend fun loadEpg(url: String): Result<Map<String, List<EpgProgram>>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}"))
            }
            val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
            val programs = parseXmlTv(body)
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateSampleEpg(channels: List<Channel>): Map<String, List<EpgProgram>> {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val result = mutableMapOf<String, List<EpgProgram>>()

        channels.forEachIndexed { index, channel ->
            val key = channel.epgId ?: channel.name
            val programs = mutableListOf<EpgProgram>()
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
                programs.add(
                    EpgProgram(
                        channelId = key,
                        title = titles[(index + i).mod(titles.size)],
                        description = "Programa em ${channel.name}",
                        startTime = start,
                        endTime = end
                    )
                )
            }
            result[key] = programs
        }

        return result
    }

    private fun parseXmlTv(xml: String): Map<String, List<EpgProgram>> {
        val programs = mutableMapOf<String, MutableList<EpgProgram>>()
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

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    when (currentTag) {
                        "programme" -> {
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
                        "title" -> currentTitle = parser.text ?: ""
                        "desc" -> currentDesc = parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && currentChannelId.isNotEmpty()) {
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
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return programs
    }

    private fun parseXmlTvDate(value: String): Long {
        return try {
            xmlTvDateFormat.parse(value)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
