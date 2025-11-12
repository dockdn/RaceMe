package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityFriendRequestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestsActivity : BaseActivity() {
    private lateinit var binding: ActivityFriendRequestsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val requests = mutableListOf<FriendRequest>()
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FriendRequestsAdapter(requests, ::acceptRequest, ::rejectRequest)
        binding.rvIncomingRequests.layoutManager = LinearLayoutManager(this)
        binding.rvIncomingRequests.adapter = adapter

        binding.btnSendRequest.setOnClickListener {
            val toUid = binding.etFriendUid.text.toString().trim()
            sendFriendRequest(toUid)
        }

        loadIncomingRequests()
    }

    private fun sendFriendRequest(toUid: String) {
        val fromUid = auth.currentUser?.uid
        if (fromUid == null) {
            binding.tvStatus.text = "Not signed in"
            return
        }

        if (toUid.isEmpty()) {
            binding.tvStatus.text = "Please enter a UID"
            return
        }

        if (toUid == fromUid) {
            binding.tvStatus.text = "You cannot send a request to yourself"
            return
        }

        // Check existing request
        db.collection("friendRequests")
            .whereEqualTo("fromUid", fromUid)
            .whereEqualTo("toUid", toUid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    binding.tvStatus.text = "Friend request already sent"
                } else {
                    val request = hashMapOf(
                        "fromUid" to fromUid,
                        "toUid" to toUid,
                        "status" to "pending",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    db.collection("friendRequests")
                        .add(request)
                        .addOnSuccessListener {
                            binding.tvStatus.text = "Friend request sent!"
                            binding.etFriendUid.text?.clear()
                        }
                        .addOnFailureListener { e ->
                            binding.tvStatus.text = "Failed: ${e.message}"
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "Error: ${e.message}"
            }
    }

    private fun loadIncomingRequests() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    binding.tvStatus.text = "Error loading: ${error.message}"
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromUid = doc.getString("fromUid") ?: "",
                        toUid = doc.getString("toUid") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()
                adapter.setData(list)
                // optional: clear status
                if (list.isEmpty()) binding.tvStatus.text = ""
            }
    }

    private fun acceptRequest(request: FriendRequest) {
        val currentUid = auth.currentUser?.uid ?: return
        val requestRef = db.collection("friendRequests").document(request.id)
        requestRef.update("status", "accepted")
            .addOnSuccessListener {
                // add each other to friends arrays
                db.collection("users").document(currentUid)
                    .update("friends", FieldValue.arrayUnion(request.fromUid))
                    .addOnFailureListener {
                        // if field doesn't exist, create with merge
                        db.collection("users").document(currentUid)
                            .set(mapOf("friends" to listOf(request.fromUid)), com.google.firebase.firestore.SetOptions.merge())
                    }

                db.collection("users").document(request.fromUid)
                    .update("friends", FieldValue.arrayUnion(currentUid))
                    .addOnFailureListener {
                        db.collection("users").document(request.fromUid)
                            .set(mapOf("friends" to listOf(currentUid)), com.google.firebase.firestore.SetOptions.merge())
                    }

                Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Accept failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(request: FriendRequest) {
        db.collection("friendRequests").document(request.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Reject failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}