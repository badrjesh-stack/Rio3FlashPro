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
    private val documentVin = "Z94CB41BBHR406592"
    private val worker = Executors.newSingleThreadExecutor()
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var status: TextView
    private lateinit var values: TextView
    private lateinit var connectButton: Button
    private lateinit var readButton: Button
    private lateinit var codesButton: Button
    private lateinit var vinButton: Button
    private lateinit var supportedButton: Button
    private lateinit var clearCodesButton: Button
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
        supportedButton.isEnabled = connected
        clearCodesButton.isEnabled = connected
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
            contentDescription = "Логотип RioTech Pro"
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
            text = "RioTech Pro"
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
        supportedButton = makeButton("Проверить поддерживаемые параметры") { readSupportedPids() }
        clearCodesButton = makeButton("Сбросить ошибки двигателя") { confirmClearCodes() }
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
            text = "OBD-II: стандартная диагностика двигателя через ELM327. Доступ к ABS, АКПП и фирменным адаптациям Kia зависит от блока и интерфейса. SHA-256 не подтверждает совместимость прошивки. Запись ЭБУ отключена."
            textSize = 13f
            setPadding(4, 18, 4, 4)
        }
        root.addView(logo)
        root.addView(title, match())
        root.addView(subtitle, match())
        val vehicle = TextView(this).apply {
            text = "Автомобиль: Kia Rio III, 2016\\nVIN из документов: $documentVin"
            textSize = 14f
            setTextColor(0xFF174B3A.toInt())
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFE7F3EC.toInt())
        }
        root.addView(vehicle, match())
        root.addView(status, match())
        root.addView(connectButton, match())
        root.addView(readButton, match())
        root.addView(codesButton, match())
        root.addView(vinButton, match())
        root.addView(supportedButton, match())
        root.addView(clearCodesButton, match())
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
        supportedButton.isEnabled = false
        clearCodesButton.isEnabled = false
        disconnectButton.isEnabled = false
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 12 }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply { text = label; setOnClickListener { action() } }

    private fun showAdaptations() {
        val items = arrayOf(
            "Идентификация ЭБУ двигателя (OBD-II)",
            "Почему адаптации пока недоступны"
        )
        AlertDialog.Builder(this)
            .setTitle("Адаптации Kia Rio III")
            .setItems(items) { _, which ->
                if (which == 0) {
                    readEcuIdentification()
                    return@setItems
                }
                val details = "Стандартный ELM327 и универсальный OBD-II не дают подтверждённого доступа к фирменным процедурам Kia, ABS и отдельному блоку АКПП. Программа не будет имитировать эти функции: нужны точные идентификаторы блоков, совместимый интерфейс и проверенная заводская процедура."
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
                    supportedButton.isEnabled = true
                    clearCodesButton.isEnabled = true
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

    private fun readSupportedPids() {
        setStatus("Определение поддерживаемых PID…")
        worker.execute {
            try {
                val supported = linkedSetOf<Int>()
                val report = StringBuilder("Поддерживаемые параметры SAE J1979\\n\\n")
                var base = 0
                for (group in 0 until 4) {
                    val raw = command("01%02X".format(base))
                    val bytes = parseHexResponse(raw, "41%02X".format(base), 4)
                    report.append("Группа 0x%02X: ".format(base))
                    if (bytes == null) {
                        report.append("нет ответа\\n")
                        break
                    }
                    report.append("ответ получен\\n")
                    for (bit in 0 until 32) {
                        if ((bytes[bit / 8] and (1 shl (7 - bit % 8))) != 0) supported.add(base + bit + 1)
                    }
                    if ((bytes[3] and 1) == 0) break
                    base += 0x20
                }
                val labels = mapOf(
                    0x04 to "Расчётная нагрузка", 0x05 to "Температура ОЖ",
                    0x0C to "Обороты двигателя", 0x0D to "Скорость",
                    0x0F to "Температура впускного воздуха", 0x10 to "Расход воздуха",
                    0x11 to "Положение дросселя", 0x1F to "Время работы двигателя",
                    0x2F to "Уровень топлива", 0x42 to "Напряжение ЭБУ",
                    0x46 to "Температура окружающего воздуха"
                )
                report.append("\\nОбнаруженные PID: ")
                report.append(if (supported.isEmpty()) "не определены" else supported.sorted().joinToString { "0x%02X".format(it) })
                report.append("\\n\\nПоддерживаемые известные параметры:\\n")
                labels.forEach { (pid, label) -> if (pid in supported) report.append("• $label (PID 0x%02X)\\n".format(pid)) }
                report.append("\\nПоддержка PID не гарантирует доступность каждого значения при любых условиях.")
                runOnUiThread { values.text = report.toString(); setStatus("Проверка PID завершена") }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка проверки PID: ${e.message}") }
            }
        }
    }

    private fun confirmClearCodes() {
        AlertDialog.Builder(this)
            .setTitle("Сбросить ошибки двигателя?")
            .setMessage("Mode 04 очищает поддерживаемые эмиссионные коды и может сбросить readiness-мониторы. Сначала сохраните коды. Сброс не устраняет неисправность. Продолжить?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сбросить") { _, _ -> clearTroubleCodes() }
            .show()
    }

    private fun clearTroubleCodes() {
        setStatus("Отправка команды Mode 04…")
        worker.execute {
            try {
                val raw = command("04")
                val accepted = raw.uppercase().replace("[^0-9A-F]".toRegex(), "").contains("44")
                runOnUiThread {
                    values.text = "Ответ на очистку (Mode 04):\\n$raw\\n\\n" +
                        if (accepted) "ЭБУ подтвердил положительный ответ 44. Повторно прочитайте ошибки." else "Подтверждение 44 не обнаружено. Сброс не подтверждён."
                    setStatus(if (accepted) "Команда очистки подтверждена" else "Очистка не подтверждена")
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка очистки: ${e.message}") }
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
                // ELM327 may print multi-frame CAN replies as "0: HEX", "1: HEX", ...
                // The decimal frame labels are not payload bytes and must not be joined to the VIN.
                // Strip every numbered ELM frame label, including labels on a single line:
                // "0: 4902... 1: ... 2: ...". Keep only the actual response bytes.
                val withoutFrameLabels = raw.replace(
                    Regex("(?:^|\\\\s)\\\\d+\\\\s*:\\\\s*"),
                    ""
                )
                val clean = withoutFrameLabels.uppercase().replace("[^0-9A-F]".toRegex(), "")
                val marker = clean.indexOf("4902")
                if (marker < 0 || clean.length < marker + 8) {
                    runOnUiThread {
                        values.text = "ЭБУ не вернул VIN через стандартный запрос OBD-II.\nОтвет адаптера: $raw"
                        setStatus("VIN не получен; это не означает неисправность.")
                    }
                    return@execute
                }

                // 49 02 = positive response; 01 is the record count (one byte).
                // The 17 VIN ASCII bytes therefore begin at marker + 6 hex characters.
                val payloadStart = marker + 6
                val payload = clean.substring(payloadStart)
                val bytes = payload.chunked(2).mapNotNull { pair ->
                    pair.takeIf { it.length == 2 }?.toIntOrNull(16)
                }
                val vin = bytes.takeWhile { it in 0x20..0x7E }
                    .map { it.toChar() }
                    .joinToString("")
                    .trim()

                val valid = vin.length == 17 && vin.all { it in "ABCDEFGHJKLMNPRSTUVWXYZ0123456789" }
                runOnUiThread {
                    val comparison = when {
                        !valid -> "Сравнение с VIN из документов: невозможно — VIN ЭБУ не распознан."
                        vin == documentVin -> "СОВПАДЕНИЕ: VIN ЭБУ совпадает с VIN в документах."
                        else -> "ВНИМАНИЕ: VIN ЭБУ НЕ совпадает с VIN в документах. Проверьте автомобиль и блок управления."
                    }
                    values.text = if (valid) {
                        "VIN из документов: $documentVin\nVIN из ЭБУ: $vin\n\n$comparison\n\nИсточник: OBD-II Mode 09 PID 02."
                    } else {
                        "VIN из документов: $documentVin\nОтвет на запрос VIN получен, но его не удалось надёжно распознать.\nСырой ответ: $raw\n\n$comparison"
                    }
                    setStatus(when {
                        !valid -> "VIN ЭБУ не подтверждён"
                        vin == documentVin -> "VIN ЭБУ совпадает с документами"
                        else -> "Обнаружено несовпадение VIN"
                    })
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка чтения VIN: ${e.message}") }
            }
        }
    }


    /**
     * Reads standard SAE J1979 Mode 09 identification records.
     * PID 04 = Calibration ID, PID 06 = CVN, PID 0A = ECU name.
     * These records do not necessarily expose hardware or software part numbers.
     */
    private fun readEcuIdentification() {
        setStatus("Чтение идентификаторов ЭБУ через OBD-II Mode 09…")
        worker.execute {
            try {
                val calRaw = command("0904")
                val cvnRaw = command("0906")
                val nameRaw = command("090A")

                fun findPayload(raw: String, pid: String): String? {
                    val hex = raw.uppercase().replace("[^0-9A-F]".toRegex(), "")
                    val marker = "49$pid"
                    val index = hex.indexOf(marker)
                    if (index < 0 || hex.length < index + 6) return null
                    // One byte after the service/PID is the record count.
                    return hex.substring(index + 6)
                }

                fun asciiRecord(raw: String, pid: String): String? {
                    val payload = findPayload(raw, pid) ?: return null
                    val chars = payload.chunked(2).mapNotNull { pair ->
                        pair.takeIf { it.length == 2 }?.toIntOrNull(16)
                    }.takeWhile { it != 0 }
                        .filter { it in 0x20..0x7E }
                        .map { it.toChar() }
                        .joinToString("")
                        .trim()
                    return chars.takeIf { it.isNotBlank() }
                }

                val calibration = asciiRecord(calRaw, "04")
                val ecuName = asciiRecord(nameRaw, "0A")
                val cvnPayload = findPayload(cvnRaw, "06")
                val cvn = cvnPayload?.take(8)?.takeIf { it.length == 8 }
                    ?.chunked(2)?.joinToString(" ")?.takeIf { it.isNotBlank() }

                val report = buildString {
                    appendLine("Идентификация по стандартному OBD-II Mode 09")
                    appendLine()
                    appendLine("Calibration ID: ${calibration ?: "не поддерживается или не получен"}")
                    appendLine("CVN: ${cvn ?: "не поддерживается или не получен"}")
                    appendLine("Название ЭБУ: ${ecuName ?: "не поддерживается или не получено"}")
                    appendLine()
                    appendLine("Ответ PID 04: $calRaw")
                    appendLine("Ответ PID 06: $cvnRaw")
                    appendLine("Ответ PID 0A: $nameRaw")
                    appendLine()
                    append("Важно: стандартные запросы не гарантируют получение HW/SW-номеров и не идентифицируют отдельный блок АКПП. Отсутствие ответа не означает неисправность ЭБУ.")
                }

                runOnUiThread {
                    values.text = report
                    val found = calibration != null || cvn != null || ecuName != null
                    setStatus(if (found) "Получены некоторые идентификаторы OBD-II" else "ЭБУ не вернул поддерживаемые записи Mode 09")
                }
            } catch (e: Exception) {
                runOnUiThread { setStatus("Ошибка идентификации ЭБУ: ${e.message}") }
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
        supportedButton.isEnabled = false
        clearCodesButton.isEnabled = false
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
