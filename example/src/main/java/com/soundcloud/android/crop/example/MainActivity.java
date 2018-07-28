package com.soundcloud.android.crop.example;

import com.soundcloud.android.crop.Crop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    private ImageView resultView;
    private View progress;
    private int REQUEST_SHOOT = 5015;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultView = (ImageView) findViewById(R.id.result_image);
        progress = findViewById(R.id.progress);
        hideProgress();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select) {
            showProgress();
            resultView.setImageDrawable(null);
            Crop.pickImage(this);
            return true;
        } else if (item.getItemId() == R.id.action_camera) {
            showProgress();
            resultView.setImageDrawable(null);
            shoot();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shoot() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getCaptureUri());
        startActivityForResult(intent, REQUEST_SHOOT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        hideProgress();
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == REQUEST_SHOOT && resultCode == RESULT_OK) {
            beginCrop(getCaptureUri());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private Uri getCaptureUri() {
        return Uri.parse("content://com.soundcloud.android.crop.example.capture/capture.jpg");
    }

    private void beginCrop(Uri source) {
        showProgress();
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        hideProgress();
        if (resultCode == RESULT_OK) {
            resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void hideProgress() {
        progress.setVisibility(View.INVISIBLE);
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }
}
