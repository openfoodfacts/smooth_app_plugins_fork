// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.camera.features.focuspoint;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.util.Size;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.systemchannels.PlatformChannel;
import io.flutter.plugins.camera.CameraProperties;
import io.flutter.plugins.camera.CameraRegionUtils;
import io.flutter.plugins.camera.features.CameraFeature;
import io.flutter.plugins.camera.features.Point;
import io.flutter.plugins.camera.features.sensororientation.SensorOrientationFeature;

/**
 * Focus point controls where in the frame focus will come from.
 */
public class FocusPointFeature extends CameraFeature<Point> {

    private Size cameraBoundaries;
    private Point focusPoint;
    private MeteringRectangle focusRectangle;
    private FocusPointMode mode;
    private final SensorOrientationFeature sensorOrientationFeature;

    /**
     * Creates a new instance of the {@link FocusPointFeature}.
     *
     * @param cameraProperties Collection of the characteristics for the current camera device.
     */
    public FocusPointFeature(
            CameraProperties cameraProperties, SensorOrientationFeature sensorOrientationFeature) {
        super(cameraProperties);
        this.sensorOrientationFeature = sensorOrientationFeature;
    }

    /**
     * Sets the camera boundaries that are required for the focus point feature to function.
     *
     * @param cameraBoundaries - The camera boundaries to set.
     */
    public void setCameraBoundaries(@NonNull Size cameraBoundaries) {
        this.cameraBoundaries = cameraBoundaries;
        this.buildFocusRectangle();
    }

    @Override
    public String getDebugName() {
        return "FocusPointFeature";
    }

    @Override
    public Point getValue() {
        return focusPoint;
    }

    public FocusPointMode getMode() {
        return mode;
    }

    public void setMode(FocusPointMode mode) {
        if (mode == FocusPointMode.auto) {
            this.mode = FocusPointHelper.detectFocusPointMode();
        } else {
            this.mode = mode;
        }
    }

    @Override
    public void setValue(Point value) {
        this.focusPoint = value == null || value.x == null || value.y == null ? null : value;
        this.buildFocusRectangle();
    }

    // Whether or not this camera can set the focus point.
    @Override
    public boolean checkIsSupported() {
        Integer supportedRegions = cameraProperties.getControlMaxRegionsAutoFocus();
        return supportedRegions != null && supportedRegions > 0;
    }

    @Override
    public void updateBuilder(CaptureRequest.Builder requestBuilder) {
        if (!checkIsSupported()) {
            return;
        }

        if (mode == FocusPointMode.newAlgorithm) {
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        }

        requestBuilder.set(
                CaptureRequest.CONTROL_AF_REGIONS,
                focusRectangle == null ? null : new MeteringRectangle[]{focusRectangle});

        requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
    }

    private void buildFocusRectangle() {
        if (this.cameraBoundaries == null) {
            throw new AssertionError(
                    "The cameraBoundaries should be set (using `FocusPointFeature.setCameraBoundaries(Size)`) before updating the focus point.");
        }
        if (this.focusPoint == null) {
            this.focusRectangle = null;
        } else {
            PlatformChannel.DeviceOrientation orientation =
                    this.sensorOrientationFeature.getLockedCaptureOrientation();
            if (orientation == null) {
                orientation =
                        this.sensorOrientationFeature.getDeviceOrientationManager().getLastUIOrientation();
            }
            this.focusRectangle =
                    CameraRegionUtils.convertPointToMeteringRectangle(
                            this.cameraBoundaries, this.focusPoint.x, this.focusPoint.y, orientation);
        }
    }
}
