package com.tilt.view;

import android.os.Handler;
import android.os.Message;

import com.google.zxing.Result;
import com.tilt.R;
import com.tilt.scanner.DecodeThread;
import com.tilt.scanner.ScannerManager;

public class ScannerActivityHandler extends Handler {
	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	private final ScannerActivity activity;
	private final DecodeThread decodeThread;
	private ScannerManager scannerManager;
	private State state;

	ScannerActivityHandler(ScannerActivity activity,
			ScannerManager scannerManager) {
		this.activity = activity;
		this.scannerManager = scannerManager;
		this.decodeThread = new DecodeThread(activity);
		this.decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		scannerManager.startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case com.tilt.R.id.restart_preview:
			restartPreviewAndDecode();
			System.out.println("Restart preview");
			break;
		case com.tilt.R.id.decode_successful:
			state = State.SUCCESS;
			System.out.println("Success");
			activity.handleDecode((Result) msg.obj);
			break;
		case com.tilt.R.id.decode_failed:
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			state = State.PREVIEW;
			System.out.println("Failed");
			scannerManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
			break;
		default:
			break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		scannerManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit_decode);
		quit.sendToTarget();
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_successful);
		removeMessages(R.id.decode_failed);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			scannerManager.requestPreviewFrame(decodeThread.getHandler(),
					R.id.decode);
		}
	}
}
