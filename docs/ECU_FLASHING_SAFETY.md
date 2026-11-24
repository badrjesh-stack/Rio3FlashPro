# ECU flashing safety gates

Rio3FlashPro currently provides diagnostic scaffolding only. No ECU erase/write/reprogram routine is included.

Before enabling programming for a specific ECU, the project requires documented evidence for the ECU hardware number, software/calibration ID, processor/flash type, supported programming session, memory regions, file format, checksum/correction rules, and recovery path.

A generic ELM327 diagnostic adapter is not a programming interface. A Scanmatik model must be connected through its officially supported driver/API, and the exact ECU must be supported by a validated programming protocol. The existence of a J2534 interface alone does not identify the ECU protocol or guarantee flash support.

Never begin programming without a stable regulated power supply appropriate to the vehicle and adapter, a verified original backup, a known-good recovery method, and a bench-tested procedure. Do not experiment on the vehicle's only ECU.

The software must block programming if identification is incomplete, compatibility is uncertain, file validation fails, communication is unstable, or required power checks are unavailable.
