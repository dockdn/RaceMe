package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityCreateRaceBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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

        val raceDoc = hashMapOf(
            "name" to name,
            "description" to desc,
            "createdBy" to uid,
            "createdAt" to Timestamp.now()
        )

        // Save creator's copy (optional)
        db.collection("users").document(uid)
            .collection("myRaces")
            .add(raceDoc)

        if (makePublic) {
            db.collection("publicRaces")
                .add(raceDoc)
                .addOnSuccessListener { Toast.makeText(this, "Published publicly", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e -> Toast.makeText(this, e.message ?: "Failed to publish", Toast.LENGTH_LONG).show() }
        }

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
