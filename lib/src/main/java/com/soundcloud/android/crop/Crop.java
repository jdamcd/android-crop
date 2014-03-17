package com.soundcloud.android.crop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * Builder for crop Intents and utils for handling result
 */
public class Crop {

    public static final int REQUEST_CROP = 10;
    public static final int REQUEST_PICK = 11;
    public static final int RESULT_ERROR = 404;

    static interface Extra {
        String ASPECT_X = "aspect_x";
        String ASPECT_Y = "aspect_y";
        String MAX_X = "max_x";
        String MAX_Y = "max_y";
        String ERROR = "error";
        String RETURN_DATA = "return_data";
        String IMAGE_DATA = "data";
    }

    private Intent cropIntent;

    /**
     * Create a crop Intent builder with source image
     *
     * @param source Source image URI
     */
    public Crop(Uri source) {
        cropIntent = new Intent();
        cropIntent.setData(source);
    }

    /**
     * Set output URI where the cropped image will be saved
     *
     * @param output Output image URI
     */
    public Crop output(Uri output) {
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        return this;
    }

    /**
     * Request that the Bitmap data is sent in the return Intent
     *
     * Note: This is only useful for debugging or if you have a small max crop size. Trying to
     * return large Bitmaps will fail. The image will not be written to the output URI in this case.
     */
    public Crop returnBitmap() {
        cropIntent.putExtra(Extra.RETURN_DATA, true);
        return this;
    }

    /**
     * Set fixed aspect ratio for crop area
     *
     * @param x Aspect X
     * @param y Aspect Y
     */
    public Crop withAspect(int x, int y) {
        cropIntent.putExtra(Extra.ASPECT_X, x);
        cropIntent.putExtra(Extra.ASPECT_Y, y);
        return this;
    }

    /**
     * Crop area with fixed 1:1 aspect ratio
     */
    public Crop asSquare() {
        cropIntent.putExtra(Extra.ASPECT_X, 1);
        cropIntent.putExtra(Extra.ASPECT_Y, 1);
        return this;
    }

    /**
     * Set maximum crop size
     *
     * @param width Max width
     * @param height Max height
     */
    public Crop withMaxSize(int width, int height) {
        cropIntent.putExtra(Extra.MAX_X, width);
        cropIntent.putExtra(Extra.MAX_Y, height);
        return this;
    }

    /**
     * Send the crop Intent!
     *
     * @param activity Activity that will receive result
     */
    public void start(Activity activity) {
        activity.startActivityForResult(getIntent(activity), REQUEST_CROP);
    }

    //VisibleForTesting
    Intent getIntent(Activity activity) {
        cropIntent.setClass(activity, CropImageActivity.class);
        return cropIntent;
    }

    /**
     * Retrieve error that caused crop to fail
     *
     * @param result Result Intent
     * @return Throwable handled in CropImageActivity
     */
    public static Throwable getError(Intent result) {
        return (Throwable) result.getSerializableExtra(Extra.ERROR);
    }

    /**
     * Retrieve the Bitmap data when using returnBitmap() option
     *
     * @param result Result Intent
     * @return Cropped image as Bitmap
     */
    public static Bitmap getBitmap(Intent result) {
        return (Bitmap) result.getParcelableExtra(Extra.IMAGE_DATA);
    }

    /**
     *  Utility method that starts an image picker. Often preceded a crop.
     *
     * @param activity Activity that will receive result
     */
    public static void pickImage(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            activity.startActivityForResult(intent, REQUEST_PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.crop__pick_error, Toast.LENGTH_SHORT).show();
        }
    }

}
