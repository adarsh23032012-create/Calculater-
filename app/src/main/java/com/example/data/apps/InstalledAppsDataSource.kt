package com.example.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsDataSource(private val context: Context) {

  private val packageManager: PackageManager = context.packageManager
  private val iconCache = LruCache<String, ImageBitmap>(120)

  suspend fun loadLaunchableApps(): List<InstalledAppInfo> =
    withContext(Dispatchers.IO) {
      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolved =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(0L),
          )
        } else {
          @Suppress("DEPRECATION")
          packageManager.queryIntentActivities(launcherIntent, 0)
        }

      val selfPkg = context.packageName
      val seenPackages = HashSet<String>()

      resolved
        .asSequence()
        .mapNotNull { info ->
          val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
          if (pkg == selfPkg || !seenPackages.add(pkg)) return@mapNotNull null

          val appInfo = info.activityInfo.applicationInfo
          val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty().ifEmpty { pkg }
          val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
          val versionName =
            try {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager
                  .getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
                  .versionName
              } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(pkg, 0).versionName
              }
            } catch (_: Exception) {
              null
            } ?: "1.0"

          val iconBitmap = loadAppIconSync(pkg)

          InstalledAppInfo(
            packageName = pkg,
            label = label,
            versionName = versionName,
            isSystemApp = isSystem,
            icon = iconBitmap,
          )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
    }

  fun loadAppIconSync(packageName: String): ImageBitmap? {
    iconCache.get(packageName)?.let { return it }
    return try {
      val drawable = packageManager.getApplicationIcon(packageName)
      val bitmap = drawable.toBitmap(width = 128, height = 128).asImageBitmap()
      iconCache.put(packageName, bitmap)
      bitmap
    } catch (_: Exception) {
      null
    }
  }

  fun isAppInstalled(packageName: String): Boolean {
    return try {
      packageManager.getLaunchIntentForPackage(packageName) != null
    } catch (_: Exception) {
      false
    }
  }

  fun launchApp(packageName: String): Boolean {
    return try {
      val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun openAppSystemInfo(packageName: String): Boolean {
    return try {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", packageName, null)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }
}
