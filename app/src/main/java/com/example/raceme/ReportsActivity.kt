package com.example.raceme

import android.os.Bundle
import com.example.raceme.databinding.ActivityReportsBinding

class ReportsActivity : BaseActivity() {
    private lateinit var b: ActivityReportsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.txtWeekly.text = "WEEKLY/MONTHLY REPORTS"
        b.txtDesc.text = "This feature will provide users with detailed weekly and monthly performance reports including distance, pace, and streaks."
        b.btnBack.setOnClickListener { finish() }
    }
}
