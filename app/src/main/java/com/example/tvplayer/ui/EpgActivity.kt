package com.example.tvplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvplayer.R
import com.example.tvplayer.TvPlayerApplication
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgProgram
import java.text.SimpleDateFormat
import java.util.Locale

class EpgActivity : AppCompatActivity() {

    private lateinit var rvEpg: RecyclerView
    private lateinit var ivFeatured: ImageView
    private lateinit var tvFeaturedChannel: TextView
    private lateinit var tvFeaturedTitle: TextView
    private lateinit var tvFeaturedTime: TextView
    private lateinit var tvFeaturedDesc: TextView
    private lateinit var featuredProgress: ProgressBar
    private lateinit var timelineContainer: LinearLayout

    private lateinit var epgAdapter: EpgAdapter
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val windowStart: Long
        get() {
            val hour = 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            return (now / hour) * hour
        }

    private val windowEnd: Long
        get() = windowStart + 2 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg)

        bindViews()
        setupRecyclerView()
        loadData()
    }

    private fun bindViews() {
        rvEpg = findViewById(R.id.rvEpg)
        ivFeatured = findViewById(R.id.ivFeatured)
        tvFeaturedChannel = findViewById(R.id.tvFeaturedChannel)
        tvFeaturedTitle = findViewById(R.id.tvFeaturedTitle)
        tvFeaturedTime = findViewById(R.id.tvFeaturedTime)
        tvFeaturedDesc = findViewById(R.id.tvFeaturedDesc)
        featuredProgress = findViewById(R.id.featuredProgress)
        timelineContainer = findViewById(R.id.timelineContainer)
    }

    private fun setupRecyclerView() {
        epgAdapter = EpgAdapter(windowStart, windowEnd) { channel, _ ->
            openPlayer(channel)
        }
        rvEpg.adapter = epgAdapter
        rvEpg.layoutManager = LinearLayoutManager(this)
        rvEpg.setHasFixedSize(true)
    }

    private fun loadData() {
        val channels = TvPlayerApplication.instance.currentChannels
        val epgData = TvPlayerApplication.instance.epgData

        val items = channels.map { channel ->
            channel to (epgData[channel.epgId ?: channel.name] ?: emptyList())
        }
        epgAdapter.submitList(items)
        populateTimeline()

        val now = System.currentTimeMillis()
        val featured = items.firstOrNull { (_, programs) ->
            programs.any { it.startTime <= now && it.endTime > now }
        }

        featured?.let { (channel, programs) ->
            val program = programs.first { it.startTime <= now && it.endTime > now }
            tvFeaturedChannel.text = channel.name
            tvFeaturedTitle.text = program.title
            tvFeaturedTime.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"
            tvFeaturedDesc.text = program.description ?: ""
            tvFeaturedDesc.visibility = if (program.description.isNullOrBlank()) View.GONE else View.VISIBLE

            val total = program.endTime - program.startTime
            val current = now - program.startTime
            featuredProgress.max = total.toInt()
            featuredProgress.progress = current.toInt()

            if (!channel.logoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(channel.logoUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .into(ivFeatured)
            }
        }

        rvEpg.post {
            rvEpg.requestFocus()
        }
    }

    private fun populateTimeline() {
        timelineContainer.removeAllViews()
        val start = windowStart
        val interval = 30 * 60 * 1000L
        for (i in 0 until 4) {
            val time = start + (i * interval)
            val textView = TextView(this).apply {
                text = timeFormat.format(time)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            timelineContainer.addView(textView)
        }
    }

    private fun openPlayer(channel: Channel) {
        val channels = TvPlayerApplication.instance.currentChannels
        val index = channels.indexOfFirst { it.id == channel.id }
        TvPlayerApplication.instance.currentChannelIndex = index

        val intent = Intent(this, PlayerActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finishAffinity()
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                showResetConfigDialog()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showResetConfigDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.setup_clear)
            .setMessage(R.string.setup_clear_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                MainActivity.clearConfiguration(this)
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
