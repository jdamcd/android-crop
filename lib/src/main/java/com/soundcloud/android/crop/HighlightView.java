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

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

/*
 * Modified from version in AOSP.
 *
 * This class is used to display a highlighted cropping rectangle
 * overlayed on the image. There are two coordinate spaces in use. One is
 * image, another is screen. computeLayout() uses matrix to map from image
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
    enum HandleMode { Changing, Always, Never }

    RectF cropRect; // Image space
    Rect drawRect; // Screen space
    Matrix matrix;
    private RectF imageRect; // Image space

    private final Paint outsidePaint = new Paint();
    private final Paint outlinePaint = new Paint();
    private final Paint handlePaint = new Paint();

    private View viewContext; // View displaying image
    private boolean showThirds;
    private boolean showCircle;
    private int highlightColor;

    private ModifyMode modifyMode = ModifyMode.None;
    private HandleMode handleMode = HandleMode.Changing;
    private boolean maintainAspectRatio;
    private float initialAspectRatio;
    private float handleRadius;
    private float outlineWidth;
    private boolean isFocused;

    public HighlightView(View context) {
        viewContext = context;
        initStyles(context.getContext());
    }

    private void initStyles(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.cropImageStyle, outValue, true);
        TypedArray attributes = context.obtainStyledAttributes(outValue.resourceId, R.styleable.CropImageView);
        try {
            showThirds = attributes.getBoolean(R.styleable.CropImageView_showThirds, false);
            showCircle = attributes.getBoolean(R.styleable.CropImageView_showCircle, false);
            highlightColor = attributes.getColor(R.styleable.CropImageView_highlightColor,
                    DEFAULT_HIGHLIGHT_COLOR);
            handleMode = HandleMode.values()[attributes.getInt(R.styleable.CropImageView_showHandles, 0)];
        } finally {
            attributes.recycle();
        }
    }

    public void setup(Matrix m, Rect imageRect, RectF cropRect, boolean maintainAspectRatio) {
        matrix = new Matrix(m);

        this.cropRect = cropRect;
        this.imageRect = new RectF(imageRect);
        this.maintainAspectRatio = maintainAspectRatio;

        initialAspectRatio = this.cropRect.width() / this.cropRect.height();
        drawRect = computeLayout();

        outsidePaint.setARGB(125, 50, 50, 50);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setAntiAlias(true);
        outlineWidth = dpToPx(OUTLINE_DP);

        handlePaint.setColor(highlightColor);
        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setAntiAlias(true);
        handleRadius = dpToPx(HANDLE_RADIUS_DP);

        modifyMode = ModifyMode.None;
    }

    private float dpToPx(float dp) {
        return dp * viewContext.getResources().getDisplayMetrics().density;
    }

    protected void draw(Canvas canvas) {
        canvas.save();
        Path path = new Path();
        outlinePaint.setStrokeWidth(outlineWidth);
        if (!hasFocus()) {
            outlinePaint.setColor(Color.BLACK);
            canvas.drawRect(drawRect, outlinePaint);
        } else {
            Rect viewDrawingRect = new Rect();
            viewContext.getDrawingRect(viewDrawingRect);

            path.addRect(new RectF(drawRect), Path.Direction.CW);
            outlinePaint.setColor(highlightColor);

            if (isClipPathSupported(canvas)) {
                canvas.clipPath(path, Region.Op.DIFFERENCE);
                canvas.drawRect(viewDrawingRect, outsidePaint);
            } else {
                drawOutsideFallback(canvas);
            }

            canvas.restore();
            canvas.drawPath(path, outlinePaint);

            if (showThirds) {
                drawThirds(canvas);
            }

            if (showCircle) {
                drawCircle(canvas);
            }

            if (handleMode == HandleMode.Always ||
                    (handleMode == HandleMode.Changing && modifyMode == ModifyMode.Grow)) {
                drawHandles(canvas);
            }
        }
    }

    /*
     * Fall back to naive method for darkening outside crop area
     */
    private void drawOutsideFallback(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), drawRect.top, outsidePaint);
        canvas.drawRect(0, drawRect.bottom, canvas.getWidth(), canvas.getHeight(), outsidePaint);
        canvas.drawRect(0, drawRect.top, drawRect.left, drawRect.bottom, outsidePaint);
        canvas.drawRect(drawRect.right, drawRect.top, canvas.getWidth(), drawRect.bottom, outsidePaint);
    }

    /*
     * Clip path is broken, unreliable or not supported on:
     * - JellyBean MR1
     * - ICS & ICS MR1 with hardware acceleration turned on
     */
    @SuppressLint("NewApi")
    private boolean isClipPathSupported(Canvas canvas) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            || Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return true;
        } else {
            return !canvas.isHardwareAccelerated();
        }
    }

    private void drawHandles(Canvas canvas) {
        int xMiddle = drawRect.left + ((drawRect.right  - drawRect.left) / 2);
        int yMiddle = drawRect.top + ((drawRect.bottom - drawRect.top) / 2);

        canvas.drawCircle(drawRect.left, yMiddle, handleRadius, handlePaint);
        canvas.drawCircle(xMiddle, drawRect.top, handleRadius, handlePaint);
        canvas.drawCircle(drawRect.right, yMiddle, handleRadius, handlePaint);
        canvas.drawCircle(xMiddle, drawRect.bottom, handleRadius, handlePaint);
    }

    private void drawThirds(Canvas canvas) {
        outlinePaint.setStrokeWidth(1);
        float xThird = (drawRect.right - drawRect.left) / 3;
        float yThird = (drawRect.bottom - drawRect.top) / 3;
        
        canvas.drawLine(drawRect.left + xThird, drawRect.top,
                drawRect.left + xThird, drawRect.bottom, outlinePaint);
        canvas.drawLine(drawRect.left + xThird * 2, drawRect.top,
                drawRect.left + xThird * 2, drawRect.bottom, outlinePaint);
        canvas.drawLine(drawRect.left, drawRect.top + yThird,
                drawRect.right, drawRect.top + yThird, outlinePaint);
        canvas.drawLine(drawRect.left, drawRect.top + yThird * 2,
                drawRect.right, drawRect.top + yThird * 2, outlinePaint);
    }

    private void drawCircle(Canvas canvas) {
        outlinePaint.setStrokeWidth(1);
        canvas.drawOval(new RectF(drawRect), outlinePaint);
    }

    public void setMode(ModifyMode mode) {
        if (mode != modifyMode) {
            modifyMode = mode;
            viewContext.invalidate();
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
            moveBy(dx * (cropRect.width() / r.width()),
                   dy * (cropRect.height() / r.height()));
        } else {
            if (((GROW_LEFT_EDGE | GROW_RIGHT_EDGE) & edge) == 0) {
                dx = 0;
            }

            if (((GROW_TOP_EDGE | GROW_BOTTOM_EDGE) & edge) == 0) {
                dy = 0;
            }

            // Convert to image space before sending to growBy()
            float xDelta = dx * (cropRect.width() / r.width());
            float yDelta = dy * (cropRect.height() / r.height());
            growBy((((edge & GROW_LEFT_EDGE) != 0) ? -1 : 1) * xDelta,
                    (((edge & GROW_TOP_EDGE) != 0) ? -1 : 1) * yDelta);
        }
    }

    // Grows the cropping rectangle by (dx, dy) in image space
    void moveBy(float dx, float dy) {
        Rect invalRect = new Rect(drawRect);

        cropRect.offset(dx, dy);

        // Put the cropping rectangle inside image rectangle
        cropRect.offset(
                Math.max(0, imageRect.left - cropRect.left),
                Math.max(0, imageRect.top  - cropRect.top));

        cropRect.offset(
                Math.min(0, imageRect.right  - cropRect.right),
                Math.min(0, imageRect.bottom - cropRect.bottom));

        drawRect = computeLayout();
        invalRect.union(drawRect);
        invalRect.inset(-(int) handleRadius, -(int) handleRadius);
        viewContext.invalidate(invalRect);
    }

    // Grows the cropping rectangle by (dx, dy) in image space.
    void growBy(float dx, float dy) {
        if (maintainAspectRatio) {
            if (dx != 0) {
                dy = dx / initialAspectRatio;
            } else if (dy != 0) {
                dx = dy * initialAspectRatio;
            }
        }

        // Don't let the cropping rectangle grow too fast.
        // Grow at most half of the difference between the image rectangle and
        // the cropping rectangle.
        RectF r = new RectF(cropRect);
        if (dx > 0F && r.width() + 2 * dx > imageRect.width()) {
            dx = (imageRect.width() - r.width()) / 2F;
            if (maintainAspectRatio) {
                dy = dx / initialAspectRatio;
            }
        }
        if (dy > 0F && r.height() + 2 * dy > imageRect.height()) {
            dy = (imageRect.height() - r.height()) / 2F;
            if (maintainAspectRatio) {
                dx = dy * initialAspectRatio;
            }
        }

        r.inset(-dx, -dy);

        // Don't let the cropping rectangle shrink too fast
        final float widthCap = 25F;
        if (r.width() < widthCap) {
            r.inset(-(widthCap - r.width()) / 2F, 0F);
        }
        float heightCap = maintainAspectRatio
                ? (widthCap / initialAspectRatio)
                : widthCap;
        if (r.height() < heightCap) {
            r.inset(0F, -(heightCap - r.height()) / 2F);
        }

        // Put the cropping rectangle inside the image rectangle
        if (r.left < imageRect.left) {
            r.offset(imageRect.left - r.left, 0F);
        } else if (r.right > imageRect.right) {
            r.offset(-(r.right - imageRect.right), 0F);
        }
        if (r.top < imageRect.top) {
            r.offset(0F, imageRect.top - r.top);
        } else if (r.bottom > imageRect.bottom) {
            r.offset(0F, -(r.bottom - imageRect.bottom));
        }

        cropRect.set(r);
        drawRect = computeLayout();
        viewContext.invalidate();
    }

    // Returns the cropping rectangle in image space with specified scale
    public Rect getScaledCropRect(float scale) {
        return new Rect((int) (cropRect.left * scale), (int) (cropRect.top * scale),
                (int) (cropRect.right * scale), (int) (cropRect.bottom * scale));
    }

    // Maps the cropping rectangle from image space to screen space
    private Rect computeLayout() {
        RectF r = new RectF(cropRect.left, cropRect.top,
                            cropRect.right, cropRect.bottom);
        matrix.mapRect(r);
        return new Rect(Math.round(r.left), Math.round(r.top),
                        Math.round(r.right), Math.round(r.bottom));
    }

    public void invalidate() {
        drawRect = computeLayout();
    }

    public boolean hasFocus() {
        return isFocused;
    }

    public void setFocus(boolean isFocused) {
        this.isFocused = isFocused;
    }

}
