package com.example.raceme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.raceme.databinding.ItemPostBinding
import com.example.raceme.models.Post
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.BitmapFactory
import android.util.Base64


// adapter for Explore posts list
// update: you are now able to delete your own post (with confirmation)
class PostsAdapter(
    private val currentUserId: String?,
    private val onLikeClicked: (Post) -> Unit,
    private val onDeleteClicked: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.VH>() {

    // backing list + submit helper
    private val items = mutableListOf<Post>()
    fun submit(list: List<Post>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    // view holder
    inner class VH(val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root)

    // inflate item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    // bind post data to row views
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.b.tvName.text = p.userName
        holder.b.tvText.text = p.text
        val ts = p.createdAt?.toDate()
        holder.b.tvTime.text =
            ts?.let { SimpleDateFormat("MMM d, h:mm a", Locale.US).format(it) } ?: ""

        if (p.imageBase64.isNullOrBlank()) {
            holder.b.ivPhoto.visibility = View.GONE
        } else {
            holder.b.ivPhoto.visibility = View.VISIBLE
            try {
                val bytes = Base64.decode(p.imageBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.b.ivPhoto.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.b.ivPhoto.visibility = View.GONE
            }
        }

        // like button
        holder.b.btnLike.text = "♥ ${p.likesCount}"
        holder.b.btnLike.setOnClickListener { onLikeClicked(p) }

        // delete button only for your own posts
        val canDelete = currentUserId != null && p.userId == currentUserId
        holder.b.btnDelete.visibility = if (canDelete) View.VISIBLE else View.GONE

        if (canDelete) {
            holder.b.btnDelete.setOnClickListener {
                AlertDialog.Builder(holder.b.root.context)
                    .setTitle("Delete post?")
                    .setMessage("This action can't be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        onDeleteClicked(p)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            // avoid leaking old listeners when rows are reused
            holder.b.btnDelete.setOnClickListener(null)
        }
    }

    // list size
    override fun getItemCount(): Int = items.size
}
