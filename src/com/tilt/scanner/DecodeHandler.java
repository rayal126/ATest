package com.tilt.scanner;

import java.util.Map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.tilt.R;
import com.tilt.view.ScannerActivity;

final class DecodeHandler extends Handler {
	private final String TAG = DecodeHandler.class.getSimpleName();
	private final ScannerActivity activity;
	private final MultiFormatReader reader;
	private boolean isRunning = true;

	DecodeHandler(ScannerActivity activity, Map<DecodeHintType, Object> hints) {
		this.activity = activity;
		reader = new MultiFormatReader();
		reader.setHints(hints);
	}

	@Override
	public void handleMessage(Message msg) {
		if (!isRunning) {
			return;
		}

		switch (msg.what) {
		case R.id.decode:
			decode((byte[]) msg.obj, msg.arg1, msg.arg2);
			break;
		case R.id.quit_decode:
			isRunning = false;
			Looper.myLooper().quit();
			break;
		default:
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		Result rawResult = null;
		PlanarYUVLuminanceSource source = activity.getScannerManager().buildLuminanceSource(data, width, height);
		
		if (source != null) {
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			try {
				rawResult = reader.decodeWithState(bitmap);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				reader.reset();
			}
		}

		Handler handler = activity.getHandler();
		if (handler == null) {
			Log.e(TAG, "The handler is null!");
			return ;
		}
		
		if (rawResult != null) {
			Message msg = Message.obtain(handler, R.id.decode_successful, rawResult);
			msg.sendToTarget();
		} else {
			Message msg = Message.obtain(handler, R.id.decode_failed, rawResult);
			msg.sendToTarget();
		}
	}

//	private Bitmap toBitmap(LuminanceSource source, int[] pixels) {
//		int width = source.getWidth();
//		int height = source.getHeight();
//		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//		return bitmap;
//	}
}
