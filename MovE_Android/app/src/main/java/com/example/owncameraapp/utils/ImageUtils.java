package com.example.owncameraapp.utils;

import com.example.owncameraapp.tflitemodel.MLResult;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int LOCATION_VALUE_MIDDLE = 150;
    private static final int LOCATION_VALUE_LEFT_LIMIT = 80;
    private static final int LOCATION_VALUE_RIGHT_LIMIT = 220;

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     */
    public static void saveBitmap(final Bitmap bitmap) {
        saveBitmap(bitmap, "preview.png");
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        final String root =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow";
        final File myDir = new File(root);

        if (!myDir.mkdirs()) {
        }

        final String fname = filename;
        final File file = new File(myDir, fname);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
        }
    }

    /**
     * Returns a rotated bitmap image.
     *
     * @param source Source image.
     * @param orientation Amount of rotation to apply from one frame to another. Must be a multiple of 90.
     */
    public static Bitmap RotateBitmap(Bitmap source, int orientation)
    {
        int rotate = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90; break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180; break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270; break;
        }
        final Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Returns a transformation matrix from one reference frame into another. Handles cropping (if
     * maintaining aspect ratio is desired) and rotation.
     *
     * @param srcWidth Width of source frame.
     * @param srcHeight Height of source frame.
     * @param dstWidth Width of destination frame.
     * @param dstHeight Height of destination frame.
     * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant.
     * @return The transformation fulfilling the desired requirements.
     */
    public static Matrix getTransformationMatrix(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final boolean maintainAspectRatio) {

        final Matrix matrix = new Matrix();
        // Apply scaling if necessary.
        if (srcWidth != dstWidth || srcHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) srcWidth;
            final float scaleFactorY = dstHeight / (float) srcHeight;

            if (maintainAspectRatio) {
                // Scale by minimum factor so that dst is filled completely while
                // maintaining the aspect ratio. Some image may fall off the edge.
                final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
                matrix.postScale(scaleFactor, scaleFactor);
            } else {
                // Scale exactly to fill dst from src.
                matrix.postScale(scaleFactorX, scaleFactorY);
            }
        }

        return matrix;
    }

    /**
     * Checks the amount of green traffic light pixels vs red traffic light pixels
     *
     * @param bitmap Photo of a traffic light.
     * @return True if traffic light has more green pixels.
     */
    public static boolean checkGreenVsRedPixels(final Bitmap bitmap) {
        final int pxLength = bitmap.getWidth() * bitmap.getHeight();
        final int pixelsArray[] = new int[pxLength];
        bitmap.getPixels(pixelsArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int totalR = 0, totalG = 0;
        for (int i = 0; i < pxLength; ++i) {
            final int R = ((pixelsArray[i] >> 16) & 0xFF);
            final int G = ((pixelsArray[i] >> 8) & 0xFF);
            final int B = (pixelsArray[i] & 0xFF);

            if( (G > B && G > R+100) ||
                (G > 200 && B > 200 && R < 100) ||
                (G > B && R < 15  && G < 100 && B < 100)) {
                totalG++;
            }
            else if( (R > 200 && G < 80 && B < 80) ||
                     (R > 200 && G > 120 && G < R-10 && B < 100) ||
                     (R < 180 && R > G+60 && G < 80 && B < 80 )) {
                totalR++;
            }
        }
        Log.i(TAG, ", totalG: " + totalG + ", totalR: " + totalR);

        return (totalG > totalR) ? true : false;
    }

    /**
     * Based on the location, checks if the object can be avoided or not
     *
     * @param location the object location as a RectF.
     * @return MLResult LEFT, RIGHT or UNAVOIDABLE
     */
    public static MLResult checkAvoidOnObjectLocation(RectF location) {
        MLResult retValue = MLResult.RESULT_UNDEFINED;

        Log.i(TAG, "location left: " + location.left);
        Log.i(TAG, "location right: " + location.left);
        Log.i(TAG, "location center x : " + location.centerX());
        Log.i(TAG, "location center y: " + location.centerY());

        if(location.centerX() > LOCATION_VALUE_MIDDLE) {
            Log.i(TAG, "Check Left!");
            if(location.left >= LOCATION_VALUE_LEFT_LIMIT) {
                retValue = MLResult.RESULT_OBSTACLE_AVOID_LEFT;
            }
        }
        if(location.centerX() < LOCATION_VALUE_MIDDLE) {
            Log.i(TAG, "Check Right!");
            if(location.right <= LOCATION_VALUE_RIGHT_LIMIT) {
                retValue = MLResult.RESULT_OBSTACLE_AVOID_RIGHT;
            }
        }
        if(location.left < LOCATION_VALUE_LEFT_LIMIT && location.right > LOCATION_VALUE_RIGHT_LIMIT) {
            Log.i(TAG, "Can't be avoided!");
            retValue = MLResult.RESULT_OBSTACLE_UNAVOIDABLE;
        }

        return retValue;
    }
}
