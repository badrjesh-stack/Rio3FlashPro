package ru.rio3flashpro.core

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Performs basic file hygiene checks and calculates SHA-256.
 *
 * This does NOT decode firmware formats, validate checksums required by an ECU,
 * establish vehicle compatibility, or authorize programming.
 */
object FirmwareFileInspector {
    private const val MAX_FILE_SIZE_BYTES = 64L * 1024L * 1024L
    private val supportedExtensions = setOf("bin", "hex", "mot")

    data class Inspection(
        val fileName: String,
        val sizeBytes: Long,
        val sha256: String,
        val formatStatus: String =
            "Формат и совместимость с ЭБУ не подтверждены."
    )

    fun inspect(file: File): Result<Inspection> = runCatching {
        require(file.exists() && file.isFile) { "Файл не найден." }
        val extension = file.extension.lowercase()
        require(extension in supportedExtensions) {
            "Поддерживаются только файлы BIN, HEX и MOT."
        }
        val size = file.length()
        require(size in 1..MAX_FILE_SIZE_BYTES) {
            "Размер файла должен быть от 1 байта до 64 МБ."
        }

        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { stream ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }

        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        Inspection(file.name, size, hash)
    }
}
