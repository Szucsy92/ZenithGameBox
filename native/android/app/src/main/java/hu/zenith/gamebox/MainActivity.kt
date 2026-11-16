package hu.zenith.gamebox

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import hu.zenith.gamebox.app.Prefs
import hu.zenith.gamebox.app.UpdateViewModel
import hu.zenith.gamebox.ui.ZenithApp
import java.io.File

class MainActivity : ComponentActivity() {
    private val updateVm: UpdateViewModel by viewModels()
    private var pendingApk: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        val prefs = Prefs(this)
        setContent {
            ZenithApp(
                prefs = prefs,
                vibrate = { ms -> vibrate(ms) },
                updateVm = updateVm,
                onInstall = { f -> installApk(f) },
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(ms)
        } catch (_: Exception) {
        }
    }

    /** A letöltött frissítés telepítése. Kell hozzá az „ismeretlen forrás” engedély; a rendszer telepítője egy jóváhagyást kér. */
    private fun installApk(file: File) {
        if (!packageManager.canRequestPackageInstalls()) {
            pendingApk = file
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            } catch (e: Exception) { e.printStackTrace() }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.update", file)
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        val f = pendingApk
        if (f != null) {
            pendingApk = null
            if (packageManager.canRequestPackageInstalls()) installApk(f)
        }
    }
}
