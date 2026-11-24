package ru.rio3flashpro

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var status: TextView
    private lateinit var values: TextView
    private lateinit var connectButton: Button
    private lateinit var readButton: Button
    private lateinit var codesButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFFF3F5F8.toInt())
        }
        val title = TextView(this).apply {
            text = "Rio3FlashPro"
            textSize = 28f
            setTextColor(0xFF14243A.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Диагностика Kia Rio III • OBD-II"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }
        status = TextView(this).apply {
            text = "Статус: адаптер не подключён"
            textSize = 16f
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        connectButton = makeButton("Подключить Bluetooth OBD2") { chooseDevice() }
        readButton = makeButton("Показать параметры двигателя") { readLiveData() }
        codesButton = makeButton("Прочитать ошибки двигателя") { readTroubleCodes() }
        values = TextView(this).apply {
            text = "Здесь появятся данные автомобиля."
            textSize = 16f
            setTextColor(0xFF172333.toInt())
            setPadding(14, 18, 14, 18)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val note = TextView(this).apply {
            text = "Важно: приложение пока выполняет диагностику через совместимый ELM327 Bluetooth. Функция прошивки ЭБУ не реализована. Не отключайте адаптер при работе двигателя."
            textSize = 13f
            setPadding(4, 18, 4, 4)
        }
        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(status, match())
        root.addView(connectButton, match())
        root.addView(readButton, match())
        root.addView(codesButton, match())
        root.addView(values, match())
        root.addView(note, match())
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        readButton.isEnabled = false
        codesButton.isEnabled = false
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 12 }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { action() } }

    private fun chooseDevice() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41)
            return
        }
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            setStatus("На телефоне нет Bluetooth.")
            return
        }
        if (!adapter.isEnabled) {
            setStatus("Включите Bluetooth в настройках телефона.")
            return
        }
        try {
            val devices = adapter.bondedDevices.toList()
            if (devices.isEmpty()) {
                setStatus("Сначала выполните сопряжение ELM327 в настройках Bluetooth Android.")
                return
            }
            val labels = devices.map { "${it.name ?: "Bluetooth"}  •  ${it.address}" }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Выберите OBD2-адаптер")
                .setItems(labels) { _, which -> connect(devices[which]) }
                .setNegativeButton("Отмена", null)
                .show()
        } catch (e: SecurityException) {
            setStatus("Нет разрешения Bluetooth. Разрешите доступ и повторите.")
        }
    }

    private fun connect(device: android.bluetooth.BluetoothDevice) {
        setStatus("Подключение к ${device.name ?: device.address}…")
        worker.execute {
            try {
                socket?.close()
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                newSocket.connect()
                socket = newSocket
                input = newSocket.inputStream
                output = newSocket.outputStream
                val init = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
                init.forEach { command(it) }
                runOnUiThread {
                    setStatus("Подключено: ${device.name ?: device.address}")
                    readButton.isEnabled = true
                    codesButton.isEnabled = true
                    values.text = "Адаптер подключён. Можно читать параметры и ошибки."
                }
            } catch (e: Exception) {
                closeConnection()
                runOnUiThread { setStatus("Не удалось подключиться: ${e.message ?: "проверьте адаптер"}") }
            }
        }
    }

    private fun command(cmd: String): String {
        val out = output ?: throw IllegalStateException("Адаптер не подключён")
        val ins = input ?: throw IllegalStateException("Нет входящего потока")
        out.write((cmd + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()
        val buffer = StringBuilder()
        val deadline = System.currentTimeMillis() + 2500
        while (System.currentTimeMillis() < deadline) {
            while (ins.available() > 0) {
                val c = ins.read()
                if (c >= 0 && c.toChar() != '>') buffer.append(c.toChar())
                if (c == '>'.code) return buffer.toString().trim()
            }
            Thread.sleep(30)
        }
        return buffer.toString().trim()
    }

    private fun readLiveData() {
        setStatus("Чтение параметров…")
        worker.execute {
            try {
                val rpm = parseHexResponse(command("010C"), "410C", 4)?.let { ((it[0] * 256 + it[1]) / 4).toString() } ?: "нет данных"
                val speed = parseHexResponse(command("010D"), "410D", 1)?.get(0)?.toString() ?: "нет данных"
                val temp = parseHexResponse(command("0105"), "4105", 1)?.get(0)?.let { (it - 40).toString() } ?: "нет данных"
                runOnUiThread {
                    values.text = "Обороты двигателя: $rpm об/мин\nСкорость: $speed км/ч\nТемпература ОЖ: $temp °C"
                    setStatus("Данные получены")
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка чтения: ${e.message}") }
            }
        }
    }

    private fun readTroubleCodes() {
        setStatus("Чтение кодов ошибок…")
        worker.execute {
            try {
                val response = command("03").replace("\\s".toRegex(), "")
                    .replace("SEARCHING...", "").replace(" ", "")
                val codes = decodeCodes(response)
                runOnUiThread {
                    values.text = if (codes.isEmpty()) "Коды ошибок не получены. Это не всегда означает отсутствие неисправностей.\nОтвет адаптера: $response"
                    else "Коды ошибок двигателя:\n" + codes.joinToString("\n")
                    setStatus("Чтение кодов завершено")
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка чтения ошибок: ${e.message}") }
            }
        }
    }

    private fun parseHexResponse(raw: String, header: String, bytes: Int): List<Int>? {
        val clean = raw.uppercase().replace("[^0-9A-F]".toRegex(), "")
        val index = clean.indexOf(header)
        if (index < 0 || clean.length < index + header.length + bytes * 2) return null
        return (0 until bytes).map { clean.substring(index + header.length + it * 2, index + header.length + it * 2 + 2).toInt(16) }
    }

    private fun decodeCodes(raw: String): List<String> {
        val clean = raw.uppercase().replace("[^0-9A-F]".toRegex(), "")
        val start = clean.indexOf("43")
        if (start < 0) return emptyList()
        val data = clean.substring(start + 2)
        val names = listOf("P", "C", "B", "U")
        val result = mutableListOf<String>()
        var i = 0
        while (i + 4 <= data.length) {
            val a = data.substring(i, i + 2).toIntOrNull(16) ?: break
            val b = data.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4
            if (a == 0 && b == 0) continue
            val prefix = names[(a shr 6) and 3]
            val digit = (a shr 4) and 3
            result.add(prefix + digit + "%02X".format(a and 15) + "%02X".format(b))
        }
        return result
    }

    private fun setStatus(message: String) {
        runOnUiThread { status.text = "Статус: $message" }
    }

    private fun closeConnection() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        input = null
        output = null
    }

    override fun onDestroy() {
        closeConnection()
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            setStatus("Разрешение Bluetooth получено. Нажмите «Подключить».")
        } else if (requestCode == 41) {
            setStatus("Без разрешения Bluetooth подключение невозможно.")
        }
    }
}
