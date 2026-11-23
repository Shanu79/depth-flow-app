# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

DepthFlow is a single-module Android app (`:app`, package `com.shin.depthflow`, "Depthflow AI") built with Kotlin and Jetpack Compose. Users upload an image, and a remote Python/FastAPI backend (`https://api.depthflow.io/`) renders a 3D parallax ("depth flow") MP4. The app also covers auth (email+OTP and Google), a credit balance, Google Play subscriptions, and a local history of generated videos.

Related docs in the repo root:
- `ANDROID_INTEGRATION.md` is the backend contract. It covers the API, subscriptions, the **Happy Hour** feature and FCM/inbox notifications. If it disagrees with the running server (`<BASE_URL>/docs`), the server wins.
- No README, Cursor rules or Copilot rules exist.

## Commands

Run from the repo root (`gradlew.bat` on Windows, or `./gradlew` in Git Bash):

- Debug APK: `./gradlew assembleDebug`. Install with `./gradlew installDebug`.
- Release: `./gradlew assembleRelease` (R8 and resource shrinking on). It is currently signed with the **debug** signing config. A built AAB sits in `app/release/` (untracked).
- Unit tests: `./gradlew testDebugUnitTest`
- Single test: `./gradlew testDebugUnitTest --tests "com.shin.depthflow.ExampleUnitTest"`
- Instrumented tests (needs a device): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lint`

Tests are only the default templates. Robolectric, Roborazzi and Compose UI-test dependencies are declared but unused.

## Required local setup (not in git)

- JDK: `JAVA_HOME` is not set on this machine. Use Android Studio's JBR, e.g. `export JAVA_HOME="/c/Program Files/Android/Android Studio1/jbr"` (the `Android Studio` folder's JBR is broken).

- `app/google-services.json` is needed for the Firebase plugin (Analytics, Crashlytics). It is untracked.
- Optional `API_BASE_URL=http://10.0.2.2:8000/` in `local.properties` points **debug** builds at a local backend. Release always uses prod. The value ends up in `BuildConfig.BASE_URL`. A debug-only `network_security_config` (in `app/src/debug/`) allows cleartext HTTP.
- `local.properties` must contain `GOOGLE_WEB_CLIENT_ID=...`. `app/build.gradle.kts` strips any quotes and exposes it as `BuildConfig.GOOGLE_WEB_CLIENT_ID`. If it is blank, Google Sign-In shows a "Config Error" toast.

## Build configuration

- Version catalog: `gradle/libs.versions.toml`. AGP 9.2.1, Kotlin 2.2.10, KSP, Compose BOM 2025.12.00. minSdk 30, compile/target 36, versionCode 10 / versionName 1.3, Java 11.
- The catalog has leftovers: placeholder `"<version>"` entries (haze-blur, haze-materials, hazeVersion) and duplicate aliases for the same library (navigation-compose, material-icons-extended, coil). Check which alias `app/build.gradle.kts` uses before touching one. Use the BoM-managed aliases for Compose and Firebase.
- Moshi codegen (KSP) is configured, but the repository builds Moshi with `KotlinJsonAdapterFactory` (reflection). The models are plain Kotlin data classes with snake_case field names that match the JSON directly. There is no `@Json`.
- `proguard-rules.pro` keeps `androidx.credentials`, `googleid`, Retrofit and Kotlin metadata. Check that new DTOs survive R8, since reflection-based Moshi needs the Kotlin metadata and class names kept.
- Manifest: only `INTERNET` and `POST_NOTIFICATIONS`. There is a single `MainActivity` and a `FileProvider` (`${applicationId}.fileprovider`, paths in `res/xml/provider_paths.xml`). The only service is `push/DepthFlowMessagingService`. `MainActivity` is `launchMode="singleTop"`.

## Architecture

Single-activity MVVM with no DI framework. Objects are constructed by hand. Files are under `app/src/main/java/com/shin/depthflow/`.

