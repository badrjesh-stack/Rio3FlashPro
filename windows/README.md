# RioTech Pro for Windows

This folder is reserved for the Windows desktop client. The first Windows milestone is a buildable UI shell and diagnostic/session architecture; it does not yet communicate with Scanmatik or flash an ECU.

Planned stack: .NET 8 desktop application. Adapter access will be implemented only through Scanmatik's supported Windows driver/API and a documented interface. J2534 availability alone is not sufficient to enable programming.

The Windows client will share project rules and ECU compatibility records with the Android client, while keeping platform-specific adapter transports separate.
