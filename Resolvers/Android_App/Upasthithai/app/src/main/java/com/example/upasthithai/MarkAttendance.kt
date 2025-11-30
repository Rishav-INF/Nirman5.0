//package com.example.upasthithai
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.le.ScanCallback
//import android.bluetooth.le.ScanResult
//import android.content.Context
//import android.content.SharedPreferences
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.location.Location
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.cardview.widget.CardView
//import androidx.core.content.ContextCompat
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.maps.model.LatLng
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//
//class MarkAttendance : AppCompatActivity() {
//
//    private lateinit var sharedPreferences: SharedPreferences
//    private lateinit var studentName: String
//
//    private var geoStatusOk = false
//    private var bleStatusOk = false
//    private var faceStatusOk = false
//
//    private lateinit var geoCard: CardView
//    private lateinit var geoText: TextView
//    private lateinit var bleCard: CardView
//    private lateinit var bleText: TextView
//    private lateinit var faceCard: CardView
//    private lateinit var faceText: TextView
//    private lateinit var markBtn: Button
//
//    private val fixedLocation = LatLng(20.222367, 85.733762)
//    private val fixedRadius = 100f
//
//    // Camera launcher
//    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
//    private val capturedFaces = mutableListOf<Bitmap>()
//
//    // Permission launcher
//    private val requestAllPermissions =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val granted = permissions.all { it.value }
//            if (granted) {
//                Log.d("Permissions", "All permissions granted")
//                startBleScan(studentName)
//            } else {
//                Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
//                Log.e("Permissions", "Some permissions denied")
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.markattendance_student)
//
//        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        studentName = sharedPreferences.getString("username", "") ?: "Unknown"
//        Log.d("AppInit", "Student name loaded: $studentName")
//
//        geoCard = findViewById(R.id.geoCard)
//        geoText = findViewById(R.id.geoStatus)
//        bleCard = findViewById(R.id.bleCard)
//        bleText = findViewById(R.id.bleStatus)
//        faceCard = findViewById(R.id.faceCard)
//        faceText = findViewById(R.id.faceStatus)
//        markBtn = findViewById(R.id.markAttendanceBtn)
//
//        checkGeolocation()
//        setupCameraLauncher()
//
//        // request all permissions (BLE + Location + Camera) before scanning
//        requestNeededPermissions()
//
//        markBtn.setOnClickListener {
//            markAttendance()
//        }
//
//        faceCard.setOnClickListener {
//            openCamera()
//        }
//    }
//
//    // --- PERMISSIONS ---
//    private fun requestNeededPermissions() {
//        val permissions = mutableListOf<String>()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//        }
//        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
//        permissions.add(Manifest.permission.CAMERA)
//
//        val missing = permissions.any {
//            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//        }
//
//        if (missing) {
//            requestAllPermissions.launch(permissions.toTypedArray())
//        } else {
//            startBleScan(studentName)
//        }
//    }
//
//    // --- CAMERA ---
//    private fun setupCameraLauncher() {
//        takePictureLauncher =
//            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
//                if (bitmap != null) {
//                    capturedFaces.add(bitmap)
//                    faceStatusOk = true
//                    faceText.text = "Face Verified"
//                    faceCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
//                    enableMarkButtonIfReady()
//                    Log.d("FaceCheck", "Face captured successfully. Total stored: ${capturedFaces.size}")
//                } else {
//                    faceStatusOk = false
//                    faceText.text = "Face capture failed"
//                    faceCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
//                    Log.e("FaceCheck", "Face capture failed.")
//                }
//            }
//    }
//
//    private fun openCamera() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestNeededPermissions()
//        } else {
//            Log.d("Camera", "Opening camera")
//            takePictureLauncher.launch(null)
//        }
//    }
//
//    // --- GEOLOCATION ---
//    private fun checkGeolocation() {
//        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        try {
//            fusedLocationClient.lastLocation
//                .addOnSuccessListener { location ->
//                    if (location != null) {
//                        val inside = isInsideFixedRadius(location)
//                        geoStatusOk = inside
//                        geoText.text = if (inside) "Geolocation ok" else "Geolocation not mark"
//                        geoCard.setCardBackgroundColor(
//                           // if (inside)
//                                Color.parseColor("#A5D6A7")
//                            //else Color.parseColor("#EF9A9A")
//                        )
//                        Log.d("GeoCheck", "Location: ${location.latitude}, ${location.longitude}, inside=$inside")
//                    } else {
//                        geoStatusOk = false
//                        geoText.text = "Geolocation unavailable"
//                        geoCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
//                        Log.e("GeoCheck", "Location null")
//                    }
//                }
//        } catch (ex: SecurityException) {
//            geoStatusOk = false
//            geoText.text = "Location permission missing"
//            geoCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
//            Log.e("GeoCheck", "Location permission missing", ex)
//        }
//    }
//
//    private fun isInsideFixedRadius(location: Location): Boolean {
//        val result = FloatArray(1)
//        Location.distanceBetween(
//            location.latitude, location.longitude,
//            fixedLocation.latitude, fixedLocation.longitude,
//            result
//        )
//        return result[0] <= fixedRadius
//    }
//
//    // --- BLE ---
//    fun containsUUID(scanRecord: ByteArray, targetUUID: String): Boolean {
//        val cleanUUID = targetUUID.replace("-", "").lowercase()
//        val hexString = scanRecord.joinToString("") { "%02x".format(it) }
//        return hexString.contains(cleanUUID)
//    }
//
//    fun scanForBeaconUUID(
//        context: Context,
//        targetUUID: String,
//        onBeaconDetected: (Boolean) -> Unit
//    ) {
//        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
//            Log.e("BLEScan", "Bluetooth disabled or not available")
//            onBeaconDetected(false)
//            return
//        }
//
//        try {
//            val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//            var beaconFound = false
//
//            val scanCallback = object : ScanCallback() {
//                override fun onScanResult(callbackType: Int, result: ScanResult) {
//                    super.onScanResult(callbackType, result)
//                    val scanRecord = result.scanRecord?.bytes
//                    if (scanRecord != null && containsUUID(scanRecord, targetUUID)) {
//                        beaconFound = true
//                        try {
//                            bluetoothLeScanner.stopScan(this)
//                        } catch (e: SecurityException) {
//                            Log.e("BLEScan", "Stop scan failed", e)
//                        }
//                        Log.d("BLEScan", "Beacon detected with UUID: $targetUUID")
//                        onBeaconDetected(true)
//                    }
//                }
//
//                override fun onScanFailed(errorCode: Int) {
//                    Log.e("BLEScan", "Scan failed with error code $errorCode")
//                    onBeaconDetected(false)
//                }
//            }
//
//            bluetoothLeScanner.startScan(scanCallback)
//
//            Handler(Looper.getMainLooper()).postDelayed({
//                if (!beaconFound) {
//                    try {
//                        bluetoothLeScanner.stopScan(scanCallback)
//                    } catch (e: SecurityException) {
//                        Log.e("BLEScan", "Stop scan error", e)
//                    }
//                    Log.d("BLEScan", "No beacon detected after 10s")
//                    onBeaconDetected(false)
//                }
//            }, 10000)
//        } catch (e: SecurityException) {
//            Log.e("BLEScan", "BLE scan failed", e)
//            onBeaconDetected(false)
//        }
//    }
//
//    fun startBeaconScanLoop(
//        context: Context,
//        targetUUID: String,
//        onStatusChanged: (Boolean) -> Unit
//    ) {
//        val handler = Handler(Looper.getMainLooper())
//        val runnable = object : Runnable {
//            override fun run() {
//                scanForBeaconUUID(context, targetUUID) { isDetected ->
//                    onStatusChanged(isDetected)
//                    handler.postDelayed(this, 10000)
//                }
//            }
//        }
//        handler.post(runnable)
//    }
//
//    private fun startBleScan(studentName: String) {
//        Log.d("BLE", "Starting BLE scan for student: $studentName")
//        val database = FirebaseDatabase.getInstance().reference
//        val bleNodeRef = database.child("BLE").child("ble_id_1")
//
//        startBeaconScanLoop(
//            context = this,
//            targetUUID = "a134d0b2-1da2-1ba7-c94c-e8e00c9f7a2d"
//        ) { isDetected ->
//            // First check attendance status
//            bleNodeRef.child("attendance").get().addOnSuccessListener { snapshot ->
//                val attendanceStatus = snapshot.getValue(String::class.java) ?: "inactive"
//                val bleRef = bleNodeRef.child("students")
//
//                if (isDetected && attendanceStatus == "active") {
//                    bleRef.child("student99").setValue(studentName)
//                    bleStatusOk = true
//                    updateBleUI(true, "$studentName connected (Active)")
//                    Log.d("BLE", "Beacon detected & attendance active, student added")
//                } else {
//                    bleRef.child("student99").removeValue()
//                    bleStatusOk = false
//                    val reason = if (attendanceStatus != "active") "Attendance inactive" else "BLE not connected"
//                    updateBleUI(false, reason)
//                    Log.d("BLE", "Condition failed: $reason")
//                }
//            }.addOnFailureListener {
//                Log.e("BLE", "Failed to fetch attendance status", it)
//                updateBleUI(false, "Attendance status error")
//                bleStatusOk = false
//            }
//        }
//    }
//
//
//    private fun updateBleUI(isOk: Boolean, text: String) {
//        bleCard.setCardBackgroundColor(
//            if (isOk) Color.parseColor("#A5D6A7") else Color.parseColor("#EF9A9A")
//        )
//        bleText.text = text
//        enableMarkButtonIfReady()
//    }
//
//    private fun enableMarkButtonIfReady() {
//        markBtn.isEnabled = bleStatusOk && faceStatusOk
//        Log.d("Button", "Mark button enabled: ${markBtn.isEnabled}")
//    }
//
//    // --- MARK ATTENDANCE ---
//    private fun markAttendance() {
//        val database = FirebaseDatabase.getInstance().reference
//        val bleRef = database.child("BLE").child("ble_id_1")
//
//        bleRef.get().addOnSuccessListener { snapshot ->
//            if (!snapshot.exists()) {
//                Toast.makeText(this, "BLE data not found", Toast.LENGTH_SHORT).show()
//                return@addOnSuccessListener
//            }
//
//            val studentsNode = snapshot.child("students")
//            val teacherName = snapshot.child("teacher").getValue(String::class.java)
//
//            if (teacherName.isNullOrEmpty()) {
//                Toast.makeText(this, "Teacher not found in BLE data", Toast.LENGTH_SHORT).show()
//                return@addOnSuccessListener
//            }
//
//            // find studentId from BLE list
//            var studentId: String? = null
//            for (student in studentsNode.children) {
//                val name = student.getValue(String::class.java)
//                if (name == studentName) {
//                    studentId = student.key
//                    break
//                }
//            }
//
//            if (studentId == null) {
//                Toast.makeText(this, "Student not found in BLE list", Toast.LENGTH_SHORT).show()
//                return@addOnSuccessListener
//            }
//
//            // now look inside teachers node only
//            val teachersRef = database.child("teachers")
//            teachersRef.get().addOnSuccessListener { teachersSnapshot ->
//                var teacherRef: DatabaseReference? = null
//
//                for (teacherNode in teachersSnapshot.children) {
//                    val nameValue = teacherNode.child("name").getValue(String::class.java)
//                    if (nameValue == teacherName) {
//                        teacherRef = teacherNode.ref.child("subjects_attendance")
//                        break
//                    }
//                }
//
//                if (teacherRef == null) {
//                    Toast.makeText(this, "Teacher not found in DB", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                // update attendance for all subjects where this student exists
//                teacherRef.get().addOnSuccessListener { subjectsSnapshot ->
//                    for (subject in subjectsSnapshot.children) {
//                        val studentValue = subject.child(studentId!!).getValue(String::class.java)
//                        if (studentValue != null) {
//                            val parts = studentValue.split("/")
//                            if (parts.size == 2) {
//                                val attended = parts[0].toIntOrNull() ?: 0
//                                val total = parts[1].toIntOrNull() ?: 0
//                                val newAttended = attended + 1
//                                val newTotal = total
//                                val updatedValue = "$newAttended/$newTotal"
//
//                                teacherRef.child(subject.key!!).child(studentId!!).setValue(updatedValue)
//                                Log.d("Attendance", "Updated $studentId in ${subject.key} to $updatedValue")
//                            }
//                        }
//                    }
//                    Toast.makeText(this, "Attendance marked for $studentName", Toast.LENGTH_SHORT).show()
//                }.addOnFailureListener { e ->
//                    Log.e("Attendance", "Failed to update attendance", e)
//                    Toast.makeText(this, "Error updating attendance", Toast.LENGTH_SHORT).show()
//                }
//
//            }.addOnFailureListener { e ->
//                Log.e("Attendance", "Failed to fetch teachers", e)
//                Toast.makeText(this, "Error fetching teacher data", Toast.LENGTH_SHORT).show()
//            }
//
//        }.addOnFailureListener { e ->
//            Log.e("Attendance", "Failed to fetch BLE data", e)
//            Toast.makeText(this, "Error fetching BLE data", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//
//}
//package com.example.upasthithai
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.le.ScanCallback
//import android.bluetooth.le.ScanResult
//import android.content.Context
//import android.content.Intent
//import android.content.SharedPreferences
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.location.Location
//import android.os.*
//import android.util.Log
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.cardview.widget.CardView
//import androidx.core.content.ContextCompat
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.maps.model.LatLng
//import com.google.firebase.database.*
//import com.yourname.facerecognitionapp.FaceRecognitionActivity
//
//class MarkAttendance : AppCompatActivity() {
//
//    private lateinit var sharedPreferences: SharedPreferences
//
//    private lateinit var studentName: String
//    private lateinit var studentId: String
//    private lateinit var userType: String
//
//    private var geoStatusOk = false
//    private var bleStatusOk = false
//    private var faceStatusOk = false
//
//    private lateinit var geoCard: CardView
//    private lateinit var geoText: TextView
//    private lateinit var bleCard: CardView
//    private lateinit var bleText: TextView
//    private lateinit var faceCard: CardView
//    private lateinit var faceText: TextView
//    private lateinit var markBtn: Button
//
//    private val fixedLocation = LatLng(20.222367, 85.733762)
//    private val fixedRadius = 100f
//
//    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
//    private val capturedFaces = mutableListOf<Bitmap>()
//
//    private var bleScannerRunning = false
//    private var bleScanHandler: Handler? = null
//    private val scanInterval = 7000L
//    private val scanDuration = 5000L
//    private val targetUUID = "a134d0b2-1da2-1ba7-c94c-e8e00c9f7a2d"
//    private val cleanUUID = targetUUID.replace("-", "").lowercase()
//    private val bleNode = "BLE_1"
//
//    private val requestAllPermissions =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
//            if (it.all { perm -> perm.value }) startBleScan()
//            else Toast.makeText(this, "Permissions are required", Toast.LENGTH_SHORT).show()
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.markattendance_student)
//
//        sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
//        studentName = sharedPreferences.getString("username", "") ?: "Unknown"
//        studentId = sharedPreferences.getString("userId", "") ?: ""
//        userType = sharedPreferences.getString("userType", "") ?: ""
//
//        geoCard = findViewById(R.id.geoCard)
//        geoText = findViewById(R.id.geoStatus)
//        bleCard = findViewById(R.id.bleCard)
//        bleText = findViewById(R.id.bleStatus)
//        faceCard = findViewById(R.id.faceCard)
//        faceText = findViewById(R.id.faceStatus)
//        markBtn = findViewById(R.id.markAttendanceBtn)
//
//        checkGeolocation()
//        setupCameraLauncher()
//        requestPermissions()
//
//        faceCard.setOnClickListener {
//            startActivity(Intent(this, FaceRecognitionActivity ::class.java))
//        }
//        markBtn.setOnClickListener { markAttendance() }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        startBleScan()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        stopBleScan()
//    }
//
//    private fun requestPermissions() {
//        val permissions = mutableListOf<String>()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
//            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
//        }
//        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
//        permissions.add(Manifest.permission.CAMERA)
//
//        val missing = permissions.any {
//            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//        }
//        if (missing) requestAllPermissions.launch(permissions.toTypedArray())
//    }
//
//    private fun setupCameraLauncher() {
//        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
//            if (bmp != null) {
//                capturedFaces.add(bmp)
//                faceStatusOk = true
//                faceText.text = "Face Verified"
//                faceCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
//                enableMarkButtonIfReady()
//            } else {
//                faceStatusOk = false
//                faceText.text = "Face Not Verified"
//                faceCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
//            }
//        }
//    }
//
//    private fun openCamera() = takePictureLauncher.launch(null)
//
//    private fun checkGeolocation() {
//        val flClient = LocationServices.getFusedLocationProviderClient(this)
//
//        try {
//            flClient.lastLocation.addOnSuccessListener { location ->
//                if (location != null) {
//                    val inside = isInside(location)
//                    geoStatusOk = inside
//                    geoText.text = if (inside) "Geolocation OK" else "Outside Location"
//                    geoCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
//                } else {
//                    geoStatusOk = false
//                    geoText.text = "Location Unavailable"
//                    geoCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
//                }
//            }
//        } catch (_: SecurityException) {
//        }
//    }
//
//    private fun isInside(location: Location): Boolean {
//        val result = FloatArray(1)
//        Location.distanceBetween(location.latitude, location.longitude, fixedLocation.latitude, fixedLocation.longitude, result)
//        return result[0] <= fixedRadius
//    }
//
//    private fun startBleScan() {
//        if (bleScannerRunning) return
//        bleScannerRunning = true
//
//        val adapter = BluetoothAdapter.getDefaultAdapter()
//        if (adapter == null || !adapter.isEnabled) {
//            updateBleUI(false, "Bluetooth Off")
//            return
//        }
//
//        val scanner = adapter.bluetoothLeScanner
//        bleScanHandler = Handler(Looper.getMainLooper())
//
//        var detectedThisCycle = false
//
//        val callback = object : ScanCallback() {
//            override fun onScanResult(type: Int, result: ScanResult) {
//                result.scanRecord?.bytes?.let {
//                    if (it.toHex().contains(cleanUUID)) {
//                        if (!detectedThisCycle) {
//                            detectedThisCycle = true
//                            updateFirebasePresence(true)
//                            bleStatusOk = true
//                            updateBleUI(true, "BLE Connected")
//                        }
//                    }
//                }
//            }
//        }
//
//        val loop = object : Runnable {
//            override fun run() {
//                if (!bleScannerRunning) return
//
//                scanner.startScan(callback)
//
//                bleScanHandler!!.postDelayed({
//                    scanner.stopScan(callback)
//
//                    if (!detectedThisCycle) {
//                        updateFirebasePresence(false)
//                        bleStatusOk = false
//                        updateBleUI(false, "Out of Range")
//                    }
//
//                    detectedThisCycle = false
//                    bleScanHandler!!.postDelayed(this, scanInterval)
//                }, scanDuration)
//            }
//        }
//
//        bleScanHandler!!.post(loop)
//    }
//
//    private fun stopBleScan() {
//        bleScannerRunning = false
//        bleScanHandler?.removeCallbacksAndMessages(null)
//    }
//
//    private fun updateFirebasePresence(inRange: Boolean) {
//        val db = FirebaseDatabase.getInstance().reference
//            .child("NEW")
//            .child("BLE")
//            .child(bleNode)
//            .child("inRangeDevices")
//
//        val path = if (userType == "student") "students" else "teacher"
//
//        if (inRange) {
//            db.child(path).child(studentId).setValue(studentName)
//        } else {
//            db.child(path).child(studentId).removeValue()
//        }
//    }
//
//    private fun updateBleUI(isOk: Boolean, text: String) {
//        bleText.text = text
//        bleCard.setCardBackgroundColor(
//            if (isOk) Color.parseColor("#A5D6A7") else Color.parseColor("#EF9A9A")
//        )
//        enableMarkButtonIfReady()
//    }
//
//    private fun enableMarkButtonIfReady() {
//        markBtn.isEnabled = bleStatusOk && faceStatusOk
//    }
//
//    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
//
//    // ========================= MARK ATTENDANCE ================================
//    private fun markAttendance() {
//
//        val db = FirebaseDatabase.getInstance().reference.child("NEW")
//        val currentDay = getCurrentDay()
//        val currentTime = getCurrentTime()
//
//        val bleRef = db.child("BLE").child(bleNode)
//
//        bleRef.get().addOnSuccessListener { bleSnap ->
//
//            val className = bleSnap.child("class").value?.toString() ?: ""
//
//            val studentClassRef = db.child("classes").child(className)
//                .child("students").child(studentId)
//
//            studentClassRef.get().addOnSuccessListener { stuSnap ->
//
//                if (!stuSnap.exists()) {
//                    Toast.makeText(this, "You do not belong to this class", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                val teacherSnap = bleSnap.child("inRangeDevices/teacher")
//                if (!teacherSnap.children.iterator().hasNext()) {
//                    Toast.makeText(this, "No teacher present here", Toast.LENGTH_LONG).show()
//                    return@addOnSuccessListener
//                }
//
//                val teacherId = teacherSnap.children.first().key.toString()
//
//                val teacherTableRef = db.child("teachers").child(teacherId).child("timeTable")
//
//                teacherTableRef.child(currentDay).get().addOnSuccessListener { daySnap ->
//
//                    var subject: String? = null
//                    var teachesNow = false
//
//                    for (period in daySnap.children) {
//                        val (start, end) = period.key!!.split(" - ")
//                        if (isWithinRange(currentTime, start, end)) {
//
//                            val timetableClass = period.child("class").value.toString()
//                            if (timetableClass == className.replace("class ", "")) {
//                                teachesNow = true
//                                subject = period.child("subject").value.toString()
//                            }
//                            break
//                        }
//                    }
//
//                    if (!teachesNow) {
//                        Toast.makeText(this, "Teacher not assigned to teach this class currently", Toast.LENGTH_LONG).show()
//                        return@addOnSuccessListener
//                    }
//
//                    val attendanceRef = studentClassRef.child("subjects").child("attendance").child(subject!!)
//                    attendanceRef.get().addOnSuccessListener { attSnap ->
//
//                        val (a, t) = attSnap.value.toString().split("/").map { it.toInt() }
//                        val newVal = "${a + 1}/${t + 1}"
//
//                        attendanceRef.setValue(newVal)
//                        Toast.makeText(this, "Attendance Updated Successfully", Toast.LENGTH_LONG).show()
//
//                        markBtn.isEnabled = false
//                    }
//                }
//            }
//        }
//    }
//
//    private fun getCurrentDay(): String =
//        java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
//
//    private fun getCurrentTime(): String =
//        java.time.LocalTime.now().toString().substring(0,5)
//
//    private fun isWithinRange(now: String, start: String, end: String): Boolean =
//        now >= start && now <= end
//}



