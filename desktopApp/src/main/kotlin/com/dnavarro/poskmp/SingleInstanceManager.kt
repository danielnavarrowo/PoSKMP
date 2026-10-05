package com.dnavarro.poskmp

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.time.LocalDateTime
import kotlin.concurrent.thread

object SingleInstanceManager {
    private var lockChannel: FileChannel? = null
    private var fileLock: FileLock? = null
    private var serverSocket: ServerSocket? = null

    @Volatile
    var onBringToFront: (() -> Unit)? = null

    /**
     * Attempts to acquire an exclusive lock for this application instance.
     * If another instance is already running:
     * - Sends a "FOCUS" signal via localhost socket to bring the existing window to front.
     * - Returns false so the current process can exit immediately.
     * If this is the only instance:
     * - Holds the file lock for the process lifetime.
     * - Listens on a localhost port for focus requests.
     * - Returns true.
     */
    fun acquireLock(appDir: File, logFile: File): Boolean {
        val lockFile = File(appDir, "app.lock")
        val portFile = File(appDir, "app.port")

        try {
            val raf = RandomAccessFile(lockFile, "rw")
            val channel = raf.channel
            val lock = channel.tryLock()

            if (lock == null) {
                // Another instance holds the lock
                val timestamp = LocalDateTime.now()
                val logMsg = "[$timestamp] [INSTANCE] Another instance of Punto de Venta is already running. Exiting current process."
                println(logMsg)
                try {
                    logFile.appendText(logMsg + "\n")
                } catch (_: Exception) {
                }

                notifyRunningInstance(portFile)
                channel.close()
                raf.close()
                return false
            }

            // Successfully acquired lock
            lockChannel = channel
            fileLock = lock

            // Start localhost IPC server to receive "FOCUS" signals from subsequent launches
            startIpcServer(portFile)

            // Register shutdown hook to clean up resources gracefully
            Runtime.getRuntime().addShutdownHook(Thread {
                release(portFile)
            })

            return true
        } catch (e: Exception) {
            val timestamp = LocalDateTime.now()
            val warnMsg = "[$timestamp] [INSTANCE] Warning: Could not check instance lock (${e.message}). Proceeding anyway."
            println(warnMsg)
            try {
                logFile.appendText(warnMsg + "\n")
            } catch (_: Exception) {
            }
            return true
        }
    }

    private fun startIpcServer(portFile: File) {
        try {
            // Bind to loopback address (127.0.0.1) on port 0 (OS picks available ephemeral port)
            val socket = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            portFile.writeText(socket.localPort.toString())

            thread(isDaemon = true, name = "SingleInstanceIpcListener") {
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        client.use {
                            val msg = it.getInputStream().bufferedReader().readLine()
                            if (msg == "FOCUS") {
                                onBringToFront?.invoke()
                            }
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            println("Could not start single instance IPC server: ${e.message}")
        }
    }

    private fun notifyRunningInstance(portFile: File) {
        try {
            if (portFile.exists()) {
                val port = portFile.readText().trim().toIntOrNull()
                if (port != null && port in 1..65535) {
                    Socket(InetAddress.getByName("127.0.0.1"), port).use { socket ->
                        socket.soTimeout = 1000
                        socket.getOutputStream().write("FOCUS\n".toByteArray(Charsets.UTF_8))
                        socket.getOutputStream().flush()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore socket errors, primary goal (preventing second instance) is already fulfilled
        }
    }

    private fun release(portFile: File) {
        try {
            portFile.delete()
        } catch (_: Exception) {
        }

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }

        try {
            fileLock?.release()
        } catch (_: Exception) {
        }

        try {
            lockChannel?.close()
        } catch (_: Exception) {
        }
    }
}
