package com.selflock.app.domain.usecase

import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
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
            .filter {
                it.packageName != "com.selflock.app" &&
                    it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0 &&
                    packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    appName = packageManager.getApplicationLabel(it).toString()
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
}
