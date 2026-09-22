package com.rk.terminal.ui.screens.terminal

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.rk.libcommons.child
import com.rk.libcommons.localDir
import com.rk.settings.Settings

enum class ExecMode(val value: Int) {
    CHROOT(0),
    PROOT(1);

    companion object {
        fun fromInt(v: Int): ExecMode? = entries.firstOrNull { it.value == v }
    }
}

object Rootfs {
    var isInstalled = mutableStateOf(false)
    var execMode = mutableStateOf(ExecMode.fromInt(Settings.exec_mode))

    fun setExecMode(mode: ExecMode) {
        execMode.value = mode
        Settings.exec_mode = mode.value
    }

    fun checkInstallation(context: Context) {
        isInstalled.value = isRootfsInstalled(context)
    }

    fun isRootfsInstalled(context: Context): Boolean {
        val ubuntuDir = context.localDir().child("ubuntu")
        val isExtracted = ubuntuDir.child(".rootfs-ready").isFile
        val isArchivePresent = context.filesDir.child("ubuntu.tar.gz").let { it.isFile && it.length() > 0L }
        return isExtracted || isArchivePresent
    }
}
