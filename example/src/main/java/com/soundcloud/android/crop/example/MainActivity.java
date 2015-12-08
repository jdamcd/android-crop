package com.soundcloud.android.crop.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity {

    private ImageView resultView;
    private int REQUEST_PHOTO = 1;
    private File PHOTO_CAPTURED, PHOTO_CROPPED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultView = (ImageView) findViewById(R.id.result_image);

        PHOTO_CAPTURED = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PhotoCaptured.jpg");
        PHOTO_CROPPED = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PhotoCropped.jpg");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select) {
            resultView.setImageDrawable(null);
            Crop.pickImage(this);
            return true;
        } else if (item.getItemId() == R.id.action_camera) {
            resultView.setImageDrawable(null);
            takePhoto();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void takePhoto() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(PHOTO_CAPTURED));
        startActivityForResult(intent, REQUEST_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            beginCrop(Uri.fromFile(PHOTO_CAPTURED));
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private void beginCrop(Uri source) {
        Crop.of(source, Uri.fromFile(PHOTO_CROPPED))
                .withMaxSize(1600,1600)
                .withJpgQuality(90)
                .start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