### Entry and navigation
- `MainActivity` owns the one activity-scoped `DepthFlowViewModel` and a `BillingManager`, and calls `NotificationHelper.createNotificationChannel`. The start route is hardcoded to `Routes.HOME`. The Pricing UI passes `(planId, isYearly)`. `MainActivity` maps it to a Play product ID:
  - `Pro Plan` → `sub_pro_mo` / `sub_pro_yr`
  - `Basic Plan` → `sub_basic_mo` / `sub_basic_yr`
- `MainActivity.onResume` refetches Happy Hour status and the unread count (no-ops while logged out).
- `DepthFlowApp.kt` (about 600 lines) holds the Scaffold, the Haze-blurred floating bottom bar, the Firebase screen-view listener and the whole `NavHost`. Route strings are in `ui/navigation/Routes.kt` (login, home, workspace, pricing, profile, contact_us, billing, faq, my_projects, edit_profile, register, verify_otp, reset_password, explore_library, happy_hour, notifications).
- Adding a route means updating the hardcoded lists in `DepthFlowApp.kt`: `routesWithoutTopBar`, and the `isTargetSectionCurrent` block that decides tab "section root" behaviour. `ui/navigation/NavigationComponents.kt` has a parallel section check. Keep the two in sync.
- `DepthFlowApp` also wires the two ViewModels together: it pushes `viewModel.authToken` into `HappyHourViewModel.onAuthChanged`, refetches Happy Hour when `viewModel.purchaseVerified` fires, shows `HappyHourEvent.Message` toasts, and calls `viewModel.logout()` on `HappyHourEvent.Unauthorized` (any 401 from Happy Hour/inbox calls).
- Workspace and Profile are protected. Both the bottom-nav `onNavigate` and the composables redirect when `isLoggedIn` is false. Login success calls `viewModel.setAuthToken(token)` and then navigates to Workspace.
- Google Sign-In (Credential Manager with a nonce) is implemented only in the `LOGIN` composable. The `REGISTER` route's `onGoogleSignInClick` is an empty stub.
- The top bar (`DepthFlowTopBar`) shows the logo, a credits pill plus a notification bell with an unread badge when logged in (otherwise "Sign In"), and a website icon. Bottom nav items are Home, Workspace, a center Plus button (`PlusButtonModalSheet`), Pricing and Profile.

### UI layout
- Features are in `feature/{home,workspace,pricing,profile,projects,billing,faq,contact,happyhour,notifications}/`.
- Shared UI is in `ui/` (auth screens, `PlusButtonModalSheet`), `ui/components` (`GlassmorphicButton`, `GradientOutlineButton`, auth components) and `ui/theme`.
- Theme is dark only, with purple/blue "glass" styling. Colors are in `ui/theme/Color.kt` (`DepthFlowPurple`, `Surface`, `SurfaceDark`, `TextMuted`, `WarningRed`, `sectionContainerOuter` and others). Reuse these rather than adding new ones.
- `Workspace.kt` (about 1100 lines) holds `WorkspaceScreen`, the upload/result area, an ExoPlayer `UriVideoPlayer`, the settings tabs, `saveVideoToGallery` and `shareVideo`. `RecentCreationsRow.kt` is empty.
- Most UI uses hardcoded English strings. Only a few messages are in `res/values/strings.xml`.

### State: `viewmodel/DepthFlowViewModel.kt` (~800 lines)
It is an `AndroidViewModel` holding almost all app state as private `MutableStateFlow` with public read-only flows. It covers:
- **Auth:** `isLoggedIn`, `loginToken`, `authError`, `isAuthLoading`, `registrationEmail`.
- **Profile and credits:** credits, email, name, plan, subscription status, join/renewal/credit-drop dates, `isProfileLoading`.
- **Billing:** `billingInfo`, `paymentHistory`, `generationLogs`.
- **Pipeline:** `uiState`, `selectedImageUri`.
- **Workspace settings:** motion path, depth intensity, speed, length, SSAA, FPS, focus, cinematic intensity, bokeh quality, reverse/smooth/loop.
- **Connectivity:** `isNetworkAvailable`.

