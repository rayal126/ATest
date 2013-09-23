package com.tilt.view;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tilt.R;
import com.tilt.camera.CameraManager;

public class CameraActivity extends Activity {
	private static final String TAG = CameraActivity.class.getSimpleName();
	private SurfaceView displayView;
	private Button takePhotoBtn;
	private CameraManager cameraManager;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case R.id.camera_to_display:
				Intent intent = new Intent(CameraActivity.this, ProcessActivity.class);
				intent.putExtras(msg.getData());
				startActivity(intent);
				break;
			default:
				Log.w(TAG, "Undefined hanlder id");
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_camera);

		// gets the widgets
		displayView = (SurfaceView) this.findViewById(R.id.displayView);
		takePhotoBtn = (Button) this.findViewById(R.id.takePhotoBtn);
		initCamera();
		takePhotoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Thread takingPhotoThread = new TakingPhotoThread();
				takingPhotoThread.start();
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cameraManager.closeCamera();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		// return to the last UI
		// ----------------------------------------------------------------------------------------------------------------------------------------------------------------
		case KeyEvent.KEYCODE_BACK:
//			Message msg = new Message();
//			if (currentUI == UI_CAMERA) {
//				finish();
//			} else if (currentUI == UI_DISPLAY) {
//				msg.what = UPDATE_DISPLAY_TO_CAMERA;
//			} else if (currentUI == UI_TILT_AND_CUT) {
//				msg.what = UPDATE_TILT_TO_DISPLAY;
//			}
//
//			handler.sendMessage(msg);
			finish();
			break;
		// ----------------------------------------------------------------------------------------------------------------------------------------------------------------
		// take photo
		case KeyEvent.KEYCODE_CAMERA:
			Thread takingPhotoThread = new TakingPhotoThread();
			takingPhotoThread.start();
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			int currentZoom = cameraManager.getZoom();
			// if the camera does not support zoom
			if (currentZoom == -1) {
				Toast.makeText(CameraActivity.this, "相机不支持变焦",
						Toast.LENGTH_SHORT).show();
			} else {
				cameraManager.setZoom(currentZoom + 1);
			}
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			int zoom = cameraManager.getZoom();
			// if the camera does not support zoom
			if (zoom == -1) {
				Toast.makeText(CameraActivity.this, "相机不支持变焦",
						Toast.LENGTH_SHORT).show();
			} else {
				cameraManager.setZoom(zoom - 1);
			}
			break;
		default:
			break;
		}

		return true;
	}

	/**
	 * initializes the camera and start previewing
	 */
	private void initCamera() {
		cameraManager = new CameraManager(this, handler);
		SurfaceHolder holder = displayView.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		holder.addCallback(new Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
				// do nothing
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				try {
					cameraManager.openCamera(holder);
					resizeDisplayView();
					cameraManager.startPreview();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				cameraManager.stopPreview();
			}
		});
	}

	/**
	 * resizes the displayView according to the size of the picture
	 */
	private void resizeDisplayView() {
		// get the size of the window
		WindowManager wm = (WindowManager) this
				.getSystemService(this.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();

		// get the size of the picture
		Size pictureSize = cameraManager.getPictureSize();
		int picWidth = pictureSize.width;
		int picHeight = pictureSize.height;

		// calculate the size of the displayView
		double widthScale = (double) picWidth / (double) screenWidth;
		double heightScale = (double) picHeight / (double) screenHeight;

		if (widthScale > heightScale) {
			screenHeight = (int) (picHeight / widthScale);
		} else {
			screenWidth = (int) (picWidth / heightScale);
		}

		// resize the displayView
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				screenWidth, screenHeight);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		displayView.setLayoutParams(lp);
	}

	/**
	 * uses the sub-thread to take photo
	 */
	private class TakingPhotoThread extends Thread {
		@Override
		public void run() {
			if (cameraManager.isOpen()) {
				cameraManager.takePhoto();
			}
		}
	}
}
