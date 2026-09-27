package com.nestling.baby

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance criterion: zero network calls, verified by a test that fails if the
 * INTERNET permission ever shows up in the merged manifest — including via a
 * dependency's manifest.
 *
 * This is the marketing claim, so it is also a build gate.
 */
@RunWith(AndroidJUnit4::class)
class NoInternetPermissionTest {

    private val allowed = setOf(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.FOREGROUND_SERVICE,
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        // Declared by androidx.core for its own dynamically registered receivers.
        // Signature-level, derived from the application id, never shown to the user.
        "com.nestling.baby.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    )

    private val banned = listOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        "android.permission.ACCESS_WIFI_STATE",
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        "android.permission.READ_PHONE_STATE",
        "com.google.android.gms.permission.AD_ID",
    )

    private fun requestedPermissions(): List<String> {
        val context = appContext()
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        return info.requestedPermissions?.toList().orEmpty()
    }

    @Test
    fun internetPermissionIsNotDeclared() {
        assertThat(requestedPermissions()).doesNotContain(Manifest.permission.INTERNET)
    }

    @Test
    fun noNetworkOrTrackingPermissionIsDeclared() {
        val requested = requestedPermissions()
        banned.forEach { permission ->
            assertThat(requested).doesNotContain(permission)
        }
    }

    @Test
    fun onlyLocalPermissionsAreDeclared() {
        val requested = requestedPermissions()
        val unexpected = requested.filterNot { it in allowed }
        assertThat(unexpected).isEmpty()
    }
}
