# VoiceBridge

**VoiceBridge** is an offline, serverless, peer-to-peer mesh chat app for Android. It lets nearby devices discover each other and exchange end-to-end encrypted messages **without internet access, cellular data, or any central server** — using a self-healing mesh network built on Bluetooth and Wi-Fi.

Built for situations where connectivity is unreliable or unavailable: disaster response, remote areas, festivals/events, campuses, or anywhere people need to communicate offline.

---

## ✨ Features

- 🔌 **No internet, no server** — fully peer-to-peer, works completely offline
- 🕸️ **Self-forming mesh network** — devices auto-discover and auto-connect to build the mesh
- 🔒 **End-to-end encryption** for private 1:1 chats (ECDH + AES-256-GCM)
- 🗄️ **Encrypted local storage** — message history secured via Android Keystore
- 📡 **Multi-hop message relay** — messages hop across intermediate devices to extend range (up to 5 hops)
- 📬 **Store-and-forward** — messages queue automatically when a recipient is offline and retry when they reconnect
- 📢 **Broadcast channel** ("Everyone") for open, unencrypted announcements to the whole mesh
- 👥 **Friend system** with QR-code based friend adding
- 💬 Rich chat features: text, images, voice notes, file attachments, replies, reactions, typing indicators, read receipts

---

## 🧠 How It Works

### Transport layer
VoiceBridge uses Google's **Nearby Connections API** (`P2P_CLUSTER` strategy), which transparently uses a mix of **Bluetooth, BLE, and Wi-Fi Direct** to discover and connect nearby devices — whichever combination gives the best range/throughput at each stage.

### Mesh formation
Every device simultaneously **advertises** its presence and **discovers** others. When two devices find each other, a lexicographic tie-breaking rule on device IDs decides who initiates the connection, avoiding simultaneous-connection collisions. Once connected, devices automatically become part of the mesh — no manual pairing required.

### Message routing
Every message is wrapped in a `MeshPacket` containing a packet ID, sender/recipient IDs, a **TTL (time-to-live)** of 5, and an encrypted payload. When a device receives a packet:
1. Duplicate packets (already seen) are dropped.
2. If it's addressed to this device, or is a broadcast, it's delivered locally.
3. If it's not fully "consumed" and TTL > 1, it's relayed to all neighbors (except the one it arrived from), with TTL decremented by 1.

This lets messages hop across multiple devices to reach recipients outside direct radio range.

### Offline handling
If a recipient isn't reachable, messages are queued locally and automatically flushed once a connection path becomes available again (detected via a `NODE_HELLO` handshake packet).

### Security
- **In transit (E2EE):** Each device generates an EC (secp256r1) keypair. Devices exchange public keys, derive a shared secret via **ECDH**, hash it with **SHA-256** into an AES-256 key, and use **AES/GCM** to encrypt/decrypt private chat payloads. Only the sender and recipient can decrypt — relaying devices only forward ciphertext.
- **At rest:** The local message database is encrypted with **AES-256/GCM** using a key held in the **Android Keystore**.
- Broadcast ("Everyone") messages are sent in plaintext, since there's no single recipient to derive a shared key with.

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Repository pattern) |
| Dependency Injection | Hilt (Dagger) |
| Local Database | Room |
| P2P Networking | Google Play Services — Nearby Connections API |
| Serialization | Gson |
| Image Loading | Coil |
| QR Codes | ZXing |
| Build System | Gradle (Kotlin DSL) |

---

## 📋 Requirements

- Android 8.0 (API 26) or higher
- Bluetooth and Wi-Fi hardware
- Google Play Services installed
- Camera (for QR-based friend adding) and microphone (for voice notes) — optional, only needed for those features

---

## 🔑 Permissions Used

| Permission | Purpose |
|---|---|
| `BLUETOOTH_SCAN` / `BLUETOOTH_ADVERTISE` / `BLUETOOTH_CONNECT` | Discover and connect to nearby devices |
| `NEARBY_WIFI_DEVICES`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | Wi-Fi Direct based connections |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Required by Android for BLE scanning |
| `CAMERA` | Scanning QR codes to add friends |
| `RECORD_AUDIO` | Recording voice messages |
| `FOREGROUND_SERVICE` | Keeping the mesh network alive in the background |
| `POST_NOTIFICATIONS` | Notifying users of new messages |

---

## 🚀 Getting Started

### Clone the repository
```bash
git clone https://github.com/Tarak-Nath-Dey/VoiceBridge.git
cd VoiceBridge
```

### Build the project
```bash
./gradlew assembleDebug
```

### Install on a connected device
```bash
./gradlew installDebug
```

Or open the project in **Android Studio** and run it directly on a physical device (the mesh networking features require real hardware — Bluetooth/Wi-Fi Direct won't work reliably on emulators).

> ⚠️ To test the mesh functionality, you'll need **at least two physical Android devices** with the app installed.

---

## 📁 Project Structure

```
app/src/main/java/com/voicebridge/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── network/        # Nearby Connections wrapper, mesh packet routing
│   └── security/       # Encryption & key exchange managers
├── di/                  # Hilt dependency injection modules
├── domain/
│   └── repository/     # ChatRepository, UserRepository (business logic)
└── ui/
    ├── navigation/      # Navigation graph
    ├── screens/         # Compose screens (Chats, Friends, Nearby, Profile, etc.)
    ├── theme/           # App theming
    └── viewmodel/       # ViewModels
```

---

## 🗺️ Future Roadmap

- [ ] Group/channel-based mesh rooms
- [ ] Message delivery acknowledgments (true end-to-end receipts)
- [ ] Chunked file transfer for larger attachments
- [ ] Hybrid mode — bridge mesh islands via internet when available
- [ ] Adaptive TTL based on mesh density
- [ ] Native BLE/Wi-Fi Direct implementation to remove the Google Play Services dependency

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Feel free to open an issue or submit a pull request.
