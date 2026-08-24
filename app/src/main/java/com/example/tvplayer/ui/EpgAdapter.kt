package com.example.tvplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tvplayer.R
import com.example.tvplayer.data.Channel
import com.example.tvplayer.data.EpgProgram
import java.text.SimpleDateFormat
import java.util.Locale

class EpgAdapter(
    private val onProgramClick: (Channel, EpgProgram) -> Unit
) : ListAdapter<Pair<Channel, List<EpgProgram>>, EpgAdapter.EpgViewHolder>(EpgDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    var windowStart: Long = 0L
        private set
    var windowEnd: Long = 0L
        private set
    private var rowHeight: Int = 0

    private val windowDuration: Long
        get() = (windowEnd - windowStart).coerceAtLeast(1)

    fun setWindow(start: Long, end: Long) {
        windowStart = start
        windowEnd = end
        notifyDataSetChanged()
    }

    fun setRowHeight(heightPx: Int) {
        rowHeight = heightPx
        notifyDataSetChanged()
    }

    private val channelBackgrounds = listOf(
        R.drawable.bg_channel_blue,
        R.drawable.bg_channel_indigo,
        R.drawable.bg_channel_badge,
        R.drawable.bg_channel_red,
        R.drawable.bg_channel_pink
    )

    private val cardBackgroundColors = listOf(
        R.color.epg_card_teal,
        R.color.epg_card_red,
        R.color.epg_card_brown,
        R.color.epg_card_purple,
        R.color.epg_card_dark_purple,
        R.color.epg_card_green
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg_row, parent, false)
        return EpgViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpgViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class EpgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val channelBlock: FrameLayout = itemView.findViewById(R.id.channelBlock)
        private val ivChannelLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        private val tvChannelNumber: TextView = itemView.findViewById(R.id.tvChannelNumber)
        private val programContainer: LinearLayout = itemView.findViewById(R.id.programContainer)

        fun bind(item: Pair<Channel, List<EpgProgram>>, position: Int) {
            val channel = item.first
            val programs = item.second.sortedBy { it.startTime }

            if (rowHeight > 0) {
                itemView.layoutParams = itemView.layoutParams.apply {
                    height = rowHeight
                }
            }

            channelBlock.setBackgroundResource(channelBackgrounds[position % channelBackgrounds.size])

            if (channel.logoUrl.isNullOrEmpty()) {
                ivChannelLogo.visibility = View.GONE
                tvChannelNumber.visibility = View.VISIBLE
                tvChannelNumber.text = channel.number.toString()
            } else {
                ivChannelLogo.visibility = View.VISIBLE
                tvChannelNumber.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(channel.logoUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .error(R.drawable.bg_control_button)
                    .into(ivChannelLogo)
            }

            programContainer.removeAllViews()

            val visiblePrograms = programs.filter { it.endTime > windowStart && it.startTime < windowEnd }
            if (visiblePrograms.isEmpty()) {
                val emptyView = TextView(itemView.context).apply {
                    text = itemView.context.getString(R.string.no_epg_data)
                    setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    textSize = 14f
                }
                programContainer.addView(emptyView)
                return
            }

            visiblePrograms.forEachIndexed { index, program ->
                val cardView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_epg_program, programContainer, false)

                val cardStart = program.startTime.coerceAtLeast(windowStart)
                val cardEnd = program.endTime.coerceAtMost(windowEnd)
                val duration = (cardEnd - cardStart).coerceAtLeast(1)
                val weight = duration.toFloat() / windowDuration.toFloat()

                cardView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                    marginStart = if (index == 0) 0 else 8
                }

                val isLive = System.currentTimeMillis() in program.startTime..program.endTime
                // Remove background color override - let the drawable handle it
                // Only set background for live cards to white
                if (isLive) {
                    cardView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.epg_card_white))
                } else {
                    // Use transparent background to show the drawable's default color
                    cardView.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.transparent))
                }

                val tvBadge: TextView = cardView.findViewById(R.id.tvBadge)
                val tvTitle: TextView = cardView.findViewById(R.id.tvProgramTitle)
                val tvTime: TextView = cardView.findViewById(R.id.tvProgramTime)

                tvBadge.text = if (isLive) itemView.context.getString(R.string.featured_live) else channel.name
                tvBadge.visibility = if (tvBadge.text.isEmpty()) View.GONE else View.VISIBLE
                tvTitle.text = program.title
                tvTime.text = "${timeFormat.format(program.startTime)} - ${timeFormat.format(program.endTime)}"

                val textColor = if (isLive) R.color.text_dark else R.color.text_primary
                tvBadge.setTextColor(ContextCompat.getColor(itemView.context, textColor))
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, textColor))

                cardView.setOnClickListener { onProgramClick(channel, program) }

                programContainer.addView(cardView)
            }
        }
    }

    class EpgDiffCallback : DiffUtil.ItemCallback<Pair<Channel, List<EpgProgram>>>() {
        override fun areItemsTheSame(
            oldItem: Pair<Channel, List<EpgProgram>>,
            newItem: Pair<Channel, List<EpgProgram>>
        ): Boolean {
            return oldItem.first.id == newItem.first.id
        }

        override fun areContentsTheSame(
            oldItem: Pair<Channel, List<EpgProgram>>,
            newItem: Pair<Channel, List<EpgProgram>>
        ): Boolean {
            return oldItem == newItem
        }
    }
}
