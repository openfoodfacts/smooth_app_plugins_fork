package io.flutter.plugins.camera.features.focuspoint;

import io.flutter.plugins.camera.features.autofocus.FocusMode;

public enum FocusPointMode {
    auto("auto"), newAlgorithm("new"), oldAlgorithm("old");

    private final String strValue;

    FocusPointMode(String strValue) {
        this.strValue = strValue;
    }

    public static FocusPointMode getValueForString(String modeStr) {
        for (FocusPointMode value : values()) {
            if (value.strValue.equals(modeStr)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return strValue;
    }
}
