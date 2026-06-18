package com.uilover.project1983.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.uilover.project1983.Activity.DetailActivity
import com.uilover.project1983.Domain.DoctorsModel
import com.uilover.project1983.databinding.ViewholderTopDoctorBinding

class TopDoctorAdapter(val items: MutableList<DoctorsModel>) :
    RecyclerView.Adapter<TopDoctorAdapter.Viewholder>() {
    private var context: Context? = null

    class Viewholder(val binding: ViewholderTopDoctorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopDoctorAdapter.Viewholder {
        context = parent.context
        val binding =
            ViewholderTopDoctorBinding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: TopDoctorAdapter.Viewholder, position: Int) {
        holder.binding.nameTxt.text = items[position].Name
        holder.binding.specialTxt.text = items[position].Special
        holder.binding.scoreTxt.text = items[position].Rating.toString()
        holder.binding.yearTxt.text = items[position].Experience.toString() + " Năm KN"

        Glide.with(holder.itemView.context)
            .load(items[position].Picture)
            .apply { RequestOptions().transform(CenterCrop()) }
            .into(holder.binding.img)

        // Đã fix lỗi Context gây văng app
        holder.itemView.setOnClickListener {
            val safeContext = holder.itemView.context
            val intent = Intent(safeContext, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            safeContext.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}