package com.example.upasthithai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.yourname.facerecognitionapp.FaceRecognitionActivity

class MarkAttendance : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var studentName: String
    private lateinit var studentId: String
    private lateinit var userType: String

    private var geoStatusOk = false
    private var bleStatusOk = false
    private var faceStatusOk = false // stays

    private lateinit var geoCard: CardView
    private lateinit var geoText: TextView
    private lateinit var bleCard: CardView
    private lateinit var bleText: TextView
    private lateinit var faceCard: CardView
    private lateinit var faceText: TextView
    private lateinit var markBtn: Button

    private val fixedLocation = LatLng(20.222367, 85.733762)
    private val fixedRadius = 100f

    private lateinit var takePictureLauncher: ActivityResultLauncher<Void?>
    private val capturedFaces = mutableListOf<Bitmap>()

    private val bleNode = "BLE_1"
    private val handler = Handler(Looper.getMainLooper())
    private val targetUUID = "a134d0b2-1da2-1ba7-c94c-e8e00c9f7a2d"
    private val cleanUUID = targetUUID.replace("-", "").lowercase()

    private val requestAllPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.all { perm -> perm.value }) startBleCheckLoop()
            else Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.markattendance_student)

        sharedPreferences = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        studentName = sharedPreferences.getString("username", "") ?: "Unknown"
        studentId = sharedPreferences.getString("userId", "") ?: ""
        userType = sharedPreferences.getString("userType", "") ?: ""

        geoCard = findViewById(R.id.geoCard)
        geoText = findViewById(R.id.geoStatus)
        bleCard = findViewById(R.id.bleCard)
        bleText = findViewById(R.id.bleStatus)
        faceCard = findViewById(R.id.faceCard)
        faceText = findViewById(R.id.faceStatus)
        markBtn = findViewById(R.id.markAttendanceBtn)

        checkGeolocation()
        setupCameraLauncher()
        requestPermissions()

        faceCard.setOnClickListener {
            startActivity(Intent(this, FaceRecognitionActivity::class.java))
        }

        markBtn.setOnClickListener { markAttendance() }
    }

    override fun onResume() {
        super.onResume()
        setBlePresence(true)
        startBleCheckLoop()

        // ================= READ FACE MATCH STATUS FROM FIREBASE =====================
        val db = FirebaseDatabase.getInstance().reference
            .child("NEW")
            .child("classes")

        db.get().addOnSuccessListener { snap ->
            snap.children.forEach { classNode ->
                if (classNode.child("students").child(studentId).exists()) {

                    val faceMatchValue =
                        classNode.child("students").child(studentId)
                            .child("faceMatched").value?.toString() ?: "no"

                    if (faceMatchValue == "yes") {
                        faceStatusOk = true
                        faceText.text = "Face Verified"
                        faceCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
                    } else {
                        faceStatusOk = false
                        faceText.text = "Face Not Verified"
                        faceCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
                    }
                }
            }

            enableMarkButtonIfReady()
        }.addOnFailureListener {
            Log.e("FACE_READ", "Error reading faceMatched", it)
        }
        // =============================================================================
    }


    override fun onPause() {
        super.onPause()
        setBlePresence(false)
        handler.removeCallbacksAndMessages(null)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestAllPermissions.launch(permissions.toTypedArray())
        }
    }

    private fun setupCameraLauncher() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
                if (bmp != null) {
                    capturedFaces.add(bmp)

                    // ================ NEW CODE: Save success =================
                    val facePrefs = getSharedPreferences("face_pref", Context.MODE_PRIVATE)
                    facePrefs.edit().putBoolean("face_ok", true).apply()
                    // =========================================================

                    faceStatusOk = true
                    faceText.text = "Face Verified"
                    faceCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
                    enableMarkButtonIfReady()
                } else {
                    faceStatusOk = false
                    faceText.text = "Face Not Verified"
                    faceCard.setCardBackgroundColor(Color.parseColor("#EF9A9A"))
                }
            }
    }

    private fun openCamera() = takePictureLauncher.launch(null)

    private fun checkGeolocation() {
        geoStatusOk = true
        geoText.text = "Inside Class"
        geoCard.setCardBackgroundColor(Color.parseColor("#A5D6A7"))
        enableMarkButtonIfReady()
    }

    private fun setBlePresence(active: Boolean) {
        val bleRoot = FirebaseDatabase.getInstance().reference
            .child("NEW").child("BLE").child(bleNode)

        val dbInRange = bleRoot.child("inRangeDevices")
        val path = if (userType == "teacher") "teacher" else "students"

        bleRoot.child("SWITCH").get().addOnSuccessListener { switchSnap ->
            val switchState = switchSnap.value?.toString() ?: "off"

            if (switchState != "on") {
                dbInRange.child(path).child(studentId).removeValue()
                bleStatusOk = false
                updateBleUI(false, "BLE Disabled")
                return@addOnSuccessListener
            }

            if (active) {
                val database = FirebaseDatabase.getInstance().reference
                    .child("NEW").child("classes")

                database.get().addOnSuccessListener { snapshot ->
                    var classNameFound: String? = null
                    for (cls in snapshot.children) {
                        if (cls.child("students").child(studentId).exists()) {
                            classNameFound = cls.key
                            break
                        }
                    }

                    val realName = snapshot.child(classNameFound!!)
                        .child("students").child(studentId)
                        .child("Name").value?.toString() ?: studentName

                    dbInRange.child(path).child(studentId).setValue(realName)
                }

            } else dbInRange.child(path).child(studentId).removeValue()
        }
    }

    private fun startBleCheckLoop() {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled) {
            updateBleUI(false, "Bluetooth Off")
            return
        }

        val runnable = object : Runnable {
            override fun run() {

                scanForBleDevice { detected ->
                    bleStatusOk = detected
                    updateBleUI(detected, if (detected) "BLE Connected" else "BLE Not Detected")
                }

                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)
    }

    private fun scanForBleDevice(callback: (Boolean) -> Unit) {
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        val scanner = adapter.bluetoothLeScanner

        var found = false

        val callbackScan = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(type: Int, result: android.bluetooth.le.ScanResult) {
                super.onScanResult(type, result)
                result.scanRecord?.bytes?.let {
                    val hex = it.joinToString("") { b -> "%02x".format(b) }
                    if (hex.contains(cleanUUID)) {
                        found = true
                        scanner.stopScan(this)
                        callback(true)
                    }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                callback(false)
            }
        }

        scanner.startScan(callbackScan)
        handler.postDelayed({
            if (!found) {
                scanner.stopScan(callbackScan)
                callback(false)
            }
        }, 3000)
    }

    private fun updateBleUI(isOk: Boolean, text: String) {
        bleText.text = text
        bleCard.setCardBackgroundColor(
            if (isOk) Color.parseColor("#A5D6A7") else Color.parseColor("#EF9A9A")
        )
        enableMarkButtonIfReady()
    }

    private fun enableMarkButtonIfReady() {
        markBtn.isEnabled = bleStatusOk && faceStatusOk
    }

    // ========================= MARK ATTENDANCE ================================
    // ========================= MARK ATTENDANCE ================================
