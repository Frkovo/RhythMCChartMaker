# Contract Map

Updated: 2026-06-28

## Contract Change Definition

A task crosses a contract boundary when it changes any of these:

- `rhythmc:chart_preview` plugin channel opcode set, direction, payload schema, chunking protocol, lifecycle, or trust model.
- Chart JSON parsing semantics (field names, types, defaults) that the Preview plugin's deserializers also follow.
- Mod config keys or user workflow.
- Preview lifecycle behavior (start/stop/restart semantics, clock sync).

## The Only Contract: `rhythmc:chart_preview`

A bidirectional Bukkit plugin channel between this mod (client) and the RhythMC-Preview Paper plugin (server). Opcodes and payload format must match `ChartPreviewChannel.java` on both sides.

### Direction Client → Server

| Opcode | Name | Payload | Notes |
|---|---|---|---|
| 1 | `HELLO` | `int protocolVersion`, `String modVersion` | Required before editing/upload/preview. Server responds `HELLO_ACK`. |
| 2 | `CHART_LOAD` | `String manifestJson`, `String levelJson` (chunked if large) | Replace the current in-memory chart for the sender. Server responds `CHART_LOAD_ACK`. |
| 3 | `PREVIEW_START` | `double startBeat`, `long clientWallClockMs` | Spawn a preview `GameInstance` for the sender. Server responds `PREVIEW_READY`. |
| 4 | `PREVIEW_STOP` | (none) | Pause the sender's preview `GameInstance`. Arena and editor overlay are retained. Server responds `PREVIEW_STOPPED`. |
| 5 | `PREVIEW_RESTART` | `double newStartBeat`, `long clientWallClockMs` | Tear down and respawn at new beat. Server responds `PREVIEW_READY`. |
| 6 | `CHART_LOAD_CHUNK_START` | `String chunkId`, `int totalChunks` | Begin chunked chart upload. |
| 7 | `CHART_LOAD_CHUNK` | `String chunkId`, `int index`, `byte[] bytes` | One chart chunk. |
| 8 | `CHART_LOAD_CHUNK_END` | `String chunkId` | Finish chunked chart upload. |
| 9 | `FILE_UPLOAD_START` | `String uploadId`, `String fileType`, `String fileName`, `long totalBytes`, `String sha256`, `int totalChunks` | Begin schematic/audio upload. `fileType` is `SCHEMATIC` or `AUDIO`. |
| 10 | `FILE_UPLOAD_CHUNK` | `String uploadId`, `int index`, `byte[] bytes` | One file chunk. |
| 11 | `FILE_UPLOAD_END` | `String uploadId` | Finish file upload. Server validates sha256 and caches the file. |
| 12 | `EDITOR_OPEN` | `String data` | Request the server to open/refresh the in-world editor session. |
| 13 | `EDITOR_SELECT` | `String data` | Report client-side selection to the server. |

### Direction Server → Client

| Opcode | Name | Payload | Notes |
|---|---|---|---|
| 101 | `HELLO_ACK` | `boolean ok`, `int protocolVersion`, `int maxPayloadBytes`, `String defaultSchematicName`, `String message` | Unlocks editor when `ok=true`. |
| 102 | `CHART_LOAD_ACK` | `boolean ok`, `String error` | Confirms chart loaded/parsed. |
| 103 | `PREVIEW_READY` | `long serverWallClockMs` | Preview instance spawned; mod uses this + RTT to align local audio start. |
| 104 | `PREVIEW_STOPPED` | (none) | Preview paused (arena retained). |
| 105 | `ERROR` | `String message` | Generic error. |
| 106 | `FILE_UPLOAD_ACK` | `String uploadId`, `String fileType`, `boolean ok`, `String message` | Confirms file upload or reports rejection. |
| 107 | `EDITOR_OPEN` | `String data` | Server tells client to open the editor GUI. |
| 108 | `EDITOR_SELECT` | `String data` | Server tells client to select an object (`track:<id>`, `note:<trackId>:<noteIndex>`, `effect:<index>`, `bpm:<index>`). |

### Chunking

Bukkit `Messenger` packets are ~32KB. If a `CHART_LOAD` payload exceeds the safe size, the client splits it:

- `CHART_LOAD_CHUNK_START` (opcode 6): `String chunkId`, `int totalChunks`.
- `CHART_LOAD_CHUNK` (opcode 7): `String chunkId`, `int index`, `byte[] bytes`.
- `CHART_LOAD_CHUNK_END` (opcode 8): `String chunkId`.

The server reassembles by `chunkId`, then parses manifest+level JSON and responds `CHART_LOAD_ACK`. `FILE_UPLOAD_*` uses the same chunk-size limit with `uploadId`.

### Payload Encoding

All payloads use plugin-channel raw bytes. Opcode is the first big-endian `int`. Strings are encoded as `int byteLength` followed by UTF-8 bytes. Chunk bytes are encoded as `int byteLength` followed by raw bytes. This deliberately avoids Java `writeUTF` and Minecraft `writeString`/varint string encoding drift.

### Trust Model

- The sender is a Bukkit `Player` currently online on the preview server. Identity = that online player. No token, no session binding.
- The server only spawns a preview `GameInstance` for the online player who sent the packet.
- Any online player can trigger a preview by default. Configurable restriction optional.

### Lifecycle

1. Client joins multiplayer server and sends `HELLO`.
2. Server validates protocol version and responds `HELLO_ACK`; the mod unlocks editing only when `ok=true`.
3. Client may upload `SCHEMATIC` and `AUDIO` files. Default schematic is `VILLAGE.schem`.
4. Client sends `CHART_LOAD` (manifest + level JSON of the current chart).
5. Server parses JSON → `RawLevel`, stores it keyed by sender, responds `CHART_LOAD_ACK`.
6. Client sends `PREVIEW_START` with the desired start beat and client wall clock.
7. Server spawns a preview `GameInstance` (visual-only, no song audio, no scoring), aligns timing to the start beat, and sends `PREVIEW_READY` with the server wall clock.
8. Client starts local audio at the beat, RTT-corrected against `PREVIEW_READY`.
9. At any time: `PREVIEW_STOP` destroys the instance; `PREVIEW_RESTART` tears down and respawns at a new beat.

## Chart JSON Semantics

The mod's `ChartProjectIo` must serialize charts with field names and types that the Preview plugin's `File/Deserializers/*` accept:

- `manifest.yml` (YAML) → `SongManifest`.
- Level JSON with `meta`, `tracks[]`, `effects[]`. Track fields: `id`, `beatDivision`, `speedEvents`, `xTransformEvents`/`y`/`z`, `xRotateEvents`/`y`/`z`, `xScaleEvents`/`y`/`z`, `notes[]`. Note fields: `noteType`, `beat`, `pos[x,y,z]`, `scale[x,y,z]`, `rotation[x,y,z]`, `holdGroup`, `holdLengthBeats`.

If parsing semantics change on either side, update both repos and docs in the same task.

## Out of Scope

There is no HTTP, WebSocket, DB, auth/session, resource-pack, collection, unlock, or frontend DTO contract in this repo. Do not reintroduce any of these.
