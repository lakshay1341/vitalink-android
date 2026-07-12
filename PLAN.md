# VitaLink Android — Improvement Plan

Multi-session plan. Bring senior-grade architecture + real features to the app, inspired by
patterns seen in a production RPM app (ANNE View) but written entirely as our own code — nothing
copied. Reference only.

**Workflow:** one step = one commit. After each commit, `/compact` to compress context, then
continue with the next unchecked step. Each step must compile (`./gradlew :app:compileDebugKotlin`)
before commit. Commit as Lakshay, no co-author.

**Server dep:** none of these need the backend running to compile. Manual on-device retest is
optional per step; full end-to-end retest at the end.

---

## Phase A — Foundation

- [x] **A1 — Hilt DI.** Add hilt-android plugin + deps. `@HiltAndroidApp` Application class
  (register in manifest). `@AndroidEntryPoint` on MainActivity. `NetworkModule` @Provides for
  Moshi, OkHttp (with the auth interceptor), Retrofit, `VitaLinkApi`. Delete the hand-rolled
  `Backend` object's singletons once provided (keep `token`/`baseUrl` holder or move to a
  SessionManager). Compile.

- [x] **A2 — Repository layer.** `VitaLinkRepository` interface + `VitaLinkRepositoryImpl`
  (@Inject) wrapping the API calls (encounters, news2, latestVitals, alerts, authenticate) and
  `WaveformStomp`. Returns domain results / `Result<T>`. Provided via Hilt. This is the seam the
  ViewModels depend on. Compile.

## Phase B — MVVM refactor (ViewModel + StateFlow + sealed UiState)

- [x] **B1 — Dashboard.** `DashboardViewModel` (@HiltViewModel) exposing
  `StateFlow<DashboardUiState>` (Loading / Content(list) / Error). Move the encounters+news2+vitals
  fan-out off the composable into the VM. Screen collects with
  `collectAsStateWithLifecycle()`. Compile.

- [ ] **B2 — Monitor.** `MonitorViewModel` owning the waveform stream collection, the 5s vitals/
  news2/alerts poll, and `StateFlow<MonitorUiState>`. Screen becomes render-only. Compile.

- [ ] **B3 — Login.** `LoginViewModel` with form state + `authenticate` call + error state.
  Screen render-only. Compile.

## Phase C — New features (against existing backend APIs)

- [ ] **C1 — Edit alarm thresholds.** New screen listing this encounter's `AlertRule`s and editing
  min/max/severity/enabled via the existing `/api/alert-rules` CRUD. Add API methods + repo +
  ViewModel + screen + nav entry from the monitor. Compile.

- [ ] **C2 — Alarm history.** Screen showing resolved+active alerts for an encounter
  (`/api/alerts?encounterId.equals=`, include RESOLVED). ViewModel + screen + nav. Compile.

- [ ] **C3 — Technical vs physiological alarms.** Split alarm display into technical
  (sensor/device: from AdtEvent or a device-status signal) vs physiological (vital breach), per
  IEC 60601-1-8. Group/tag in the monitor alarms section. Compile. (If backend lacks a technical
  signal, tag by rule vitalType == device-status heuristic and note the ceiling with a ponytail
  comment.)

## Phase D — Offline cache

- [ ] **D1 — Room cache.** Room DB with `EncounterEntity` + `VitalEntity` (+ Daos). Repository
  writes through on fetch, reads cache first so the dashboard/monitor aren't blank offline.
  Provide DB via Hilt. Compile.

## Phase E — Wrap-up

- [ ] **E1 — Update UI tests + previews** for the refactored screens (ViewModels made testable).
  Compile androidTest.
- [ ] **E2 — README + full end-to-end retest** (backend + simulator + tab), then final commit.

---

## Progress log
(append one line per completed step)
- A1 done — Hilt DI: hilt+ksp plugins, @HiltAndroidApp VitaLinkApp, @AndroidEntryPoint MainActivity, SessionManager (single source for baseUrl/token), NetworkModule (Moshi/OkHttp/Retrofit/VitaLinkApi, dynamic base-url via interceptor). Backend kept as delegating shim. kspDebugKotlin + compile pass.
- A2 done — VitaLinkRepository interface (domain) + VitaLinkRepositoryImpl (@Inject, Result<T> via runCatching, waveform Flow) + RepositoryModule (@Binds). Compiles.
- B1 done — DashboardViewModel (@HiltViewModel) with StateFlow<DashboardUiState> (Loading/Content(rows)/Error); fan-out moved to VM (repo.encounters + concurrent news2/vitals). Screen render-only DashboardContent, collectAsStateWithLifecycle, hiltViewModel(). Compiles.
