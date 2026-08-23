package com.example.tvplayer

import android.app.Application
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgProgram

class TvPlayerApplication : Application() {
    var currentChannels: List<Channel> = emptyList()
    var currentChannelIndex: Int = -1
    var epgData: Map<String, List<EpgProgram>> = emptyMap()

    companion object {
        lateinit var instance: TvPlayerApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
