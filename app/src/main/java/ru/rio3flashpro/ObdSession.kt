package ru.rio3flashpro

import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream

/** Process-wide OBD session retained while the foreground service is active. */
object ObdSession {
    @Volatile var socket: BluetoothSocket? = null
    @Volatile var input: InputStream? = null
    @Volatile var output: OutputStream? = null
    @Volatile var deviceName: String? = null

    @Synchronized
    fun close() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        input = null
        output = null
        deviceName = null
    }
}
