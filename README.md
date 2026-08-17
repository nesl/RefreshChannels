# RefreshChannels

**Exploiting Dynamic Refresh Rate Switching for Mobile Device Attacks**

Gaofeng Dong, Jason Wu, Julian de Gortari Briseno, Akash Deep Singh, Justin Feng, Ankur Sarker, Nader Sehatbakhsh, and Mani Srivastava (University of California, Los Angeles).

Published at *The 22nd Annual International Conference on Mobile Systems, Applications and Services* (**MobiSys '24**), June 3–7, 2024, Tokyo, Japan.
DOI: [10.1145/3643832.3661864](https://doi.org/10.1145/3643832.3661864)

## Abstract

Mobile devices with dynamic refresh rate (DRR) switching displays have recently become increasingly common. For power optimization, these devices switch to lower refresh rates when idling, and switch to higher refresh rates when the content displayed requires smoother transitions. However, the security and privacy vulnerabilities of DRR switching have not been investigated properly. In this paper, we propose a novel attack vector called RefreshChannels that exploits DRR switching capabilities for mobile device attacks. Specifically, we first create a covert channel between two colluding apps that are able to stealthily share users' private information by modulating the data with the refresh rates, bypassing the OS sandboxing and isolation measures. Second, we further extend its applicability by creating a covert channel between a malicious app and either a phishing webpage or a malicious advertisement on a benign webpage. Our extensive evaluations on five popular mobile devices from four different vendors demonstrate the effectiveness and widespread impacts of these attacks. Finally, we investigate several countermeasures, such as restricting access to refresh rates, and find they are inadequate for thwarting RefreshChannels due to DRR's unique characteristics.

## About this repository

This repository contains the source code for the proof-of-concept Android apps used in the paper to demonstrate the app-to-app covert channel built on dynamic refresh rate (DRR) switching. Two apps run concurrently on the same device and communicate covertly, without any conventional inter-process communication, by encoding bits as display refresh rates:

- **`Transmitter_RefreshChannel/`** — The sender. It requests specific refresh rates (via `Surface.setFrameRate`) from a tiny, transparent 1×1 overlay `Surface` rendered by a foreground service (`FloatingSurfaceService`). A simple state machine (`StateMachine` / `State`) sequences the transmission through *idle → sync → data → end* phases, modulating each bit as a distinct refresh rate. A JNI/NDK layer listens to the system's actual refresh rate (via `AChoreographer_registerRefreshRateCallback`) to confirm each requested rate was applied before advancing.

- **`Receiver_RefreshChannel/`** — The receiver. Any co-resident app can passively observe the current display refresh rate through the same `AChoreographer` callback, with no special permissions. The native code (`native-lib.cpp`) demodulates the observed refresh rates back into bits — detecting the sync marker, decoding `0`/`1` symbols, counting symbol durations against a configurable bit interval, and reassembling the transmitted bitstream at the end marker.

Together they realize the covert channel: the transmitter drives the shared display refresh rate, and the receiver reads it, so private data crosses the OS app-isolation boundary through a channel the sandbox does not mediate.

### Symbol encoding

The refresh-rate-to-symbol mapping is configurable.
The receiver's `native-lib.cpp` includes several alternative rate profiles for devices that support different sets of refresh rates.

## Repository layout

```
RefreshChannels/
├── Transmitter_RefreshChannel/   # Android Studio project — the sender app
│   └── app/src/main/
│       ├── java/.../MainActivity.java
│       ├── java/.../FloatingSurfaceService.java
│       ├── java/.../StateMachine.java, State.java
│       └── cpp/transmitter_refreshchannel.cpp   # NDK refresh-rate listener
└── Receiver_RefreshChannel/      # Android Studio project — the receiver app
    └── app/src/main/
        ├── java/.../MainActivity.java
        └── cpp/native-lib.cpp                    # NDK demodulator
```

Only source files are included — compiled outputs, IDE/Gradle caches, and local configuration are omitted.

## Building

Each app is a standard Android Studio / Gradle project.

- **Requirements:** Android Studio with the Android SDK (compileSdk 32, minSdk 31) and the Android NDK + CMake 3.18.1 (the native code uses the `AChoreographer` refresh-rate API introduced in API level 30).
- Open `Transmitter_RefreshChannel/` and `Receiver_RefreshChannel/` as separate projects, or build from the command line with `./gradlew assembleDebug` inside each.
- Create a `local.properties` file in each project pointing to your SDK (e.g. `sdk.dir=/path/to/Android/sdk`); this file is intentionally not committed.
- The transmitter requires the **Display over other apps** (`SYSTEM_ALERT_WINDOW`) permission, which it prompts for on first launch.

## Usage

1. Install both apps on a device that supports dynamic refresh rate switching.
2. Launch the receiver and tap **Start Receiving** (optionally set the bit interval, in milliseconds).
3. Launch the transmitter and tap **Start Transmitting**.
4. The receiver decodes the incoming bitstream and displays/logs the received bits; both apps log detailed timing and refresh-rate traces via Android `Log` for analysis.

## Disclaimer

This code is released for research and educational purposes to reproduce the findings in the paper and to support the development of countermeasures. Do not use it to exfiltrate data or otherwise compromise devices or users without explicit authorization.

## Citation

```bibtex
@inproceedings{dong2024refreshchannels,
  author    = {Dong, Gaofeng and Wu, Jason and de Gortari Brise\~{n}o, Julian and Singh, Akash Deep and Feng, Justin and Sarker, Ankur and Sehatbakhsh, Nader and Srivastava, Mani},
  title     = {RefreshChannels: Exploiting Dynamic Refresh Rate Switching for Mobile Device Attacks},
  booktitle = {Proceedings of the 22nd Annual International Conference on Mobile Systems, Applications and Services (MobiSys '24)},
  year      = {2024},
  location  = {Minato-ku, Tokyo, Japan},
  publisher = {ACM},
  doi       = {10.1145/3643832.3661864}
}
```
