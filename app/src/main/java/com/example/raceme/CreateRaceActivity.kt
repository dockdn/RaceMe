package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateRaceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateRaceActivity : BaseActivity() {
    private lateinit var b: ActivityCreateRaceBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateRaceBinding.inflate(layoutInflater)
        setContentView(b.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Race"

        b.btnSaveRace.setOnClickListener { saveRace() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun saveRace() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show()
            return
        }

        val name = b.inputRaceName.text?.toString()?.trim().orEmpty()
        val desc = b.inputDescription.text?.toString()?.trim().orEmpty()
        val makePublic = b.switchPublic.isChecked

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a race name", Toast.LENGTH_SHORT).show()
            return
        }

        // base doc used for both private + public
        val baseDoc = hashMapOf(
            "name" to name,
            "description" to desc,
            "ownerId" to uid,
            // public collection expects this exact field for rules
            "visibility" to if (makePublic) "public" else "private",
            // server timestamp so ordering works + rules accept timestamp
            "createdAt" to FieldValue.serverTimestamp()
        )

        // Save creator's private copy (optional)
        db.collection("users").document(uid)
            .collection("myRaces")
            .add(baseDoc)
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "Failed to save to My Races", Toast.LENGTH_LONG).show()
            }

        if (makePublic) {
            // Must use the collection and fields your rules allow
            db.collection("public_races")
                .add(baseDoc)
                .addOnSuccessListener {
                    Toast.makeText(this, "Published publicly", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Failed to publish", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "Saved (private)", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
