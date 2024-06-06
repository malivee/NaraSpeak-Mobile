package com.belajar.naraspeak

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.belajar.naraspeak.data.repository.VideoCallRepository
import com.belajar.naraspeak.data.webrtc.FirebaseClient
import com.belajar.naraspeak.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    //move
    private lateinit var repository: VideoCallRepository
    private val firebaseClient: FirebaseClient = FirebaseClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = VideoCallRepository.getInstance(firebaseClient)

        testDatabase()


    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        when {
            it[android.Manifest.permission.CAMERA] ?: false -> {
                getPermission()
            }

            it[android.Manifest.permission.RECORD_AUDIO] ?: false -> {
                getPermission()
            }

            else -> {
            }
        }
    }

    private fun getPermission() {
        if (ActivityCompat.checkSelfPermission(
                this@RegisterActivity,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && (ActivityCompat.checkSelfPermission(
                this@RegisterActivity,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            val intent = Intent(this@RegisterActivity, VideoCallActivity::class.java)
            startActivity(intent)

        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    private fun testDatabase() {
        binding.btnRegister.setOnClickListener {
            repository.login(
                binding.edEmailRegister.text.toString(),
                object : FirebaseClient.FirebaseStatusListener {
                    override fun onError() {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                    override fun onSuccess() {
                        Log.d("USERSUCCESS", binding.edEmailRegister.text.toString())
                        getPermission()
                    }

                },
                this@RegisterActivity
            )
            Log.d("username", binding.edEmailRegister.text.toString())


        }
    }
}