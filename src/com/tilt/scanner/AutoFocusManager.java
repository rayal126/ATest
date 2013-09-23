package com.tilt.scanner;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.AsyncTask;

public class AutoFocusManager implements AutoFocusCallback {
	private static final long AUTO_FOCUS_INTERVAL_MS = 1000L;
	private static final List<String> FOCUS_MODES_CALLING_AF;
	static {
		FOCUS_MODES_CALLING_AF = new ArrayList<String>(2);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO);
		FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO);
	}

	private final Camera camera;
	private boolean useAutoFocus;
	private boolean active;
	private AsyncTask autoFocusTask;
//	private final AsyncTaskExecInterface taskExec;

	AutoFocusManager(Context context, Camera camera) {
		this.camera = camera;
//		SharedPreferences sharedPrefs = PreferenceManager
//				.getDefaultSharedPreferences(context);
//		String currentFocusMode = camera.getParameters().getFocusMode();
//		useAutoFocus = sharedPrefs.getBoolean(
//				PreferencesActivity.KEY_AUTO_FOCUS, true)
//				&& FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
		useAutoFocus = true;
		start();
	}

	@Override
	public synchronized void onAutoFocus(boolean success, Camera theCamera) {
		if (active) {
			autoFocusTask = new AutoFocusTask();
			autoFocusTask.execute();
		}
	}

	synchronized void start() {
		if (useAutoFocus) {
			active = true;
			try {
				camera.autoFocus(this);
			} catch (RuntimeException re) {
				// Have heard RuntimeException reported in Android 4.0.x+;
				// continue?
				// Log.w(TAG, "Unexpected exception while focusing", re);
			}
		}
	}

	synchronized void stop() {
		if (useAutoFocus) {
			try {
				camera.cancelAutoFocus();
			} catch (RuntimeException re) {
				// Have heard RuntimeException reported in Android 4.0.x+;
				// continue?
				// Log.w(TAG, "Unexpected exception while cancelling focusing",
				// re);
			}
		}

		if (autoFocusTask != null) {
			autoFocusTask.cancel(true);
			autoFocusTask = null;
		}
		
		active = false;
	}

	private final class AutoFocusTask extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... voids) {
			try {
				Thread.sleep(AUTO_FOCUS_INTERVAL_MS);
			} catch (InterruptedException e) {
				// continue
			}

			synchronized (AutoFocusManager.this) {
				if (active) {
					start();
				}
			}
			return null;
		}
	}
}
