# Android core foundation

The Android project now has a small core boundary for future adapter support:

- `VehicleProfile` stores the owner's known vehicle details (Kia Rio III, 2016, 1.6 L).
- `EcuIdentity` keeps protocol, ECU name, hardware ID, software ID, calibration ID and VIN separate. Vehicle model/year/engine are not treated as proof of ECU identity.
- `AdapterTransport` is a diagnostic-only interface. A future Scanmatik implementation must use the manufacturer's documented API and verified Android support; the interface deliberately contains no ECU erase/write/programming method.
- `FirmwareFileInspector` checks file existence, extension and size, then calculates SHA-256. It does not parse the file, verify ECU-specific checksums, prove compatibility, or make a file safe to flash.

## Current limitations

The running Android screen still uses Bluetooth SPP with ELM327-compatible adapters for standard OBD-II diagnostics. This is not Scanmatik support. Scanmatik 2 PRO/3 connectivity is not implemented or verified on Android.

No ECU read, unlock, erase or write operation is implemented. Before any future programming work, the exact ECU hardware/software identifiers, official adapter API, programming protocol, file format/checksum rules, recovery procedure and stable power requirements must be established and bench-tested.
