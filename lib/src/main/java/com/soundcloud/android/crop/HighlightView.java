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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.TypedValue;
import android.view.View;

/*
 * Modified from version in AOSP.
 *
 * This class is used to display a highlighted cropping rectangle
 * overlayed on the image. There are two coordinate spaces in use. One is
 * image, another is screen. computeLayout() uses mMatrix to map from image
 * space to screen space.
 */
class HighlightView {

    public static final int GROW_NONE        = (1 << 0);
    public static final int GROW_LEFT_EDGE   = (1 << 1);
    public static final int GROW_RIGHT_EDGE  = (1 << 2);
    public static final int GROW_TOP_EDGE    = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE             = (1 << 5);

    private static final int DEFAULT_HIGHLIGHT_COLOR = 0xFF33B5E5;
    private static final float HANDLE_RADIUS_DP = 12f;
    private static final float OUTLINE_DP = 2f;

    enum ModifyMode { None, Move, Grow }

    RectF mCropRect; // Image space
    Rect mDrawRect; // Screen space
    Matrix mMatrix;
    private RectF mImageRect; // Image space

    private final Paint mFocusPaint = new Paint();
    private final Paint mNoFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();
    private final Paint mHandlePaint = new Paint();

    private View mContext; // View displaying image
    private boolean mShowThirds;
    private int mHighlightColor;

    private ModifyMode mMode = ModifyMode.None;
    private boolean mMaintainAspectRatio;
    private float mInitialAspectRatio;
    private float mHandleRadius;
    private float mOutlineWidth;
    private boolean mIsFocused;

    public HighlightView(View context) {
        mContext = context;
        initStyles(context.getContext());
    }

