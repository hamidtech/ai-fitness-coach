"""Capture native screens on a clean CI emulator using the installed debug APK."""

from pathlib import Path
import hashlib
import json
import re
import struct
import subprocess
from time import monotonic, sleep
import xml.etree.ElementTree as ET
from zipfile import ZipFile


COMPONENT = "com.aifitnesscoach/.app.MainActivity"
OUTPUT = Path("screenshots")
STRINGS = ET.parse("app/src/main/res/values/strings.xml").getroot()


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=25)


def label(name):
    return STRINGS.find(f"./string[@name='{name}']").text


def find_node(text):
    xml_path = "/data/local/tmp/fitness-screen.xml"
    deadline = monotonic() + 25
    while monotonic() < deadline:
        adb("shell", "rm", "-f", xml_path)
        try:
            adb("shell", "uiautomator", "dump", xml_path)
            tree = ET.fromstring(adb("shell", "cat", xml_path))
        except (subprocess.CalledProcessError, ET.ParseError):
            sleep(0.5)
            continue
        for node in tree.iter("node"):
            labels = [node.get("text", ""), node.get("content-desc", "")]
            if any(value == text or value.startswith((text + ",", text + "\n")) for value in labels):
                return node
        sleep(0.3)
    raise RuntimeError(f"Could not find {text!r} on the emulator screen")


def tap(text):
    node = find_node(text)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def capture(filename, expected_label):
    find_node(expected_label)
    (OUTPUT / filename).write_bytes(adb("exec-out", "screencap", "-p"))


def video_pixels():
    # ADB screencap emits little-endian display dimensions and packed RGBA/RGBX pixels.
    frame = adb("exec-out", "screencap")
    width, height, pixel_format = struct.unpack_from("<III", frame)
    header = len(frame) - width * height * 4
    if pixel_format not in (1, 2) or header not in (12, 16):
        raise RuntimeError("Unexpected emulator screenshot format")
    pixels = []
    for y in range(48):
        row = round(height * (0.27 + y * 0.10 / 48))
        for x in range(96):
            column = round(width * (0.15 + x * 0.70 / 96))
            offset = header + (row * width + column) * 4
            pixels.append(sum(frame[offset:offset + 3]) / 3)
    return pixels