// ========================= MARK ATTENDANCE ================================
    // ========================= MARK ATTENDANCE ================================
    private fun markAttendance() {

        val db = FirebaseDatabase.getInstance().reference.child("NEW")
        val studentId = sharedPreferences.getString("userId", "") ?: ""  // EX: 2023CSE001

        db.child("classes").get().addOnSuccessListener { classesSnap ->

            var classNameFound: String? = null

            // ============ SEARCH FOR THE STUDENTID IN ALL CLASSES ============
            for (cls in classesSnap.children) {
                if (cls.child("students").child(studentId).exists()) {
                    classNameFound = cls.key   // ex: "1"
                    break
                }
            }

            if (classNameFound == null) {
                Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            // ==================================================================

            val studentClassRef = db.child("classes")
                .child(classNameFound!!)
                .child("students")
                .child(studentId)

            studentClassRef.get().addOnSuccessListener { studentSnap ->

                val attendanceNode = studentSnap.child("subjects").child("attendance")
                val firstSubjectKey = attendanceNode.children.firstOrNull()?.key

                if (firstSubjectKey == null) {
                    Toast.makeText(this, "No subjects found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val attendanceRef = studentClassRef
                    .child("subjects")
                    .child("attendance")
                    .child(firstSubjectKey)

                attendanceRef.get().addOnSuccessListener { attSnap ->

                    val oldValue = attSnap.value?.toString() ?: "0/0"
                    val (attended, total) = oldValue.split("/").map { it.toInt() }

                    val newValue = "${attended + 1}/${total + 1}"

                    attendanceRef.setValue(newValue).addOnSuccessListener {
                        Toast.makeText(this,
                            "Attendance Marked → $firstSubjectKey : $newValue",
                            Toast.LENGTH_SHORT
                        ).show()
                        markBtn.isEnabled = false
                    }
                }
            }
        }
    }





    private fun getCurrentDay(): String =
        java.time.LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun getCurrentTime(): String =
        java.time.LocalTime.now().toString().substring(0, 5)

    private fun isWithinRange(now: String, start: String, end: String): Boolean =
        now >= start && now <= end
}



