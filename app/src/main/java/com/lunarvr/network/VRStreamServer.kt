package com.lunarvr.network

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class VRStreamServer {

    var connectionPin: String = generateRandomPin()
        private set

    val isRunning = AtomicBoolean(false)
    val isClientConnected = AtomicBoolean(false)
    var clientIp: String? = null
        private set

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var activeSocket: Socket? = null
    private var activeOutStream: OutputStream? = null

    // Streaming settings
    var streamQuality: Int = 60 // JPEG quality 1-100
    var targetFps: Int = 30
    private var lastFrameTime = 0L

    companion object {
        const val PORT = 8089
    }

    private fun generateRandomPin(): String {
        val rand = SecureRandom()
        val num = 100000 + rand.nextInt(900000)
        return num.toString()
    }

    fun regeneratePin(): String {
        connectionPin = generateRandomPin()
        return connectionPin
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        executor.execute {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d("VRStreamServer", "Server started on port $PORT. PIN: $connectionPin")

                while (isRunning.get()) {
                    val socket = serverSocket?.accept() ?: break
                    handleClientAuth(socket)
                }
            } catch (e: Exception) {
                Log.e("VRStreamServer", "Server socket error", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClientAuth(socket: Socket) {
        executor.execute {
            try {
                socket.tcpNoDelay = true
                val input = socket.getInputStream().bufferedReader()
                val output = socket.getOutputStream()

                val authLine = input.readLine()?.trim()
                if (authLine == "AUTH $connectionPin") {
                    output.write("AUTH_OK\n".toByteArray())
                    output.flush()

                    synchronized(this) {
                        try { activeSocket?.close() } catch (_: Exception) {}
                        activeSocket = socket
                        activeOutStream = output
                        isClientConnected.set(true)
                        clientIp = socket.inetAddress.hostAddress
                    }
                    Log.d("VRStreamServer", "Client authenticated successfully: $clientIp")
                } else {
                    output.write("AUTH_FAIL\n".toByteArray())
                    output.flush()
                    socket.close()
                    Log.w("VRStreamServer", "Rejected unauthorized client connection attempt.")
                }
            } catch (e: Exception) {
                Log.e("VRStreamServer", "Client auth error", e)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun pushFrame(bitmap: Bitmap?) {
        if (bitmap == null || !isClientConnected.get()) return

        val now = SystemClock.uptimeMillis()
        val minInterval = 1000L / targetFps
        if (now - lastFrameTime < minInterval) return
        lastFrameTime = now

        executor.execute {
            try {
                val out = activeOutStream ?: return@execute
                val byteStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, streamQuality, byteStream)
                val bytes = byteStream.toByteArray()

                val header = "FRAME ${bytes.size}\n".toByteArray()
                synchronized(this) {
                    out.write(header)
                    out.write(bytes)
                    out.flush()
                }
            } catch (e: Exception) {
                isClientConnected.set(false)
                clientIp = null
                try { activeSocket?.close() } catch (_: Exception) {}
                activeSocket = null
                activeOutStream = null
            }
        }
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.1.x"
    }

    fun stop() {
        isRunning.set(false)
        isClientConnected.set(false)
        clientIp = null
        try { activeSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        activeOutStream = null
    }
}