def capture_video_playback(pack):
    with ZipFile(pack) as archive:
        media = next(item for item in json.loads(archive.read("catalog.json"))["media"]
                     if item["exerciseId"] == "hasan-0289")

    # Media3's controls can keep uiautomator waiting for an idle accessibility tree.
    # Verify the app-private file before interacting with the player in the fixed Pixel 6 AVD.
    pattern = "files/exercise_media/hasan-0289_*_primary.mp4"
    deadline = monotonic() + 75
    while monotonic() < deadline:
        command = f"run-as com.aifitnesscoach sh -c 'sha256sum {pattern} 2>/dev/null'"
        check = subprocess.run(["adb", "shell", command], capture_output=True, timeout=10)
        if check.stdout.decode().startswith(media["sha256"] + " "):
            break
        sleep(1)
    else:
        (OUTPUT / "17-video-download-diagnostic.png").write_bytes(adb("exec-out", "screencap", "-p"))
        events = adb("logcat", "-d", "-s", "ExerciseMediaDownload", "WM-WorkerWrapper").decode()
        print("\n".join(line for line in events.splitlines() if "hasan-0289" in line))
        raise RuntimeError("The downloaded video did not match the catalog SHA-256")

    sleep(2)
    ready = adb("exec-out", "screencap", "-p")
    (OUTPUT / "17-video-available-offline.png").write_bytes(ready)
    width, height = struct.unpack(">II", ready[16:24])
    wifi_enabled = adb("shell", "settings", "get", "global", "wifi_on").strip() == b"1"
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    try:
        adb("shell", "svc", "wifi", "disable")
        if adb("shell", "settings", "get", "global", "airplane_mode_on").strip() != b"1":
            raise RuntimeError("The emulator did not enter airplane mode")
        # The play button is centered in the preview at this position on the CI device.
        adb("shell", "input", "tap", str(width // 2), str(round(height * 0.34)))
        sleep(6)
        before = video_pixels()
        sleep(1)
        after = video_pixels()
        screen = adb("exec-out", "screencap", "-p")
        difference = sum(abs(a - b) for a, b in zip(before, after)) / len(before)
        if difference < 0.25:
            (OUTPUT / "18-video-playback-diagnostic.png").write_bytes(screen)
            raise RuntimeError("Video frames did not advance in airplane mode")
        (OUTPUT / "18-offline-video-playback.png").write_bytes(screen)
        print("Verified the catalog video SHA-256 and advancing frames in airplane mode.")
    finally:
        adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
        if wifi_enabled:
            adb("shell", "svc", "wifi", "enable")


def main():
    OUTPUT.mkdir(exist_ok=True)
    adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
    # Install this PR's catalog in the emulator cache before it is published on main.
    pack = Path("../catalog-content/pack.zip")
    install_pack = "run-as com.aifitnesscoach sh -c 'mkdir -p files && cat > files/exercise-catalog.pack'"
    subprocess.run(["adb", "exec-in", install_pack], input=pack.read_bytes(), timeout=25, check=True)
    adb("shell", "svc", "power", "stayon", "true")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    launch = adb("shell", "am", "start", "-W", "-n", COMPONENT).decode()
    print(launch)
    if "Status: ok" not in launch:
        raise RuntimeError("The emulator could not launch AI Fitness Coach")
    capture("01-welcome.png", label("onboarding_welcome_title"))
    tap(label("onboarding_get_started"))
    for _ in range(3):
        tap(label("onboarding_continue"))
    capture("02-training-goal.png", label("wizard_goals"))
    for _ in range(2):
        tap(label("onboarding_continue"))
    tap(label("onboarding_finish"))

    tap(label("nav_settings"))
    capture("03-settings.png", label("settings_appearance_language"))
    tap(label("feature_layout"))
    capture("11-app-features-simple.png", label("feature_simple"))
    tap(label("feature_advanced"))
    capture("12-app-features-advanced.png", label("feature_advanced"))
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    adb("shell", "am", "start", "-W", "-n", COMPONENT, "--es", "aifc_open_route", "widget_quick_start")
    capture("04-exercise-picker.png", label("widget_start"))
    tap("Bodyweight Squat")
    capture("05-workout-setup.png", label("home_start_workout"))
    tap(label("camera_setup_title"))
    capture("06-camera-guide.png", label("camera_setup_title"))


    # Return to the top-level tabs after entering the Quick Start deep link from Settings.
    for _ in range(4):
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
    tap(label("nav_ai_analysis"))
    capture("13-ai-analysis-tab.png", label("ai_coach_insights_title"))
    tap(label("nav_home"))
    adb("shell", "input", "swipe", "540", "1660", "540", "1120", "300")
    tap(label("nav_plan"))
    capture("07-weekly-plan.png", label("plan_rest_day"))
    tap(label("coach_plan_title"))
    capture("19-adaptive-workout.png", label("coach_plan_description"))
    tap(label("coach_review_plan"))
    capture("24-adaptive-workout-review.png", label("home_build_session"))
    tap(label("workout_discard"))
    tap(label("coach_hide"))
    tap(label("nav_progress"))
    capture("08-progress-empty.png", label("history_empty"))
    tap(label("nav_exercises"))
    tap(label("discovery_source_value").replace("%1$s", label("discovery_all_sources")))
    capture("14-catalog-sources.png", label("catalog_title"))
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    tap(label("discovery_equipment"))
    capture("09-equipment-filter.png", label("discovery_all_equipment"))
    adb("shell", "input", "keyevent", "KEYCODE_BACK")

    tap(label("exercise_filter_video"))
    # The extra tracking label makes rows taller; search for the exact test movement.
    tap(label("exercises_search_hint"))
    adb("shell", "input", "text", "dumbbell%sbench")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    capture("15-video-filter.png", "Dumbbell bench press")
    tap("Dumbbell bench press")
    capture("16-video-download.png", label("exercise_download_video"))
    tap(label("exercise_download_video"))
    capture_video_playback(pack)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    tap(label("common_clear"))
    tap(label("exercise_filter_video"))

    # Break UI with a practical narrow-display and large-type configuration.
    initial_font = adb("shell", "settings", "get", "system", "font_scale").decode().strip()
    try:
        adb("shell", "settings", "put", "system", "font_scale", "1.65")
        tap(label("discovery_muscles"))
        capture("10-muscle-filter-large-type.png", label("discovery_all_muscles"))
    finally:
        if initial_font in {"", "null"}:
            adb("shell", "settings", "delete", "system", "font_scale")
        else:
            adb("shell", "settings", "put", "system", "font_scale", initial_font)

    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    tap(label("zing_guides"))
    capture("20-zing-guide-filter.png", label("zing_open_library"))
    tap(label("exercises_search_hint"))
    adb("shell", "input", "text", "high%splank")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    tap("High plank")
    capture("21-new-high-plank.png", label("setup_general_hold"))
    tap(label("home_start_workout"))
    capture("22-universal-camera-setup.png", label("workout_without_camera"))
    tap(label("workout_without_camera"))
    capture("23-manual-camera-fallback.png", label("workout_manual_only"))


if __name__ == "__main__":
    main()
