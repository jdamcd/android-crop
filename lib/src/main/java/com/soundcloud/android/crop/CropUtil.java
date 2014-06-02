/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/*
 * Modified from original in AOSP.
 */
class CropUtil {

  private static final String SCHEME_FILE = "file";

  private static final String SCHEME_CONTENT = "content";

  public static void closeSilently(Closeable c) {
    if (c == null) {
      return;
    }
    try {
      c.close();
    } catch (Throwable t) {
      // Do nothing
    }
  }

  public static int getExifRotation(File imageFile) {
    if (imageFile == null) {
      return 0;
    }
    try {
      ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
      // We only recognize a subset of orientation tag values
      switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          return 90;
        case ExifInterface.ORIENTATION_ROTATE_180:
          return 180;
        case ExifInterface.ORIENTATION_ROTATE_270:
          return 270;
        default:
          return ExifInterface.ORIENTATION_UNDEFINED;
      }
    } catch (IOException e) {
      Log.e("Error getting Exif data", e);
      return 0;
    }
  }

  public static boolean copyExifRotation(File sourceFile, File destFile) {
    if (sourceFile == null || destFile == null) {
      return false;
    }
    try {
      ExifInterface exifSource = new ExifInterface(sourceFile.getAbsolutePath());
      ExifInterface exifDest = new ExifInterface(destFile.getAbsolutePath());
      exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, exifSource.getAttribute(ExifInterface.TAG_ORIENTATION));
      exifDest.saveAttributes();
      return true;
    } catch (IOException e) {
      Log.e("Error copying Exif data", e);
      return false;
    }
  }

  public static File getFromMediaUri(ContentResolver resolver, Uri uri) {
    if (uri == null) {
      return null;
    }

    if (SCHEME_FILE.equals(uri.getScheme())) {
      return new File(uri.getPath());
    } else if (SCHEME_CONTENT.equals(uri.getScheme())) {
      final String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
      Cursor cursor = null;
      try {
        cursor = resolver.query(uri, filePathColumn, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
          final int columnIndex = (uri.toString().startsWith("content://com.google.android.gallery3d")) ?
              cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME) :
              cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
          // Picasa image on newer devices with Honeycomb and up
          if (columnIndex != -1) {
            String filePath = cursor.getString(columnIndex);
            if (!TextUtils.isEmpty(filePath)) {
              return new File(filePath);
            }
          }
        }
      } catch (SecurityException ignored) {
        // Nothing we can do
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
    return null;
  }

  public static void startBackgroundJob(MonitoredActivity activity,
      String title, String message, Runnable job, Handler handler) {
    // Make the progress dialog uncancelable, so that we can gurantee
    // the thread will be done before the activity getting destroyed
    ProgressDialog dialog = ProgressDialog.show(
        activity, title, message, true, false);
    new Thread(new BackgroundJob(activity, job, dialog, handler)).start();
  }

  private static class BackgroundJob extends MonitoredActivity.LifeCycleAdapter implements Runnable {

    private final MonitoredActivity mActivity;

    private final ProgressDialog mDialog;

    private final Runnable mJob;

    private final Handler mHandler;

    private final Runnable mCleanupRunner = new Runnable() {
      public void run() {
        mActivity.removeLifeCycleListener(BackgroundJob.this);
        if (mDialog.getWindow() != null) {
          mDialog.dismiss();
        }
      }
    };

    public BackgroundJob(MonitoredActivity activity, Runnable job,
        ProgressDialog dialog, Handler handler) {
      mActivity = activity;
      mDialog = dialog;
      mJob = job;
      mActivity.addLifeCycleListener(this);
      mHandler = handler;
    }

    public void run() {
      try {
        mJob.run();
      } finally {
        mHandler.post(mCleanupRunner);
      }
    }

    @Override
    public void onActivityDestroyed(MonitoredActivity activity) {
      // We get here only when the onDestroyed being called before
      // the mCleanupRunner. So, run it now and remove it from the queue
      mCleanupRunner.run();
      mHandler.removeCallbacks(mCleanupRunner);
    }

    @Override
    public void onActivityStopped(MonitoredActivity activity) {
      mDialog.hide();
    }

    @Override
    public void onActivityStarted(MonitoredActivity activity) {
      mDialog.show();
    }
  }

  /**
   * Decode and rotate a new Bitmap from a file. The size of the decoded bitmap is limited by the
   * given amount of pixels.
   */
  public static Bitmap decodeUri(Context context, Uri uri, int width, int height) {
    BitmapFactory.Options o = new BitmapFactory.Options();
    // Decode image size
    o.inJustDecodeBounds = true;
    try {
      decodeBitmapFromStream(context.getContentResolver().openInputStream(uri), null, o);
    } catch (OutOfMemoryError e) {
      return null;
    } catch (Exception e) {
      return null;
    }

    // Find the correct scale value, should be the power of 2
    int widthTmp = o.outWidth, heightTmp = o.outHeight;
    int scale = 1;
    // Math.max(width_tmp / width, height_tmp / height);
    while (widthTmp > width && heightTmp > height) {
      widthTmp >>= 1;
      heightTmp >>= 1;
      scale <<= 1;
    }

    Bitmap bmp = null;
    if (scale > 1) {
      // Decode with inSampleSize
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = scale;
      o2.inScaled = false;
      o2.inDither = false;
      o2.inPurgeable = true;
      try {
        bmp = decodeBitmapFromStream(context.getContentResolver().openInputStream(uri), null, o2);
      } catch (Exception e) {

      }
    } else {
      BitmapFactory.Options o3 = new BitmapFactory.Options();
      o3.inPurgeable = true;
      try {
        bmp = decodeBitmapFromStream(context.getContentResolver().openInputStream(uri), null, o3);
      } catch (Exception e) {
      }
    }
    return bmp;
  }

  /**
   * Rotate and return a new bitmap.
   */
  public static Bitmap rotateBitmap(Bitmap bmp, int degrees) {
    if (bmp == null || degrees % 360 == 0) {
      return bmp;
    }
    int w = bmp.getWidth();
    int h = bmp.getHeight();
    try {
      Matrix mtx = new Matrix();
      mtx.postRotate(degrees, w >> 1, h >> 1);
      return Bitmap.createBitmap(bmp, 0, 0, w, h, mtx, true);
    } catch (OutOfMemoryError e) {
      return bmp;
    } catch (Exception e) {
      return bmp;
    }
  }

  /**
   * Decode a new Bitmap from an InputStream then close the stream. This InputStream was obtained
   * from resources, which we pass to be able to scale the bitmap accordingly.
   *
   * @param is         Decode a new Bitmap from an InputStream. This InputStream was obtained from
   *                   resources, which we pass to be able to scale the bitmap accordingly.
   * @param outPadding Decode a new Bitmap from an InputStream. This InputStream was obtained from
   *                   resources, which we pass to be able to scale the bitmap accordingly.
   * @param options    Decode a new Bitmap from an InputStream. This InputStream was obtained from
   *                   resources, which we pass to be able to scale the bitmap accordingly.
   * @return Decode a new Bitmap from an InputStream. This InputStream was obtained from
   * resources, which we pass to be able to scale the bitmap accordingly.
   * @throws java.io.IOException If an error occurs when closing the given {@link java.io.InputStream}.
   */
  public static Bitmap decodeBitmapFromStream(InputStream is, Rect outPadding, BitmapFactory.Options options)
      throws IOException {
    try {
      return BitmapFactory.decodeStream(is, outPadding, options);
    } finally {
      try {
        is.close();
      } catch (Exception e) {

      }
    }
  }
}
