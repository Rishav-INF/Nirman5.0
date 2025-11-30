package com.example.upasthithai
//
//import android.os.Bundle
//import android.util.Log
//import android.widget.ArrayAdapter
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.ListView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.ai.client.generativeai.GenerativeModel
//import com.google.ai.client.generativeai.type.content
//import kotlinx.coroutines.launch
//import androidx.core.content.ContentProviderCompat.requireContext as requireContext1
//
//class chatbotActivity : AppCompatActivity() {
//
//    private var datalist  = mutableListOf<recyclerdataclass>()
//    private lateinit var chatAdapter: ArrayAdapter<String>
//    private lateinit var chatListView: ListView
//    private lateinit var message : String
//    private lateinit var recyleradapter : chatRecyclerAdapter
//    private lateinit var recyler : RecyclerView
//    private lateinit var sendtext : EditText
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.chatbotview)
//
//        recyler = findViewById<RecyclerView>(R.id.chatListView)
//        sendtext = findViewById<EditText>(R.id.inputField)
//        val sendbutton = findViewById<ImageButton>(R.id.sendButton)
//
//
//        message = sendtext.text.toString()
//        val generativeModel = GenerativeModel(
//            // Set the model name to the latest Gemini model.
//            modelName = "gemini-1.5-pro-latest",
//            // Set your Gemini API key in the API_KEY variable in your
//            // local.properties file
//            apiKey = getString(R.string.gemini),
//            // Set a system instruction to set the behavior of the model.
//        )
//
//        sendbutton.setOnClickListener()
//        {
//            sendmessage(recyler,message,generativeModel)
////            lifecycleScope.launch {
////                val response = generativeModel.generateContent(prompt = message)
////                datalist.add(recyclerdataclass(message,response.text.toString()))
////                sendtext.text.clear()
////                recyleradapter.notifyItemInserted(datalist.size - 1) // Notify the adapter of the new item
////            }
//
//        }
//        recyler.layoutManager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
//        recyleradapter = chatRecyclerAdapter(datalist)
//        recyler.adapter = recyleradapter
//
//        // Generate content with the prompt
//
//
//        }
//
//
//
//    private fun sendmessage(recycler : RecyclerView,string : String,model : GenerativeModel)
//    {
//        lifecycleScope.launch {
////            val chat = model.startChat()
////            val response = chat.sendMessage(string)
//            val response = model.generateContent(prompt = string)
//            Log.i("Gemini response",response.text.toString())
//            datalist.add(recyclerdataclass(string,response.text.toString()))
//            sendtext.text.clear()
//            recyleradapter.notifyItemInserted(datalist.size - 1) // Notify the adapter of the new item
//            recycler.scrollToPosition(datalist.size - 1)
//
//        }
//    }
//
//    }
//
//
//

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.upasthithai.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class chatbotActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatBotApp"

        // 🔥 USE THE WORKING URL YOU CONFIRMED
        // (this is the ONLY required change)
        private const val BASE_URL = "http://10.54.139.61:8000/chatbot"

        private const val LOG_FILE_NAME = "chatbot_logs.txt"
        private val TIMESTAMP_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    private val gson = Gson()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chatbot)

        val input = findViewById<EditText>(R.id.userInput)
        val sendButton = findViewById<Button>(R.id.sendBtn)
        val botReply = findViewById<TextView>(R.id.botReply)

        logInfo("App started. BASE_URL = $BASE_URL")

        sendButton.setOnClickListener {
            val query = input.text.toString().trim()
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            logDebug("User Query: $query")

            lifecycleScope.launch(Dispatchers.IO) {
                val replyText = sendQuery(query)
                withContext(Dispatchers.Main) {
                    botReply.text = replyText
                }
            }
        }
    }

    private fun sendQuery(query: String): String {
        try {
            val payload = mapOf(
                "student_id" to "2023CSE002",
                "query" to query
            )
            val json = gson.toJson(payload)

            logDebug("📤 Sending JSON: $json")
            logDebug("➡ URL: $BASE_URL")

            val requestBody = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                logDebug("📥 HTTP Response Code: ${response.code}")

                val bodyText = response.body?.string()
                logDebug("📥 Raw Response Body: $bodyText")

                if (!response.isSuccessful) {
                    logError("Server returned non-success code: ${response.code}. Body: $bodyText")
                    return "Server error: ${response.code}"
                }

                if (bodyText.isNullOrBlank()) {
                    logError("Empty response body")
                    return "Empty response from server"
                }

                return try {
                    val botResponse = gson.fromJson(bodyText, BotResponse::class.java)
                    val reply = botResponse?.reply ?: "No reply field in response"
                    logDebug("🤖 Bot Reply: $reply")
                    reply
                } catch (pe: Exception) {
                    logException("JSON parse error", pe)
                    "Error parsing server response"
                }
            }

        } catch (e: Exception) {
            logException("Network/Unexpected error", e)
            return "Error: ${e.localizedMessage ?: "Network error"}"
        }
    }

    /* ---------------- Logging helpers ---------------- */

    private fun logDebug(message: String) {
        Log.d(TAG, message)
        appendLogToFile("D", message, null)
    }

    private fun logInfo(message: String) {
        Log.i(TAG, message)
        appendLogToFile("I", message, null)
    }

    private fun logError(message: String) {
        Log.e(TAG, message)
        appendLogToFile("E", message, null)
    }

    private fun logException(message: String, t: Throwable) {
        Log.e(TAG, message, t)
        appendLogToFile("E", message, t)
    }

    private fun appendLogToFile(level: String, message: String, t: Throwable?) {
        try {
            val ts = TIMESTAMP_FMT.format(Date())
            val header = "$ts [$level] $message\n"

            openFileOutput(LOG_FILE_NAME, MODE_APPEND).use { fos ->
                fos.write(header.toByteArray(Charsets.UTF_8))

                t?.let {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    it.printStackTrace(pw)
                    pw.flush()

                    fos.write((sw.toString() + "\n").toByteArray(Charsets.UTF_8))
                }

                fos.flush()
            }

        } catch (io: Exception) {
            Log.e(TAG, "Failed to write log file: ${io.localizedMessage}", io)
        }
    }
}

data class BotResponse(val reply: String?)
