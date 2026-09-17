# Handoff

## Status

Implementation and compile validation are complete. Live Minecraft visual and
two-player ownership validation remain pending.

## Completed

- Audited the current ChartMaker editor layout/input/lifecycle.
- Audited the current Preview editor overlay and directional opcode drift.
- Defined the world-first shell and vanilla hotbar tool mapping.
- Implemented the transparent non-pausing shell, responsive timeline drawer,
  compact transport, and vanilla hotbar-backed nine-tool model.
- Kept inline Test/LOAD inside the shell and removed implicit Inspector Apply
  from preview serialization.
- Corrected editor message directions and global server-selection focus.
- Implemented owner-only Preview overlays and current-instance interaction
  ownership checks in `E:/Dev/RhythMC-Preview`.
- Added a client Stop barrier and server lifecycle generation fence. Stable
  previews pause and retain their Arena/Overlay; incomplete initialization is
  cancelled safely before a later request can reuse its slot.
- Updated contracts, CURRENT files, shortcut/channel docs, AGENTS summaries,
  active specs, and changelogs in both repositories.
- Passed ChartMaker compilation:
  `$env:JAVA_HOME='E:\.jdk\zulu25'; .\gradlew.bat "-Dorg.gradle.java.home=E:\.jdk\zulu25" --no-daemon compileClientJava`.
- Passed Preview compilation:
  `$env:JAVA_HOME='E:\.jdk\zulu25'; mvn compile`.

## Pending Runtime Validation

- Check the transparent shell and Inspector containment at representative
  Minecraft GUI scales.
- With two players, verify only the owner sees and can activate their overlay.
- Verify right-click produces one S→C 108 and no C→S echo.

## Residual Risks

- Note selection still uses mutable zero-based list indices.
- A loaded chart does not hot-apply to an already paused `GameInstance`.
- Arbitrary seek does not yet deterministically rebuild all Effects.
- The open `Screen` owns the cursor; `F` is still required for direct camera and
  crosshair interaction.
- Full PREVIEW_READY RTT/clock correction is not implemented.
- Existing lossless round-trip, multi-event, Effect schema, and Inspector Apply
  defects are outside this phase.

## Important Constraints

- Preserve the current 52px toolbar, Auto/Judge mode, and unrelated dirty work.
- Do not revive the deprecated `EditorWorldLauncher` implementation.
- Do not add chart fields, auth, resource-pack, HTTP, or gameplay systems.
- Stable IDs, direct world placement, and live incremental chart mutation are
  follow-up tasks.
