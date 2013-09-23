package com.tilt.scanner;

import java.io.IOException;

import com.google.zxing.PlanarYUVLuminanceSource;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

public class ScannerManager {
	private final String TAG = ScannerManager.class.getSimpleName();

	private final static int MIN_FRAME_WIDTH = 240;
	private final static int MIN_FRAME_HEIGHT = 240;
	private final static int MAX_FRAME_WIDTH = 600;
	private final static int MAX_FRAME_HEIGHT = 400;

	private Context context;
	private Camera scanner;
	private final ScannerPreviewCallback previewCallback;
	private AutoFocusManager autoFocusManager;
	private ScannerConfigurationManager configManager;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean isPreviewing;
	private boolean initialized;
	private int requestedFramingWidth = 0;
	private int requestedFramingHeight = 0;

	public ScannerManager(Context context) {
		this.context = context;
		this.configManager = new ScannerConfigurationManager(this.context);
		previewCallback = new ScannerPreviewCallback(configManager);
	}

	public synchronized void openDriver(SurfaceHolder holder)
			throws IOException {
		if (scanner == null) {
			int numOfCameras = Camera.getNumberOfCameras();
			if (numOfCameras > 1) {
				scanner = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			} else {
				scanner = Camera.open(Camera.getNumberOfCameras() - 1);
			}

			if (scanner == null) {
				throw new IOException();
			}

			scanner.setPreviewDisplay(holder);

			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(this.scanner
						.getParameters());
				if ((this.requestedFramingWidth > 0)
						&& (this.requestedFramingHeight > 0)) {
					this.setManualFramingRect(this.requestedFramingWidth,
							this.requestedFramingHeight);
					this.requestedFramingWidth = 0;
					this.requestedFramingHeight = 0;
				}
			}

			Camera.Parameters parameters = this.scanner.getParameters();
			String parametersFlattened = parameters == null ? null : parameters
					.flatten();
			try {
				configManager.setDesiredScannerParameters(scanner, false);
			} catch (RuntimeException re) {
				// Driver failed
				Log.w(TAG,
						"Camera rejected parameters. Setting only minimal safe-mode parameters");
				Log.i(TAG, "Resetting to saved camera params: "
						+ parametersFlattened);
				// Reset
				if (parametersFlattened != null) {
					parameters = this.scanner.getParameters();
					parameters.unflatten(parametersFlattened);
					try {
						this.scanner.setParameters(parameters);
						configManager.setDesiredScannerParameters(scanner, true);
					} catch (RuntimeException e) {
						// Well, darn. Give up
						Log.w(TAG,
								"Camera rejected even safe-mode parameters! No configuration");
					}
				}
			}
		}
	}

	/**
	 * Closes the scanner driver if still in use.
	 */
	public synchronized void closeDriver() {
		if (isOpen()) {
			scanner.release();
			scanner = null;
			framingRect = null;
			framingRectInPreview = null;
		}
	}

	/**
	 * Whether the scanner is open
	 * 
	 * @return
	 */
	public boolean isOpen() {
		return (scanner != null);
	}

	/**
	 * Asks the scanner hardware to begin drawing preview frames to the screen.
	 */
	public synchronized void startPreview() {
		if ((isOpen()) && !isPreviewing) {
			isPreviewing = true;
			scanner.startPreview();
			autoFocusManager = new AutoFocusManager(context, scanner);
		}
	}

	/**
	 * Tells the scanner to stop drawing preview frames.
	 */
	public synchronized void stopPreview() {
		if (autoFocusManager != null) {
			autoFocusManager.stop();
			autoFocusManager = null;
		}

		if (isPreviewing) {
			scanner.stopPreview();
			previewCallback.setHandler(null, 0);
			isPreviewing = false;
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 * 
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public synchronized Rect getFramingRect() {
		if (framingRect == null) {
			if (!isOpen()) {
				return null;
			}

			Point screenResolution = configManager.getScreenResolution();
			if (screenResolution == null) {
				return null;
			}

			int width = screenResolution.x * 3 / 4;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}

			int height = screenResolution.y * 3 / 4;
			if (height < MIN_FRAME_HEIGHT) {
				height = MIN_FRAME_HEIGHT;
			} else if (height > MAX_FRAME_HEIGHT) {
				height = MAX_FRAME_HEIGHT;
			}

			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			Log.d(TAG, "Calculated framing rect: " + framingRect);
		}

		return framingRect;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 * 
	 * @return
	 */
	public synchronized Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect framingRect = this.getFramingRect();
			if (framingRect == null) {
				return null;
			}

			Rect rect = new Rect(framingRect);
			Point screenResolution = configManager.getScreenResolution();
			Point scannerResolution = configManager.getScannerResolution();
			if ((screenResolution == null) || (scannerResolution == null)) {
				return null;
			}

			double xRatio = (double) scannerResolution.x
					/ (double) screenResolution.x;
			double yRatio = (double) scannerResolution.y
					/ (double) screenResolution.y;
			rect.left = (int) (rect.left * xRatio);
			rect.right = (int) (rect.right * xRatio);
			rect.top = (int) (rect.top * yRatio);
			rect.bottom = (int) (rect.bottom * yRatio);
			framingRectInPreview = rect;
		}

		return framingRectInPreview;
	}

	/**
	 * Convenience method for
	 * {@link com.google.zxing.client.android.CaptureActivity}
	 */
	public synchronized void setTorch(boolean newSetting) {
		if (this.scanner != null) {
			if (autoFocusManager != null) {
				autoFocusManager.stop();
			}
			configManager.setTorch(this.scanner, newSetting);
			if (autoFocusManager != null) {
				autoFocusManager.start();
			}
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data
	 * will arrive as byte[] in the message.obj field, with width and height
	 * encoded as message.arg1 and message.arg2, respectively.
	 * 
	 * @param handler
	 *            The handler to send the message to.
	 * @param message
	 *            The what field of the message to be sent.
	 */
	public synchronized void requestPreviewFrame(Handler handler, int message) {
		if ((this.scanner != null) && isPreviewing) {
			previewCallback.setHandler(handler, message);
			this.scanner.setOneShotPreviewCallback(previewCallback);
		}
	}

	/**
	 * Allows third party apps to specify the scanning rectangle dimensions,
	 * rather than determine them automatically based on screen resolution.
	 * 
	 * @param width
	 *            The width in pixels to scan.
	 * @param height
	 *            The height in pixels to scan.
	 */
	public synchronized void setManualFramingRect(int width, int height) {
		if (initialized) {
			Point screenResolution = configManager.getScreenResolution();

			if (width > screenResolution.x) {
				width = screenResolution.x;
			}

			if (height > screenResolution.y) {
				height = screenResolution.y;
			}

			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			Log.d(TAG, "Calculated manual framing rect: " + framingRect);
			framingRectInPreview = null;
		} else {
			this.requestedFramingWidth = width;
			this.requestedFramingHeight = height;
		}
	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Rect framingRectInPreview = this.getFramingRectInPreview();
		if (framingRectInPreview == null) {
			return null;
		}

		return new PlanarYUVLuminanceSource(data, width, height,
				framingRectInPreview.left, framingRectInPreview.top,
				framingRectInPreview.width(), framingRectInPreview.height(),
				false);
	}
}
