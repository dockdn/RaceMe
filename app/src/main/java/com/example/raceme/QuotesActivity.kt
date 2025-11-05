package com.example.raceme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.raceme.databinding.ActivityQuotesBinding

class QuotesActivity : AppCompatActivity() {
    private lateinit var b: ActivityQuotesBinding

    private val quotes = listOf(
        "Small steps win races." to "RaceMe",
        "You’re lapping everyone still on the couch." to "Unknown",
        "Run the day, don’t let it run you." to "Unknown",
        "Consistency beats intensity." to "Unknown",
        "One more mile, one more smile." to "Unknown",
        "Progress > perfection." to "Unknown"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityQuotesBinding.inflate(layoutInflater)
        setContentView(b.root)

        showRandomQuote()
        b.btnNext.setOnClickListener { showRandomQuote() }
    }

    private fun showRandomQuote() {
        val (q, a) = quotes.random()
        b.tvQuote.text = "“$q”"
        b.tvAuthor.text = "— $a"
    }
}
