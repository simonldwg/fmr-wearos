# Fitness Music Recommender - Watch app

Play music for your workout and achieve your target exercise intensity.
The Fitness Music Recommender monitors your heart rate and selects a suitable song accordingly.
The app was developed as part of my bachelor's thesis at the
[University of Applied Sciences Düsseldorf](https://www.hs-duesseldorf.de/).

<img src="https://github.com/simonldwg/fmr-wearos/blob/6573c876fb796557732ebf80561309463185360a/watch_app_screenshot.png" alt="app screenshot" width="200">

---

This repository contains the source code for the smartwatch app. The app is written purely in Kotlin
using Compose for Wear OS. It has been verified to work with a Pixel Watch 3. Other devices have
yet to be tested.

This app uses a git submodule called [FMRAndroidCore](https://github.com/simonldwg/fmr-android-core).
Cloning this repo should therefore be done recursively so that git also loads the submodule. The
submodule contains a library which provides common code for the Wear OS (smartwatch) app and the
smartphone app. The FMRAndroidCore module implements shared classes for message passing between the
watch and the phone.

---


## Getting Started

The app is built and installed using Gradle and the Android CLI. The commands below are written for 
Unix-based systems (i.e. macOS / Linux). On Windows, the steps are the same, but use `gradlew.bat`
instead of `./gradlew`, and set environment variables with `setx` (or via the system settings) instead of `export`.

It is assumed that the watch is already paired with your phone and that the developer options are enabled on the
watch. The [phone app](https://github.com/simonldwg/fmr-phone) should be installed as well, since the watch app communicates
with it.

### Setting up the toolchain
1. Install a JDK. **The Android Gradle plugin requires JDK 17 or newer.**
```shell
   # Linux (Debian / Ubuntu)
   sudo apt install openjdk-17-jdk
   # macOS
   brew install --cask temurin@17
```
You can check the installed version with `java -version`.

2. Install the Android CLI and update it to the latest version
```shell
   # Linux
   curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
   # macOS
   brew tap android/tap
   brew install android-cli
   # Windows
   winget install --id Google.AndroidCLI

   android update
```

3. Install the required SDK packages
```shell
   android sdk install platform-tools platforms/android-36 build-tools/36.0.0
```

You can list all available packages with `android sdk list --all`. Any further components required by the
project are downloaded automatically by Gradle during the first build.

4. Set the SDK location and add `adb` to your `PATH`
```shell
   export ANDROID_HOME="$(android info sdk)"
   export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

Gradle uses `ANDROID_HOME` to find the SDK. To make the change permanent, add the `export` lines to your
`~/.bashrc` or `~/.zshrc`.

### Building and installing the app
1. Clone the repository **including the submodule**
```shell
   git clone --recurse-submodules https://github.com/simonldwg/fmr-wearos
   cd fmr-wearos
```

If you have already cloned the repository without the submodule, you can load it afterward:
```shell
   git submodule update --init --recursive
```

2. Connect to the watch via wireless debugging. Make sure the watch and your computer are on the same Wi-Fi network.
   On the watch, open *Settings → Developer options → Wireless debugging* and enable it. Then select
   *Pair new device* and run:
```shell
   adb pair <ip>:<pairing-port>
   # Enter the pairing code shown on the watch
   adb connect <ip>:<port>
```

Note: The pairing port and the connection port are different. The connection port is shown on the
*Wireless debugging* screen itself. Pairing is only necessary once, but the connection port can change after
a reboot of the watch.

Check that the watch is listed as `device`:
```shell
   adb devices
```

3. Build and install the app
```shell
   ANDROID_SERIAL=<ip>:<port> ./gradlew installDebug
```
Setting `ANDROID_SERIAL` makes sure the app is only installed on the watch. Without it, Gradle installs the app
on **all** connected devices, including your phone if it is connected as well.
Note: The first build can take several minutes since Gradle downloads all dependencies. If the wrapper is not
executable, run `chmod +x gradlew` first.

4. Start the app, either from the app list on the watch or via adb:
```shell
   adb -s <ip>:<port> shell monkey -p <application-id> -c android.intent.category.LAUNCHER 1
```
The application ID can be found as `applicationId` in `app/build.gradle(.kts)`.

> **Important:** The Wear OS Data Layer only lets the phone and watch apps communicate if both have the
> **same application ID** and are **signed with the same key**. Debug builds are signed with the debug keystore
> at `~/.android/debug.keystore`, so build and install both apps from the same machine. Otherwise, the watch
> will not be able to reach the phone app.

---

### Other repos of this project
* [Smartphone app](https://github.com/simonldwg/fmr-phone)
* [FMRAndroidCore library](https://github.com/simonldwg/fmr-android-core)
* [Backend](https://github.com/simonldwg/fmr-backend)
