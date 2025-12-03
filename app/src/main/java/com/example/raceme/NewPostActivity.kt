package com.example.raceme

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.raceme.databinding.ActivityNewPostBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class NewPostActivity : AppCompatActivity() {

    // view binding + firebase
    private lateinit var b: ActivityNewPostBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    // selected image uri (if any)
    private var pickedImage: Uri? = null

    // image picker launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedImage = uri
            b.ivPreview.load(uri)
        }
    }

    // lifecycle: onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(b.root)

        // back button in header
        b.btnBackNewPost.setOnClickListener {
            finish()
        }

        // pick image click
        b.btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // post click
        b.btnPost.setOnClickListener {
            submitPost()
        }
    }

    // create post (upload image first if there is one)
    private fun submitPost() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_LONG).show()
            return
        }

        val text = b.inputText.text?.toString()?.trim().orEmpty()
        if (text.isEmpty() && pickedImage == null) {
            Toast.makeText(this, "Write something or add a photo", Toast.LENGTH_LONG).show()
            return
        }

        b.btnPost.isEnabled = false

        fun createPostDoc(imageUrl: String?) {
            val doc = hashMapOf(
                "userId" to user.uid,
                "userName" to (user.displayName ?: user.email ?: "Racer"),
                "text" to text,
                "imageUrl" to imageUrl,
                "createdAt" to Timestamp.now(),
                "likesCount" to 0L
            )

            db.collection("posts")
                .add(doc)
                .addOnSuccessListener {
                    Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    b.btnPost.isEnabled = true
                    Toast.makeText(
                        this,
                        e.message ?: "Failed to post",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        val image = pickedImage
        if (image == null) {
            // text-only post
            createPostDoc(null)
        } else {
            // upload image then create post
            val path = "posts/${user.uid}/${System.currentTimeMillis()}.jpg"
            storage.reference.child(path)
                .putFile(image)
                .continueWithTask { it.result.storage.downloadUrl }
                .addOnSuccessListener { uri ->
                    createPostDoc(uri.toString())
                }
                .addOnFailureListener { e ->
                    b.btnPost.isEnabled = true
                    Toast.makeText(
                        this,
                        e.message ?: "Upload failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
