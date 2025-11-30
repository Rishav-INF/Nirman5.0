package com.example.upasthithai.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.upasthithai.R
import com.google.firebase.database.FirebaseDatabase

class studentAnalytics : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var studentId: String
    private lateinit var studentName: String
    private var className: String = ""

    private lateinit var subjectValue: TextView
    private lateinit var teacherValue: TextView
    private lateinit var attendanceList: LinearLayout
    private lateinit var criticalList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.studentanalytics)

        sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        studentId = sharedPreferences.getString("userId", "") ?: ""
        studentName = sharedPreferences.getString("username", "") ?: "Unknown"

        subjectValue = findViewById(R.id.currentSubjectValue)
        teacherValue = findViewById(R.id.currentTeacherValue)
        attendanceList = findViewById(R.id.attendanceList)
        criticalList = findViewById(R.id.criticalList)

        findStudentClass()
    }

    private fun findStudentClass() {
        val classRef = FirebaseDatabase.getInstance().reference
            .child("NEW").child("classes")

        classRef.get().addOnSuccessListener { snap ->
            for (cls in snap.children) {
                if (cls.child("students").child(studentId).exists()) {
                    className = cls.key.toString()
                    Log.d("Analytics", "Student belongs to class $className")
                    break
                }
            }

            if (className.isNotEmpty()) {
                loadCurrentSubject()
                loadAttendance()
            } else {
                Toast.makeText(this, "Class not assigned", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Log.e("Analytics", "Class fetch failed: ${it.message}")
        }
    }

    private fun loadCurrentSubject() {

        val bleRef = FirebaseDatabase.getInstance().reference
            .child("NEW").child("BLE")

        bleRef.get().addOnSuccessListener { snap ->
            var teacherId = ""

            for (node in snap.children) {
                val tSnap = node.child("inRangeDevices").child("teacher")
                if (tSnap.exists()) {
                    teacherId = tSnap.children.first().key ?: ""
                    break
                }
            }

            Log.d("Analytics", "Teacher detected = $teacherId")

            if (teacherId.isEmpty()) {
                subjectValue.text = "No teacher in range"
                teacherValue.text = "Unavailable"
                return@addOnSuccessListener
            }

            val timetableRef = FirebaseDatabase.getInstance().reference
                .child("NEW").child("teachers").child(teacherId).child("timeTable")

            // FORCED TESTING MODE
            val currentDay = "Friday"
            val currentTime = "10:00"   // Force test period

            Log.d("Analytics", "Testing for day=$currentDay time=$currentTime")

            timetableRef.child(currentDay).get().addOnSuccessListener { daySnap ->

                var foundSubject: String? = null

                for (period in daySnap.children) {
                    val (start, end) = period.key!!.split(" - ")
                    Log.d("Analytics", "Checking Period = $start - $end")

                    if (isWithinRange(currentTime, start, end)) {
                        val timetableClass = period.child("class").value.toString()
                        if (timetableClass == className.replace("class ", "")) {
                            foundSubject = period.child("subject").value.toString()
                            break
                        }
                    }
                }

                subjectValue.text = foundSubject ?: "No active subject"
                teacherValue.text = "Teacher: $teacherId"
                Log.d("Analytics", "Found Subject = $foundSubject")

            }.addOnFailureListener {
                Log.e("Analytics", "Failed to load timetable: ${it.message}")
            }

        }.addOnFailureListener {
            Log.e("Analytics", "Failed to fetch BLE teacher data: ${it.message}")
        }
    }

    private fun loadAttendance() {

        val studentClassRef = FirebaseDatabase.getInstance().reference
            .child("NEW").child("classes").child(className)
            .child("students").child(studentId).child("subjects").child("attendance")

        studentClassRef.get().addOnSuccessListener { snap ->

            attendanceList.removeAllViews()
            val critical = mutableListOf<String>()

            for (sub in snap.children) {
                val value = sub.value.toString()
                val (a, t) = value.split("/").map { it.toInt() }
                val percentage = (a.toDouble() / t.toDouble()) * 100

                Log.d("Analytics", "Fetched attendance: $sub.key = $a/$t ($percentage%)")

                val subjectText = TextView(this)
                subjectText.text = "${sub.key}: $value  (${percentage.toInt()}%)"
                subjectText.textSize = 18f
                subjectText.setTextColor(resources.getColor(R.color.black))
                attendanceList.addView(subjectText)

                if (percentage < 75) critical.add("${sub.key} (${percentage.toInt()}%)")
            }

            criticalList.text = if (critical.isEmpty()) "None" else critical.joinToString("\n")

        }.addOnFailureListener {
            Toast.makeText(this, "Error loading attendance", Toast.LENGTH_SHORT).show()
            Log.e("Analytics", "Attendance fetch failed: ${it.message}")
        }
    }

    private fun isWithinRange(now: String, start: String, end: String) =
        now >= start && now <= end
}
