package com.soundcloud.android.crop.example;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.soundcloud.android.crop.CropImageActivity;

import static android.view.View.*;

public class MainActivity extends ActionBarActivity implements OnClickListener {

    private static final int REQUEST_PICK_IMAGE = 10;
    private static final int REQUEST_CROP_IMAGE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.select_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        pickImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                cropImage(result.getData(), 100, 100);
                break;
            case REQUEST_CROP_IMAGE:
                if (result.getExtras().containsKey("error")) {
                    Exception e = (Exception) result.getSerializableExtra("error");
                } else {
                    // TODO: Handle successful crop
                }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            // No Activity to provide images!
        }
    }

    private void cropImage(Uri input, int width, int height) {
        Intent intent = new Intent(this, CropImageActivity.class)
                .setData(input)
                .putExtra("aspectX", 1)
                .putExtra("aspectY", 1)
                .putExtra("maxX", width)
                .putExtra("maxY", height);
        startActivityForResult(intent, 0);
    }

}
