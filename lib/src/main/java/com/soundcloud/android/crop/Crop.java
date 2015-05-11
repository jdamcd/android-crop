package com.soundcloud.android.crop;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

/**
 * Builder for crop Intents and utils for handling result
 */
public class Crop {

    public static final int REQUEST_CROP = 6709;
    public static final int REQUEST_PICK = 9162;
    public static final int RESULT_ERROR = 404;

    static interface Extra {
        String ASPECT_X = "aspect_x";
        String ASPECT_Y = "aspect_y";
        String MAX_X    = "max_x";
        String MAX_Y    = "max_y";
        String ERROR    = "error";
        String FIX_X    = "fix_x";
        String FIX_Y    = "fix_y";
        String FIX_MIN  = "fix_min";
    }

    private Intent cropIntent;

    /**
     * Create a crop Intent builder with source and destination image Uris
     *
     * @param source Uri for image to crop
     * @param destination Uri for saving the cropped image
     */
    public static Crop of(Uri source, Uri destination) {
        return new Crop(source, destination);
    }

    private Crop(Uri source, Uri destination) {
        cropIntent = new Intent();
        cropIntent.setData(source);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, destination);
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
     * Incompatible with {@link #fixedRegion(int, int)}
     *
     * @param width Max width
     * @param height Max height
     */
    public Crop withMaxSize(int width, int height) {
        if (cropIntent.hasExtra(Extra.FIX_X)) {
            throw new IllegalStateException("fixed dimensions specified, cannot set max size then");
        }
        cropIntent.putExtra(Extra.MAX_X, width);
        cropIntent.putExtra(Extra.MAX_Y, height);
        return this;
    }

    /**
     * Enable the cropper to be used just as a image region selector, ensuring
     * client application a desired width and height... user won't be able to resize
     * Highlighted zone, just move it.
     *
     * The region specified is the target output fixed dimension, not screen dimensions.
     *
     * Note: this behaviour is incompatible with {@link #withMaxSize(int, int)}
     *
     * @param fixedWidth the with of the result
     * @param fixedHeight the height of the result
     */
    public Crop fixedRegion(int fixedWidth, int fixedHeight) {
        if (cropIntent.hasExtra(Extra.MAX_X)) {
            throw new IllegalStateException("maximum dimensions already defined, cannot use fixed dimensions now");
        }
        cropIntent.putExtra(Extra.FIX_X, fixedWidth);
        cropIntent.putExtra(Extra.FIX_Y, fixedHeight);
        return this;
    }

    /**
     * Set up the crop to just crop (not resize) bound to the minimum image dimension, and as a square region.
     */
    public Crop fixToMin() {
        if (cropIntent.hasExtra(Extra.MAX_X)) {
            throw new IllegalStateException("maximum dimensions already defined, cannot use fixed dimensions now");
        }
        cropIntent.putExtra(Extra.FIX_MIN, true);
        return this.asSquare();
    }

    /**
     * Send the crop Intent from an Activity
     *
     * @param activity Activity to receive result
     */
    public void start(Activity activity) {
        start(activity, REQUEST_CROP);
    }

    /**
     * Send the crop Intent from an Activity with a custom requestCode
     *
     * @param activity Activity to receive result
     * @param requestCode requestCode for result
     */
    public void start(Activity activity, int requestCode) {
        activity.startActivityForResult(getIntent(activity), requestCode);
    }

    /**
     * Send the crop Intent from a Fragment
     *
     * @param context Context
     * @param fragment Fragment to receive result
     */
    public void start(Context context, Fragment fragment) {
        start(context, fragment, REQUEST_CROP);
    }

	/**
	 * Send the crop Intent from a support library Fragment
	 *
	 * @param context Context
	 * @param fragment Fragment to receive result
	 */
	public void start(Context context, android.support.v4.app.Fragment fragment) {
		start(context, fragment, REQUEST_CROP);
	}

    /**
     * Send the crop Intent with a custom requestCode
     *
     * @param context Context
     * @param fragment Fragment to receive result
     * @param requestCode requestCode for result
     */
    public void start(Context context, Fragment fragment, int requestCode) {
        fragment.startActivityForResult(getIntent(context), requestCode);
    }

	/**
	 * Send the crop Intent with a custom requestCode
	 *
	 * @param context Context
	 * @param fragment Fragment to receive result
	 * @param requestCode requestCode for result
	 */
	public void start(Context context, android.support.v4.app.Fragment fragment, int requestCode) {
		fragment.startActivityForResult(getIntent(context), requestCode);
	}

    /**
     * Get Intent to start crop Activity
     *
     * @param context Context
     * @return Intent for CropImageActivity
     */
    public Intent getIntent(Context context) {
        cropIntent.setClass(context, CropImageActivity.class);
        return cropIntent;
    }

    /**
     * Retrieve URI for cropped image, as set in the Intent builder
     *
     * @param result Output Image URI
     */
    public static Uri getOutput(Intent result) {
        return result.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
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
     * Utility to start an image picker
     *
     * @param activity Activity that will receive result
     */
    public static void pickImage(Activity activity) {
        pickImage(activity, REQUEST_PICK);
    }

    /**
     * Utility to start an image picker with request code
     *
     * @param activity Activity that will receive result
     * @param requestCode requestCode for result
     */
    public static void pickImage(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.crop__pick_error, Toast.LENGTH_SHORT).show();
        }
    }

}
