package com.soundcloud.android.crop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

public class Crop {

    public static final int REQUEST_CROP = 10;
    public static final int REQUEST_PICK = 11;

    static interface Extra {
        String ASPECT_X = "aspect_x";
        String ASPECT_Y = "aspect_y";
        String MAX_X = "max_x";
        String MAX_Y = "max_y";
        String ERROR = "error";
    }

    private Intent cropIntent;

    public Crop(Uri input) {
        cropIntent = new Intent();
        cropIntent.setData(input);
    }

    public Crop output(Uri output) {
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        return this;
    }

    public Crop aspectX(int x) {
        cropIntent.putExtra(Extra.ASPECT_X, x);
        return this;
    }

    public Crop aspectY(int y) {
        cropIntent.putExtra(Extra.ASPECT_Y, y);
        return this;
    }

    public Crop maxX(int x) {
        cropIntent.putExtra(Extra.MAX_X, x);
        return this;
    }

    public Crop maxY(int y) {
        cropIntent.putExtra(Extra.MAX_Y, y);
        return this;
    }

    public Crop asSquare() {
        cropIntent.putExtra(Extra.ASPECT_X, 1);
        cropIntent.putExtra(Extra.ASPECT_Y, 1);
        return this;
    }

    public void start(Activity activity) {
        cropIntent.setClass(activity, CropImageActivity.class);
        activity.startActivityForResult(cropIntent, REQUEST_CROP);
    }

    public static void pickImage(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            activity.startActivityForResult(intent, REQUEST_PICK);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.error_pick_image, Toast.LENGTH_SHORT).show();
        }
    }

}
