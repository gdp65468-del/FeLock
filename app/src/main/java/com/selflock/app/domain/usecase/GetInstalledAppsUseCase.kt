package com.selflock.app.domain.usecase

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val appName: String
)

class GetInstalledAppsUseCase @Inject constructor(
    private val packageManager: PackageManager
) {
    fun execute(): List<InstalledApp> {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    appName = packageManager.getApplicationLabel(it).toString()
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