Key behaviours:
- The JWT is held in memory as `"Bearer <token>"` and persisted in DataStore. On init the VM collects `sessionManager.authTokenFlow`, which sets `isLoggedIn` and calls `fetchUserProfile()`. An empty token resets everything.
- `fetchUserProfile()` also triggers `fetchGenerationLogs()` and `fetchBillingInfo()`. A 401 or 403 there calls `logout()`. This is the only global 401 handling. Other calls just log errors.
- Network regain re-fetches the profile.
- `generate3DMotion(skipCreditCheck)` does a **client-side credit pre-check**: cost is 20, or 25 above 10 s, or 30 above 20 s. It sets `PipelineState.Error` if credits are lower, and `Workspace.kt` also shows `InsufficientCreditsDialog` before calling it. Both are skipped while Happy Hour is active.
- `authToken` (raw JWT) and `purchaseVerified` are exposed for `HappyHourViewModel`/`DepthFlowApp`.
- Analytics events go through `logEvent` (Firebase Analytics).

### Happy Hour and notification inbox
Backend contract: `ANDROID_INTEGRATION.md`. The **server is the only authority** on entitlement, tokens and expiry. The app mirrors state and never decides it.
- `viewmodel/HappyHourViewModel.kt` (separate from `DepthFlowViewModel`, activity-scoped) holds `status` (from `/happy-hour/status`), `secondsLeft`, `isUnlimited`, `isActivating`, `history`, the notification `inbox` and `unreadCount`, and a one-shot `events` flow.
  - The countdown is display-only: `HappyHourTime.skew(server_time)` is applied to the device clock, and a 1 s ticker runs only while a session is active. At 0 it refetches `/status`. Nothing is persisted.
  - `activate()` sends an `Idempotency-Key` UUID. The key is reused only when retrying after a network error, and dropped once the server gives a definitive answer.
  - It is refetched on login (`onAuthChanged`), `MainActivity.onResume`, after purchase verification, after a successful generation (Workspace), and when the Happy Hour screen opens. It is cleared on logout. There is no polling.
- `util/HappyHourTime.kt` holds the pure time helpers (unit-tested).
- UI: `HappyHourBanner` (Home and Workspace, shown only when a session is live or can be started), `HappyHourScreen` (route `happy_hour`: states, a start-confirm dialog, rules, history) and `NotificationsScreen` (route `notifications`).
- Generation while Happy Hour is active: `Workspace.kt` and `generate3DMotion(skipCreditCheck = true)` skip the **client** credit pre-check, so a 0-credit subscriber can still generate. The Generate button shows "Unlimited". If the user is not actually entitled, the server answers `402`, and `processImage` surfaces its `detail` text.
- `PipelineState.Success` carries `happyHourActive`, `happyHourRemainingVideos` and `cost` from the `X-Happy-Hour`, `X-Happy-Hour-Remaining-Videos` and `X-Cost` response headers.
- **Push (FCM)** is in `push/`:
  - `DepthFlowMessagingService` handles `onNewToken` (registers when a session exists) and `onMessageReceived` (foreground only: shows a notification on channel `happy_hour` and emits `PushEvents`, which `DepthFlowApp` turns into a status and unread refetch). In the background the system shows the backend's `notification` part itself.
  - `PushRegistrar` calls `POST /notifications/devices` on every login and app start (via `HappyHourViewModel.onAuthChanged`). `DepthFlowViewModel.logout()` calls `POST /notifications/devices/unregister` with the JWT being discarded and then `deleteToken()`. Both are best effort and never block login or logout.
  - Tap handling: `MainActivity` is `singleTop` and reads `route` and `notification_id` from the intent extras into a `PushTarget`. `DepthFlowApp` waits up to 5 s for the restored login state, opens `Routes.HAPPY_HOUR` and marks the inbox item read.
  - `POST_NOTIFICATIONS` is requested in Workspace and on the Happy Hour screen. Without it pushes are silent, and the inbox still works.
  - Channel IDs: `happy_hour` (pushes) and `download_channel` (download-complete notice). Both are created in `NotificationHelper.createNotificationChannel`.
  - The backend sends no pushes until it has the Firebase project ID and service-account JSON. Until then (and with `FCM_DRY_RUN=true`) only the inbox works.

