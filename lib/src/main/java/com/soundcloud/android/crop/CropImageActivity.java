/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.crop;

import com.soundcloud.android.crop.util.Log;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

/*
 * Modified from original in AOSP.
 */
public class CropImageActivity extends MonitoredActivity {

  private final Handler mHandler = new Handler();

  private int mAspectX;

  private int mAspectY;

  // Output image size
  private int mMaxX;

  private int mMaxY;

  private int mExifRotation;

  private Uri mSourceUri;

  private Uri mSaveUri;

  private boolean mIsSaving; // When the save button has been clicked

  private RotateBitmap mRotateBitmap;

  private CropImageView mImageView;

  private HighlightView mCrop;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.crop__activity_crop);
    initViews();

    setupFromIntent();
    if (mRotateBitmap == null) {
      finish();
      return;
    }
    startCrop();
  }

  private void initViews() {
    mImageView = (CropImageView) findViewById(R.id.crop_image);
    mImageView.mContext = this;
    mImageView.setRecycler(new ImageViewTouchBase.Recycler() {
      @Override
      public void recycle(Bitmap b) {
        b.recycle();
        System.gc();
      }
    });

    findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });

    findViewById(R.id.btn_done).setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        onSaveClicked();
      }
    });
  }

  private void setupFromIntent() {
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    if (extras != null) {
      mAspectX = extras.getInt(Crop.Extra.ASPECT_X);
      mAspectY = extras.getInt(Crop.Extra.ASPECT_Y);
      mMaxX = extras.getInt(Crop.Extra.MAX_X);
      mMaxY = extras.getInt(Crop.Extra.MAX_Y);
      mSaveUri = extras.getParcelable(MediaStore.EXTRA_OUTPUT);
    }
    mSourceUri = intent.getData();
    if (mSourceUri != null) {
      File file = CropUtil.getFromMediaUri(getContentResolver(), mSourceUri);
      if (file == null) {
        setResult(Const.RESULT_CROPPED_NOT_IMAGE_FILE);
        finish();
        return;
      }
      mExifRotation = CropUtil.getExifRotation(file);
      Log.e("rotation:" + mExifRotation);
      InputStream is = null;
      try {
        DisplayMetrics display = getResources().getDisplayMetrics();
        Bitmap scaledBmp = CropUtil.decodeUri(this,
            mSourceUri, display.widthPixels, display.heightPixels);
        mRotateBitmap = new RotateBitmap(scaledBmp, mExifRotation);
      } catch (OutOfMemoryError e) {
        Log.e("OOM while reading picture: " + e.getMessage(), e);
        setResultException(e);
      } finally {
        CropUtil.closeSilently(is);
      }
    }
  }

  private void startCrop() {
    if (isFinishing()) {
      return;
    }
    mImageView.setImageRotateBitmapResetBase(mRotateBitmap, true);
    CropUtil.startBackgroundJob(this, null, getResources().getString(R.string.crop__wait),
        new Runnable() {
          public void run() {
            final CountDownLatch latch = new CountDownLatch(1);
            mHandler.post(new Runnable() {
              public void run() {
                if (mImageView.getScale() == 1F) {
                  mImageView.center(true, true);
                }
                latch.countDown();
              }
            });
            try {
              latch.await();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            new Cropper().crop();
          }
        }, mHandler
    );
  }

  private class Cropper {

    private void makeDefault() {
      if (mRotateBitmap == null) {
        return;
      }

      HighlightView hv = new HighlightView(mImageView);
      final int width = mRotateBitmap.getWidth();
      final int height = mRotateBitmap.getHeight();

      Rect imageRect = new Rect(0, 0, width, height);

      // Make the default size about 4/5 of the width or height
      int cropWidth = Math.min(width, height) * 4 / 5;
      @SuppressWarnings("SuspiciousNameCombination")
      int cropHeight = cropWidth;

      if (mAspectX != 0 && mAspectY != 0) {
        if (mAspectX > mAspectY) {
          cropHeight = cropWidth * mAspectY / mAspectX;
        } else {
          cropWidth = cropHeight * mAspectX / mAspectY;
        }
      }

      int x = (width - cropWidth) / 2;
      int y = (height - cropHeight) / 2;

      RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
      hv.setup(mImageView.getUnrotatedMatrix(), imageRect, cropRect, mAspectX != 0 && mAspectY != 0);
      mImageView.add(hv);
    }

    public void crop() {
      mHandler.post(new Runnable() {
        public void run() {
          makeDefault();
          mImageView.invalidate();
          if (mImageView.mHighlightViews.size() == 1) {
            mCrop = mImageView.mHighlightViews.get(0);
            mCrop.setFocus(true);
          }
        }
      });
    }
  }

  /*
   * TODO
   * This should use the decode/crop/encode single step API so that the whole
   * (possibly large) Bitmap doesn't need to be read into memory
   */
  private void onSaveClicked() {
    if (mCrop == null || mIsSaving) {
      return;
    }
    mIsSaving = true;

    Bitmap croppedImage = null;
    Rect r = mCrop.getCropRect();
    int width = r.width();
    int height = r.height();

    int outWidth = width, outHeight = height;
    if (mMaxX > 0 && mMaxY > 0 && (width > mMaxX || height > mMaxY)) {
      float ratio = (float) width / (float) height;
      if ((float) mMaxX / (float) mMaxY > ratio) {
        outHeight = mMaxY;
        outWidth = (int) ((float) mMaxY * ratio + .5f);
      } else {
        outWidth = mMaxX;
        outHeight = (int) ((float) mMaxX / ratio + .5f);
      }
    }

    if (mRotateBitmap != null) {
      croppedImage = inMemoryCrop(mRotateBitmap, croppedImage, r, width, height, outWidth, outHeight);
      if (croppedImage != null) {
        mImageView.setImageBitmapResetBase(croppedImage, true);
        mImageView.center(true, true);
        mImageView.mHighlightViews.clear();
      }
    }
    saveImage(croppedImage);
  }

  private void saveImage(Bitmap croppedImage) {
    if (croppedImage != null) {
      final Bitmap b = croppedImage;
      CropUtil.startBackgroundJob(this, null, getResources().getString(R.string.crop__saving),
          new Runnable() {
            public void run() {
              saveOutput(b);
            }
          }, mHandler
      );
    } else {
      finish();
    }
  }

  private Bitmap inMemoryCrop(RotateBitmap rotateBitmap, Bitmap croppedImage, Rect r,
      int width, int height, int outWidth, int outHeight) {
    // In-memory crop means potential OOM errors,
    // but we have no choice as we can't selectively decode a bitmap with this API level
    System.gc();

    try {
      croppedImage = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.RGB_565);

      Canvas canvas = new Canvas(croppedImage);
      RectF dstRect = new RectF(0, 0, width, height);

      Matrix m = new Matrix();
      m.setRectToRect(new RectF(r), dstRect, Matrix.ScaleToFit.FILL);
      m.preConcat(rotateBitmap.getRotateMatrix());
      canvas.drawBitmap(rotateBitmap.getBitmap(), m, null);

    } catch (OutOfMemoryError e) {
      Log.e("Error cropping picture: " + e.getMessage(), e);
      System.gc();
    }

    // Release bitmap memory as soon as possible
    clearImageView();
    return croppedImage;
  }

  private void clearImageView() {
    mImageView.clear();
    if (mRotateBitmap != null) {
      mRotateBitmap.recycle();
    }
    System.gc();
  }

  private void saveOutput(Bitmap croppedImage) {
    if (mSaveUri != null) {
      OutputStream outputStream = null;
      try {
        outputStream = getContentResolver().openOutputStream(mSaveUri);
        if (outputStream != null) {
          croppedImage.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        }

      } catch (IOException e) {
        setResultException(e);
        Log.e("Cannot open file: " + mSaveUri, e);
      } finally {
        CropUtil.closeSilently(outputStream);
      }

      CropUtil.copyExifRotation(
          CropUtil.getFromMediaUri(getContentResolver(), mSourceUri),
          CropUtil.getFromMediaUri(getContentResolver(), mSaveUri)
      );

      setResultUri(mSaveUri);
    }

    final Bitmap b = croppedImage;
    mHandler.post(new Runnable() {
      public void run() {
        mImageView.clear();
        b.recycle();
      }
    });

    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mRotateBitmap != null) {
      mRotateBitmap.recycle();
    }
  }

  @Override
  public boolean onSearchRequested() {
    return false;
  }

  public boolean isSaving() {
    return mIsSaving;
  }

  private void setResultUri(Uri uri) {
    setResult(RESULT_OK, new Intent().putExtra(MediaStore.EXTRA_OUTPUT, uri));
  }

  private void setResultException(Throwable throwable) {
    setResult(Crop.RESULT_ERROR, new Intent().putExtra(Crop.Extra.ERROR, throwable));
  }

}

