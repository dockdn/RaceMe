package com.example.raceme

import android.os.Bundle
import android.widget.RadioButton
import com.example.raceme.databinding.ActivityReportsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.raceme.models.RunPoint

class ReportsActivity : BaseActivity() {   // ✅ now extends BaseActivity like others
    private lateinit var b: ActivityReportsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val tz by lazy { ZoneId.systemDefault() }

    private var allRuns: List<RunPoint> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = auth.currentUser ?: run { finish(); return }
        b = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 🔙 back arrow
        b.btnBackReports.setOnClickListener {
            finish()
        }

        // chart styling
        setupChart(b.chartMiles)
        setupChart(b.chartTime)

        // weekly / monthly toggle
        b.groupRange.setOnCheckedChangeListener { _, _ -> renderCharts() }
    }

    override fun onResume() {
        super.onResume()
        loadRunsThenRender()
    }

    // load runs from Firestore then draw charts
    private fun loadRunsThenRender() {
        val uid = auth.currentUser?.uid ?: run { finish(); return }
        db.collection("users").document(uid)
            .collection("runs")
            .get()
            .addOnSuccessListener { snap ->
                allRuns = snap.documents.mapNotNull { doc ->
                    val ts = doc.getTimestamp("startedAt") ?: return@mapNotNull null
                    val distMiles = doc.getDouble("distanceMiles")
                        ?: (doc.getDouble("distanceMeters")?.let { it / 1609.344 } ?: 0.0)
                    val startedMs = ts.seconds * 1000 + ts.nanoseconds / 1_000_000
                    val elapsedMs = doc.getLong("elapsedMs") ?: 0L
                    RunPoint(startedMs, distMiles, elapsedMs)
                }.sortedBy { it.startedAtMs }

                renderCharts()
            }
            .addOnFailureListener {
                allRuns = emptyList()
                renderCharts()
            }
    }

    // chart styling (muted brick red + more readable text)
    private fun setupChart(chart: com.github.mikephil.charting.charts.BarChart) {
        val brickRed = 0xFFB64D4D.toInt()
        val axisTextColor = 0xFF5A4A42.toInt()

        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false

        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.textColor = axisTextColor
        chart.axisLeft.textSize = 11f
        chart.axisLeft.gridColor = 0x33AAAAAA   // very light grid

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.textColor = axisTextColor
        chart.xAxis.textSize = 10f
        chart.xAxis.axisLineColor = brickRed

        chart.legend.isEnabled = false
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.animateY(400)
    }

    // switch weekly/monthly
    private fun renderCharts() {
        val weekly = findViewById<RadioButton>(b.rbWeekly.id).isChecked
        if (weekly) renderWeekly() else renderMonthly()
    }

    // calculate + draw weekly data
    private fun renderWeekly() {
        val labels = lastNDaysLabels(7)
        val milesPerDay = DoubleArray(7) { 0.0 }
        val minsPerDay = DoubleArray(7) { 0.0 }

        val cutoff = LocalDate.now(tz).minusDays(6)
        for (r in allRuns) {
            val day = LocalDate.ofInstant(Instant.ofEpochMilli(r.startedAtMs), tz)
            if (!day.isBefore(cutoff)) {
                val idx = (day.toEpochDay() - cutoff.toEpochDay()).toInt()
                if (idx in 0..6) {
                    milesPerDay[idx] += r.distanceMiles
                    minsPerDay[idx] += r.elapsedMs / 60000.0
                }
            }
        }

        setData(b.chartMiles, labels, milesPerDay)
        setData(b.chartTime, labels, minsPerDay)
        updateSummary("This Week", milesPerDay.sum(), minsPerDay.sum(), runsCountInWindow(7))
    }

    // calculate + draw monthly data
    private fun renderMonthly() {
        val labels = lastNDaysLabels(30)
        val milesPerDay = DoubleArray(30) { 0.0 }
        val minsPerDay = DoubleArray(30) { 0.0 }

        val cutoff = LocalDate.now(tz).minusDays(29)
        for (r in allRuns) {
            val day = LocalDate.ofInstant(Instant.ofEpochMilli(r.startedAtMs), tz)
            if (!day.isBefore(cutoff)) {
                val idx = (day.toEpochDay() - cutoff.toEpochDay()).toInt()
                if (idx in 0..29) {
                    milesPerDay[idx] += r.distanceMiles
                    minsPerDay[idx] += r.elapsedMs / 60000.0
                }
            }
        }

        setData(b.chartMiles, labels, milesPerDay)
        setData(b.chartTime, labels, minsPerDay)
        updateSummary("This Month", milesPerDay.sum(), minsPerDay.sum(), runsCountInWindow(30))
    }

    // count how many runs fall in the selected time window
    private fun runsCountInWindow(days: Int): Int {
        val cutoff = LocalDate.now(tz).minusDays((days - 1).toLong())
        return allRuns.count {
            !LocalDate.ofInstant(Instant.ofEpochMilli(it.startedAtMs), tz).isBefore(cutoff)
        }
    }

    // make date labels for chart X-axis
    private fun lastNDaysLabels(n: Int): List<String> {
        val today = LocalDate.now(tz)
        val start = today.minusDays((n - 1).toLong())
        return (0 until n).map { i ->
            val d = start.plusDays(i.toLong())
            "${d.monthValue}/${d.dayOfMonth}"
        }
    }

    // apply arrays of values to a chart
    private fun setData(
        chart: com.github.mikephil.charting.charts.BarChart,
        labels: List<String>,
        values: DoubleArray
    ) {
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }

        val brickRed = 0xFFB64D4D.toInt()

        val set = BarDataSet(entries, "").apply {
            color = brickRed
            valueTextSize = 11f
            valueTextColor = 0xFF333333.toInt()
        }

        chart.data = BarData(set).apply {
            barWidth = 0.6f
        }

        chart.xAxis.valueFormatter =
            com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        chart.xAxis.labelRotationAngle = -40f
        chart.invalidate()

        b.tvEmpty.visibility =
            if (values.sum() == 0.0) android.view.View.VISIBLE else android.view.View.GONE
    }

    // update summary text at top of screen
    private fun updateSummary(title: String, miles: Double, minutes: Double, runs: Int) {
        b.tvSummaryTitle.text = title
        b.tvTotals.text = String.format("%.2f mi • %.0f min • %d runs", miles, minutes, runs)
    }
}
