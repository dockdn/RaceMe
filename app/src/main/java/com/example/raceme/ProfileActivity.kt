package com.example.raceme

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import coil.load
import com.example.raceme.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

data class UserProfile(
    val displayName: String = "",
    val bio: String = "",
    val photoUrl: String = ""
)

class ProfileActivity : BaseActivity() {
    private lateinit var b: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var pendingPhotoUri: Uri? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingPhotoUri = uri
            b.imgProfile.load(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        loadProfile()

        b.btnPickPhoto.setOnClickListener { pickImage.launch("image/*") }
        b.btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            finish(); return
        }

        b.inputDisplayName.setText(user.displayName ?: "")
        user.photoUrl?.let { b.imgProfile.load(it) }

        db.collection("users").document(user.uid)
            .collection("meta").document("profile")
            .get()
            .addOnSuccessListener { snap ->
                val p = snap.toObject(UserProfile::class.java)
                if (p != null) {
                    if (p.displayName.isNotBlank()) b.inputDisplayName.setText(p.displayName)
                    b.inputBio.setText(p.bio)
                    if (p.photoUrl.isNotBlank()) b.imgProfile.load(p.photoUrl)
                }
            }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            return
        }

        val displayName = b.inputDisplayName.text?.toString()?.trim().orEmpty()
        val bio = b.inputBio.text?.toString()?.trim().orEmpty()

        b.btnSave.isEnabled = false
        b.tvStatus.text = "Saving…"

        val uri = pendingPhotoUri
        if (uri != null) {
            val ref = storage.reference.child("users/${user.uid}/profile/profile.jpg")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: RuntimeException("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { dl ->
                    persistProfile(user.uid, displayName, bio, dl.toString())
                }
                .addOnFailureListener { e ->
                    b.btnSave.isEnabled = true
                    b.tvStatus.text = ""
                    Toast.makeText(this, e.message ?: "Photo upload failed", Toast.LENGTH_LONG).show()
                }
        } else {
            db.collection("users").document(user.uid)
                .collection("meta").document("profile")
                .get()
                .addOnSuccessListener { snap ->
                    val current = snap.toObject(UserProfile::class.java)
                    val photoUrl = current?.photoUrl ?: (user.photoUrl?.toString() ?: "")
                    persistProfile(user.uid, displayName, bio, photoUrl)
                }
                .addOnFailureListener {
                    persistProfile(user.uid, displayName, bio, user.photoUrl?.toString() ?: "")
                }
        }
    }

    private fun persistProfile(uid: String, displayName: String, bio: String, photoUrl: String) {
        val profile = UserProfile(displayName, bio, photoUrl)

        db.collection("users").document(uid)
            .collection("meta").document("profile")
            .set(profile)
            .addOnSuccessListener {
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.ifBlank { null })
                    .setPhotoUri(if (photoUrl.isNotBlank()) Uri.parse(photoUrl) else null)
                    .build()

                auth.currentUser?.updateProfile(req)?.addOnCompleteListener {
                    b.btnSave.isEnabled = true
                    b.tvStatus.text = "Saved ✓"
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                b.btnSave.isEnabled = true
                b.tvStatus.text = ""
                Toast.makeText(this, e.message ?: "Failed to save profile", Toast.LENGTH_LONG).show()
            }
    }
}
