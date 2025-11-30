package com.yourname.facerecognitionapp

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.upasthithai.databinding.ActivityMainBinding
import com.example.upasthithai.databinding.DialogRegisterFaceBinding
import com.google.firebase.database.FirebaseDatabase
import com.yourname.facerecognitionapp.database.FaceDatabase
import com.yourname.facerecognitionapp.database.FaceEntity
import com.yourname.facerecognitionapp.ml.SimpleFaceMLKit
import com.yourname.facerecognitionapp.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceRecognition: SimpleFaceMLKit
    private lateinit var database: FaceDatabase
    private lateinit var sharedPreferences: SharedPreferences

    private var storedFirebaseEmbedding: FloatArray? = null

    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    @Volatile private var isRegistering = false
    @Volatile private var isRecognizing = false
    @Volatile private var isProcessingFrame = false

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (allPermissionsGranted()) startCamera()
            else Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ImageUtils.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val studentId = sharedPreferences.getString("userId", "") ?: ""

        initializeComponents()

        if (allPermissionsGranted()) startCamera()
        else activityResultLauncher.launch(REQUIRED_PERMISSIONS)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkFirebaseFaceData(studentId)

        binding.btnRegister.setOnClickListener {
            if (storedFirebaseEmbedding == null) startRegistration()
            else Toast.makeText(this, "Already registered", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecognize.setOnClickListener {
            if (!isRecognizing && !isRegistering) startRecognition()
        }
    }

    private fun initializeComponents() {
        faceRecognition = SimpleFaceMLKit()
        database = FaceDatabase.getDatabase(this)
    }

    private fun checkFirebaseFaceData(studentId: String) {
        FirebaseDatabase.getInstance().reference
            .child("NEW").child("classes")
            .get()
            .addOnSuccessListener { classSnap ->
                classSnap.children.forEach { classNode ->
                    if (classNode.child("students").child(studentId).exists()) {

                        val faceDataJson =
                            classNode.child("students").child(studentId)
                                .child("faceData")
                                .value?.toString()

                        if (!faceDataJson.isNullOrEmpty()) {
                            storedFirebaseEmbedding = ImageUtils.jsonToFloatArray(faceDataJson)

                            binding.btnRegister.isEnabled = false
                            binding.btnRegister.text = "Face Already Registered"
                        }
                    }
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val provider = cameraProvider ?: return

        val selector = CameraSelector.DEFAULT_FRONT_CAMERA
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, FaceAnalyzer()) }

        provider.unbindAll()
        provider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalyzer)
    }

    private fun startRegistration() {
        isRegistering = true
        binding.btnRecognize.isEnabled = false
        binding.btnRegister.text = "Capturing..."
        binding.tvResult.text = "Position your face"
    }

    private fun startRecognition() {
        isRecognizing = true
        binding.btnRegister.isEnabled = false
        binding.btnRecognize.text = "Recognizing..."
        binding.tvResult.text = "Scanning..."
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(proxy: ImageProxy) {
            if (!isRegistering && !isRecognizing) { proxy.close(); return }
            if (isProcessingFrame) { proxy.close(); return }

            isProcessingFrame = true

            val bitmap = ImageUtils.imageProxyToBitmap(proxy)
            if (bitmap == null) {
                isProcessingFrame = false
                proxy.close()
                return
            }

            val rotated = ImageUtils.rotateBitmap(bitmap, proxy.imageInfo.rotationDegrees)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val features = faceRecognition.extractFeatures(rotated)

                    if (features != null) {
                        if (isRegistering) handleRegistration(features)
                        else if (isRecognizing) handleRecognition(features)
                    }

                } catch (e: Exception) {
                    Log.e("FaceRecognition", "Analyzer error", e)
                } finally {
                    isProcessingFrame = false
                    proxy.close()
                }
            }
        }
    }

    private suspend fun handleRegistration(features: FloatArray) =
        withContext(Dispatchers.Main) {
            isRegistering = false
            binding.btnRegister.text = "Register Face"
            binding.btnRecognize.isEnabled = true
            showRegistrationDialog(features)
        }

    private suspend fun handleRecognition(features: FloatArray) =
        withContext(Dispatchers.Main) {

            binding.btnRecognize.text = "Recognize"
            isRecognizing = false

            if (storedFirebaseEmbedding == null) {
                binding.tvResult.text = "No registered face"
                Toast.makeText(this@FaceRecognitionActivity, "Face saved", Toast.LENGTH_SHORT).show()
                return@withContext
            }

            val similarity = faceRecognition.calculateSimilarity(features, storedFirebaseEmbedding!!)
            val threshold = 0.85f

            if (similarity >= threshold) {
                binding.tvResult.text = "Face Matched (${(similarity * 100).toInt()}%)"

                // ------------- NEW CODE: Update Firebase faceMatched:YES ---------------
                val studentId = sharedPreferences.getString("userId", "") ?: ""

                FirebaseDatabase.getInstance().reference
                    .child("NEW")
                    .child("classes")
                    .get()
                    .addOnSuccessListener { snap ->
                        snap.children.forEach { classNode ->
                            if (classNode.child("students").child(studentId).exists()) {
                                classNode.ref.child("students")
                                    .child(studentId)
                                    .child("faceMatched")
                                    .setValue("yes")
                            }
                        }
                    }
                // ------------------------------------------------------------------------

            } else {
                binding.tvResult.text = "Face Not Matched"

                // ------------- NEW CODE: Update Firebase faceMatched:NO ----------------
                val studentId = sharedPreferences.getString("userId", "") ?: ""

                FirebaseDatabase.getInstance().reference
                    .child("NEW")
                    .child("classes")
                    .get()
                    .addOnSuccessListener { snap ->
                        snap.children.forEach { classNode ->
                            if (classNode.child("students").child(studentId).exists()) {
                                classNode.ref.child("students")
                                    .child(studentId)
                                    .child("faceMatched")
                                    .setValue("no")
                            }
                        }
                    }
                // ------------------------------------------------------------------------
            }
        }

    private fun showRegistrationDialog(features: FloatArray) {
        val dBinding = DialogRegisterFaceBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dBinding.root)
            .setCancelable(false)
            .create()

        dBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dBinding.btnSave.setOnClickListener {
            val name = dBinding.etName.text.toString().trim()
            val studentId = sharedPreferences.getString("userId", "") ?: ""

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val jsonEmbedding = ImageUtils.floatArrayToJson(features)
            FirebaseDatabase.getInstance().reference
                .child("NEW")
                .child("classes")
                .get()
                .addOnSuccessListener { snap ->
                    snap.children.forEach { classNode ->
                        if (classNode.child("students").child(studentId).exists()) {
                            classNode.ref.child("students")
                                .child(studentId)
                                .child("faceData")
                                .setValue(jsonEmbedding)

                            storedFirebaseEmbedding = features
                            binding.btnRegister.isEnabled = false
                            binding.btnRegister.text = "Face Already Registered"

                            Toast.makeText(this, "Face saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceRecognition.close()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }
}
