package com.example.upasthithai.analytics

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.upasthithai.R
import com.google.firebase.database.FirebaseDatabase

class teacherAnalytics : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var teacherId: String

    private lateinit var currentSubjectValue: TextView
    private lateinit var currentClassValue: TextView
    private lateinit var todayTimetableList: LinearLayout
    private lateinit var attendanceSummaryList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.teacheranalytics)

        sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        teacherId = sharedPreferences.getString("userId", "") ?: ""

        currentSubjectValue = findViewById(R.id.currentSubjectValue)
        currentClassValue = findViewById(R.id.currentClassValue)
        todayTimetableList = findViewById(R.id.todayTimetableList)
        attendanceSummaryList = findViewById(R.id.attendanceSummaryList)

        loadCurrentSession()
        loadTodaysTimetable()
    }

    private fun loadCurrentSession() {
        val db = FirebaseDatabase.getInstance().reference.child("NEW")
        val currentDay = "Friday" // temporary override for testing purposes

        db.child("teachers").child(teacherId).child("timeTable").child(currentDay)
            .get().addOnSuccessListener { daySnap ->

                if (!daySnap.exists()) {
                    currentSubjectValue.text = "No class scheduled"
                    currentClassValue.text = "--"
                    Log.e("ANALYTICS", "No timetable for teacher $teacherId on $currentDay")
                    return@addOnSuccessListener
                }

                val firstPeriod = daySnap.children.first()
                val className = firstPeriod.child("class").value.toString()
                val subject = firstPeriod.child("subject").value.toString()

                var formattedClass = className
                if (!formattedClass.contains("class", ignoreCase = true))
                    formattedClass = "class $formattedClass"

                currentClassValue.text = formattedClass
                currentSubjectValue.text = subject

                Log.d("ANALYTICS", "Current: $formattedClass, $subject")

                loadClassSubjectAttendance(formattedClass, subject)
            }
    }

    private fun loadTodaysTimetable() {
        val db = FirebaseDatabase.getInstance().reference.child("NEW")
        val currentDay = "Friday"

        Log.d("ANALYTICS", "Loading timetable for teacherId $teacherId")

        db.child("teachers").child(teacherId).child("timeTable").child(currentDay)
            .get().addOnSuccessListener { snap ->

                todayTimetableList.removeAllViews()
                attendanceSummaryList.removeAllViews()

                if (!snap.exists()) {
                    Log.e("ANALYTICS", "No timetable found")
                    return@addOnSuccessListener
                }

                for (period in snap.children) {

                    val classNameRaw = period.child("class").value.toString()
                    val subject = period.child("subject").value.toString()

                    var formattedClass = classNameRaw
                    if (!formattedClass.contains("class", ignoreCase = true))
                        formattedClass = "class $formattedClass"

                    Log.d("ANALYTICS", "Timetable entry: $formattedClass, $subject")

                    val line = TextView(this)
                    line.textSize = 16f
                    line.text = "${period.key} | Class: $formattedClass | Subject: $subject"
                    todayTimetableList.addView(line)

                    loadClassSubjectAttendance(formattedClass, subject)
                }
            }
    }

    private fun loadClassSubjectAttendance(className: String, subject: String) {

        val db = FirebaseDatabase.getInstance().reference.child("NEW")
        Log.d("ANALYTICS", "Fetching students for $className and subject $subject")

        db.child("classes").child(className).child("students").get()
            .addOnSuccessListener { studSnap ->

                if (!studSnap.exists()) {
                    Log.e("ANALYTICS", "No students found for $className")
                    return@addOnSuccessListener
                }

                val classHeader = TextView(this)
                classHeader.textSize = 18f
                classHeader.setTextColor(Color.BLACK)
                classHeader.text = "Class: $className - Subject: $subject"
                attendanceSummaryList.addView(classHeader)

                for (student in studSnap.children) {

                    val studentName =
                        student.child("Name").value?.toString() ?: "Unknown"

                    val attendanceVal =
                        student.child("subjects").child("attendance")
                            .child(subject).value?.toString()

                    Log.d("ANALYTICS",
                        "Student: $studentName | Raw: $attendanceVal")

                    val percentage = if (attendanceVal != null && attendanceVal.contains("/")) {
                        val (attended, total) = attendanceVal.split("/").map { it.toInt() }
                        if (total > 0) (attended.toDouble() / total.toDouble()) * 100 else 0.0
                    } else 0.0

                    val studentRow = TextView(this)
                    studentRow.textSize = 15f
                    studentRow.text = "     $studentName — ${String.format("%.1f", percentage)}%"

                    if (percentage < 75) studentRow.setTextColor(Color.RED)
                    else studentRow.setTextColor(Color.DKGRAY)

                    attendanceSummaryList.addView(studentRow)
                }
            }
            .addOnFailureListener {
                Log.e("ANALYTICS", "Failed to fetch attendance data", it)
                Toast.makeText(this, "Error fetching attendance", Toast.LENGTH_SHORT).show()
            }
    }
}
