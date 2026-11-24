package ru.rio3flashpro.core

/**
 * Diagnostic transport boundary. Implementations must use a documented adapter API.
 *
 * This interface intentionally exposes diagnostic requests only. It has no erase,
 * unlock, programming-voltage, or ECU-write operation.
 */
interface AdapterTransport {
    val adapterName: String
    val isConnected: Boolean

    fun connect(): Result<Unit>
    fun disconnect()

    /** Sends a supported diagnostic request and returns the raw response. */
    fun sendDiagnosticRequest(request: DiagnosticRequest): Result<String>
}

data class DiagnosticRequest(
    val serviceAndPid: String
) {
    init {
        require(serviceAndPid.matches(Regex("^[0-9A-Fa-f]{2,8}$"))) {
            "Diagnostic request must contain 1–4 hexadecimal bytes."
        }
    }
}
