package com.example.tvplayer

import androidx.multidex.MultiDexApplication
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgData

class TvPlayerApplication : MultiDexApplication() {
    var currentChannels: List<Channel> = emptyList()
    var currentChannelIndex: Int = -1
    var epgData: EpgData = EpgData()

    companion object {
        lateinit var instance: TvPlayerApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
