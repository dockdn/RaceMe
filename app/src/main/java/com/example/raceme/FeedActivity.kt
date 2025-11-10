package com.example.raceme

import android.os.Bundle
import android.widget.Toast
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
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var adapter: PostsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(b.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Feed"

        adapter = PostsAdapter(
            onLikeClicked = { post -> toggleLike(post) }
        )

        b.rvPosts.layoutManager = LinearLayoutManager(this)
        b.rvPosts.adapter = adapter

        b.fabNewPost.setOnClickListener { startActivity(android.content.Intent(this, NewPostActivity::class.java)) }

        observePosts()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun observePosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Toast.makeText(this, err.message ?: "Failed to load posts", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { d ->
                    Post(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        userName = d.getString("userName") ?: "",
                        text = d.getString("text") ?: "",
                        imageUrl = d.getString("imageUrl"),
                        createdAt = d.getTimestamp("createdAt"),
                        likesCount = d.getLong("likesCount") ?: 0L
                    )
                }.orEmpty()
                adapter.submit(list)
            }
    }

    private fun toggleLike(post: Post) {
        val u = auth.currentUser ?: run {
            Toast.makeText(this, "Sign in to like", Toast.LENGTH_SHORT).show()
            return
        }
        val likeRef = db.collection("posts").document(post.id)
            .collection("likes").document(u.uid)
        val postRef = db.collection("posts").document(post.id)

        likeRef.get().addOnSuccessListener { doc ->
            val batch = db.batch()
            if (doc.exists()) {
                batch.delete(likeRef)
                batch.update(postRef, "likesCount", FieldValue.increment(-1))
            } else {
                batch.set(likeRef, mapOf("uid" to u.uid, "at" to com.google.firebase.Timestamp.now()))
                batch.update(postRef, "likesCount", FieldValue.increment(1))
            }
            batch.commit()
        }
    }
}