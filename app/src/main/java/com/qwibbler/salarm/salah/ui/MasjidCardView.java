package com.qwibbler.salarm.salah.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qwibbler.salarm.salah.MasjidCardData;
import com.qwibbler.salarm.salah.MasjidCardRenderer;

public class MasjidCardView extends View {

    private MasjidCardData data;
    private MasjidCardRenderer renderer;

    public MasjidCardView(Context context) {
        super(context);
        init();
    }

    public MasjidCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MasjidCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        renderer = new MasjidCardRenderer(getContext());
    }

    public void setData(MasjidCardData data) {
        this.data = data;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        float vWidth = renderer.getVirtualWidth(data);
        float vHeight = renderer.getVirtualHeight(data);
        float targetRatio = vWidth / vHeight;

        int width, height;

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            width = widthSize;
            height = heightSize;
        } else if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
            height = (int) (width / targetRatio);
        } else if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
            width = (int) (height * targetRatio);
        } else {
            width = widthSize > 0 ? widthSize : 600;
            height = (int) (width / targetRatio);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (data != null) {
            float vWidth = renderer.getVirtualWidth(data);
            float vHeight = renderer.getVirtualHeight(data);
            MasjidCardRenderer.CanvasDrawTarget target = new MasjidCardRenderer.CanvasDrawTarget(canvas, getWidth(), getHeight(), vWidth, vHeight);
            renderer.render(target, data);
        }
    }
}
