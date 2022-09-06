// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.camera;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
public class ImageSaver implements Runnable {

    /**
     * The JPEG image
     */
    private final Image image;

    /**
     * The file we save the image into.
     */
    private final File file;

    /**
     * Used to report the status of the save action.
     */
    private final Callback callback;

    /**
     * Creates an instance of the ImageSaver runnable
     *
     * @param image    - The image to save
     * @param file     - The file to save the image to
     * @param callback - The callback that is run on completion, or when an error is encountered.
     */
    ImageSaver(@NonNull Image image, @NonNull File file, @NonNull Callback callback) {
        this.image = image;
        this.file = file;
        this.callback = callback;
    }

    @Override
    public void run() {
        FileOutputStream output = null;
        try {
            output = FileOutputStreamFactory.create(file);
            output.write(NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100));

            callback.onComplete(file.getAbsolutePath());

        } catch (IOException e) {
            callback.onError("IOError", "Failed saving image");
        } finally {
            image.close();

            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    callback.onError("cameraAccess", e.getMessage());
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private ByteBuffer getBuffer(Map<String, Object> stringObjectMap) {
        if (stringObjectMap.get("bytes") instanceof byte[]) {
            return ByteBuffer.wrap((byte[]) stringObjectMap.get("bytes"));
        }

        return null;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    /**
     * The interface for the callback that is passed to ImageSaver, for detecting completion or
     * failure of the image saving task.
     */
    public interface Callback {
        /**
         * Called when the image file has been saved successfully.
         *
         * @param absolutePath - The absolute path of the file that was saved.
         */
        void onComplete(String absolutePath);

        /**
         * Called when an error is encountered while saving the image file.
         *
         * @param errorCode    - The error code.
         * @param errorMessage - The human readable error message.
         */
        void onError(String errorCode, String errorMessage);
    }

    /**
     * Factory class that assists in creating a {@link FileOutputStream} instance.
     */
    static class FileOutputStreamFactory {
        /**
         * Creates a new instance of the {@link FileOutputStream} class.
         *
         * <p>This method is visible for testing purposes only and should never be used outside this *
         * class.
         *
         * @param file - The file to create the output stream for
         * @return new instance of the {@link FileOutputStream} class.
         * @throws FileNotFoundException when the supplied file could not be found.
         */
        @VisibleForTesting
        public static FileOutputStream create(File file) throws FileNotFoundException {
            return new FileOutputStream(file);
        }
    }
}
