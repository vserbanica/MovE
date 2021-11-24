package com.example.owncameraapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;

public class BoxBorderText {
    private Paint interiorPaint;
    private Paint exteriorPaint;
    private final float DEFAUT_TEXT_SIZE = 16;

    private float textSize;

    /**
     * Constructor
     */
    public BoxBorderText() {
        textSize = DEFAUT_TEXT_SIZE;
        setPaints();
    }

    /**
     * Unused constructor
     */
    public BoxBorderText(float text_size) {
        textSize = text_size;
        setPaints();
    }

    /**
     * Init Paint parrameters
     */
    private void setPaints() {
        interiorPaint = new Paint();
        interiorPaint.setTextSize(textSize);
        interiorPaint.setColor(Color.WHITE);
        interiorPaint.setStyle(Paint.Style.FILL);
        interiorPaint.setAntiAlias(false);
        interiorPaint.setAlpha(255);

        exteriorPaint = new Paint();
        exteriorPaint.setTextSize(textSize);
        exteriorPaint.setColor(Color.BLACK);
        exteriorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        exteriorPaint.setStrokeWidth(textSize / 8);
        exteriorPaint.setAntiAlias(false);
        exteriorPaint.setAlpha(255);
    }

    /**
     * Draws the specified text in the corner of the rectangle
     */
    public void drawText(final Canvas canvas, final float posX, final float posY, final String text) {
        canvas.drawText(text, posX, posY, exteriorPaint);
        canvas.drawText(text, posX, posY, interiorPaint);
    }

    /**
     * Draws the specified text in the corner of the rectangle
     */
    public void drawText(
            final Canvas canvas, final float posX, final float posY, final String text, Paint bgPaint) {

        float width = exteriorPaint.measureText(text);
        float textSize = exteriorPaint.getTextSize();
        Paint paint = new Paint(bgPaint);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(160);
        canvas.drawRect(posX, (posY + (int) (textSize)), (posX + (int) (width)), posY, paint);

        canvas.drawText(text, posX, (posY + textSize), interiorPaint);
    }
}
