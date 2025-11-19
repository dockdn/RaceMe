package com.example.raceme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityFeedBinding
import com.example.raceme.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedActivity : AppCompatActivity() {

    private lateinit var b: ActivityFeedBinding
    private lateinit var adapter: PostsAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(b.root)

        adapter = PostsAdapter { post ->
            toggleLike(post)
        }

        b.rvPosts.layoutManager = LinearLayoutManager(this)
        b.rvPosts.adapter = adapter


        b.fabNewPost.setOnClickListener {
            startActivity(Intent(this, NewPostActivity::class.java))
        }

        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val posts = snap.documents.mapNotNull { doc ->
                        val post = doc.toObject(Post::class.java)
                        // 🔹 inject Firestore document ID into the Post model
                        post?.copy(id = doc.id)
                    }
                    adapter.submit(posts)
                }
            }
    }

    private fun toggleLike(post: Post) {
        val currentUid = auth.currentUser?.uid ?: return
        val postId = post.id

        if (postId.isBlank()) {
            return
        }

        val postDoc = db.collection("posts").document(postId)
        val likeDoc = postDoc.collection("likes").document(currentUid)

        db.runTransaction { transaction ->
            val postSnapshot = transaction.get(postDoc)
            val currentLikes = postSnapshot.getLong("likesCount") ?: 0L

            val userLikeSnapshot = transaction.get(likeDoc)
            val alreadyLiked = userLikeSnapshot.exists()

            if (alreadyLiked) {
                // UNLIKE: delete like doc, decrement count
                transaction.delete(likeDoc)
                transaction.update(postDoc, "likesCount", currentLikes - 1)
            } else {
                // LIKE: create like doc with current user's UID as ID
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
