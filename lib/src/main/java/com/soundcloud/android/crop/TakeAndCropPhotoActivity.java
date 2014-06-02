package com.soundcloud.android.crop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;

/**
 * Created by silong on 5/30/14.
 */
public class TakeAndCropPhotoActivity extends Activity {


  public static final String EXTRA_TYPE = "type";

  public static final String EXTRA_WIDTH = "width";

  public static final String EXTRA_HEIGHT = "height";

  public static final String EXTRA_REQUEST = "request";

  private static Uri outputFileUri;

  private static final String URL_PHOTOS = "Photos/%d.png";

  public static final int TYPE_SQUARE = 0;

  public static final int TYPE_CUSTOM = 1;

  public static final int REQUEST_TAKE = 0;

  public static final int REQUEST_STORAGE = 1;

  private static final int DEFAULT_SIZE = 200;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    if (isSDPresent) {
      startGetPhoto();
    } else {
      setResult(R.id.result_croped_photo_fail_no_sdcard);
      finish();
    }
  }

  private void startGetPhoto() {
    int request = getIntent().getIntExtra(EXTRA_REQUEST, REQUEST_TAKE);
    outputFileUri = getIntent().getData();
    if (outputFileUri == null) {
      //if null, create default uri
      String urlNewPhoto = String.format(URL_PHOTOS, System.currentTimeMillis() / 1000);
      outputFileUri = Uri.withAppendedPath(Uri.parse(Environment.getExternalStorageDirectory().toString()), urlNewPhoto);
    }
    File f = new File(outputFileUri.toString());
    if (f.getParentFile() != null) {
      f.getParentFile().mkdirs();
    }
    outputFileUri = Uri.fromFile(f);
    if (request == REQUEST_TAKE) {
      startTakePhoto();
    } else {
      Crop.pickImage(this);
    }
  }


  private void startTakePhoto() {
    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
    startActivityForResult(cameraIntent, R.id.request_take_photo);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      if (requestCode == R.id.request_take_photo || requestCode == Crop.REQUEST_PICK) {
        Uri inputUri = outputFileUri;
        if (requestCode == Crop.REQUEST_PICK) {
          inputUri = data.getData();
        }
        Crop crop = new Crop(inputUri).output(outputFileUri);
        int type = getIntent().getIntExtra(EXTRA_TYPE, TYPE_SQUARE);
        switch (type) {
          case TYPE_SQUARE:
            crop = crop.asSquare();
            break;
          case TYPE_CUSTOM:
            int width = getIntent().getIntExtra(EXTRA_WIDTH, DEFAULT_SIZE);
            int height = getIntent().getIntExtra(EXTRA_HEIGHT, DEFAULT_SIZE);
            crop = crop.withAspect(width, height);
            break;
          default:
            throw new IllegalArgumentException(String.format("type must be use %1$s.TYPE_SQUAR or %1$s.TYPE_CUSTOM"
                , getClass().getSimpleName()));
        }
        crop.start(this);
      } else if (requestCode == Crop.REQUEST_CROP) {
        data.setData(outputFileUri);
        setResult(RESULT_OK, data);
        finish();
      }
    } else {
      setResult(resultCode, data);
      finish();
    }
  }
}
