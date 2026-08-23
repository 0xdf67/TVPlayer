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

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumberBadge: TextView = itemView.findViewById(R.id.tvNumberBadge)
        private val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        private val tvChannelName: TextView = itemView.findViewById(R.id.tvChannelName)
        private val tvChannelMeta: TextView = itemView.findViewById(R.id.tvChannelMeta)

        fun bind(channel: Channel) {
            tvChannelName.text = channel.name
            tvChannelMeta.text = if (channel.group.isNotEmpty()) {
                "${channel.number} · ${channel.group}"
            } else {
                itemView.context.getString(R.string.channel_number, channel.number)
            }

            if (channel.logoUrl.isNullOrEmpty()) {
                ivLogo.visibility = View.GONE
                tvNumberBadge.visibility = View.VISIBLE
                tvNumberBadge.text = channel.number.toString()
            } else {
                ivLogo.visibility = View.VISIBLE
                tvNumberBadge.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(channel.logoUrl)
                    .placeholder(R.drawable.bg_control_button)
                    .error(R.drawable.bg_control_button)
                    .into(ivLogo)
            }

            itemView.setOnClickListener { onChannelClick(channel) }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                itemView.animate()
                    .scaleX(if (hasFocus) 1.03f else 1f)
                    .scaleY(if (hasFocus) 1.03f else 1f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel): Boolean {
            return oldItem == newItem
        }
    }
}
