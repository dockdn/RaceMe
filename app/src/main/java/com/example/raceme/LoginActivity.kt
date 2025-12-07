package com.example.raceme

import android.os.Bundle
import android.widget.Toast
import com.example.raceme.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {
    private lateinit var b: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        auth.currentUser?.let {
//            go(HomeActivity::class.java)
//            finish()
//            return
//        }

        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        // LOGIN BUTTON

        b.btnLogin.setOnClickListener {
            val email = b.inputEmail.text.toString().trim()
            val pw = b.inputPassword.text.toString()

            if (email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            b.btnLogin.isEnabled = false
            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
                b.btnLogin.isEnabled = true
                if (task.isSuccessful) {
                    go(HomeActivity::class.java)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Login failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // REGISTRATION REDIRECTION

        b.linkRegister.setOnClickListener {
            go(RegisterActivity::class.java)
        }

        // FORGOT PASSWORD LINK

        b.tvForgotPassword.setOnClickListener {
            go(ForgotPasswordActivity::class.java)
        }
    }
}
