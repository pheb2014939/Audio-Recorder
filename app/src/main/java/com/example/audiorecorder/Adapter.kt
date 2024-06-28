package com.example.audiorecorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.text.SimpleDateFormat
import java.util.Date

class Adapter(private var records: ArrayList<AudioRecord>, var listener: OnItemClickListener) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    private var editMode = false
    fun isEditMode(): Boolean {return editMode}

    fun setEditMode(mode: Boolean){
         if (editMode != mode){
             editMode = mode
             notifyDataSetChanged()
         }

    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener{
        val tvFilename: TextView = itemView.findViewById(R.id.tvFilename)
        val tvMeta1: TextView = itemView.findViewById(R.id.tvMeta1)
        val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)


        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }
        override fun onClick(v: View?) {
            val position= adapterPosition
            if(position != RecyclerView.NO_POSITION)
                listener.onItemClickListener(position)
        }

        override fun onLongClick(v: View?): Boolean {
            val position= adapterPosition
            if(position != RecyclerView.NO_POSITION)
                listener.onItemLongClickListener(position)
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itemview_layout, parent, false)
        return ViewHolder(view)


    }

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION){
            var record = records[position]

            var sdf = SimpleDateFormat("dd/MM/yyyy")
            var date = Date(record.timestamp)
            var strDate = sdf.format(date)




            holder.tvFilename.text = record.filename
            holder.tvMeta.text = "${record.duration} $strDate"
            holder.tvMeta1.text = "${record.username} ${record.phonenumber}" // Display username and phone number


            if (editMode){
                holder.checkbox.visibility = View.VISIBLE
                holder.checkbox.isChecked = record.isChecked

            }else{
                holder.checkbox.visibility = View.GONE
                holder.checkbox.isChecked = false
            }

        }
    }
}