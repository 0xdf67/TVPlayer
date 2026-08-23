package com.example.tvplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvplayer.R
import com.example.tvplayer.TvPlayerApplication
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgRepository
import com.example.tvplayer.data.PlaylistRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etM3uUrl: EditText
    private lateinit var etEpgUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var rvChannels: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val playlistRepository = PlaylistRepository()
    private val epgRepository = EpgRepository()
    private lateinit var channelAdapter: ChannelAdapter

    private var currentChannels: List<Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etM3uUrl = findViewById(R.id.etM3uUrl)
        etEpgUrl = findViewById(R.id.etEpgUrl)
        btnLoad = findViewById(R.id.btnLoad)
        rvChannels = findViewById(R.id.rvChannels)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        setupRecyclerView()
        setupInput()

        btnLoad.setOnClickListener { loadPlaylist() }

        val savedChannels = TvPlayerApplication.instance.currentChannels
        if (savedChannels.isNotEmpty()) {
            currentChannels = savedChannels
            showChannels(savedChannels)
            etM3uUrl.setText("Playlist carregada")
        }
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter { channel ->
            openPlayer(channel)
        }
        rvChannels.adapter = channelAdapter
        rvChannels.layoutManager = GridLayoutManager(this, calculateSpanCount())
        rvChannels.setHasFixedSize(true)
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 320).coerceAtLeast(2)
    }

    private fun setupInput() {
        etM3uUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loadPlaylist()
                true
            } else {
                false
            }
        }

        etM3uUrl.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                loadPlaylist()
                true
            } else {
                false
            }
        }
    }

    private fun loadPlaylist() {
        val url = etM3uUrl.text.toString().trim()
        if (url.isEmpty()) {
            etM3uUrl.error = getString(R.string.invalid_url)
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            val result = playlistRepository.loadPlaylist(url)
            showLoading(false)

            result.onSuccess { channels ->
                currentChannels = channels
                TvPlayerApplication.instance.currentChannels = channels
                loadEpgForChannels(channels)
                showChannels(channels)
            }.onFailure { error ->
                showError(error.message ?: getString(R.string.error_loading))
            }
        }
    }

    private fun loadEpgForChannels(channels: List<Channel>) {
        lifecycleScope.launch {
            val epgUrl = etEpgUrl.text.toString().trim()
            val epgData = if (epgUrl.isNotEmpty()) {
                epgRepository.loadEpg(epgUrl).getOrNull() ?: epgRepository.generateSampleEpg(channels)
            } else {
                epgRepository.generateSampleEpg(channels)
            }
            TvPlayerApplication.instance.epgData = epgData
        }
    }

    private fun showChannels(channels: List<Channel>) {
        channelAdapter.submitList(channels)
        tvEmpty.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
        if (channels.isNotEmpty()) {
            rvChannels.post {
                rvChannels.requestFocus()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLoad.isEnabled = !show
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.error_loading)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun openPlayer(channel: Channel) {
        val index = currentChannels.indexOfFirst { it.id == channel.id }
        TvPlayerApplication.instance.currentChannelIndex = index

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_CHANNELS, ArrayList(currentChannels))
            putExtra(PlayerActivity.EXTRA_START_INDEX, index)
        }
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO) {
            if (currentChannels.isNotEmpty()) {
                startActivity(Intent(this, EpgActivity::class.java))
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
