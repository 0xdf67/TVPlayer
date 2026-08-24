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
import java.util.Calendar
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
    private lateinit var dateSelectorContainer: LinearLayout
    private lateinit var tvDateSelector: TextView

    private lateinit var epgAdapter: EpgAdapter
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    private val dateLabelFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var selectedDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var currentChannels: List<Pair<Channel, List<EpgProgram>>> = emptyList()

    private val windowStart: Long
        get() {
            val hour = 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val selectedStart = selectedDate.timeInMillis
            return if (selectedStart == todayStart) {
                (now / hour) * hour
            } else {
                selectedStart
            }
        }

    private val windowEnd: Long
        get() = windowStart + 2 * 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg)

        bindViews()
        setupRecyclerView()
        setupDateSelector()
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
        dateSelectorContainer = findViewById(R.id.dateSelectorContainer)
        tvDateSelector = findViewById(R.id.tvDateSelector)
    }

    private fun setupRecyclerView() {
        epgAdapter = EpgAdapter { channel, _ ->
            openPlayer(channel)
        }
        rvEpg.adapter = epgAdapter
        rvEpg.layoutManager = LinearLayoutManager(this)
        rvEpg.setHasFixedSize(true)
        
        // Add scroll listener to update Featured Banner when scrolling
        rvEpg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateFeaturedBannerFromVisibleChannel()
            }
        })
    }

    private fun setupDateSelector() {
        dateSelectorContainer.setOnClickListener { showDatePicker() }
    }

    private fun adjustRowHeightForFiveChannels() {
        rvEpg.post {
            val recyclerHeight = rvEpg.height
            if (recyclerHeight > 0) {
                val rowHeight = recyclerHeight / 5
                epgAdapter.setRowHeight(rowHeight)
            }
        }
    }

    private fun showDatePicker() {
        val allPrograms = TvPlayerApplication.instance.epgData.programs.values.flatten()
        val dates = allPrograms.map { getDayKey(it.startTime) }.distinct().sorted()
        if (dates.size <= 1) return

        val labels = dates.map { formatDateLabel(it) }.toTypedArray()
        val currentKey = getDayKey(selectedDate.timeInMillis)
        val selectedIndex = dates.indexOf(currentKey).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.today)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                val parts = dates[which].split("-")
                selectedDate = Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                updateDateSelectorLabel()
                loadData()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateDateSelectorLabel() {
        val todayKey = getDayKey(System.currentTimeMillis())
        val selectedKey = getDayKey(selectedDate.timeInMillis)
        tvDateSelector.text = if (selectedKey == todayKey) {
            getString(R.string.today)
        } else {
            dateFormat.format(selectedDate.timeInMillis)
        }
    }

    private fun getDayKey(timeMillis: Long): String {
        return dateLabelFormat.format(timeMillis)
    }

    private fun formatDateLabel(dayKey: String): String {
        val parts = dayKey.split("-")
        val cal = Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        return dateFormat.format(cal.timeInMillis)
    }

    private fun isSameDay(timeMillis: Long, day: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return cal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    }

    private fun filterProgramsByDay(programs: List<EpgProgram>): List<EpgProgram> {
        return programs.filter { isSameDay(it.startTime, selectedDate) }
    }

    private fun loadData() {
        val channels = TvPlayerApplication.instance.currentChannels
        val epgData = TvPlayerApplication.instance.epgData

        val items = channels.map { channel ->
            val allPrograms = epgData.programs[channel.epgId ?: channel.name] ?: emptyList()
            channel to filterProgramsByDay(allPrograms).sortedBy { it.startTime }
        }
        epgAdapter.setWindow(windowStart, windowEnd)
        epgAdapter.submitList(items)
        currentChannels = items
        adjustRowHeightForFiveChannels()
        populateTimeline()
        updateDateSelectorLabel()

        val now = System.currentTimeMillis()
        updateFeaturedBanner(items, now)

        rvEpg.post {
            rvEpg.requestFocus()
        }
    }

    private fun updateFeaturedBanner(items: List<Pair<Channel, List<EpgProgram>>>, currentTime: Long) {
        val featured = if (isSameDay(currentTime, selectedDate)) {
            items.firstOrNull { (_, programs) ->
                programs.any { it.startTime <= currentTime && it.endTime > currentTime }
            }
        } else {
            items.firstOrNull { (_, programs) -> programs.isNotEmpty() }
        }

        featured?.let { (channel, programs) ->
            val program = if (isSameDay(currentTime, selectedDate)) {
                programs.first { it.startTime <= currentTime && it.endTime > currentTime }
            } else {
                programs.first()
            }
            tvFeaturedChannel.text = channel.name
            tvFeaturedTitle.text = program.title
            tvFeaturedTime.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"
            tvFeaturedDesc.text = program.description ?: ""
            tvFeaturedDesc.visibility = if (program.description.isNullOrBlank()) View.GONE else View.VISIBLE

            val total = program.endTime - program.startTime
            val current = if (isSameDay(currentTime, selectedDate)) currentTime - program.startTime else 0L
            featuredProgress.max = total.toInt()
            featuredProgress.progress = current.toInt().coerceIn(0, featuredProgress.max)

            // Use program iconUrl first, fallback to channel logoUrl from XMLTV data
            val imageUrl = program.iconUrl ?: channel.logoUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .into(ivFeatured)
            }
        }
    }

    private fun updateFeaturedBannerFromVisibleChannel() {
        val layoutManager = rvEpg.layoutManager as? LinearLayoutManager ?: return
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        
        if (firstVisiblePosition >= 0 && firstVisiblePosition < currentChannels.size) {
            val (channel, programs) = currentChannels[firstVisiblePosition]
            val now = System.currentTimeMillis()
            
            val program = programs.firstOrNull { 
                it.startTime <= now && it.endTime > now 
            } ?: programs.firstOrNull() ?: return
            
            tvFeaturedChannel.text = channel.name
            tvFeaturedTitle.text = program.title
            tvFeaturedTime.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"
            tvFeaturedDesc.text = program.description ?: ""
            tvFeaturedDesc.visibility = if (program.description.isNullOrBlank()) View.GONE else View.VISIBLE

            val total = program.endTime - program.startTime
            val current = if (isSameDay(now, selectedDate)) now - program.startTime else 0L
            featuredProgress.max = total.toInt()
            featuredProgress.progress = current.toInt().coerceIn(0, featuredProgress.max)

            // Use program iconUrl first, fallback to channel logoUrl from XMLTV data
            val imageUrl = program.iconUrl ?: channel.logoUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .into(ivFeatured)
            }
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
