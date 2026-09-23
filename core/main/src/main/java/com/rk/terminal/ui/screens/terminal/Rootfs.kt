package com.rk.terminal.ui.screens.terminal

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.rk.libcommons.child
import com.rk.libcommons.localDir

object Rootfs {
    var isInstalled = mutableStateOf(false)

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
