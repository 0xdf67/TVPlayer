package com.example.tvplayer.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tvplayer.R
import com.example.tvplayer.TvPlayerApplication
import com.example.tvplayer.data.EpgRepository
import com.example.tvplayer.data.PlaylistRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var btnConfigure: Button
    private lateinit var progressBar: ProgressBar

    private val playlistRepository = PlaylistRepository()
    private val epgRepository = EpgRepository()

    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnConfigure = findViewById(R.id.btnConfigure)
        progressBar = findViewById(R.id.progressBar)

        btnConfigure.setOnClickListener { showSetupDialog() }

        val savedM3u = prefs.getString(KEY_M3U_URL, null)
        if (!savedM3u.isNullOrBlank()) {
            openEpg()
            finish()
            return
        }

        btnConfigure.requestFocus()
    }

    private fun showSetupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_setup, null)
        val etM3uUrl: EditText = dialogView.findViewById(R.id.etM3uUrl)
        val etEpgUrl: EditText = dialogView.findViewById(R.id.etEpgUrl)
        val btnLoad: Button = dialogView.findViewById(R.id.btnLoad)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCancel)

        val dialog = AlertDialog.Builder(this, R.style.SetupDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnLoad.setOnClickListener {
            val m3uUrl = etM3uUrl.text.toString().trim()
            val epgUrl = etEpgUrl.text.toString().trim()

            if (!validateInputs(m3uUrl, epgUrl, etM3uUrl, etEpgUrl)) {
                return@setOnClickListener
            }

            dialog.dismiss()
            loadPlaylistAndEpg(m3uUrl, epgUrl)
        }

        dialog.setOnShowListener {
            etM3uUrl.requestFocus()
        }

        dialog.show()
    }

    private fun validateInputs(
        m3uUrl: String,
        epgUrl: String,
        etM3uUrl: EditText,
        etEpgUrl: EditText
    ): Boolean {
        var valid = true

        if (m3uUrl.isEmpty()) {
            etM3uUrl.error = getString(R.string.setup_error_empty_m3u)
            valid = false
        } else if (!isValidUrl(m3uUrl)) {
            etM3uUrl.error = getString(R.string.setup_error_invalid_m3u)
            valid = false
        } else {
            etM3uUrl.error = null
        }

        if (epgUrl.isNotEmpty() && !isValidUrl(epgUrl)) {
            etEpgUrl.error = getString(R.string.setup_error_invalid_xmltv)
            valid = false
        } else {
            etEpgUrl.error = null
        }

        return valid
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true)
    }

    private fun loadPlaylistAndEpg(m3uUrl: String, epgUrl: String) {
        showLoading(true)
        lifecycleScope.launch {
            val playlistResult = playlistRepository.loadPlaylist(m3uUrl)
            playlistResult.onSuccess { channels ->
                if (channels.isEmpty()) {
                    showLoading(false)
                    showError(getString(R.string.setup_error_no_channels))
                    return@onSuccess
                }

                TvPlayerApplication.instance.currentChannels = channels

                val epgData = if (epgUrl.isNotEmpty()) {
                    epgRepository.loadEpg(epgUrl).getOrNull() ?: epgRepository.generateSampleEpg(channels)
                } else {
                    epgRepository.generateSampleEpg(channels)
                }
                TvPlayerApplication.instance.epgData = epgData

                saveConfiguration(m3uUrl, epgUrl)
                showLoading(false)
                openEpg()
                finish()
            }.onFailure { error ->
                showLoading(false)
                showError(error.message ?: getString(R.string.error_loading))
            }
        }
    }

    private fun saveConfiguration(m3uUrl: String, epgUrl: String) {
        prefs.edit()
            .putString(KEY_M3U_URL, m3uUrl)
            .putString(KEY_EPG_URL, epgUrl)
            .apply()
    }

    private fun openEpg() {
        startActivity(Intent(this, EpgActivity::class.java))
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfigure.isEnabled = !show
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.error_loading)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishAffinity()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val PREFS_NAME = "tvplayer_setup"
        private const val KEY_M3U_URL = "m3u_url"
        private const val KEY_EPG_URL = "epg_url"

        fun clearConfiguration(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}
