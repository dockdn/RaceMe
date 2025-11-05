package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.raceme.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest   // <-- THIS import fixes it
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var b: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Prefill current values (Firestore preferred; fall back to Auth)
        val u = auth.currentUser
        if (u == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Load from Firestore, then fall back to Auth displayName/email
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
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show(); return
        }

        // Update FirebaseAuth profile (so Home can read displayName immediately too)
        val req = userProfileChangeRequest {
            displayName = name
        }
        u.updateProfile(req).addOnFailureListener {
            // Non-fatal; we still save to Firestore below
        }

        // Persist to Firestore
        val data = hashMapOf(
            "displayName" to name,
            "bio" to bio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(u.uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
    }
}
