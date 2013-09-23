package com.tilt.scanner;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class ScannerPreviewCallback implements Camera.PreviewCallback {
	private static String TAG = ScannerPreviewCallback.class.getSimpleName();
	
	private ScannerConfigurationManager configManager;
	private Handler previewHandler;
	private int handlerMsg;
	
	ScannerPreviewCallback(ScannerConfigurationManager configManager) {
		this.configManager = configManager;
	}
	
	void setHandler(Handler previewHandler, int handlerMsg) {
		this.previewHandler = previewHandler;
		this.handlerMsg = handlerMsg;
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera scaner) {
		Point scannerResolution = configManager.getScannerResolution();
		if ((scannerResolution != null) && (previewHandler != null)) {
			Message msg = Message.obtain(previewHandler, handlerMsg, scannerResolution.x, scannerResolution.y, data);
			msg.sendToTarget();
			previewHandler = null;
		} else {
			Log.d(TAG, "Got preview callback, but no handler or resolution available");
		}
	}
}
