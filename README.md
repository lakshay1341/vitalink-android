# VitaLink Android

Clinician companion app for the [VitaLink](https://github.com/lakshay1341/vitalink) remote
patient-monitoring backend. Jetpack Compose, styled like a bedside patient monitor (dark,
instrument-grade). Five screens:

- **Login** — server URL + clinician credentials, JWT auth; inline error state.
- **Ward dashboard** — active encounters with a colour-coded NEWS2 badge, live mini-vitals, an
  empty state, and offline fallback from the local cache.
- **Patient monitor** — **live ECG waveform** over STOMP, colour-coded vital tiles
  (HR / SpO₂ / RESP / BP / TEMP) flagged by their NEWS2 sub-score, the NEWS2 panel with
  per-parameter breakdown, and alarms split into **physiological** vs **technical**
  (IEC 60601-1-8).
- **Edit alarm thresholds** — per-rule min / max / severity / enabled, saved via the backend's
  `AlertRule` API.
- **Alarm history** — active and resolved alerts for the encounter.

The design was generated with Google Stitch (project *VitaLink — Remote Patient Monitor*) and
hand-built in Compose to match.

## Architecture

- **Jetpack Compose**, single activity, Navigation-Compose.
- **MVVM** — every screen has a `@HiltViewModel` exposing a `StateFlow<…UiState>` (sealed
  `Loading / Content / Error` where it fits). Screens are split into a thin injected wrapper and a
  render-only `…Content` composable, so previews and instrumented tests render them with fake
  state and **no Hilt**.
- **Hilt DI** — `NetworkModule` (Moshi / OkHttp / Retrofit / API, with a dynamic base URL:
  Retrofit uses a placeholder and an interceptor rewrites each request's scheme/host/port from
  `SessionManager`), `RepositoryModule`, `DatabaseModule`.
- **Repository layer** (`VitaLinkRepository`) — the single seam between ViewModels and
  data. Suspend calls return `Result<T>`; the live waveform is a cold `Flow`.
- **Room offline cache** — encounters and latest vitals write through on every successful fetch
  and are returned on network failure, so the dashboard and monitor aren't blank offline.
- **Live ECG** — STOMP hand-rolled on OkHttp (no STOMP library; a single broadcast subscription
  only needs CONNECT / SUBSCRIBE / MESSAGE frames), drawn on a Compose `Canvas` (no chart library).

## How it talks to the backend

| Feature              | Transport | Endpoint                                                   |
|----------------------|-----------|-----------------------------------------------------------|
| Login                | REST      | `POST /api/authenticate` → `id_token`                     |
| Encounter list       | REST      | `GET /api/encounters`                                     |
| NEWS2 score          | REST      | `GET /api/encounters/{id}/news2`                          |
| Vital tiles          | REST      | `GET /api/encounters/{id}/vitals/latest`                  |
| Alarms / history     | REST      | `GET /api/alerts?encounterId.equals={id}`                 |
| Alarm thresholds     | REST      | `GET /api/alert-rules` · `PUT /api/alert-rules/{id}`      |
| **Live ECG**         | STOMP/WS  | `SUBSCRIBE /topic/waveform/{id}` via `ws://…/websocket/app` |

The JWT is attached to every REST call and to the WebSocket handshake (OkHttp interceptor), so the
plain `/websocket/app` endpoint authenticates the upgrade.

## Build & run

1. Open this folder in **Android Studio**. It syncs Gradle and offers to create the Gradle wrapper
   — accept (the binary `gradle-wrapper.jar` is intentionally not committed). Hilt + Room codegen
   runs via KSP on first build. JDK 17.
2. Start the backend (`./mvnw` in the `vitalink` project) and a device streaming data (below).
3. Run on an emulator — it defaults the server to `http://10.0.2.2:8080` (the emulator's alias for
   your host). On a physical device on the same LAN, enter your host's IP, or tunnel with
   `adb reverse tcp:8080 tcp:8080` and use `http://127.0.0.1:8080`.
4. Log in with the JHipster admin (`admin` / `admin`).

> Cleartext HTTP is enabled for local dev only (`usesCleartextTraffic="true"`). Use TLS in production.

## Demo bootstrap

Create a patient + admitted encounter + a bound device + an SpO₂ alarm rule, then run the ECG
simulator that ships with the backend. Against a fresh backend (admin/admin):

```bash
# 1. admin JWT
TOKEN=$(curl -s -X POST localhost:8080/api/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | sed 's/.*"id_token":"\([^"]*\)".*/\1/')

# 2. patient  (note the returned id)
curl -s -X POST localhost:8080/api/patients -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"mrn":"4471","firstName":"Eleanor","lastName":"Shaw"}'

# 3. ADMITTED encounter for that patient  (patient id from step 2; note the encounter id)
curl -s -X POST localhost:8080/api/encounters -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"ADMITTED","wardLabel":"A","bedLabel":"12","patient":{"id":1}}'

# 4. device bound to the encounter  (status ASSIGNED; note the device id)
curl -s -X POST localhost:8080/api/devices -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"serialNumber":"SIM-1","status":"ASSIGNED","encounter":{"id":1}}'

# 5. SpO2 alarm rule so the scripted desat fires an alert
curl -s -X POST localhost:8080/api/alert-rules -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"vitalType":"SPO2","minValue":92,"severity":"HIGH","scope":"ENCOUNTER_OVERRIDE","debounceCount":1,"latching":false,"enabled":true,"encounter":{"id":1}}'

# 6. stream synthetic ECG + vitals through the pipeline  (device id from step 4)
cd ../vitalink
java tools/EcgSimulator.java --url http://localhost:8080 --admin admin:admin --device 1
```

Open the app, log in, tap the patient — the ECG trace animates, the tiles fill, NEWS2 updates, and
around 60 s in the simulator's scripted SpO₂ desaturation raises an alarm. Stop the simulator and a
**technical** "ECG signal loss" alarm appears within a few seconds.

## Deliberate shortcuts

- `WaveformStomp` still uses the legacy static `Backend` shim for its OkHttp client; the REST path
  is fully DI'd. Inject the client into `WaveformStomp` to delete `Backend` entirely.
- Technical alarms are synthesized from one signal (no waveform for 8 s = signal loss). Wire real
  battery / sensor-off alarms when the device reports status (e.g. the MQTT last-will path).
- Inter is declared in the design system but the app uses the platform default font; drop the font
  files in `res/font` and wire a `Typography` to match pixel-for-pixel.
