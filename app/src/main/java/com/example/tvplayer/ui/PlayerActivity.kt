package com.example.tvplayer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tvplayer.R
import com.example.tvplayer.TvPlayerApplication
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgProgram
import java.text.SimpleDateFormat
import java.util.Locale

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNELS = "extra_channels"
        const val EXTRA_START_INDEX = "extra_start_index"
    }

    private lateinit var playerView: PlayerView
    private lateinit var tvChannelBadge: TextView
    private lateinit var tvChannelName: TextView
    private lateinit var tvChannelMeta: TextView
    private lateinit var tvProgramTitle: TextView
    private lateinit var tvTimeRange: TextView
    private lateinit var progressFill: View
    private lateinit var overlayContainer: View
    private lateinit var tvOverlayNumber: TextView
    private lateinit var tvOverlayChannelName: TextView
    private lateinit var tvOverlayProgram: TextView
    private lateinit var tvOverlayTime: TextView

    private var player: ExoPlayer? = null
    private var channels: ArrayList<Channel> = arrayListOf()
    private var currentIndex: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private val overlayHideRunnable = Runnable { hideChannelOverlay() }
    private val controlsHideRunnable = Runnable { hideControls() }
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 60000)
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        channels = intent.getSerializableExtra(EXTRA_CHANNELS) as? ArrayList<Channel> ?: arrayListOf()
        currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        if (channels.isEmpty()) {
            channels = ArrayList(TvPlayerApplication.instance.currentChannels)
            currentIndex = TvPlayerApplication.instance.currentChannelIndex.coerceAtLeast(0)
        }

        bindViews()
        setupButtons()
        initializePlayer()
        playChannel(currentIndex)
    }

    private fun bindViews() {
        playerView = findViewById(R.id.playerView)
        val controls = playerView.findViewById<View>(R.id.controlsContainer)

        tvChannelBadge = controls.findViewById(R.id.tvChannelBadge)
        tvChannelName = controls.findViewById(R.id.tvChannelName)
        tvChannelMeta = controls.findViewById(R.id.tvChannelMeta)
        tvProgramTitle = controls.findViewById(R.id.tvProgramTitle)
        tvTimeRange = controls.findViewById(R.id.tvTimeRange)
        progressFill = controls.findViewById(R.id.progressFill)

        overlayContainer = findViewById(R.id.channelOverlay)
        tvOverlayNumber = findViewById(R.id.tvOverlayNumber)
        tvOverlayChannelName = findViewById(R.id.tvOverlayChannelName)
        tvOverlayProgram = findViewById(R.id.tvOverlayProgram)
        tvOverlayTime = findViewById(R.id.tvOverlayTime)
    }

    private fun setupButtons() {
        val controls = playerView.findViewById<View>(R.id.controlsContainer)
        controls.findViewById<ImageButton>(R.id.btnAudio).setOnClickListener {
            Toast.makeText(this, R.string.audio, Toast.LENGTH_SHORT).show()
        }
        controls.findViewById<ImageButton>(R.id.btnSubtitle).setOnClickListener {
            Toast.makeText(this, R.string.subtitle, Toast.LENGTH_SHORT).show()
        }
        controls.findViewById<ImageButton>(R.id.btnAspect).setOnClickListener {
            toggleAspectRatio()
        }
        controls.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            openEpg()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            updateProgress()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            this@PlayerActivity,
                            "Erro: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }
        playerView.player = player
    }

    private fun playChannel(index: Int) {
        if (index < 0 || index >= channels.size) return
        currentIndex = index
        val channel = channels[currentIndex]
        TvPlayerApplication.instance.currentChannelIndex = currentIndex

        val mediaItem = MediaItem.fromUri(channel.streamUrl)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()

        updateChannelInfo(channel)
        showChannelOverlay(channel)
        updateProgress()
    }

    private fun updateChannelInfo(channel: Channel) {
        tvChannelBadge.text = extractShortName(channel.name)
        tvChannelName.text = channel.name
        tvChannelMeta.text = channel.group.ifEmpty { getString(R.string.live) }

        val now = System.currentTimeMillis()
        val program = findCurrentProgram(channel)
        if (program != null) {
            tvProgramTitle.text = program.title
            tvTimeRange.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"
        } else {
            tvProgramTitle.text = getString(R.string.live)
            tvTimeRange.text = timeFormat.format(now)
        }
    }

    private fun findCurrentProgram(channel: Channel): EpgProgram? {
        val epgData = TvPlayerApplication.instance.epgData
        val key = channel.epgId ?: channel.name
        val programs = epgData.programs[key] ?: return null
        val now = System.currentTimeMillis()
        return programs.find { it.startTime <= now && it.endTime > now }
    }

    private fun updateProgress() {
        val channel = channels.getOrNull(currentIndex) ?: return
        val program = findCurrentProgram(channel)
        if (program != null) {
            val total = program.endTime - program.startTime
            val current = System.currentTimeMillis() - program.startTime
            val progress = (current.toFloat() / total).coerceIn(0f, 1f)
            val params = progressFill.layoutParams as FrameLayout.LayoutParams
            val parent = progressFill.parent as? View
            val parentWidth = parent?.measuredWidth ?: 0
            params.width = (parentWidth * progress).toInt()
            progressFill.layoutParams = params
        }
    }

    private fun showChannelOverlay(channel: Channel) {
        overlayContainer.visibility = View.VISIBLE
        tvOverlayNumber.text = channel.number.toString()
        tvOverlayChannelName.text = channel.name

        val program = findCurrentProgram(channel)
        if (program != null) {
            tvOverlayProgram.text = program.title
            tvOverlayTime.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"
        } else {
            tvOverlayProgram.text = getString(R.string.live)
            tvOverlayTime.text = timeFormat.format(System.currentTimeMillis())
        }

        handler.removeCallbacks(overlayHideRunnable)
        handler.postDelayed(overlayHideRunnable, 3000)
    }

    private fun hideChannelOverlay() {
        overlayContainer.visibility = View.GONE
    }

    private fun showControls() {
        playerView.showController()
        handler.removeCallbacks(controlsHideRunnable)
        handler.postDelayed(controlsHideRunnable, 5000)
    }

    private fun hideControls() {
        playerView.hideController()
    }

    private fun toggleAspectRatio() {
        val resizeModes = listOf(
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )
        val currentMode = playerView.resizeMode
        val nextMode = resizeModes[(resizeModes.indexOf(currentMode) + 1) % resizeModes.size]
        playerView.resizeMode = nextMode
    }

    private fun openEpg() {
        startActivity(Intent(this, EpgActivity::class.java))
    }

    private fun nextChannel() {
        if (currentIndex < channels.size - 1) {
            playChannel(currentIndex + 1)
        }
    }

    private fun previousChannel() {
        if (currentIndex > 0) {
            playChannel(currentIndex - 1)
        }
    }

    private fun extractShortName(name: String): String {
        return name.take(4).uppercase()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                nextChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                previousChannel()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                showControls()
                return true
            }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> {
                openEpg()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (playerView.isControllerFullyVisible) {
                    hideControls()
                    return true
                }
                // Pressionar Return/Back regressa ao EPG
                openEpg()
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        player?.play()
        handler.post(progressUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        handler.removeCallbacks(progressUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(overlayHideRunnable)
        handler.removeCallbacks(controlsHideRunnable)
        handler.removeCallbacks(progressUpdateRunnable)
        player?.release()
        player = null
    }
}