### Data layer (`data/`)
- `DepthFlowApi.kt` has `DepthFlowApiService` (Retrofit) plus all request/response DTOs in the same file. There is no auth interceptor. Every authenticated call takes `@Header("Authorization") token` explicitly. Authenticated endpoints in use are `auth/me`, `auth/update-profile`, `auth/change-password`, `payments/{history,cancel-subscription,resubscribe,verify-purchase}`, `ai/depthflow/{logs,billing,generate-3d}`, `happy-hour/{status,activate,history}` and `notifications/*` (list, unread-count, read, read-all). Happy Hour and notification DTOs are in `HappyHourApi.kt`, along with `ApiException(httpCode, code, detail)` and `HttpException.toApiException()`, which parse the server's `{"detail","code"}` error body.
- `ApiClient` is a process-wide singleton that lazily builds Retrofit and OkHttp (60 s connect, 300 s read). The base URL is `BuildConfig.BASE_URL`. Body logging is enabled in **debug builds only**. If the lazy init throws, `ApiClient.service` is null and the repository wrappers throw "API Service not initialized". Older wrappers mostly `throw Exception(errorBody)`, so raw JSON bodies reach the UI in places. The Happy Hour and inbox wrappers go through `call { }`, which converts failures into `ApiException`.
- `processImage(...)`: a `Flow<PipelineState>` (`Idle`, `Uploading`, `Processing`, `Success`, `Error`).
  1. Copies the picked URI to a temp file.
  2. Posts multipart `file` plus a JSON `payload` (`render`, `motion`, `effects`, `plan`) plus `request_source = "android_app"`.
  3. The response body is the MP4, saved to `cacheDir/generated_3d_*.mp4`.
  4. It reads `X-Remaining-Credits`, `X-Happy-Hour`, `X-Happy-Hour-Remaining-Videos` and `X-Cost` from the headers.
  5. It inserts a `ProjectEntity` into Room, then emits `Success`.
  6. Any non-2xx becomes `Error("Generation failed: <server detail or raw body>")`.
- Persistence is split. Room (`AppDatabase` `depthflow_database`, v1, `exportSchema=false`, no migrations) holds `ProjectEntity`, the history of generated videos. DataStore (`SessionManager`, "user_session") holds the JWT and the local profile-pic URI. Changing `ProjectEntity` needs a version bump plus a migration.

### Billing and notifications
- `util/BillingManager`: wraps the Play `BillingClient`, connects at construction, and queries subscription details. It takes the first offer token and launches the flow. On `PURCHASED` and not acknowledged, it acknowledges, then calls back into `viewModel.verifyPurchaseOnBackend(purchaseToken, productId)`. There is no handling of pending or already-acknowledged purchases and no restore on start.
- `verifyPurchaseOnBackend` calls `payments/verify-purchase`. It then refreshes the profile and billing and emits `purchaseVerified`, because Happy Hour tokens are granted inside that call.
- `util/NotificationHelper`: one channel (`download_channel`) and a "download complete" notification. There is **no FCM** (no `firebase-messaging` dependency or service). `WorkspaceScreen` requests `POST_NOTIFICATIONS` on entry.
- `util/ResultExtractManager.kt` has `saveBitmapToGallery` (MediaStore, `Pictures/DepthFlow`) and a bitmap share via FileProvider. Video save/share is in `Workspace.kt`.
- `util/Logger` logs only in debug builds.

## Gotchas

- A **401 outside of `fetchUserProfile` and the Happy Hour/inbox calls** does not log the user out. Most other VM calls swallow errors into logs.
- The `request_source` value sent is `android_app`. `ANDROID_INTEGRATION.md` says to send `workspace` from the app UI. Confirm with the backend owner before changing it.
- `LoginResponse` has only `access_token` and `token_type`.
- Several duplicate imports and unused code paths exist (for example `loadDummyProfileDates`). Don't assume everything referenced is live.
