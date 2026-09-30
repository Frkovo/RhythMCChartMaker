# RhythMCChartMaker Project Brief

Updated: 2026-06-23

## 30-Second Summary

RhythMCChartMaker is a Fabric client mod that provides a full chart editor for RhythMC charts. It features a timeline-based UI with note/track/effect editing, local audio playback, and preview integration with a RhythMC-Preview Paper server over the `rhythmc:chart_preview` plugin channel.

## Repositories

- Chart maker mod: `D:/Dev/RhythMCChartMaker` (this repo)
- Preview plugin: `D:/Dev/RhythMC-Preview`
- Full-game plugin: `D:/Dev/RhythMC-Reborn`

## Product Shape

- This mod owns: chart editor UI (`ChartEditorScreen`), local audio playback (Java Sound + SPI decoders), plugin channel client (`PreviewClient`), chart project IO (YAML + JSON).
- The Preview plugin owns: chart visual playback, arena paste, file cache.
- There is no game backend. The mod connects to a Preview server through the Bukkit plugin channel system.

## User Requirements

- Reborn compatibility matters. Track event semantics should follow Reborn utilities/ChartUtils behavior.
- Track `Speed`, `X/Y/Z Transform`, `X/Y/Z Rotation`, and `X/Y/Z Scale/Stretch` should each allow only one event per track lane.
- Note coordinates are center-relative and can be decimal values.
- Note `X`, `Y`, and `Z` should be edited separately, not packed into one confusing field.
- The UI should feel closer to an editor such as Visual Maimai / video timeline tools, not a generic debug panel.
- Important note editing shortcuts are expected: multi-select, copy, cut, paste, move, undo, redo.
- The user expects broader rewrites rather than tiny cosmetic patches.

## Visual Maimai Reference

The user explicitly asked to follow Visual Maimai ideas and images:

- doc: `https://visual-maimai-manual.github.io/guide/gui.html`
- important reference concepts:
  - menu-style top controls (`File / Edit / Options`)
  - note editing shortcuts and clipboard workflow
  - clearer editor zoning between note tools, preview, and track area
