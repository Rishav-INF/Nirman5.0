package com.example.upasthithai

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class takeAttendance : AppCompatActivity() {

    private lateinit var btnToggleAttendance: Button
    private lateinit var tvDate: TextView

    // NEW Firebase reference
    private val bleRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("NEW")
            .child("BLE")
            .child("BLE_1")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_attendance)

        btnToggleAttendance = findViewById(R.id.btnToggleAttendance)
        tvDate = findViewById(R.id.tvDate)

        // Set today's date
        tvDate.text = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())

        // Listener for SWITCH state changes
        bleRef.child("SWITCH").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val switchState = snapshot.getValue(String::class.java) ?: "off"
                updateUI(switchState)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Toggle switch on click
        btnToggleAttendance.setOnClickListener { toggleAttendanceSwitch() }
    }

    private fun toggleAttendanceSwitch() {
        val switchRef = bleRef.child("SWITCH")

        switchRef.get().addOnSuccessListener { snapshot ->
            val current = snapshot.getValue(String::class.java) ?: "off"
            val newState = if (current == "on") "off" else "on"

            switchRef.setValue(newState).addOnSuccessListener {
                Toast.makeText(
                    this,
                    if (newState == "on") "Attendance Enabled" else "Attendance Disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to update switch!", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener {
            Toast.makeText(this, "Error reading state!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(state: String) {
        if (state == "on") {
            btnToggleAttendance.text = "Attendance ACTIVE (Tap to Stop)"
            btnToggleAttendance.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, theme))
        } else {
            btnToggleAttendance.text = "Start Attendance"
            btnToggleAttendance.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, theme))
        }
    }
}
