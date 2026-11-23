"""Capture native screens on a clean CI emulator using the installed debug APK."""

from pathlib import Path
import re
import subprocess
from time import sleep
import xml.etree.ElementTree as ET


COMPONENT = "com.aifitnesscoach/.app.MainActivity"
OUTPUT = Path("screenshots")
STRINGS = ET.parse("app/src/main/res/values/strings.xml").getroot()


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=25)


def label(name):
    return STRINGS.find(f"./string[@name='{name}']").text


def find_node(text):
    xml_path = "/data/local/tmp/fitness-screen.xml"
    for _ in range(10):
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


def main():
    OUTPUT.mkdir(exist_ok=True)
    adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk")
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
    tap(label("nav_plan"))
    capture("07-weekly-plan.png", label("plan_rest_day"))
    tap(label("nav_progress"))
    capture("08-progress-empty.png", label("history_empty"))
    tap(label("nav_exercises"))
    tap(label("discovery_equipment"))
    capture("09-equipment-filter.png", label("discovery_all_equipment"))
    adb("shell", "input", "keyevent", "KEYCODE_BACK")

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


if __name__ == "__main__":
    main()
