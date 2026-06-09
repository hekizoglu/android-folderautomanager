package com.armutlu.apporganizer.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Helper for managing runtime permissions
 */
class PermissionHelper(private val context: Context) {
    
    companion object {
        // Required permissions
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.QUERY_ALL_PACKAGES,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.QUERY_ALL_PACKAGES
            )
        }
        
        // Optional permissions
        private val OPTIONAL_PERMISSIONS = arrayOf(
            Manifest.permission.INSTALL_SHORTCUT,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get list of permissions to request
     */
    fun getPermissionsToRequest(): Array<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
    
    /**
     * Request required permissions
     */
    fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
        val permissionsToRequest = getPermissionsToRequest()
        
        if (permissionsToRequest.isNotEmpty()) {
            Timber.d("Requesting permissions: ${permissionsToRequest.joinToString()}")
            launcher.launch(permissionsToRequest)
        } else {
            Timber.d("All permissions already granted")
        }
    }
    
    /**
     * Check if permission was granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get permission status for logging
     */
    fun getPermissionStatus(): String {
        val sb = StringBuilder()
        sb.append("Permission Status:\n")
        
        REQUIRED_PERMISSIONS.forEach { permission ->
            val status = if (hasPermission(permission)) "✅" else "❌"
            sb.append("$status $permission\n")
        }
        
        return sb.toString()
    }
}
