package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : BaseActivity() {
    private lateinit var b: ActivityRegisterBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnCreate.setOnClickListener {
            val email = b.inputRegEmail.text.toString().trim()
            val pw = b.inputRegPassword.text.toString()
            val name = b.inputName.text.toString().trim()
            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            b.btnCreate.isEnabled = false
            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
                b.btnCreate.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created. You can login now.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, task.exception?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
