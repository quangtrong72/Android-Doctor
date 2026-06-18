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
import com.uilover.project1983.databinding.ViewholderTopDoctor2Binding

class TopDoctorAdapter2(val items: MutableList<DoctorsModel>) :
    RecyclerView.Adapter<TopDoctorAdapter2.Viewholder>() {
    private var context: Context? = null

    class Viewholder(val binding: ViewholderTopDoctor2Binding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopDoctorAdapter2.Viewholder {
        context = parent.context
        val binding =
            ViewholderTopDoctor2Binding.inflate(LayoutInflater.from(context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: TopDoctorAdapter2.Viewholder, position: Int) {
        holder.binding.nameTxt.text = items[position].Name
        holder.binding.specialTxt.text = items[position].Special
        holder.binding.scoreTxt.text = items[position].Rating.toString()
        holder.binding.ratingBar.rating = items[position].Rating.toFloat()
        holder.binding.degreeTxt.text = "Bác sĩ chuyên nghiệp"

        Glide.with(holder.itemView.context)
            .load(items[position].Picture)
            .apply { RequestOptions().transform(CenterCrop()) }
            .into(holder.binding.img)

        // Fix lỗi Context và bắt sự kiện click cho toàn bộ khung chứa
        holder.itemView.setOnClickListener {
            val safeContext = holder.itemView.context
            val intent = Intent(safeContext, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            safeContext.startActivity(intent)
        }

        // Bắt sự kiện click riêng cho nút Button
        holder.binding.makeBtn.setOnClickListener {
            val safeContext = holder.itemView.context
            val intent = Intent(safeContext, DetailActivity::class.java)
            intent.putExtra("object", items[position])
            safeContext.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}