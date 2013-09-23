package com.tilt.view;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.tilt.R;
import com.tilt.scanner.InactivityTimer;
import com.tilt.scanner.IntentSource;
import com.tilt.scanner.ScannerManager;
import com.tilt.util.BeepManager;

public class ScannerActivity extends Activity implements SurfaceHolder.Callback {
	private final String TAG = ScannerActivity.class.getSimpleName();

	private ScannerActivityHandler handler;
	private Result lastResult;
	private IntentSource source;
	private InactivityTimer inactivityTimer;
	private BeepManager beepManager;
	private SurfaceView previewView;
	private ViewFinderView viewFinderView;
	private ScannerManager scannerManager;
	private boolean hasSurface;

	private TextView barcodeText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// initializes the window and the layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = this.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		this.setContentView(R.layout.activity_scanner);

		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);
		previewView = (SurfaceView) this.findViewById(R.id.preview_view);
		
		barcodeText = (TextView)this.findViewById(R.id.barcodeText);
	}

	@Override
	protected void onResume() {
		super.onResume();
		System.out.println("ScannerActivity onResume");
		
		/*
		 * CameraManager must be initialized here, not in onCreate(). This is
		 * necessary because we don't want to open the camera driver and measure
		 * the screen size if we're going to show the help on first launch. That
		 * led to bugs where the scanning rectangle was the wrong size and
		 * partially off screen.
		 */
		scannerManager = new ScannerManager(getApplication());
		viewFinderView = (ViewFinderView) this
				.findViewById(R.id.viewfinder_view);
		viewFinderView.setScannerManager(scannerManager);

		handler = null;
		lastResult = null;

		SurfaceHolder surfaceHolder = previewView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore surfaceCreated() won't be called, so init the
			// camera here.
			initScanner(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			System.out.println("else");
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		beepManager.updatePrefs();
		inactivityTimer.onResume();
		Intent intent = getIntent();
		source = IntentSource.NONE;
	}

	public ViewFinderView getViewFinderView() {
		return this.viewFinderView;
	}

	public Handler getHandler() {
		return this.handler;
	}

	public ScannerManager getScannerManger() {
		return this.scannerManager;
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}

		inactivityTimer.onPause();
		scannerManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}

		super.onPause();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}

		System.out.println("surfaceCreated");
		if (!hasSurface) {
			hasSurface = true;
			initScanner(holder);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// do nothing
	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult) {
		inactivityTimer.onActivity();
		beepManager.playBeepSoundAndVibrate();
		lastResult = rawResult;
		ParsedResult parsedResult = ResultParser.parseResult(rawResult);
		String content = parsedResult.getDisplayResult().replace("\r", "");
		Intent intent = new Intent(ScannerActivity.this, ProductListActivity.class);
		intent.putExtra(getResources().getText(R.string.search_key).toString(), content);
		intent.putExtra(getResources().getText(R.string.search_method).toString(), getResources().getText(R.string.search_by_barcode).toString());
		startActivity(intent);
	}

	public ScannerManager getScannerManager() {
		return this.scannerManager;
	}

	private void initScanner(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}

		if (scannerManager.isOpen()) {
			Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
			return;
		}

		try {
			scannerManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new ScannerActivityHandler(this, scannerManager);
			}
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.lang.RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
		}
	}
}
