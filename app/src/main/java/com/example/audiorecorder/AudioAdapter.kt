package com.example.audiorecorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AudioAdapter(
    private val audioList: ArrayList<AudioRecord>,
    private val onAudioSelected: (AudioRecord) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.audio_layout, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audioList[position]
        holder.bind(audio)
    }

    override fun getItemCount(): Int = audioList.size

    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAudioName: TextView = itemView.findViewById(R.id.tvAudioName)

        fun bind(audio: AudioRecord) {
            tvAudioName.text = audio.filename
            itemView.setOnClickListener {
                onAudioSelected(audio)
            }
        }
    }
}
