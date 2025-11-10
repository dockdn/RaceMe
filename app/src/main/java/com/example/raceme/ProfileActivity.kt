package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.raceme.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        val u = auth.currentUser
        if (u == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Prefill from Firestore if available, else from Auth
        db.collection("users").document(u.uid)
            .get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("displayName") ?: u.displayName ?: u.email ?: ""
                val bio  = snap.getString("bio") ?: ""
                b.inputDisplayName.setText(name)
                b.inputBio.setText(bio)
            }
            .addOnFailureListener {
                val name = u.displayName ?: u.email ?: ""
                b.inputDisplayName.setText(name)
            }

        b.btnSave.setOnClickListener {
            val name = b.inputDisplayName.text?.toString()?.trim().orEmpty()
            val bio  = b.inputBio.text?.toString()?.trim().orEmpty()
            saveProfile(name, bio)
        }
    }

    private fun saveProfile(name: String, bio: String) {
        val u = auth.currentUser ?: run {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            return
        }

        // 1) Save profile fields to Firestore
        val data = mapOf(
            "displayName" to name,
            "bio" to bio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(u.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                // 2) Update Auth displayName so FirebaseAuth.currentUser.displayName matches
                val req = userProfileChangeRequest { displayName = name }
                u.updateProfile(req).addOnCompleteListener {
                    // 3) Refresh in-memory user and finish
                    u.reload().addOnCompleteListener {
                        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
    }
}
