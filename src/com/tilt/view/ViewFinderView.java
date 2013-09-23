package com.tilt.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.tilt.R;
import com.tilt.scanner.ScannerManager;

public class ViewFinderView extends View {
	private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192,
			128, 64 };
	private static final long ANIMATION_DELAY = 80L;
	private static final int POINT_SIZE = 6;
	
	private ScannerManager scannerManager;
	private final Paint paint;
	private final int maskColor;
	private final int laserColor;
	private final int resultPointColor;
	private int scannerAlpha;

	public ViewFinderView(Context context) {
		this(context, null);
	}

	/**
	 * This constructor is used when the class is built from an XML resource
	 * 
	 * @param context
	 * @param attrs
	 */
	public ViewFinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw()
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = this.getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
	}

	public void setScannerManager(ScannerManager scannerManager) {
		this.scannerManager = scannerManager;
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (scannerManager == null) {
			return;
		}

		Rect frame = scannerManager.getFramingRect();
		if (frame == null) {
			return;
		}

		int width = canvas.getWidth();
		int height = canvas.getHeight();
		int top = frame.top;
		int bottom = frame.bottom;
		int left = frame.left;
		int right = frame.right;

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(maskColor);
		canvas.drawRect(0, 0, width, top, paint);
		canvas.drawRect(0, top, left, bottom + 1, paint);
		canvas.drawRect(right + 1, top, width, bottom + 1, paint);
		canvas.drawRect(0, bottom + 1, width, height, paint);

		// Draw a red "laser scanner" line through the middle to show decoding
		// is active
		paint.setColor(laserColor);
		paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
		scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
		int middle = frame.height() / 2 + top;
		canvas.drawRect(left + 2, middle - 1, right - 1, middle + 2, paint);

		// Request another update at the animation interval, but only repaint
		// the laser line, not the entire viewfinder mask.
		postInvalidateDelayed(ANIMATION_DELAY, left - POINT_SIZE,
				top - POINT_SIZE, right + POINT_SIZE, bottom + POINT_SIZE);
	}
}
