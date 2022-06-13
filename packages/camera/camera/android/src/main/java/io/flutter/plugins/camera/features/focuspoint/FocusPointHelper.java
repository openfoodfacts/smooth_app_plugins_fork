package io.flutter.plugins.camera.features.focuspoint;

import android.os.Build;

public final class FocusPointHelper {

    public static FocusPointMode detectFocusPointMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isASamsungDevice()) {
            return FocusPointMode.newAlgorithm;
        } else {
            return FocusPointMode.oldAlgorithm;
        }
    }

    private static boolean isASamsungDevice() {
        return android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung");
    }

}
