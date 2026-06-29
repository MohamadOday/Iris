#!/data/data/com.termux/files/usr/bin/bash
# Iris Keyboard Termux + Shizuku Quick Deploy Script
set -e

# Configuration
RISH="$HOME/shizuku/rish"
APK_PATH="app/build/outputs/apk/nomlkit/debug/app-nomlkit-debug.apk"
TEMP_DEVICE_APK="/data/local/tmp/iris-keyboard-debug.apk"
PACKAGE_NAME="nabu.iris.keyboard"
LAUNCH_ACTIVITY="nabu.iris.keyboard.latin.settings.SettingsActivity"

echo "============================================="
echo "   Iris Keyboard: Compiling & Quick Deploy   "
echo "============================================="

# 1. Verify Rish / Shizuku
if [ ! -f "$RISH" ]; then
    echo "[!] Shizuku CLI wrapper ('rish') not found at $RISH"
    echo "    Please verify Shizuku is running and configured."
    exit 1
fi

# 2. Run Gradle Build
echo "[*] Step 1: Compiling application..."
bash gradlew assembleNomlkitDebug

if [ ! -f "$APK_PATH" ]; then
    echo "[!] Compiled APK not found at $APK_PATH"
    exit 1
fi
echo "[+] Compilation successful!"

# 3. Push APK to Device Temp Folder via Rish
echo "[*] Step 2: Transferring APK to device temp storage..."
cat "$APK_PATH" | "$RISH" -c "cat > $TEMP_DEVICE_APK"
echo "[+] Transfer complete!"

# 4. Install the APK
echo "[*] Step 3: Installing APK via Shizuku Package Manager..."
"$RISH" -c "pm install -r -d $TEMP_DEVICE_APK"
echo "[+] Installation successful!"

# 5. Launch the Application
echo "[*] Step 4: Launching Iris Settings..."
"$RISH" -c "am start -n $PACKAGE_NAME/$LAUNCH_ACTIVITY"
echo "[+] Application launched successfully!"
echo "============================================="
