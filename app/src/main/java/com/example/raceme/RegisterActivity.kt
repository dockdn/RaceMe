package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : BaseActivity() {
    private lateinit var b: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnCreateAccount.setOnClickListener {
            val name = b.inputName.text?.toString()?.trim().orEmpty()
            val email = b.inputEmail.text?.toString()?.trim().orEmpty()
            val pw = b.inputPassword.text?.toString().orEmpty()
            val confirm = b.inputConfirm.text?.toString().orEmpty()

            if (email.isEmpty() || pw.isEmpty()) {
                toast("Email and password are required"); return@setOnClickListener
            }
            if (confirm.isNotEmpty() && pw != confirm) {
                toast("Passwords don’t match"); return@setOnClickListener
            }

            b.btnCreateAccount.isEnabled = false

            auth.createUserWithEmailAndPassword(email, pw)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        b.btnCreateAccount.isEnabled = true
                        toast("Account created, but no user returned"); return@addOnSuccessListener
                    }

                    // UPDATE DISPLAY NAME (WHEN CHANGED)
                    if (name.isNotEmpty()) {
                        val profile = userProfileChangeRequest { displayName = name }
                        user.updateProfile(profile)
                            .addOnFailureListener { /* non-fatal */ }
                    }

                    val doc = mapOf(
                        "displayName" to (name.ifEmpty { null }),
                        "email" to email,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(user.uid).set(doc)
                        .addOnCompleteListener {
                            b.btnCreateAccount.isEnabled = true
                            toast("Account created! Welcome 🎉")
                            go(HomeActivity::class.java)
                            finishAffinity()
                        }
                }
                .addOnFailureListener { e ->
                    b.btnCreateAccount.isEnabled = true
                    toast(e.message ?: "Registration failed")
                }
        }

        b.linkLogin.setOnClickListener {
            go(LoginActivity::class.java)
            finish()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
