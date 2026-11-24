# Adaptations and service functions (Android)

## Current implementation

The Android app includes an informational "Адаптации и сервисные функции" section for Kia Rio III. It lists:
- throttle-body relearn (candidate procedure; not executed)
- engine adaptation reset (not executed)
- ABS service functions (not executed)
- automatic transmission service functions (not executed; transmission type is unconfirmed)
- ECU identification (values are never inferred from model year or displacement)

## Safety boundary

This screen is a catalog and education layer only. It does not send diagnostic or actuator commands, clear learned values, run ABS bleeding, calibrate steering, reset transmission adaptations, unlock an ECU, or write firmware.

A procedure may be enabled only after all of the following are established:
1. Exact ECU/module identity and hardware/software/calibration identifiers from the actual vehicle.
2. Exact vehicle configuration, including transmission type where relevant.
3. Documented protocol and adapter API support for the selected operation.
4. Manufacturer procedure, prerequisites, stable vehicle power, and safety checks.
5. A verified test plan and a way to recover from interruption or failure.

Generic ELM327 OBD-II access does not imply support for manufacturer-specific service functions. Scanmatik support must be implemented against its documented API; it is not currently integrated.

## Next engineering step

Build a verified ECU identification path and adapter capability reporting. Keep every unsupported service procedure visibly unavailable rather than presenting a button that could imply it is safe to run.
