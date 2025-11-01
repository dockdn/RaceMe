package com.example.raceme

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.raceme.databinding.ActivityProfileBinding
import coil.load

data class UserProfile(
    val username: String = "",
    val bio: String = "",
    val photoUrl: String = ""
)

class ProfileActivity : BaseActivity() {
    private lateinit var b: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var pickedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            b.imgProfile.setImageURI(uri) // preview
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        // Load existing profile
        loadProfile()

        b.btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }
        b.btnSave.setOnClickListener { saveProfile() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { snap ->
            val p = snap.toObject(UserProfile::class.java) ?: return@addOnSuccessListener
            b.inputUsername.setText(p.username)
            b.inputBio.setText(p.bio)
            if (p.photoUrl.isNotEmpty()) b.imgProfile.load(p.photoUrl)
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val username = b.inputUsername.text?.toString()?.trim().orEmpty()
        val bio = b.inputBio.text?.toString()?.trim().orEmpty()

        b.btnSave.isEnabled = false

        // If a new image is picked, upload first, then save Firestore
        val uri = pickedUri
        if (uri != null) {
            val ref = storage.reference.child("users/$uid/profile.jpg")
            ref.putFile(uri).continueWithTask { ref.downloadUrl }.addOnSuccessListener { dl ->
                writeUser(uid, username, bio, dl.toString())
            }.addOnFailureListener { e ->
                b.btnSave.isEnabled = true
                Toast.makeText(this, e.message ?: "Upload failed", Toast.LENGTH_LONG).show()
            }
        } else {
            // Save text-only changes
            writeUser(uid, username, bio, null)
        }
    }

    private fun writeUser(uid: String, username: String, bio: String, photoUrl: String?) {
        val map = hashMapOf<String, Any>(
            "username" to username,
            "bio" to bio
        )
        if (photoUrl != null) map["photoUrl"] = photoUrl

        db.collection("users").document(uid).set(map, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                b.btnSave.isEnabled = true
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                b.btnSave.isEnabled = true
                Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
    }
}
