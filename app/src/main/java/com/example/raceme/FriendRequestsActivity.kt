package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.raceme.databinding.ActivityFriendRequestsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendRequestsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: FriendRequestsAdapter
    private val incomingRequests = mutableListOf<FriendRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadIncomingRequests()
        setupSendButton()
    }

    // SEND FRIEND REQUEST BY EMAIL
    private fun setupSendButton() {
        binding.btnSendRequest.setOnClickListener {
            val emailInput = binding.etFriendUid.text.toString().trim()
            val currentUser = auth.currentUser

            if (emailInput.isEmpty()) {
                binding.tvStatus.text = "Please enter an email"
                return@setOnClickListener
            }

            if (currentUser == null) {
                binding.tvStatus.text = "You must be logged in"
                return@setOnClickListener
            }

            // Prevent sending request to yourself
            if (emailInput == currentUser.email) {
                binding.tvStatus.text = "You cannot send a request to yourself"
                return@setOnClickListener
            }

            // Find user by email
            db.collection("users")
                .whereEqualTo("email", emailInput)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        binding.tvStatus.text = "No user found with that email"
                        return@addOnSuccessListener
                    }

                    val toUserDoc = result.documents[0]
                    val toUid = toUserDoc.id
                    val toEmail = toUserDoc.getString("email") ?: ""

                    val fromUid = currentUser.uid
                    val fromEmail = currentUser.email ?: ""

                    // Check if request already exists
                    db.collection("friendRequests")
                        .whereEqualTo("fromUid", fromUid)
                        .whereEqualTo("toUid", toUid)
                        .get()
                        .addOnSuccessListener { existing ->
                            if (!existing.isEmpty) {
                                binding.tvStatus.text = "Request already sent!"
                                return@addOnSuccessListener
                            }

                            // Create friend request document
                            val requestData = mapOf(
                                "fromUid" to fromUid,
                                "toUid" to toUid,
                                "fromEmail" to fromEmail,
                                "toEmail" to toEmail,
                                "status" to "pending",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            db.collection("friendRequests")
                                .add(requestData)
                                .addOnSuccessListener {
                                    binding.tvStatus.text = "Friend request sent!"
                                    binding.etFriendUid.text.clear()
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
    }

    // LOAD INCOMING REQUESTS
    private fun loadIncomingRequests() {
        val currentUid = auth.currentUser?.uid ?: return

        db.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                incomingRequests.clear()

                snapshot?.documents?.forEach { doc ->
                    val request = FriendRequest(
                        id = doc.id,
                        fromUid = doc.getString("fromUid") ?: "",
                        fromEmail = doc.getString("fromEmail") ?: "",
                        toUid = doc.getString("toUid") ?: "",
                        toEmail = doc.getString("toEmail") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                    incomingRequests.add(request)
                }

                adapter.setData(incomingRequests)
            }
    }

    // SET UP RECYCLER VIEW
    private fun setupRecyclerView() {
        adapter = FriendRequestsAdapter(
            incomingRequests,
            onAccept = { request -> acceptRequest(request) },
            onReject = { request -> rejectRequest(request) }
        )

        binding.rvIncomingRequests.layoutManager = LinearLayoutManager(this)
        binding.rvIncomingRequests.adapter = adapter
    }

    // ACCEPT FRIEND REQUEST
    private fun acceptRequest(request: FriendRequest) {
        val currentUid = auth.currentUser?.uid ?: return
        val senderUid = request.fromUid
        val requestRef = db.collection("friendRequests").document(request.id)

        // Step 1 — update status
        requestRef.update("status", "accepted")
            .addOnSuccessListener {

                // Step 2 — add sender to current user's friend list
                db.collection("users").document(currentUid)
                    .update("friends", FieldValue.arrayUnion(senderUid))
                    .addOnSuccessListener {

                        // Step 3 — add current user to sender's friend list
                        db.collection("users").document(senderUid)
                            .update("friends", FieldValue.arrayUnion(currentUid))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Friend added!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                // If sender's friend list doesn't exist
                                db.collection("users").document(senderUid)
                                    .set(mapOf("friends" to listOf(currentUid)), SetOptions.merge())
                            }
                    }
                    .addOnFailureListener {
                        // If current user's friend list doesn't exist
                        db.collection("users").document(currentUid)
                            .set(mapOf("friends" to listOf(senderUid)), SetOptions.merge())
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error accepting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // REJECT FRIEND REQUEST
    private fun rejectRequest(request: FriendRequest) {
        db.collection("friendRequests").document(request.id)
            .update("status", "rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error rejecting", Toast.LENGTH_SHORT).show()
            }
    }
}
