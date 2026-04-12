# Cardify — Smart Credit Card Transaction Analysis

An Android application that helps users track, categorize, and analyze their credit card transactions. Users upload bank statement files, view spending statistics, flag irregular charges, and share transactions with friends for discussion in a built-in chat.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Backend Setup (Flask)](#backend-setup-flask)
- [Android Setup](#android-setup)
- [Connecting Android to the Backend via ADB](#connecting-android-to-the-backend-via-adb)
- [API Overview](#api-overview)
- [Error Handling](#error-handling)
- [Security](#security)

---

## Features

- **Authentication** — Register and log in with username/email/password. JWT tokens are stored in encrypted `SharedPreferences` and sent automatically on every request.
- **Transaction Dashboard** — View recent transactions on the home screen with category icons, amounts, and status badges. Share or toggle the status of any transaction inline.
- **Full Transaction List** — A separate screen with real-time business-name search, status filter chips (All / Regular / Irregular), and infinite-scroll pagination (20 items per page, cursor-based).
- **File Upload** — Upload a `.xlsx` bank statement directly from the home screen to import transactions in bulk. The transaction list refreshes automatically on success.
- **Spending Statistics** — Monthly spending breakdown with an interactive donut chart by category, total spend, and regular vs. irregular transaction counts. Navigate across up to 6 months with arrow controls.
- **Irregular Transaction Detection** — Mark individual transactions as Regular or Irregular with an optimistic UI update that reverts automatically if the server call fails.
- **Friends System** — Add friends by phone number (with server-side user lookup), accept/decline incoming requests, and remove connections with optional cascade-deletion of associated shares.
- **Transaction Sharing** — Share any transaction with an approved friend. Shared transactions appear in a dedicated "Shared Info" screen with sent/received direction filters.
- **In-App Chat** — Each shared transaction has a private chat thread that polls for new messages every 5 seconds. Messages are auto-scrolled on arrival and display timestamps in HH:mm format.
- **Edit Account** — Update username, email, phone number, and password with field-level client-side validation before any network call is made.
- **Admin Dashboard** — Admin-role users see a system-wide dashboard (user counts, transaction totals, fraud rate, top suspicious businesses) instead of the regular stats screen. Non-admins receive HTTP 403 if they attempt to access it directly.

---

## Tech Stack

### Android (Client)

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation Compose |
| Architecture | MVVM (ViewModel + StateFlow) |
| Networking | Retrofit 2 + OkHttp 3 |
| JSON | Gson |
| Async | Kotlin Coroutines |
| Secure Storage | `EncryptedSharedPreferences` (AES-256-GCM) |
| Charts | Vico + `ir.mahozad.android:pie-chart` |
| Image Loading | Coil |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 |
| Compile SDK | 36 |

### Backend (Server)

| Layer | Technology |
|---|---|
| Language | Python |
| Framework | Flask |
| Database | Firebase |
| Auth | JWT (JSON Web Tokens) |
| Default Port | `5001` |
| API Prefix | `/auth/` (auth, users, friends, stats) and `/api/` (transactions, shares, chat, uploads) |

---

## Architecture

The app follows **MVVM** with a clean separation between layers:

```
UI Layer       →  Compose screens observe StateFlow from ViewModels
ViewModel      →  Calls repository / RetrofitClient, holds UI state
Repository     →  Wraps API calls (AuthRepository, StatsRepository)
API Layer      →  RetrofitClient (singleton OkHttp + Retrofit) + AuthApiService (interface)
Data Layer     →  Models, UserSession (in-memory), PreferencesManager (encrypted disk)
```

All network errors are converted to user-friendly strings by a single `Exception.toUserMessage()` extension function in `RetrofitClient.kt`. Every ViewModel exposes an `errorMessage: StateFlow<String?>` and a `clearError()` function; every screen collects it and shows a `Toast`.

Key patterns used throughout the codebase:

| Pattern | Where used |
|---|---|
| Optimistic UI updates | Transaction status toggle — updates list immediately, reverts on server failure |
| Cursor-based pagination | `TransactionsViewModel` — tracks `lastDocId` and `isLastPage`; loads 20 items per page |
| Long-polling | `ChatViewModel` — re-fetches messages every 5 seconds via a `while(true)` + `delay(5000)` loop in `viewModelScope` |
| One-shot error flow | All ViewModels — `errorMessage` StateFlow set then cleared after display |
| Role-based routing | `AppNavigation` — `"stats"` route renders `AdminDashboardScreen` for admins, `StatsScreen` for regular users |

---

## Project Structure

```
Smart-Credit-Card-Transaction-Analysis/
│
├── README.md
├── build.gradle.kts                        # Root Gradle build file
├── settings.gradle
│
└── app/
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/cardify/app/
            │
            ├── MainActivity.kt                    # Entry point
            ├── NetworkConfig.kt                   # Runtime base-URL configuration
            │
            ├── data/
            │   ├── UserSession.kt                 # In-memory singleton: JWT + user info
            │   ├── api/
            │   │   ├── AuthApiService.kt           # Retrofit interface — all HTTP endpoints
            │   │   └── RetrofitClient.kt           # OkHttp client, auth interceptor, toUserMessage()
            │   ├── model/
            │   │   ├── Models.kt                  # Request/response data classes
            │   │   └── Transaction.kt             # Transaction domain model
            │   └── repository/
            │       ├── AuthRepository.kt          # Auth and profile API calls
            │       └── StatsRepository.kt         # Stats API calls
            │
            ├── utils/
            │   └── PreferencesManager.kt          # Encrypted SharedPreferences
            │
            └── ui/
                ├── theme/
                │   └── CardifyTheme.kt            # Material 3 theme + colour tokens
                ├── navigation/
                │   └── Navigation.kt             # NavHost — all Compose routes
                ├── components/
                │   ├── AppScaffold.kt            # Shared bottom navigation bar
                │   └── TransactionRow.kt         # Reusable transaction list item
                │
                ├── login/
                │   ├── LoginActivity.kt
                │   ├── LoginViewModel.kt
                │   ├── RegisterActivity.kt
                │   └── RegisterViewModel.kt
                │
                ├── home/
                │   ├── HomeActivity.kt           # Host Activity for Compose nav graph
                │   ├── HomeScreen.kt             # Dashboard + file upload
                │   └── HomeViewModel.kt
                │
                ├── transactions/
                │   ├── TransactionsScreen.kt     # Full list with search, filter, pagination
                │   └── TransactionsViewModel.kt  # Cursor-based pagination, filter + search state
                │
                ├── stats/
                │   ├── Statsscreen.kt            # Monthly stats + donut chart
                │   └── StatsViewModel.kt
                │
                ├── shared_info/
                │   ├── SharedInfoScreen.kt       # Sent / received shares
                │   ├── SharedInfoViewModel.kt
                │   ├── ShareWithFriendsScreen.kt # Friend picker for sharing
                │   └── ShareWithFriendsViewModel.kt
                │
                ├── chat/
                │   ├── ChatScreen.kt             # Per-share chat with 5s polling + auto-scroll
                │   └── ChatViewModel.kt          # Message polling loop, send + refetch
                │
                ├── account/
                │   ├── AccountScreen.kt          # Profile + friends management
                │   ├── AccountViewModel.kt
                │   ├── Addfriendsheet.kt         # Bottom sheet: add friend
                │   ├── Friendrequestsheet.kt     # Bottom sheet: incoming request
                │   └── Friendsheet.kt            # Bottom sheet: friend options
                │
                ├── edit_account/
                │   ├── EditAccountScreen.kt
                │   └── EditAccountViewModel.kt
                │
                ├── admin/
                │   ├── AdminDashboardScreen.kt   # Admin-only system overview
                │   └── AdminDashboardViewModel.kt
                │
                └── activity/
                    ├── ActivityActivity.kt       # Legacy Activity host (not in main nav graph)
                    └── ActivityScreen.kt         # Legacy activity feed using mock data
```

---

## Prerequisites

### Backend
- Python 3.9+
- `pip` with Flask and other dependencies from `requirements.txt`
- Firebase project credentials configured in the backend

### Android
- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Android SDK with API 34 and Build Tools 36.x installed
- A physical Android device **or** an AVD running API 24+
- `adb` available on your `PATH` (bundled with Android Studio's platform-tools)

---

## Backend Setup (Flask)

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Create and activate a virtual environment:
   ```bash
   # macOS / Linux
   python3 -m venv venv
   source venv/bin/activate

   # Windows (PowerShell)
   python -m venv venv
   .\venv\Scripts\Activate.ps1
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure your Firebase credentials as required by the backend (e.g. set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable or place the service account JSON file in the expected location).

5. Start the server on port `5001`:
   ```bash
   python app.py
   ```
   Or with the Flask CLI:
   ```bash
   flask run --host=0.0.0.0 --port=5001
   ```

6. Verify the server is running:
   ```bash
   curl http://127.0.0.1:5001/auth/
   ```

---

## Android Setup

1. Open the project in Android Studio:
   ```
   File → Open → Smart-Credit-Card-Transaction-Analysis
   ```

2. Let Gradle sync finish — all dependencies are downloaded automatically.

3. The backend URL is configured in `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://127.0.0.1:5001/auth/"
   ```
   When connecting via ADB reverse (see next section), `127.0.0.1` on the device tunnels back to your development machine, so no URL change is needed for local development.

4. Build and run the app:
   - Select your device or AVD from the toolbar
   - Press **Shift+F10** or click **Run → Run 'app'**

---

## Connecting Android to the Backend via ADB

When Flask runs on your development machine and the Android app runs on a **physical device**, the device cannot reach `127.0.0.1` on your machine directly. `adb reverse` solves this by forwarding a port on the device back to your machine.

### One-time setup per connection

1. Connect your device via USB and enable **USB Debugging** (Settings → Developer Options).

2. Verify the device is recognised:
   ```bash
   adb devices
   ```
   Your device should appear as `device` (not `unauthorized`).

3. Forward port `5001` from the device to your machine:
   ```bash
   adb reverse tcp:5001 tcp:5001
   ```
   After this, any request from the Android app to `http://127.0.0.1:5001` is transparently routed to `localhost:5001` on your computer.

4. Start the Flask server if it is not already running:
   ```bash
   python app.py
   ```

5. Run the Android app. All API calls will reach the Flask backend.

### Tips

- Re-run `adb reverse tcp:5001 tcp:5001` each time you reconnect the device or restart the `adb` server (`adb kill-server && adb start-server`).
- For wireless debugging (`adb connect <device-ip>`), `adb reverse` works identically.
- For an **emulator**, you can also use `10.0.2.2` as the host instead of `127.0.0.1` to reach the development machine, but the ADB reverse approach works for both without changing the URL.

---

## API Overview

All authenticated requests include an `Authorization: Bearer <token>` header injected automatically by the OkHttp auth interceptor. Unauthenticated requests to protected endpoints return HTTP `401`.

### Authentication & Profile (`/auth/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Log in and receive a JWT token |
| POST | `/auth/register` | Create a new account |
| GET | `/auth/user_details` | Get the current user's profile |
| POST | `/auth/update_account` | Update username, email, phone, or password |
| POST | `/auth/update_location` | Save the user's current GPS coordinates |

### Friends (`/auth/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/friends` | List all friends and pending requests |
| POST | `/auth/add-friend` | Send a friend request by phone number |
| POST | `/auth/confirm-friend` | Accept an incoming friend request |
| POST | `/auth/delete-friend` | Remove a friend |
| POST | `/auth/delete-friend-smart` | Remove a friend; body accepts `delete_sent` and `delete_received` booleans to cascade-delete associated shares |
| GET | `/auth/search_user/{phone}` | Look up a user by phone number |

### Statistics & Admin (`/auth/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/stats/{month}` | Monthly spending stats (e.g. `Jan`, `Feb`) |
| GET | `/auth/admin/dashboard` | Admin-only system dashboard (HTTP 403 for regular users) |

### Transactions, Shares & Chat (`/api/`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions?limit=N&last_doc_id=X` | Paginated transaction list; `last_doc_id` is `null` for first page |
| PUT | `/api/transactions/{id}` | Update a transaction's status (Regular / Irregular) |
| GET | `/api/shares` | List all sent and received shares |
| POST | `/api/shares` | Share a transaction with a friend |
| GET | `/api/messages/{shareId}` | Get messages for a share conversation |
| POST | `/api/messages` | Send a message in a share conversation |
| POST | `/api/upload` | Upload a `.xlsx` transaction file for bulk import |

---

## Error Handling

The app never fails silently. Every network call is wrapped in a `try/catch`, and every error is surfaced to the user as a `Toast` message.

| Condition | User-facing message |
|-----------|---------------------|
| No internet | "No internet connection. Please check your network settings." |
| Request timeout | "Connection timed out. Please try again." |
| Server unreachable | "No internet connection. Please check your connection and try again." |
| SSL error | "Secure connection failed. Please try again." |
| HTTP 401 | "Session expired. Please log in again." |
| HTTP 403 | "Access denied." |
| HTTP 404 | "Not found." |
| HTTP 409 | "An account with this email or phone number already exists." |
| HTTP 429 | "Too many attempts. Please wait and try again." |
| HTTP 500–503 | "Server error. Please try again later." |
| Invalid input | Field-specific validation message shown before any network request |

---

## Security

- **Encrypted storage** — JWT tokens and user data are stored in `EncryptedSharedPreferences` backed by an AES-256-GCM key in the Android Keystore.
- **Token injection** — The `Authorization: Bearer` header is added by an OkHttp interceptor that reads the live in-memory token at request time.
- **Cleartext traffic** — `android:usesCleartextTraffic="true"` is enabled in the manifest for local development over HTTP. Before deploying to production, remove this flag and update `BASE_URL` in `RetrofitClient.kt` to use `https://`.
- **Password hygiene** — Passwords are never logged, never stored on the device, and the Edit Account screen never pre-fills the password field.
- **Input validation** — All user input is validated client-side before hitting the network, with a specific error message per field.
