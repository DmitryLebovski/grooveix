package com.example.grooveix.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.grooveix.R
import com.example.grooveix.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_tracks, R.id.navigation_favorites, R.id.navigation_playlists
            )
        )

        if (!hasStoragePermission()) {
            Intent(this@MainActivity, PermissionActivity::class.java).also {
                startActivity(it)
                this@MainActivity.finish()
            }
        }

        navView.setupWithNavController(navController)
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
}