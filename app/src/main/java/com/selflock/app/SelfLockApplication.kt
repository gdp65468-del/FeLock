package com.selflock.app

import android.app.Application
import android.os.Process
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class SelfLockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = buildString {
                    appendLine("Exceção: ${throwable.javaClass.name}")
                    appendLine("Mensagem: ${throwable.message}")
                    appendLine()
                    appendLine("--- Rastreamento de pilha ---")
                    throwable.stackTraceToString().lines().forEach { appendLine(it) }

                    var cause = throwable.cause
                    while (cause != null) {
                        appendLine()
                        appendLine("--- Causada por: ${cause.javaClass.name} ---")
                        appendLine("Mensagem: ${cause.message}")
                        cause.stackTraceToString().lines().forEach { appendLine(it) }
                        cause = cause.cause
                    }
                }

                CrashHandlerActivity.launch(this, thread.name, stackTrace)
            } catch (_: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
