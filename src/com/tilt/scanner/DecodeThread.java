package com.tilt.scanner;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.tilt.view.PreferencesActivity;
import com.tilt.view.ScannerActivity;

public class DecodeThread extends Thread {
	public static final String BARCODE_BITMAP = "barcode_bitmap";

	private final ScannerActivity activity;
	private final Map<DecodeHintType, Object> hints;
	private Handler handler;
	private final CountDownLatch handlerInitLatch;

	public DecodeThread(ScannerActivity activity) {
		this.activity = activity;
		this.hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		this.handlerInitLatch = new CountDownLatch(1);
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this.activity);
		Collection<BarcodeFormat> decodeFormats = EnumSet
				.noneOf(BarcodeFormat.class);
		
		if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D, false)) {
			decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
		}
		if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_QR, false)) {
			decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		}
		if (prefs.getBoolean(PreferencesActivity.KEY_DECODE_DATA_MATRIX, false)) {
			decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
		}
		
		hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
	}

	public Handler getHandler() {
		try {
			this.handlerInitLatch.await();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}

		return handler;
	}

	@Override
	public void run() {
		Looper.prepare();
		handler = new DecodeHandler(this.activity, this.hints);
		this.handlerInitLatch.countDown();
		Looper.loop();
	}
}
