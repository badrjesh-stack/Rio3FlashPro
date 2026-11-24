package ru.rio3flashpro.core

/**
 * Confirmed vehicle details supplied by the owner.
 * ECU identifiers remain empty until they are read from the actual vehicle.
 */
data class VehicleProfile(
    val make: String = "Kia",
    val model: String = "Rio III",
    val modelYear: Int = 2016,
    val engineDisplacementLitres: Double = 1.6,
    val engineCode: String? = null
)

/**
 * Identification returned by a diagnostic adapter/ECU.
 * Never infer hardware or software IDs from the vehicle model alone.
 */
data class EcuIdentity(
    val protocol: String? = null,
    val ecuName: String? = null,
    val hardwareId: String? = null,
    val softwareId: String? = null,
    val calibrationId: String? = null,
    val vin: String? = null
) {
    val hasVerifiedHardwareAndSoftwareIds: Boolean
        get() = !hardwareId.isNullOrBlank() && !softwareId.isNullOrBlank()
}
