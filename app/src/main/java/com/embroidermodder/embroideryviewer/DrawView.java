package com.embroidermodder.embroideryviewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class DrawView extends View {
    Tool tool = new ToolPan();

    private final int _height;
    private final int _width;

    Paint _paint = new Paint();
    Pattern pattern;

    private RectF viewPort;

    Matrix viewMatrix;
    Matrix invertMatrix;

    public DrawView(Context context, Pattern pattern) {
        super(context);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        _height = metrics.heightPixels;
        _width = metrics.widthPixels;
        setPattern(pattern);
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;

        this.pattern = pattern.getPositiveCoordinatePattern();

        if (pattern.getStitchBlocks().isEmpty()) {
            viewPort = new RectF(0,0,_width,_height);
        }
        else {
            viewPort = pattern.calculateBoundingBox();
        }
        calculateViewMatrixFromPort();
        setPaintScale();
    }


    public void scale(double deltascale, float x, float y) {
        viewMatrix.postScale((float)deltascale,(float)deltascale,x,y);
        calculateViewPortFromMatrix();

        setPaintScale();
    }

    public void pan(float dx, float dy) {
        viewMatrix.postTranslate(dx,dy);
        calculateViewPortFromMatrix();
    }

    private float getScale() {
        return Math.min(_height/viewPort.height(), _width/viewPort.width());

    }

    public void setPaintScale() {
        float scale = getScale();
        _paint.setStrokeWidth(scale/9.0f);
        //This will scale with the scale automatically at 1.
    }

    public void calculateViewMatrixFromPort() {
        float scale = Math.min(_height/viewPort.height(), _width/viewPort.width());
        viewMatrix = new Matrix();
        if (scale != 0) {
            viewMatrix.postTranslate(-viewPort.left, -viewPort.top);
            viewMatrix.postScale(scale, scale);
        }
        calculateInvertMatrix();
    }

    public void calculateViewPortFromMatrix() {
        float[] positions = new float[] {
                0,0,
                _width,_height
        };
        calculateInvertMatrix();
        invertMatrix.mapPoints(positions);
        viewPort.set(positions[0],positions[1],positions[2],positions[3]);
    }

    public void calculateInvertMatrix() {
        invertMatrix = new Matrix(viewMatrix);
        invertMatrix.invert(invertMatrix);
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //anything happening with event here is the X Y of the raw screen event.
        if (tool.rawTouch(this,event)) return true;
        if (invertMatrix != null) event.transform(invertMatrix);
        //anything happening with event now deals with the screen space.
        return tool.touch(this,event);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.save();
        if (viewMatrix != null) canvas.setMatrix(viewMatrix);
        for(StitchBlock stitchBlock : pattern.getStitchBlocks()){
            stitchBlock.draw(canvas,_paint);
        }
        canvas.restore();
    }

    public String getStatistics() {
        return pattern.getStatistics();
    }

}
