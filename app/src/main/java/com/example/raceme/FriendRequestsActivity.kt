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

        // Send friend request by email
        binding.btnSendRequest.setOnClickListener {
            val toEmail = binding.etFriendUid.text.toString().trim()
            sendFriendRequest(toEmail)
        }

        loadIncomingRequests()
    }

    // -------------------- SEND FRIEND REQUEST --------------------
    private fun sendFriendRequest(toEmail: String) {
        val fromUid = auth.currentUser?.uid
        val fromEmail = auth.currentUser?.email
        if (fromUid == null || fromEmail == null) {
            binding.tvStatus.text = "Not signed in"
            return
        }

        if (toEmail.isEmpty()) {
            binding.tvStatus.text = "Please enter an email"
            return
        }

        if (toEmail == fromEmail) {
            binding.tvStatus.text = "You cannot send a request to yourself"
            return
        }

        // Find the user by email
        db.collection("users")
            .whereEqualTo("email", toEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.tvStatus.text = "User not found"
                    return@addOnSuccessListener
                }

                val toUid = snapshot.documents[0].id

                // Check if a request already exists
                db.collection("friendRequests")
                    .whereEqualTo("fromUid", fromUid)
                    .whereEqualTo("toUid", toUid)
                    .get()
                    .addOnSuccessListener { requestSnap ->
                        if (!requestSnap.isEmpty) {
                            binding.tvStatus.text = "Friend request already sent"
                        } else {
                            val request = hashMapOf(
                                "fromUid" to fromUid,
                                "fromEmail" to fromEmail,
                                "toUid" to toUid,
                                "toEmail" to toEmail,
                                "status" to "pending",
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                            db.collection("friendRequests")
                                .add(request)
                                .addOnSuccessListener {
                                    binding.tvStatus.text = "Friend request sent to $toEmail!"
                                    binding.etFriendUid.text?.clear()
                                }
                                .addOnFailureListener { e ->
                                    binding.tvStatus.text = "Failed: ${e.message}"
                                }
                        }
                    }
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "Error finding user: ${e.message}"
            }
    }

    // -------------------- LOAD INCOMING REQUESTS --------------------
    private fun loadIncomingRequests() {
        val currentUid = auth.currentUser?.uid ?: return
        db.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    binding.tvStatus.text = "Error loading requests: ${error.message}"
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromUid = doc.getString("fromUid") ?: "",
                        fromEmail = doc.getString("fromEmail") ?: "",
                        toUid = doc.getString("toUid") ?: "",
                        toEmail = doc.getString("toEmail") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()

                adapter.setData(list)
                if (list.isEmpty()) binding.tvStatus.text = ""
            }
    }

    // -------------------- ACCEPT REQUEST --------------------
    private fun acceptRequest(request: FriendRequest) {
        val currentUid = auth.currentUser?.uid ?: return
        val requestRef = db.collection("friendRequests").document(request.id)

        requestRef.update("status", "accepted")
            .addOnSuccessListener {
                // Add each other to friends arrays
                db.collection("users").document(currentUid)
                    .update("friends", FieldValue.arrayUnion(request.fromUid))
                    .addOnFailureListener {
                        db.collection("users").document(currentUid)
                            .set(
                                mapOf("friends" to listOf(request.fromUid)),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                    }

                db.collection("users").document(request.fromUid)
                    .update("friends", FieldValue.arrayUnion(currentUid))
                    .addOnFailureListener {
                        db.collection("users").document(request.fromUid)
                            .set(
                                mapOf("friends" to listOf(currentUid)),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                    }

                Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Accept failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // -------------------- REJECT REQUEST --------------------
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
