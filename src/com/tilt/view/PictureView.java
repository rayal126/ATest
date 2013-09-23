package com.tilt.view;

import org.opencv.core.Point;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class PictureView extends ImageView {
	public static final int STATE_NOT_DRAWABLE = -1;
	public static final int STATE_TILT = 0;
	public static final int STATE_CUT = 1;
	
	private int curState = STATE_NOT_DRAWABLE;
	private Paint[] paints;
	
	private Context context;
	private Point upperLeftPoint;
	private Point bottomRightPoint;
	private int screenWidth;
	private int screenHeight;
	private ImageBoundary boundary;
	private boolean isUpdated;
	private Bitmap srcPicture;
	
	public PictureView(Context context) {
		this(context, null);
	}
	
	public PictureView(Context context, AttributeSet attr) {  
        super(context, attr);  
        this.context = context;
        this.initPictureView();
    }  
	
	/**
	 * initializes the picture view
	 */
	private void initPictureView() {
		upperLeftPoint = new Point(0.0, 0.0);
		bottomRightPoint = new Point(0.0, 0.0);
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		boundary = new ImageBoundary();
		isUpdated = true;
		
		paints = new Paint[2];
		paints[STATE_TILT] = new Paint();
		paints[STATE_TILT].setColor(Color.GREEN);
		paints[STATE_TILT].setAntiAlias(true);
		paints[STATE_TILT].setStyle(Paint.Style.STROKE);
		paints[STATE_TILT].setStrokeWidth(2);
		paints[STATE_CUT] = new Paint();
		paints[STATE_CUT].setColor(Color.RED);
		paints[STATE_CUT].setAntiAlias(true);
		paints[STATE_CUT].setStyle(Paint.Style.STROKE);
		paints[STATE_CUT].setStrokeWidth(2);
		
		// records the positions of the upper-left point and the bottom-right point
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				int count = event.getPointerCount();
				if (count == 2) {
					float x1 = event.getX(0);
					float x2 = event.getX(1);
					float y1 = event.getY(0);
					float y2 = event.getY(1);
					setPositioin(x1, x2, y1, y2);
					invalidate();
				}
				
				return true;
			}
		});
	}
	
	/**
	 * update the boundary of the iamge shown by the picture view
	 */
	private void updateImageBoundary() {
		// calculate the positions of the selected point in the source image
		Matrix matrix = this.getImageMatrix();
		Rect rect = this.getDrawable().getBounds();
		float[] values = new float[9];
		matrix.getValues(values);
		boundary.top = values[5];
		boundary.bottom = values[5]+rect.height()*values[0];
		boundary.left = values[2];
		boundary.right = values[2]+rect.width()*values[0];
		isUpdated = false;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (curState != STATE_NOT_DRAWABLE) {
			canvas.drawRect((float)upperLeftPoint.x, (float)upperLeftPoint.y, (float)bottomRightPoint.x, (float)bottomRightPoint.y, paints[curState]);
		}
	}
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		this.srcPicture = bm;
	}
	/**
	 * Set the corner of the drawing rectange 
	 */
	public void setPositioin(float x1, float x2, float y1, float y2) {
		if (x1 < x2) {
			upperLeftPoint.x = x1;
			bottomRightPoint.x = x2;
		} else {
			upperLeftPoint.x = x2;
			bottomRightPoint.x = x1;
		} 
		
		if (y1 < y2) {
			upperLeftPoint.y = y1;
			bottomRightPoint.y = y2;
		} else {
			upperLeftPoint.y = y2;
			bottomRightPoint.y = y1;
		}
		
		if (isUpdated) {
			updateImageBoundary();
		}
		
		double left = (boundary.getLeftBoundary() > 0) ? boundary.getLeftBoundary() : 0;
		double right = (boundary.getRightBoundary() < screenWidth) ? boundary.getRightBoundary() : screenWidth;
		double top = (boundary.getTopBoundary() > 0) ? boundary.getTopBoundary() : 0;
		double bottom = (boundary.getBottomBoundary() < screenHeight) ? boundary.getBottomBoundary() : screenHeight;
		
		if (upperLeftPoint.x < left) {
			upperLeftPoint.x = left;
		}
		if (upperLeftPoint.y < top) {
			upperLeftPoint.y = top;
		}
		if (bottomRightPoint.x > right) {
			bottomRightPoint.x = right;
		}
		if (bottomRightPoint.y > bottom) {
			bottomRightPoint.y = bottom;
		}
	}
	
	/**
	 * sets the state of hte PictureView
	 * @param state STATE_NOT_DRAWABLE, STATE_TILT, STATE_CUT
	 */
	public void setState(int state) {
		if ((state == STATE_NOT_DRAWABLE) || (state == STATE_TILT) || (state == STATE_CUT)) {
			this.curState = state;
			upperLeftPoint.x = 0;
			upperLeftPoint.y = 0;
			bottomRightPoint.x = 0;
			bottomRightPoint.y = 0;
			invalidate();
		}
	}
	
	public Bitmap getSelectedImage() {
		if (curState != STATE_CUT) {
			return null;
		} 
		
		int srcWidth = srcPicture.getWidth();
		int srcHeight = srcPicture.getHeight();
		double widthOfImage = boundary.getRightBoundary()-boundary.getLeftBoundary();
		double heightOfImage = boundary.getBottomBoundary()-boundary.getTopBoundary();
		double upperLeftX = (upperLeftPoint.x-boundary.getLeftBoundary())/widthOfImage * srcWidth;
		double upperLeftY = (upperLeftPoint.y-boundary.getTopBoundary())/heightOfImage * srcHeight;
		double bottomRightX = (bottomRightPoint.x-boundary.getLeftBoundary())/widthOfImage * srcWidth;
		double bottomRightY = (bottomRightPoint.y-boundary.getTopBoundary())/heightOfImage * srcHeight;
		int width = (int)(bottomRightX - upperLeftX);
		int height = (int)(bottomRightY - upperLeftY);
		
		Bitmap selectedPic = Bitmap.createBitmap(srcPicture, (int)upperLeftX, (int)upperLeftY, width, height);
		return selectedPic;
	}
	
	public ImageBoundary getImageBoundary() {
		return this.boundary;
	}

	/**
	 * gets the upper-left point and the bottom-right point of the selected area
	 * @return poins[0](upper-left point of the selected are); points[1](bottom-right point of the selected area)
	 */
	public Point[] getSelectedPoints() {
		Point[] points = new Point[2];
		points[0] = upperLeftPoint;
		points[1] = bottomRightPoint;
		return points;
	}
	
	public class ImageBoundary {
		float top;
		float bottom;
		float left;
		float right;
		
		ImageBoundary() {
			top = bottom = left = right = 0.0f;
		}
		
		ImageBoundary(float top, float bottom, float left, float right) {
			this.top = top;
			this.bottom = bottom;
			this.left = left;
			this.right = right;
		}
		
		public float getTopBoundary() {
			return this.top;
		}
		
		public float getBottomBoundary() {
			return this.bottom;
		}
		
		public float getLeftBoundary() {
			return this.left;
		}
		
		public float getRightBoundary() {
			return this.right;
		}
	}
	
	
}
