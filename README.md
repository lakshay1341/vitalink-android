# VitaLink Android

Clinician companion app for the [VitaLink](../vitalink) remote patient monitoring backend.
Jetpack Compose, styled like a bedside patient monitor (dark, instrument-grade). Three screens:

- **Login** — server URL + clinician credentials, JWT auth against `POST /api/authenticate`.
- **Ward dashboard** — active encounters with a colour-coded NEWS2 badge each.
- **Patient monitor** — **live ECG waveform** streamed over STOMP, colour-coded vital tiles
  (HR / SpO₂ / RESP / BP / TEMP), the NEWS2 panel, and active alarms.

The design was generated with Google Stitch (project *VitaLink — Remote Patient Monitor*) and
hand-built in Compose to match.

## How it talks to the backend

| Feature            | Transport | Endpoint                                   |
|--------------------|-----------|--------------------------------------------|
| Login              | REST      | `POST /api/authenticate` → `id_token`      |
| Encounter list     | REST      | `GET /api/encounters`                      |
| NEWS2 score        | REST      | `GET /api/encounters/{id}/news2`           |
| Vital tiles        | REST      | `GET /api/encounters/{id}/vitals/latest`   |
| Alarms             | REST      | `GET /api/alerts?encounterId.equals={id}`  |
| **Live ECG**       | STOMP/WS  | `SUBSCRIBE /topic/waveform/{id}` via `ws://…/websocket/app` |

The JWT is attached to every REST call and to the WebSocket handshake (OkHttp interceptor),
so the plain `/websocket/app` endpoint authenticates the upgrade. STOMP is hand-rolled on
OkHttp — no STOMP library — because a single broadcast subscription only needs CONNECT /
SUBSCRIBE / MESSAGE frames. The ECG trace is drawn on a Compose `Canvas`, no chart dependency.

## Build & run

1. Open this folder in **Android Studio** (Giraffe+). It will sync Gradle and offer to create
   the Gradle wrapper — accept, or run `gradle wrapper` if you have Gradle on your PATH.
   (The binary `gradle-wrapper.jar` is intentionally not committed.)
2. Start the backend (`./mvnw` in the `vitalink` project) and a device streaming data (below).
3. Run the app on an emulator. It defaults the server to `http://10.0.2.2:8080` — the emulator's
   alias for your host machine. On a physical device, enter your host's LAN IP instead.
4. Log in with the JHipster admin (`admin` / `admin`).

> Cleartext HTTP is enabled for local dev only (`usesCleartextTraffic="true"`). Use TLS in production.

## Demo bootstrap

The app shows encounters that have data. To produce some, create a patient + active encounter +
a device bound to it, then run the ECG simulator that ships with the backend. Example against a
fresh backend (admin/admin):

```bash
# 1. get an admin JWT
TOKEN=$(curl -s -X POST localhost:8080/api/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}' | sed 's/.*"id_token":"\([^"]*\)".*/\1/')

# 2. create a patient
curl -s -X POST localhost:8080/api/patients -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"mrn":"4471","firstName":"Eleanor","lastName":"Shaw"}'

# 3. create an ADMITTED encounter for that patient (use the patient id returned above)
curl -s -X POST localhost:8080/api/encounters -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"ADMITTED","wardLabel":"A","bedLabel":"12","patient":{"id":1}}'

# 4. register a device bound to that encounter (use the encounter id returned above)
curl -s -X POST localhost:8080/api/devices -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"serialNumber":"SIM-1","status":"ACTIVE","encounter":{"id":1}}'

# 5. stream synthetic ECG + vitals through the pipeline (device id from step 4)
cd ../vitalink
java tools/EcgSimulator.java --url http://localhost:8080 --admin admin:admin --device 1
```

Open the app, log in, tap the patient — the ECG trace animates, the tiles fill, NEWS2 updates,
and around 60 s in the simulator's scripted SpO₂ desaturation raises an alarm.

## Shortcuts taken (deliberate)

- No DI, no ViewModels, no persistence — a three-screen demo doesn't need them; state lives in
  `remember` and a single `Backend` object. The JWT is kept in memory (re-login on relaunch).
- Retrofit is built once and captures the server URL at first use; change it before the first
  request or restart the app.
- Inter font is declared in the design system but the app uses the platform default; drop the
  font files in `res/font` and wire a `Typography` to match pixel-for-pixel.
