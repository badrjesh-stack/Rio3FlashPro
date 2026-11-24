# Rio3FlashPro — architecture

## Target vehicle
- Kia Rio III, model year 2016, 1.6 L petrol (vehicle configuration supplied by the owner).
- ECU identity is not yet confirmed. The app must read identification from the vehicle before selecting any programming procedure.

## Supported clients
- Android: Kotlin application for guided diagnostics, vehicle identification, session logs, and file handling.
- Windows: desktop client for diagnostics and future programming workflows where a supported Scanmatik driver/API and ECU protocol are available.

## Adapter strategy
- Initial target: Scanmatik 2 Pro.
- Keep adapter access behind an interface so Scanmatik 3 or another officially supported interface can be added later.
- Do not assume that J2534 support on Windows implies Android support. Android transport and vendor SDK/API availability must be verified separately.
- Do not communicate with undocumented/private adapter protocols or distribute vendor drivers/SDKs without permission.

## ECU programming status
Programming is NOT implemented. The application must not send erase, unlock, write, or reprogram commands until all of the following are verified for the exact ECU hardware/software ID:
1. A documented, supported programming protocol and correct adapter API.
2. Correct memory layout, file format, and checksum method from a legitimate source.
3. A verified original read/backup procedure and recovery method.
4. Stable regulated vehicle power and a tested recovery setup.
5. Explicit user confirmation after compatibility checks.

## Milestones
1. Buildable Android diagnostic foundation (current repository baseline).
2. Windows desktop shell and CI build.
3. Scanmatik connection/identification on Windows using supported vendor interfaces.
4. ECU identification database and session reports.
5. Read-only backup workflow, only for validated ECU/protocol combinations.
6. File validation and checksum verification.
7. Programming support only after bench validation and recovery testing.

## Security and reliability
- Never treat a filename or vehicle year/engine alone as proof of ECU compatibility.
- Keep immutable originals and record SHA-256 hashes for firmware files.
- Fail closed on unknown ECU IDs, unknown file sizes, checksum failures, interrupted communications, or low supply voltage.
- Never claim a successful write unless the ECU verification step completes.
