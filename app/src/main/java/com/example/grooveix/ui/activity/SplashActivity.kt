package com.example.grooveix.ui.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.example.grooveix.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (!hasStoragePermission()) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(500L)
                Intent(this@SplashActivity, PermissionActivity::class.java).also {
                    startActivity(it)
                    this@SplashActivity.finish()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(500L)
                Intent(this@SplashActivity, MainActivity::class.java).also {
                    startActivity(it)
                    this@SplashActivity.finish()
                }
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED)
        } else {
            (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}