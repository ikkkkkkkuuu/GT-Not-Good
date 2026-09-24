# RTS Building port provenance

This is an unfinished staged RTS Building integration into GTNG, not an official release.

Behaviour reference: Hcrab / JerryLunar and contributors, https://github.com/Hcrab/RTSbuilding,
commit b5a70d83a41d7149f7c34d4aae2081017532ab08.
Legacy API reference: zslingy and contributors, https://github.com/zslingy/RTSbuilding-1.7.10-GTNH,
commit 5147af59f8e35cd7710db6359f7a240dd1892644.

The upstream source is LGPL-3.0-only; original media is separately restricted.
No upstream media has been copied. Session protocol and lifecycle adapters are newly authored.
Phase 3 adapts CameraMotionSolver, RtsCameraSmoothingMath, and camera controller smoothing from the pinned upstream.
These files remain LGPL-3.0-only, outside the project's general MIT grant.
ASSET_MANIFEST.json records source paths and modifications. LGPL-3.0.txt and GPL-3.0.txt
contain the applicable license terms.

Phase 2 implements server authorization and client view ownership/restoration.
Camera movement is integrated into the session lifecycle; faithful GUI and real building actions
remain unfinished. Test probe screens are not shipped.

Source mapping: reference/RTS_MIGRATION_MAP.md in the GTNG source distribution.