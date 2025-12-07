package com.example.raceme

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.example.raceme.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var b: ActivityForgotPasswordBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Cancel >> just close this screen
        b.btnCancel.setOnClickListener { finish() }

        // Go to login
        b.linkLogin.setOnClickListener {
            go(LoginActivity::class.java)
            finish()
        }

        // Go to register
        b.linkRegister.setOnClickListener {
            go(RegisterActivity::class.java)
            finish()
        }

        // Send reset link
        b.btnSendLink.setOnClickListener {
            val email = b.inputEmail.text?.toString()?.trim().orEmpty()
            sendResetEmail(email)
        }
    }

    private fun sendResetEmail(email: String) {
        // Clear old status
        setStatus("")


        if (email.isEmpty()) {
            setStatus("Please enter your email.")
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
            return
        }


        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setStatus("Please enter a valid email address.")
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button while sending
        b.btnSendLink.isEnabled = false
        b.btnSendLink.text = "Sending…"

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                b.btnSendLink.isEnabled = true
                b.btnSendLink.text = "Send link"

                val msg = "Reset link sent to $email."
                setStatus(msg)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                b.btnSendLink.isEnabled = true
                b.btnSendLink.text = "Send link"

                val friendly = when (e) {
                    is FirebaseAuthInvalidUserException ->
                        // "Email not registered” case
                        "No account found with this email."

                    else ->
                        e.message ?: "Could not send reset email. Please try again."
                }

                setStatus(friendly)
                Toast.makeText(this, friendly, Toast.LENGTH_LONG).show()
            }
    }

    private fun setStatus(msg: String) {
        b.tvStatus.text = msg
    }
}
