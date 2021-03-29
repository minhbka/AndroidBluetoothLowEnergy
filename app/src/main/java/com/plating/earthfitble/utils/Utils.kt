package com.plating.earthfitble.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager


class Utils {
    companion object {
        const val PREFS_LOCATION_NOT_REQUIRED = "location_not_required"
        const val PREFS_PERMISSION_REQUESTED = "permission_requested"

        fun isBleEnabled():Boolean{
            val adapter:BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            return adapter != null && adapter.isEnabled
        }

        fun isLocationPermissionGranted(context: Context):Boolean{
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
            return (isLocationPermissionGranted(activity) // Location permission must be denied
                    && preferences.getBoolean(
                PREFS_PERMISSION_REQUESTED,
                false
            ) // Permission must have been requested before
                    && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            )) // This method should return false
        }

        fun isLocationEnabled(context: Context):Boolean{
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val locationManager = context.getSystemService(LocationManager::class.java)
                        ?: return false
                locationManager.isLocationEnabled
            } else {
                val locationMode: Int
                locationMode = try {
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
                } catch (e: SettingNotFoundException) {
                    e.printStackTrace()
                    return false
                }
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            }
        }
        fun isLocationRequired(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, true)
        }

        fun markLocationNotRequired(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply()
        }

        fun markLocationPermissionRequested(context: Context) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply()
        }
    }

}