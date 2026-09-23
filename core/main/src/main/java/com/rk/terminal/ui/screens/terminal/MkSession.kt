package com.rk.terminal.ui.screens.terminal

import android.content.Context
import com.rk.libcommons.ubuntuHomeDir
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.libcommons.localDir
import com.rk.libcommons.localLibDir
import com.rk.settings.Settings
import com.rk.terminal.App.Companion.getTempDir
import com.rk.terminal.BuildConfig
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

object MkSession {
    fun createSession(
        context: Context,
        sessionClient: TerminalSessionClient,
        sessionId: String,
        workingMode: Int,
        pendingCommand: PendingCommand? = null
    ): TerminalSession {
        with(context) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            val workingDir = pendingCommand?.workingDir ?: ubuntuHomeDir().path

            val initFile: File = localBinDir().child("init-host")
            val initScript = assets.open("init-host.sh").bufferedReader().use { it.readText() }
            if (initFile.exists().not() || initFile.readText() != initScript) {
                initFile.writeText(initScript)
            }

            localBinDir().child("init").apply {
                if (exists().not()) {
                    createFileIfNot()
                    assets.open("init.sh").bufferedReader().use { it.readText() }.let {
                        writeText(it)
                    }
                }
            }

            val env = mutableListOf(
                "PATH=${System.getenv("PATH")}:/sbin:${localBinDir().absolutePath}",
                "HOME=/sdcard",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "BIN=${localBinDir()}",
                "DEBUG=${BuildConfig.DEBUG}",
                "PREFIX=${filesDir.parentFile!!.path}",
                "LD_LIBRARY_PATH=${localLibDir().absolutePath}",
                "LINKER=/system/bin/linker64",
                "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${getTempDir(this).child(sessionId).also { if (it.exists().not()) it.mkdirs() }}",
                "TMPDIR=${getTempDir(this).absolutePath}",
                "PROOT_LOADER=${applicationInfo.nativeLibraryDir}/libloader.so",
                "PROOT=${applicationInfo.nativeLibraryDir}/libproot.so",
            )

            if (Settings.seccomp) {
                env.add("PROOT_NO_SECCOMP=1")
            }

            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            localDir().child("stat").apply {
                if (exists().not()) {
                    writeText(TerminalUtils.stat)
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()) {
                    writeText(TerminalUtils.vmstat)
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
            }

            val args: Array<String>
            val shell = if (pendingCommand == null) {
                args = if (workingMode == WorkingMode.UBUNTU) {
                    arrayOf(initFile.absolutePath)
                } else {
                    arrayOf()
                }
                "/system/bin/sh"
            } else {
                args = pendingCommand.args
                pendingCommand.shell
            }

            return TerminalSession(
                shell,
                workingDir,
                // TerminalSession expects argv[0] before the command arguments.
                arrayOf(shell, *args),
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient,
            )
        }
    }

    fun buildCustomPendingCommand(context: Context, custom: CustomSession): PendingCommand {
        val scriptFile = File(custom.shellPath)
        val sysSh = File("/system/bin/sh")

        val shell: String
        val args: Array<String>

        if (sysSh.canExecute()) {
            shell = sysSh.absolutePath
            args = arrayOf("-c", scriptFile.absolutePath)
        } else {
            val proot = "${context.applicationInfo.nativeLibraryDir}/libproot.so"
            shell = proot
            args = arrayOf(
                "-r", "/",
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sdcard",
                "-0",
                "sh", scriptFile.absolutePath
            )
        }

        return PendingCommand(
            shell = shell,
            args = args,
            workingDir = scriptFile.parentFile?.absolutePath ?: "/sdcard/ReTerminal",
            env = null
        )
    }

    fun buildScriptPendingCommand(
        context: Context,
        script: File,
        workingMode: Int,
        custom: CustomSession? = null
    ): PendingCommand {
        val workingDir = script.parentFile?.absolutePath
        return if (custom != null) {
            val sysSh = File("/system/bin/sh")
            if (sysSh.canExecute()) {
                PendingCommand(
                    shell = sysSh.absolutePath,
                    args = arrayOf("-c", "'${custom.shellPath}' '${script.absolutePath}'"),
                    workingDir = workingDir,
                    env = null
                )
            } else {
                val proot = "${context.applicationInfo.nativeLibraryDir}/libproot.so"
                PendingCommand(
                    shell = proot,
                    args = arrayOf(
                        "-r", "/",
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sdcard",
                        "-0",
                        "sh", custom.shellPath, script.absolutePath
                    ),
                    workingDir = workingDir,
                    env = null
                )
            }
        } else if (workingMode == WorkingMode.UBUNTU) {
            val initFile = context.localBinDir().child("init-host")
            PendingCommand(
                shell = "/system/bin/sh",
                args = arrayOf(initFile.absolutePath, "sh", script.absolutePath),
                workingDir = workingDir,
                env = null
            )
        } else {
            PendingCommand(
                shell = "/system/bin/sh",
                args = arrayOf("-c", script.absolutePath),
                workingDir = workingDir,
                env = null
            )
        }
    }
}

data class PendingCommand(
    val shell: String,
    val args: Array<String>,
    val workingDir: String?,
    val env: List<String>?
)
