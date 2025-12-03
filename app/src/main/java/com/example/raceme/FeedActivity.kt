package com.example.raceme

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityFeedBinding
import com.example.raceme.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedActivity : BaseActivity() {

    // view binding
    private lateinit var b: ActivityFeedBinding

    // adapter and data
    private lateinit var adapter: PostsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(b.root)

        // header back button -> always go home
        b.btnBackFeed.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // header title (explicit, in case you want to change later)
        b.tvFeedTitle.text = "Community feed"

        // recycler and adapter
        adapter = PostsAdapter { post ->
            toggleLike(post)
        }
        b.rvPosts.layoutManager = LinearLayoutManager(this)
        b.rvPosts.adapter = adapter

        // new post button
        b.fabNewPost.setOnClickListener {
            startActivity(Intent(this, NewPostActivity::class.java))
        }

        // firestore listener - load posts
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val posts = snap.toObjects(Post::class.java)
                    adapter.submit(posts)
                }
            }
    }

    // like handling
    private fun toggleLike(post: Post) {
        val currentUid = auth.currentUser?.uid ?: return
        val postId = post.id
        if (postId.isBlank()) return

        val postDoc = db.collection("posts").document(postId)
        val likeDoc = postDoc.collection("likes").document(currentUid)

        db.runTransaction { transaction ->
            val postSnapshot = transaction.get(postDoc)
            val currentLikes = postSnapshot.getLong("likesCount") ?: 0L
            val userLikeSnapshot = transaction.get(likeDoc)
            val alreadyLiked = userLikeSnapshot.exists()

            if (alreadyLiked) {
                transaction.delete(likeDoc)
                transaction.update(postDoc, "likesCount", currentLikes - 1)
            } else {
                transaction.set(
                    likeDoc,
                    mapOf(
                        "userId" to currentUid,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(postDoc, "likesCount", currentLikes + 1)
            }

            null
        }
    }
}
