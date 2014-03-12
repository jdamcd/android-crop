package com.soundcloud.android.crop.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import java.io.File;

public class MainActivity extends Activity {

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
            Crop.pickImage(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case Crop.REQUEST_PICK:
                new Crop(result.getData())
                        .output(output)
                        .asSquare()
                        .maxX(200)
                        .maxY(200)
                        .start(this);
                break;
            case Crop.REQUEST_CROP:
                setImage(result);
        }
    }

    private void setImage(Intent result) {
        if (result.getExtras().containsKey("error")) {
            Exception e = (Exception) result.getSerializableExtra("error");
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } else {
            resultView.setImageDrawable(null);
            resultView.setImageURI(output);
        }
    }

}
