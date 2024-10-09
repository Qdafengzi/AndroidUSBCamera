package com.gemlightbox.core.logo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

@SuppressLint("AppCompatCustomView")
public class LogoImageView extends ImageView {

    public Bitmap logoBitmap;

    public LogoImageView(Context context) {
        super(context);
    }

    public LogoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LogoImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }
}
