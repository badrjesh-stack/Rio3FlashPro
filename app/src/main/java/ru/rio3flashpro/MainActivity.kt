package ru.rio3flashpro

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var status: TextView
    private lateinit var values: TextView
    private lateinit var connectButton: Button
    private lateinit var readButton: Button
    private lateinit var codesButton: Button
    private lateinit var vinButton: Button
    private lateinit var firmwareButton: Button
    private lateinit var adaptationsButton: Button
    private lateinit var disconnectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41)
        }
    }

    override fun onResume() {
        super.onResume()
        val connected = ObdSession.socket?.isConnected == true
        readButton.isEnabled = connected
        codesButton.isEnabled = connected
        vinButton.isEnabled = connected
        disconnectButton.isEnabled = connected
        if (connected) {
            setStatus("Подключено: ${ObdSession.deviceName ?: "ELM327"}. Фоновая сессия активна.")
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFFF3F5F8.toInt())
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.rio3flashpro_logo)
            contentDescription = "Логотип Rio3FlashPro"
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                (144 * resources.displayMetrics.density).toInt(),
                (144 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = (10 * resources.displayMetrics.density).toInt()
            }
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
        vinButton = makeButton("Прочитать VIN автомобиля") { readVin() }
        firmwareButton = makeButton("Выбрать файл прошивки и проверить") { chooseFirmwareFile() }
        adaptationsButton = makeButton("Адаптации и сервисные функции") { showAdaptations() }
        disconnectButton = makeButton("Отключить Bluetooth OBD2") { disconnect() }
        values = TextView(this).apply {
            text = "Здесь появятся данные автомобиля."
            textSize = 16f
            setTextColor(0xFF172333.toInt())
            setPadding(14, 18, 14, 18)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val note = TextView(this).apply {
            text = "Важно: Bluetooth-диагностика сейчас рассчитана на совместимый ELM327. Проверка файла считает SHA-256, но не подтверждает совместимость с ЭБУ и не прошивает автомобиль. Scanmatik пока не подключён."
            textSize = 13f
            setPadding(4, 18, 4, 4)
        }
        root.addView(logo)
        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(status, match())
        root.addView(connectButton, match())
        root.addView(readButton, match())
        root.addView(codesButton, match())
        root.addView(vinButton, match())
        root.addView(firmwareButton, match())
        root.addView(adaptationsButton, match())
        root.addView(disconnectButton, match())
        root.addView(values, match())
        root.addView(note, match())
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        readButton.isEnabled = false
        codesButton.isEnabled = false
        vinButton.isEnabled = false
        disconnectButton.isEnabled = false
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 12 }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { action() } }

    private fun showAdaptations() {
        val items = arrayOf(
            "Обучение дроссельной заслонки",
            "Сброс адаптаций двигателя",
            "Сервисные функции ABS",
            "Сервисные функции АКПП",
            "Идентификация ЭБУ"
        )
        AlertDialog.Builder(this)
            .setTitle("Адаптации Kia Rio III")
            .setItems(items) { _, which ->
                val details = when (which) {
                    0 -> "Процедура может требоваться после очистки или замены дроссельного узла. Нужны точный тип ЭБУ и заводская процедура. Сейчас команда не отправляется."
                    1 -> "Сброс доступен не для всех ЭБУ и не всегда необходим. Сначала нужно определить блок, прочитать его идентификаторы и проверить условия выполнения."
                    2 -> "Функции ABS зависят от блока и могут включать специальные процедуры с требованиями безопасности. Сейчас доступна только подготовка раздела."
                    3 -> "Поддержка зависит от наличия АКПП и конкретного блока управления коробкой. Тип коробки пока не подтверждён."
                    else -> "Идентификаторы HW, SW и калибровки нельзя угадывать по году и объёму двигателя. Они будут показаны только после реального чтения поддерживаемым адаптером."
                }
                AlertDialog.Builder(this)
                    .setTitle(items[which])
                    .setMessage("$details\n\nСтатус: пока информационный режим. Реальное выполнение будет включено только после проверки совместимости ЭБУ, адаптера и безопасной процедуры.")
                    .setPositiveButton("Понятно", null)
                    .show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun chooseFirmwareFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, 52)
        } catch (e: Exception) {
            setStatus("Не удалось открыть выбор файла: ${e.message}")
        }
    }

    @Deprecated("Using the platform document picker for compatibility with the current minimal app.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 52 || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: run {
            setStatus("Файл не выбран.")
            return
        }
        setStatus("Проверка выбранного файла…")
        worker.execute {
            var tempFile: File? = null
            try {
                val displayName = contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "selected_firmware.bin"

                val extension = displayName.substringAfterLast('.', "").lowercase()
                    .takeIf { it.matches(Regex("^[a-z0-9]{1,8}$")) } ?: "unknown"
                val outputFile = File.createTempFile("rio3flash_", ".$extension", cacheDir)
                tempFile = outputFile

                val maxBytes = 64L * 1024L * 1024L
                var copied = 0L
                contentResolver.openInputStream(uri)?.use { source ->
                    outputFile.outputStream().use { target ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = source.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > maxBytes) {
                                throw IllegalArgumentException("Файл больше допустимых 64 МБ.")
                            }
                            target.write(buffer, 0, count)
                        }
                    }
                } ?: throw IllegalStateException("Не удалось открыть выбранный файл.")

                val result = ru.rio3flashpro.core.FirmwareFileInspector.inspect(outputFile)
                result.fold(
                    onSuccess = { report ->
                        val sizeKb = report.sizeBytes / 1024.0
                        runOnUiThread {
                            values.text = "Файл: ${report.fileName}\nРазмер: ${"%.1f".format(sizeKb)} КБ\nSHA-256:\n${report.sha256}\n\n${report.formatStatus}\n\nЭто только базовая проверка файла. Совместимость с ЭБУ и контрольные суммы производителя не проверены."
                            setStatus("Файл проверен; прошивка не выполнялась.")
                        }
                    },
                    onFailure = { error ->
                        runOnUiThread {
                            values.text = "Файл не прошёл базовую проверку:\n${error.message}"
                            setStatus("Проверка файла завершилась с замечанием.")
                        }
                    }
                )
            } catch (e: Exception) {
                runOnUiThread {
                    values.text = "Не удалось проверить файл:\n${e.message ?: "неизвестная ошибка"}"
                    setStatus("Ошибка проверки файла.")
                }
            } finally {
                try { tempFile?.delete() } catch (_: Exception) {}
            }
        }
    }

    private fun chooseDevice() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41)
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
                ObdSession.close()
                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                newSocket.connect()
                ObdSession.socket = newSocket
                ObdSession.input = newSocket.inputStream
                ObdSession.output = newSocket.outputStream
                ObdSession.deviceName = device.name ?: device.address
                val init = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
                init.forEach { command(it) }
                runOnUiThread {
                    val backgroundStarted = try {
                        startForegroundService(Intent(this, ObdForegroundService::class.java))
                        true
                    } catch (serviceError: Exception) {
                        setStatus("Bluetooth подключён, но фоновый режим не запущен: ${serviceError.message}")
                        false
                    }
                    if (backgroundStarted) {
                        setStatus("Подключено: ${device.name ?: device.address}. Фоновый режим включён.")
                    }
                    readButton.isEnabled = true
                    codesButton.isEnabled = true
                    vinButton.isEnabled = true
                    disconnectButton.isEnabled = true
                    values.text = "Адаптер подключён. Можно читать параметры и ошибки."
                }
            } catch (e: Exception) {
                closeConnection()
                runOnUiThread { setStatus("Не удалось подключиться: ${e.message ?: "проверьте адаптер"}") }
            }
        }
    }

    private fun command(cmd: String): String {
        val out = ObdSession.output ?: throw IllegalStateException("Адаптер не подключён")
        val ins = ObdSession.input ?: throw IllegalStateException("Нет входящего потока")
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
                val rpm = parseHexResponse(command("010C"), "410C", 2)?.let { ((it[0] * 256 + it[1]) / 4).toString() } ?: "нет данных"
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

    private fun readVin() {
        setStatus("Запрос VIN через OBD-II Mode 09…")
        worker.execute {
            try {
                // Mode 09 PID 02 is a standard VIN request, but not every ECU/adapter supports it.
                val raw = command("0902")
                val clean = raw.uppercase().replace("[^0-9A-F]".toRegex(), "")
                val marker = clean.indexOf("4902")
                if (marker < 0 || clean.length < marker + 8) {
                    runOnUiThread {
                        values.text = "ЭБУ не вернул VIN через стандартный запрос OBD-II.\nОтвет адаптера: $raw"
                        setStatus("VIN не получен; это не означает неисправность.")
                    }
                    return@execute
                }

                // After 49 02 comes the record count byte (normally 01), then ASCII VIN bytes.
                val payloadStart = marker + 8
                val payload = clean.substring(payloadStart)
                val bytes = payload.chunked(2).mapNotNull { it.toIntOrNull(16) }
                val vin = bytes.mapNotNull { value ->
                    if (value in 0x20..0x7E) value.toChar() else null
                }.joinToString("").filter { it.isLetterOrDigit() }

                val valid = vin.length == 17 && vin.all { it in "ABCDEFGHJKLMNPRSTUVWXYZ0123456789" }
                runOnUiThread {
                    values.text = if (valid) {
                        "VIN автомобиля: $vin\n\nVIN прочитан стандартным запросом OBD-II Mode 09 PID 02."
                    } else {
                        "Ответ на запрос VIN получен, но его не удалось надёжно распознать.\nСырой ответ: $raw"
                    }
                    setStatus(if (valid) "VIN успешно прочитан" else "VIN не подтверждён")
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка чтения VIN: ${e.message}") }
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
        ObdSession.close()
    }

    private fun disconnect() {
        closeConnection()
        stopService(Intent(this, ObdForegroundService::class.java))
        readButton.isEnabled = false
        codesButton.isEnabled = false
        vinButton.isEnabled = false
        disconnectButton.isEnabled = false
        setStatus("Адаптер отключён вручную.")
        values.text = "Bluetooth-соединение закрыто."
    }

    override fun onDestroy() {
        // Keep the OBD session alive when Android recreates the Activity in background.
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
