package com.example.grooveix.ui.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.grooveix.R
import com.example.grooveix.databinding.ActivityPermissionBinding


class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.finishOnBoarding.setOnClickListener {
            permissionSetup()
        }

        binding.cancelBoarding.setOnClickListener {
            Toast.makeText(this, this.getString(R.string.permission_error), Toast.LENGTH_LONG).show()
            val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.setData(uri)
            startActivity(intent)
        }
    }

    private fun permissionSetup() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            permissionsResultCallback.launch(Manifest.permission.READ_MEDIA_AUDIO)
        else
            permissionsResultCallback.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val permissionsResultCallback = registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> {
                Intent(this@PermissionActivity, MainActivity::class.java).also {
                    startActivity(it)
                    this@PermissionActivity.finish()
                }
            }
            false -> {
                Toast.makeText(this, this.getString(R.string.permission_error), Toast.LENGTH_LONG).show()
                val intent: Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.setData(uri)
                startActivity(intent)
            }
        }
    }

}