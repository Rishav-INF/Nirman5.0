package com.example.upasthithai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class teacherdashboard : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacher_dashboard)

        val requests = findViewById<TextView>(R.id.requestsText_teacher)
        val map = findViewById<TextView>(R.id.liveMapText_teacher)
//        val hoursWorkedTextView = findViewById<TextView>(R.id.timeFetched)
        val takeattendance = findViewById<TextView>(R.id.attendanceText)

        // Click listeners
        map.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        requests.setOnClickListener {
            startActivity(Intent(this, Requestemployee::class.java))
        }

        takeattendance.setOnClickListener {
            startActivity(Intent(this, takeAttendance::class.java))
        }

        // Fetch hours worked safely
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val hoursWorkedStr = sharedPreferences.getString("hours_worked", "0") // default to "0" if null
        val hoursWorked = hoursWorkedStr?.toLongOrNull() ?: 0L

        val hours = hoursWorked / 3600
        val minutes = (hoursWorked % 3600) / 60
        val seconds = hoursWorked % 60

        val workedTimeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        //hoursWorkedTextView.text = workedTimeFormatted
    }
}
