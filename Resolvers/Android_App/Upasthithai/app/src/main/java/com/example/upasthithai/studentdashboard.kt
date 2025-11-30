package com.example.upasthithai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.upasthithai.analytics.studentAnalytics

class studentdashboard : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.student_dashboard) // Use your XML filename
        val analytics =findViewById<TextView>(R.id.analyticsButton)
        val chatbotActivity=findViewById<Button>(R.id.chatbotButton)
        val attendanceCardText = findViewById<TextView>(R.id.attendanceGive)
        val liveMapText = findViewById<TextView>(R.id.liveMapText)
        val requestsText = findViewById<TextView>(R.id.requestsText)
        //val hoursWorkedTextView = findViewById<TextView>(R.id.timeFetched_student)
        val markattendance = findViewById<TextView>(R.id.attendanceGive)
        // Navigate to Live Map Activity

        chatbotActivity.setOnClickListener {
            val intent = Intent(this, chatbotActivity::class.java)
            startActivity(intent)
        }
        liveMapText.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        markattendance.setOnClickListener{
            val intent = Intent(this, MarkAttendance::class.java)
            startActivity(intent)
        }
        analytics.setOnClickListener {
            val intent = Intent(this, studentAnalytics::class.java)
            startActivity(intent)
        }


        // Navigate to Requests Activity
//        requestsText.setOnClickListener {
//            val intent = Intent(this, Requeststudent::class.java)
//            startActivity(intent)
//        }
//
//        // Navigate to Mark Attendance Activity
//        attendanceCardText.setOnClickListener {
//            val intent = Intent(this, MarkAttendanceActivity::class.java)
//            startActivity(intent)
//        }

        // Get shared preferences for total class time
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val hours_worked = sharedPreferences.getString("hours_worked", "0").toString().toLongOrNull() ?: 0L
        val hours = hours_worked / 3600
        val minutes = (hours_worked % 3600) / 60
        val seconds = hours_worked % 60

        val workedTimeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        //hoursWorkedTextView.text = workedTimeFormatted
    }
}
