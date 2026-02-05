package com.geminianywhere.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.geminianywhere.app.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    
    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002
        private const val REQUEST_ACCESSIBILITY_SETTINGS = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPermissionCards()
        updatePermissionStates()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "App Permissions"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupPermissionCards() {
        // Accessibility Permission
        binding.cardAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        binding.iconAccessibilityInfo.setOnClickListener {
            showInfoDialog(
                "Accessibility Service",
                "Required to detect when you type the trigger word (@gemini) in any app and display the floating AI button.\n\n" +
                "This permission allows the app to:\n" +
                "• Monitor text fields across all apps\n" +
                "• Insert AI-generated responses\n" +
                "• Detect trigger patterns\n\n" +
                "Your privacy is protected - we don't collect or store any typed data."
            )
        }

        // Overlay Permission
        binding.cardOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            }
        }
        
        binding.iconOverlayInfo.setOnClickListener {
            showInfoDialog(
                "Display Over Other Apps",
                "Required to show the floating AI button and voice input dialog on top of other apps.\n\n" +
                "This permission allows the app to:\n" +
                "• Display the floating action button\n" +
                "• Show voice input interface\n" +
                "• Present AI responses\n\n" +
                "You can interact with Gemini AI without leaving your current app."
            )
        }

        // Microphone Permission
        binding.cardMicrophone.setOnClickListener {
            if (!hasMicrophonePermission()) {
                requestMicrophonePermission()
            }
        }
        
        binding.iconMicrophoneInfo.setOnClickListener {
            showInfoDialog(
                "Microphone Access",
                "Required for voice input feature when you use the 'voice' command.\n\n" +
                "This permission allows the app to:\n" +
                "• Capture voice input\n" +
                "• Convert speech to text\n" +
                "• Process voice commands\n\n" +
                "Audio is processed on-device using Android's speech recognition. No audio is uploaded to external servers."
            )
        }
    }

    private fun updatePermissionStates() {
        // Update Accessibility
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        binding.switchAccessibility.isChecked = isAccessibilityEnabled
        binding.statusAccessibility.text = if (isAccessibilityEnabled) "Enabled" else "Disabled"
        binding.statusAccessibility.setTextColor(
            ContextCompat.getColor(
                this,
                if (isAccessibilityEnabled) com.geminianywhere.app.R.color.success_green 
                else com.geminianywhere.app.R.color.error_red
            )
        )

        // Update Overlay
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        binding.switchOverlay.isChecked = hasOverlayPermission
        binding.statusOverlay.text = if (hasOverlayPermission) "Enabled" else "Disabled"
        binding.statusOverlay.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasOverlayPermission) com.geminianywhere.app.R.color.success_green 
                else com.geminianywhere.app.R.color.error_red
            )
        )

        // Update Microphone
        val hasMicPermission = hasMicrophonePermission()
        binding.switchMicrophone.isChecked = hasMicPermission
        binding.statusMicrophone.text = if (hasMicPermission) "Enabled" else "Disabled"
        binding.statusMicrophone.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasMicPermission) com.geminianywhere.app.R.color.success_green 
                else com.geminianywhere.app.R.color.error_red
            )
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(packageName) == true
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivityForResult(intent, REQUEST_ACCESSIBILITY_SETTINGS)
        } catch (e: Exception) {
            showErrorDialog("Unable to open Accessibility settings")
        }
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } catch (e: Exception) {
            showErrorDialog("Unable to open overlay permission settings")
        }
    }

    private fun requestMicrophonePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            updatePermissionStates()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updatePermissionStates()
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