    private void initStyles(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.cropImageStyle, outValue, true);
        TypedArray attributes = context.obtainStyledAttributes(outValue.resourceId, R.styleable.CropImageView);
        try {
            mShowThirds = attributes.getBoolean(R.styleable.CropImageView_showThirds, false);
            mHighlightColor = attributes.getColor(R.styleable.CropImageView_highlightColor,
                    DEFAULT_HIGHLIGHT_COLOR);
        } finally {
            attributes.recycle();
        }
    }

    public void setup(Matrix m, Rect imageRect, RectF cropRect, boolean maintainAspectRatio) {
        mMatrix = new Matrix(m);

        mCropRect = cropRect;
        mImageRect = new RectF(imageRect);
        mMaintainAspectRatio = maintainAspectRatio;

        mInitialAspectRatio = mCropRect.width() / mCropRect.height();
        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 50, 50, 50);
        mNoFocusPaint.setARGB(125, 50, 50, 50);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);
        mOutlineWidth = dpToPx(OUTLINE_DP);

        mHandlePaint.setColor(mHighlightColor);
        mHandlePaint.setStyle(Paint.Style.FILL);
        mHandlePaint.setAntiAlias(true);
        mHandleRadius = dpToPx(HANDLE_RADIUS_DP);

        mMode = ModifyMode.None;
    }

    private float dpToPx(float dp) {
        return dp * mContext.getResources().getDisplayMetrics().density;
    }

    protected void draw(Canvas canvas) {
        canvas.save();
        Path path = new Path();
        mOutlinePaint.setStrokeWidth(mOutlineWidth);
        if (!hasFocus()) {
            mOutlinePaint.setColor(Color.BLACK);
            canvas.drawRect(mDrawRect, mOutlinePaint);
        } else {
            Rect viewDrawingRect = new Rect();
            mContext.getDrawingRect(viewDrawingRect);

            path.addRect(new RectF(mDrawRect), Path.Direction.CW);
            mOutlinePaint.setColor(mHighlightColor);

            canvas.clipPath(path, Region.Op.DIFFERENCE);
            canvas.drawRect(viewDrawingRect, hasFocus() ? mFocusPaint : mNoFocusPaint);

            canvas.restore();
            canvas.drawPath(path, mOutlinePaint);

            if (mShowThirds) {
                drawThirds(canvas);
            }
            if (mMode == ModifyMode.Grow) {
                drawHandles(canvas);
            }
        }
    }

    private void drawHandles(Canvas canvas) {
        int xMiddle = mDrawRect.left + ((mDrawRect.right  - mDrawRect.left) / 2);
        int yMiddle = mDrawRect.top + ((mDrawRect.bottom - mDrawRect.top) / 2);

        canvas.drawCircle(mDrawRect.left, yMiddle, mHandleRadius, mHandlePaint);
        canvas.drawCircle(xMiddle, mDrawRect.top, mHandleRadius, mHandlePaint);
        canvas.drawCircle(mDrawRect.right, yMiddle, mHandleRadius, mHandlePaint);
        canvas.drawCircle(xMiddle, mDrawRect.bottom, mHandleRadius, mHandlePaint);
    }

    private void drawThirds(Canvas canvas) {
        mOutlinePaint.setStrokeWidth(1);
        float xThird = (mDrawRect.right - mDrawRect.left) / 3;
        float yThird = (mDrawRect.bottom - mDrawRect.top) / 3;
        
        canvas.drawLine(mDrawRect.left + xThird, mDrawRect.top,
                mDrawRect.left + xThird, mDrawRect.bottom, mOutlinePaint);
        canvas.drawLine(mDrawRect.left + xThird * 2, mDrawRect.top,
                mDrawRect.left + xThird * 2, mDrawRect.bottom, mOutlinePaint);
        canvas.drawLine(mDrawRect.left, mDrawRect.top + yThird,
                mDrawRect.right, mDrawRect.top + yThird, mOutlinePaint);
        canvas.drawLine(mDrawRect.left, mDrawRect.top + yThird * 2,
                mDrawRect.right, mDrawRect.top + yThird * 2, mOutlinePaint);
    }

    public void setMode(ModifyMode mode) {
        if (mode != mMode) {
            mMode = mode;
            mContext.invalidate();
        }
    }

    // Determines which edges are hit by touching at (x, y)
    public int getHit(float x, float y) {
        Rect r = computeLayout();
        final float hysteresis = 20F;
        int retval = GROW_NONE;

        // verticalCheck makes sure the position is between the top and
        // the bottom edge (with some tolerance). Similar for horizCheck.
        boolean verticalCheck = (y >= r.top - hysteresis)
                && (y < r.bottom + hysteresis);
        boolean horizCheck = (x >= r.left - hysteresis)
                && (x < r.right + hysteresis);

        // Check whether the position is near some edge(s)
        if ((Math.abs(r.left - x)     < hysteresis)  &&  verticalCheck) {
            retval |= GROW_LEFT_EDGE;
        }
        if ((Math.abs(r.right - x)    < hysteresis)  &&  verticalCheck) {
            retval |= GROW_RIGHT_EDGE;
        }
        if ((Math.abs(r.top - y)      < hysteresis)  &&  horizCheck) {
            retval |= GROW_TOP_EDGE;
        }
        if ((Math.abs(r.bottom - y)   < hysteresis)  &&  horizCheck) {
            retval |= GROW_BOTTOM_EDGE;
        }

        // Not near any edge but inside the rectangle: move
        if (retval == GROW_NONE && r.contains((int) x, (int) y)) {
            retval = MOVE;
        }
        return retval;
    }

    // Handles motion (dx, dy) in screen space.
    // The "edge" parameter specifies which edges the user is dragging.
    void handleMotion(int edge, float dx, float dy) {
        Rect r = computeLayout();
        if (edge == MOVE) {
            // Convert to image space before sending to moveBy()
            moveBy(dx * (mCropRect.width() / r.width()),
                   dy * (mCropRect.height() / r.height()));
        } else {
            if (((GROW_LEFT_EDGE | GROW_RIGHT_EDGE) & edge) == 0) {
                dx = 0;
            }

            if (((GROW_TOP_EDGE | GROW_BOTTOM_EDGE) & edge) == 0) {
                dy = 0;
            }

            // Convert to image space before sending to growBy()
            float xDelta = dx * (mCropRect.width() / r.width());
            float yDelta = dy * (mCropRect.height() / r.height());
            growBy((((edge & GROW_LEFT_EDGE) != 0) ? -1 : 1) * xDelta,
                    (((edge & GROW_TOP_EDGE) != 0) ? -1 : 1) * yDelta);
        }
    }

    // Grows the cropping rectangle by (dx, dy) in image space
    void moveBy(float dx, float dy) {
        Rect invalRect = new Rect(mDrawRect);

        mCropRect.offset(dx, dy);

        // Put the cropping rectangle inside image rectangle
        mCropRect.offset(
                Math.max(0, mImageRect.left - mCropRect.left),
                Math.max(0, mImageRect.top  - mCropRect.top));

        mCropRect.offset(
                Math.min(0, mImageRect.right  - mCropRect.right),
                Math.min(0, mImageRect.bottom - mCropRect.bottom));

        mDrawRect = computeLayout();
        invalRect.union(mDrawRect);
        invalRect.inset(-10, -10);
        mContext.invalidate(invalRect);
    }

    // Grows the cropping rectangle by (dx, dy) in image space.
    void growBy(float dx, float dy) {
        if (mMaintainAspectRatio) {
            if (dx != 0) {
                dy = dx / mInitialAspectRatio;
            } else if (dy != 0) {
                dx = dy * mInitialAspectRatio;
            }
        }

        // Don't let the cropping rectangle grow too fast.
        // Grow at most half of the difference between the image rectangle and
        // the cropping rectangle.
        RectF r = new RectF(mCropRect);
        if (dx > 0F && r.width() + 2 * dx > mImageRect.width()) {
            dx = (mImageRect.width() - r.width()) / 2F;
            if (mMaintainAspectRatio) {
                dy = dx / mInitialAspectRatio;
            }
        }
        if (dy > 0F && r.height() + 2 * dy > mImageRect.height()) {
            dy = (mImageRect.height() - r.height()) / 2F;
            if (mMaintainAspectRatio) {
                dx = dy * mInitialAspectRatio;
            }
        }

        r.inset(-dx, -dy);

        // Don't let the cropping rectangle shrink too fast
        final float widthCap = 25F;
        if (r.width() < widthCap) {
            r.inset(-(widthCap - r.width()) / 2F, 0F);
        }
        float heightCap = mMaintainAspectRatio
                ? (widthCap / mInitialAspectRatio)
                : widthCap;
        if (r.height() < heightCap) {
            r.inset(0F, -(heightCap - r.height()) / 2F);
        }

        // Put the cropping rectangle inside the image rectangle
        if (r.left < mImageRect.left) {
            r.offset(mImageRect.left - r.left, 0F);
        } else if (r.right > mImageRect.right) {
            r.offset(-(r.right - mImageRect.right), 0F);
        }
        if (r.top < mImageRect.top) {
            r.offset(0F, mImageRect.top - r.top);
        } else if (r.bottom > mImageRect.bottom) {
            r.offset(0F, -(r.bottom - mImageRect.bottom));
        }

        mCropRect.set(r);
        mDrawRect = computeLayout();
        mContext.invalidate();
    }

    // Returns the cropping rectangle in image space
    public Rect getCropRect() {
        return new Rect((int) mCropRect.left, (int) mCropRect.top,
                        (int) mCropRect.right, (int) mCropRect.bottom);
    }

    // Maps the cropping rectangle from image space to screen space
    private Rect computeLayout() {
        RectF r = new RectF(mCropRect.left, mCropRect.top,
                            mCropRect.right, mCropRect.bottom);
        mMatrix.mapRect(r);
        return new Rect(Math.round(r.left), Math.round(r.top),
                        Math.round(r.right), Math.round(r.bottom));
    }

    public void invalidate() {
        mDrawRect = computeLayout();
    }

    public boolean hasFocus() {
        return mIsFocused;
    }

    public void setFocus(boolean isFocused) {
        mIsFocused = isFocused;
    }

}
