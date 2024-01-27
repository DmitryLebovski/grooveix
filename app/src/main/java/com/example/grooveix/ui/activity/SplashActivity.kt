package com.example.grooveix.ui.activity

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.grooveix.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

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
        return EasyPermissions.hasPermissions(
            this,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                android.Manifest.permission.READ_MEDIA_AUDIO
            else
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}