package com.example.raceme

import android.os.Bundle
import com.example.raceme.databinding.ActivityQuotesBinding
import kotlin.random.Random

class QuotesActivity : BaseActivity() {
    private lateinit var b: ActivityQuotesBinding
    private val quotes = listOf(
        "Keep going—your streak loves you.",
        "Small runs add up to big wins.",
        "Today’s pace beats yesterday’s couch."
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityQuotesBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.header.text = "MOTIVATIONAL QUOTES"
        b.desc.text = "This feature will send users motivational or encouraging quotes depending on their weekly progress and streaks."
        b.btnNext.setOnClickListener { showRandom() }
        showRandom()
        b.btnBack.setOnClickListener { finish() }
    }
    private fun showRandom() {
        b.quote.text = quotes[Random.nextInt(quotes.size)]
    }
}
