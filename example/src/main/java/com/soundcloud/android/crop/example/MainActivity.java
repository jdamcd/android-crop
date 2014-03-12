package com.soundcloud.android.crop.example;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.CropImageActivity;

import java.io.File;

public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_PICK_IMAGE = 10;
    private static final int REQUEST_CROP_IMAGE = 11;

    private Uri output;
    private ImageView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = Uri.fromFile(new File(getCacheDir(), "cropped"));
        resultView = (ImageView) findViewById(R.id.result_image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select) {
            pickImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                cropImage(result.getData());
                break;
            case REQUEST_CROP_IMAGE:
                if (result.getExtras().containsKey("error")) {
                    Exception e = (Exception) result.getSerializableExtra("error");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    resultView.setImageDrawable(null);
                    resultView.setImageURI(output);
                }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_pick_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void cropImage(Uri input) {
        Intent intent = new Intent(this, CropImageActivity.class)
                .setData(input)
                .putExtra(MediaStore.EXTRA_OUTPUT, output)
                .putExtra("aspectX", 1)
                .putExtra("aspectY", 1)
                .putExtra("maxX", 200)
                .putExtra("maxY", 200);
        startActivityForResult(intent, REQUEST_CROP_IMAGE);
    }

}
