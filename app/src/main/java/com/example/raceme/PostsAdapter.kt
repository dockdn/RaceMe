package com.example.raceme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.raceme.databinding.ItemPostBinding
import com.example.raceme.models.Post
import java.text.SimpleDateFormat
import java.util.Locale

class PostsAdapter(
    private val onLikeClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.VH>() {

    private val items = mutableListOf<Post>()
    fun submit(list: List<Post>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.b.tvName.text = p.userName
        holder.b.tvText.text = p.text
        val ts = p.createdAt?.toDate()
        holder.b.tvTime.text = ts?.let { SimpleDateFormat("MMM d, h:mm a", Locale.US).format(it) } ?: ""

        if (p.imageUrl.isNullOrBlank()) {
            holder.b.ivPhoto.visibility = android.view.View.GONE
        } else {
            holder.b.ivPhoto.visibility = android.view.View.VISIBLE
            holder.b.ivPhoto.load(p.imageUrl)
        }

        holder.b.btnLike.text = "♥ ${p.likesCount}"
        holder.b.btnLike.setOnClickListener { onLikeClicked(p) }
    }

    override fun getItemCount(): Int = items.size
}