# Contract Map

Updated: 2026-08-17

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
| 3 | `PREVIEW_START` | `double startBeat`, `long clientWallClockMs`, `byte mode` | Spawn a preview `GameInstance` for the sender. Server responds `PREVIEW_READY`. `mode`: 0=AUTO (notes despawn at rail end, no judgement), 1=JUDGE (hit judgement + feedback). `PREVIEW_RESTART` does not resend mode; the running instance keeps its mode. |
| 4 | `PREVIEW_STOP` | (none) | Pause and retain a ready preview. If startup is incomplete, cancel it and release its arena only after initialization reaches a safe boundary. Server responds `PREVIEW_STOPPED`. |
| 5 | `PREVIEW_RESTART` | `double newStartBeat`, `long clientWallClockMs` | Seek and resume the retained instance at the new beat when available. Server responds `PREVIEW_READY`. |
| 6 | `CHART_LOAD_CHUNK_START` | `String chunkId`, `int totalChunks` | Begin chunked chart upload. |
| 7 | `CHART_LOAD_CHUNK` | `String chunkId`, `int index`, `byte[] bytes` | One chart chunk. |
| 8 | `CHART_LOAD_CHUNK_END` | `String chunkId` | Finish chunked chart upload. |
| 9 | `FILE_UPLOAD_START` | `String uploadId`, `String fileType`, `String fileName`, `long totalBytes`, `String sha256`, `int totalChunks` | Begin schematic/audio upload. `fileType` is `SCHEMATIC` or `AUDIO`. |
| 10 | `FILE_UPLOAD_CHUNK` | `String uploadId`, `int index`, `byte[] bytes` | One file chunk. |
| 11 | `FILE_UPLOAD_END` | `String uploadId` | Finish file upload. Server validates sha256 and caches the file. |
| 12 | `EDITOR_OPEN` | `String data` | Request server-side editor focus/refresh. Server may answer once with S2C opcode 107. |
| 13 | `EDITOR_SELECT` | `String data` | Store client-side selection on the server. The server does not echo opcode 108. |

### Direction Server → Client

| Opcode | Name | Payload | Notes |
|---|---|---|---|
| 101 | `HELLO_ACK` | `boolean ok`, `int protocolVersion`, `int maxPayloadBytes`, `String defaultSchematicName`, `String message` | Unlocks editor when `ok=true`. |
| 102 | `CHART_LOAD_ACK` | `boolean ok`, `String error` | Confirms chart loaded/parsed. |
| 103 | `PREVIEW_READY` | `long serverWallClockMs` | Preview instance is ready. The current mod starts local audio when this arrives; full RTT/clock correction remains pending. |
| 104 | `PREVIEW_STOPPED` | (none) | The previous start generation is fenced. A ready preview is paused and retained; an incomplete startup is being safely cancelled. |
| 105 | `ERROR` | `String message` | Generic error. |
| 106 | `FILE_UPLOAD_ACK` | `String uploadId`, `String fileType`, `boolean ok`, `String message` | Confirms file upload or reports rejection. |
| 107 | `EDITOR_OPEN` | `String data` | Focus the client editor shell. Handling it never sends C2S opcode 12. |
| 108 | `EDITOR_SELECT` | `String data` | Apply a world-originated selection (`track:<id>`, `note:<trackId>:<noteIndex>`, `effect:<index>`, `bpm:<index>`) and focus the shell. Handling it never sends C2S opcode 13. |

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
- Editor overlay entities are hidden by default and explicitly shown only to their owning player. Interaction is accepted only through that player's current overlay.
- Any online player can trigger a preview by default. Configurable restriction optional.

### Lifecycle

1. Client joins multiplayer server and sends `HELLO`.
2. Server validates protocol version and responds `HELLO_ACK`; the mod unlocks editing only when `ok=true`.
3. Client may upload `SCHEMATIC` and `AUDIO` files. Default schematic is `VILLAGE.schem`.
4. Client sends `CHART_LOAD` (manifest + level JSON of the current chart).
5. Server parses JSON → `RawLevel`, stores it keyed by sender, responds `CHART_LOAD_ACK`.
6. Client sends `PREVIEW_START` with the desired start beat and client wall clock.
7. Server spawns a preview `GameInstance` (visual-only, no song audio, no scoring), aligns timing to the start beat, and sends `PREVIEW_READY` with the server wall clock.
8. Client starts local audio at the requested beat when `PREVIEW_READY` arrives.
9. At any time: `PREVIEW_STOP` fences the previous start generation, pausing and retaining a ready instance or safely cancelling an incomplete startup; `PREVIEW_RESTART` seeks/resumes the retained instance when available.
10. A valid owner right-click on a Track/Note interaction stores selection and sends exactly one S2C `EDITOR_SELECT` (108).

## Chart JSON Semantics

The mod's `ChartProjectIo` must serialize charts with field names and types that the Preview plugin's `File/Deserializers/*` accept:

- `manifest.yml` (YAML) → `SongManifest`.
- Level JSON with `meta`, `tracks[]`, `effects[]`. Track fields: `id`, `beatDivision`, `speedEvents`, `xTransformEvents`/`y`/`z`, `xRotateEvents`/`y`/`z`, `xScaleEvents`/`y`/`z`, `notes[]`. Note fields: `noteType`, `beat`, `pos[x,y,z]`, `scale[x,y,z]`, `rotation[x,y,z]`, `holdGroup`, `holdLengthBeats`.

If parsing semantics change on either side, update both repos and docs in the same task.

## Out of Scope

There is no HTTP, WebSocket, DB, auth/session, resource-pack, collection, unlock, or frontend DTO contract in this repo. Do not reintroduce any of these.
