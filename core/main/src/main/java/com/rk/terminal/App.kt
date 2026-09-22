package com.rk.terminal

import android.app.Application
import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.anrwatchdog.ANRWatchDog
import com.rk.libcommons.application
import com.rk.resources.Res
import com.rk.settings.Settings
import com.rk.terminal.ui.screens.terminal.TerminalUtils
import com.rk.update.UpdateManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class App : Application() {

    override fun attachBaseContext(base: Context) {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base.getSystemService(LocaleManager::class.java).applicationLocales[0] ?: Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply {
            setLocale(locale)
        }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    companion object {
        fun getTempDir(context: Context): File {
            val tmp = File(context.cacheDir, "tmp")
            if (!tmp.exists()) {
                tmp.mkdir()
            }
            return tmp
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        application = this
        Res.application = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales.isEmpty) {
                localeManager.applicationLocales = LocaleList.forLanguageTags("zh-CN")
            }
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
        }
        AppCompatDelegate.setDefaultNightMode(Settings.default_night_mode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(UiModeManager::class.java).setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        }
        TerminalUtils.init(this)

        GlobalScope.launch(Dispatchers.IO) {
            getTempDir(this@App).apply {
                if (exists() && listFiles().isNullOrEmpty().not()) {
                    deleteRecursively()
                }
            }
        }

        ANRWatchDog().start()

        UpdateManager(this).onUpdate()

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectAll()
                    penaltyLog()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            violation.printStackTrace()
                        }
                    }
                }.build()
            )
        }
    }
}
