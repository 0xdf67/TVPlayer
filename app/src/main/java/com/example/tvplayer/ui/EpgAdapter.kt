package com.example.tvplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Pair<Channel, List<EpgProgram>>, EpgAdapter.EpgViewHolder>(EpgDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpgViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epg, parent, false)
        return EpgViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpgViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivChannelLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        private val tvChannelNumber: TextView = itemView.findViewById(R.id.tvChannelNumber)
        private val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        private val tvCurrentProgram: TextView = itemView.findViewById(R.id.tvCurrentProgram)
        private val tvNextProgram: TextView = itemView.findViewById(R.id.tvNextProgram)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)

        fun bind(item: Pair<Channel, List<EpgProgram>>) {
            val channel = item.first
            val programs = item.second.sortedBy { it.startTime }
            val now = System.currentTimeMillis()
            val current = programs.find { it.startTime <= now && it.endTime > now }
            val next = programs.firstOrNull { it.startTime > now }

            tvChannelName.text = channel.name
            tvChannelNumber.text = channel.number.toString()

            if (channel.logoUrl.isNullOrEmpty()) {
                ivChannelLogo.visibility = View.GONE
                tvChannelNumber.visibility = View.VISIBLE
            } else {
                ivChannelLogo.visibility = View.VISIBLE
                tvChannelNumber.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(channel.logoUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .error(R.drawable.bg_control_button)
                    .into(ivChannelLogo)
            }

            if (current != null) {
                tvCurrentProgram.text = current.title
                tvTimeRange.text = "${timeFormat.format(current.startTime)} - ${timeFormat.format(current.endTime)}"
            } else {
                tvCurrentProgram.text = itemView.context.getString(R.string.live)
                tvTimeRange.text = timeFormat.format(now)
            }

            tvNextProgram.text = if (next != null) {
                "Next: ${next.title}"
            } else {
                ""
            }
            tvNextProgram.visibility = if (next != null) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onChannelClick(channel) }
            itemView.setOnFocusChangeListener { _, hasFocus ->
                itemView.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
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
