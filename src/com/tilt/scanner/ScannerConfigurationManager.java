package com.tilt.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.tilt.view.PreferencesActivity;

public class ScannerConfigurationManager {
	private static String TAG = ScannerConfigurationManager.class
			.getSimpleName();

	// This is bigger than the size of a small screen, which is still supported.
	// The routine
	// below will still select the default (presumably 320x240) size for these.
	// This prevents
	// accidental selection of very low resolution on some devices.
	private static final int MIN_PREVIEW_PIXELS = 470 * 320;
	private static final int MAX_PREVIEW_PIXELS = 1280 * 720;

	private Context context;
	private Point screenResolution;
	private Point scannerResolution;

	public ScannerConfigurationManager(Context context) {
		this.context = context;
	}

	public void initFromCameraParameters(Camera.Parameters parameters) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int screenWidth = display.getWidth();
		int screenHeight = display.getHeight();

		if (screenWidth < screenHeight) {
			Log.i(TAG,
					"Display reports portrait orientation; assuming this is incorrect");
			int temp = screenWidth;
			screenWidth = screenHeight;
			screenHeight = temp;
		}

		screenResolution = new Point(screenWidth, screenHeight);
		scannerResolution = findBestPreviewSizeValue(parameters);
	}

	private Point findBestPreviewSizeValue(Camera.Parameters parameters) {
		List<Size> rawPreviewSizes = parameters.getSupportedPreviewSizes();
		if (rawPreviewSizes == null) {
			Size defaultPreviewSize = parameters.getPreviewSize();
			Point resolution = new Point(defaultPreviewSize.width,
					defaultPreviewSize.height);
			return resolution;
		}

		// Sort by size, descending
		List<Size> previewSizes = new ArrayList<Size>(rawPreviewSizes);
		Collections.sort(previewSizes, new Comparator<Size>() {
			@Override
			public int compare(Size a, Size b) {
				int aPixels = a.width * a.height;
				int bPixels = b.width * b.height;
				if (aPixels > bPixels) {
					return -1;
				} else if (aPixels < bPixels) {
					return 1;
				} else {
					return 0;
				}
			}
		});

		Point bestResolution = null;
		float screenAspectRatio = (float) screenResolution.x
				/ (float) screenResolution.y;
		float diff = Float.POSITIVE_INFINITY;

		for (Size size : previewSizes) {
			int width = size.width;
			int height = size.height;
			int pixels = width * height;

			if ((pixels < MIN_PREVIEW_PIXELS) || (pixels > MAX_PREVIEW_PIXELS)) {
				continue;
			}

			if (width < height) {
				int temp = width;
				width = height;
				height = temp;
			}

			if ((width == screenResolution.x) && (height == screenResolution.y)) {
				Point exactResolution = new Point(width, height);
				return exactResolution;
			}

			float aspectRatio = (float) width / (float) height;
			float newDiff = Math.abs(aspectRatio - screenAspectRatio);
			if (newDiff < diff) {
				diff = newDiff;
				bestResolution = new Point(width, height);
			}
		}

		if (bestResolution == null) {
			Size defaultPreviewSize = parameters.getPreviewSize();
			Point resolution = new Point(defaultPreviewSize.width,
					defaultPreviewSize.height);
			return resolution;
		}

		return bestResolution;
	}

	public Point getScreenResolution() {
		return this.screenResolution;
	}

	public Point getScannerResolution() {
		return this.scannerResolution;
	}

	void setDesiredScannerParameters(Camera scanner, boolean safeMode) {
		Camera.Parameters parameters = scanner.getParameters();
		
		if (parameters == null) {
			Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
			return;
		}

		Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

		if (safeMode) {
			Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.initializeTorch(parameters, prefs);
		this.initializeFocusMode(parameters, prefs, safeMode);
		parameters.setPreviewSize(scannerResolution.x, scannerResolution.y);
		scanner.setParameters(parameters);
	}

	void setTorch(Camera scanner, boolean newSetting) {
		Camera.Parameters parameters = scanner.getParameters();
		this.doSetTorch(parameters, newSetting);
		scanner.setParameters(parameters);
		
		// update the preferences activity
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		boolean currentSetting = prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false);
		if (currentSetting != newSetting) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(PreferencesActivity.KEY_FRONT_LIGHT, newSetting);
			editor.commit();
		}
	}

	private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs) {
		boolean newSetting = prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false);
		this.doSetTorch(parameters, newSetting);
	}
	
	private void doSetTorch(Camera.Parameters parameters, boolean newSetting) {
		String flashMode;
		if (newSetting) {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(), Camera.Parameters.FLASH_MODE_TORCH, Camera.Parameters.FLASH_MODE_ON);
		} else {
			flashMode = findSettableValue(parameters.getSupportedFlashModes(), Camera.Parameters.FLASH_MODE_OFF);
		}
		
		if (flashMode != null) {
			parameters.setFlashMode(flashMode);
		}
	}

	private void initializeFocusMode(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
		String focusMode = null;
		if (prefs.getBoolean(PreferencesActivity.KEY_AUTO_FOCUS, true)) {
			if (safeMode || prefs.getBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, false)) {
				focusMode = findSettableValue(parameters.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_AUTO);
			} else {
				focusMode = findSettableValue(parameters.getSupportedFocusModes(), "continuous-picture", "continuous-video", Camera.Parameters.FOCUS_MODE_AUTO);
				// continuous-picture : Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in Android 4.0+
				// continuous-video : Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO in Android 4.0+
			}
		}
		
		// Maybe selected auto-focus but not available, so fall through here:
		if (!safeMode && (focusMode == null)) {
			focusMode = findSettableValue(parameters.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_MACRO, "edof");
			// edof : Camera.Parameters.FOCUS_MODE_EDOF in Android 2.2+
		}

		if (focusMode != null) {
			parameters.setFocusMode(focusMode);
		}
	}
	
	private static String findSettableValue(Collection<String> supportedValues,
			String... desiredValues) {
		Log.i(TAG, "Supported values: " + supportedValues);
		String result = null;
		if (supportedValues != null) {
			for (String desiredValue : desiredValues) {
				if (supportedValues.contains(desiredValue)) {
					result = desiredValue;
					break;
				}
			}
		}
		Log.i(TAG, "Settable value: " + result);
		return result;
	}
}
