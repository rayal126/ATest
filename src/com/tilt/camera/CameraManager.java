package com.tilt.camera;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;

import com.tilt.R;
import com.tilt.util.BitmapManager;
import com.tilt.view.ProcessActivity;

public class CameraManager {
	private final int FRAME_RATE = 5;
	private final int JPEG_QUALITY = 100;
	private final int MAX_SIZE_OF_PICTURE = 1024;
	
	private final Context context;
	private Camera camera;
	private Handler handler;

	private boolean isInitialized = false;
	private boolean isPreviewing = false;
	private boolean zoomSupported = false;
	
	public CameraManager(Context context, Handler handler) {
		this.context = context;
		this.handler = handler;
	}

	/**
	 * opens the camera driver 
	 * @param holder The surface object which the camera will draw preview frames into.
	 * @throws IOException Indicates the camera driver failed to open.
	 */
	public synchronized void openCamera(SurfaceHolder holder) throws IOException {
		if (camera == null) {
			int numOfCameras = camera.getNumberOfCameras();
			if (numOfCameras > 1) {
				camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			} else {
				camera = Camera.open(Camera.getNumberOfCameras()-1);
			}
			
			if (camera == null) {
				throw new IOException();
			}
		}
	
		camera.setPreviewDisplay(holder);

		if (!isInitialized) {
			isInitialized = true;
			this.initCamera();
		}
	}

	/**
	 * initializes the camera parameters
	 */
	private void initCamera() {
		// sets the camera parameters
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewFrameRate(FRAME_RATE);
		parameters.setPictureFormat(PixelFormat.JPEG);
		parameters.setJpegQuality(JPEG_QUALITY);
		this.adjustPictureSize(parameters);
		camera.setParameters(parameters);
		Size size = parameters.getPictureSize();
		System.out.println("Picture Size: " + size.width + " x " + size.height);
		
		if (parameters.isZoomSupported()) {
			zoomSupported = true;
		} else {
			zoomSupported = false;
		}
	}
	
	/**
	 * whether the camera is open
	 * @return
	 */
	public synchronized boolean isOpen() {
		return camera != null;
	}

	/**
	 * closes the camera driver if still in use
	 */
	public synchronized void closeCamera() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	/**
	 * asks the camera hardware to begin drawing preview frames to the screen
	 */
	public synchronized void startPreview() {
		// start the preview of the camera
		if ((camera != null) && !isPreviewing) {
			camera.startPreview();
			isPreviewing = true;
//			autoFocusManager = new AutoFocusManager(context, camera);
		}
	}

	/**
	 * tells the camera to stop drawing preview frames
	 */
	public synchronized void stopPreview() {
		// close the auto focus function
//		if (autoFocusManager != null) {
//			autoFocusManager.stop();
//			autoFocusManager = null;
//		}
		
		// stop the preview of the camera
		if ((camera != null) && isPreviewing) {
			camera.stopPreview();
			isPreviewing = false;
		}
	}
	
	/**
	 * using the camera to take photo
	 * @return the data of the photo
	 */
	public synchronized void takePhoto() {
		camera.autoFocus(new AutoFocusCallback() {
			@Override
			public synchronized void onAutoFocus(boolean success, Camera camera) {
				if (success) {
					camera.takePicture(null, null, new PictureCallback() {
						@Override
						public void onPictureTaken(byte[] data, Camera camera) {
							stopPreview();
							Bitmap picture = BitmapFactory.decodeByteArray(data, 0, data.length);
							
							// tells the handler to update the activity
							Message msg = new Message();
							msg.what = R.id.camera_to_display;
			    			final String PIC_INDEX = "PICTURE_FROM_CAMERA";
			    			BitmapManager.getInstance().putBitmap(PIC_INDEX, picture);
							Bundle bundle = new Bundle();
							bundle.putString(ProcessActivity.DISPLAY_PICTURE_KEY, PIC_INDEX);
							msg.setData(bundle);
							handler.sendMessage(msg);
						}
					});
				}
			}
		});
	}

	public Size getPictureSize() {
		return camera.getParameters().getPictureSize();
	}
	
	/**
	 * gets current zoom value.	
	 * @return value between 0 and MaxZoom, if the camera does not support zoom, then return -1
	 */
	public int getZoom() {
		if (!zoomSupported) {
			return -1;
		} else {
			return camera.getParameters().getZoom();
		}
	}
	
	/**
	 * sets current zoom value
	 * @param zoom the valid range is 0 to getMaxZoom()
	 */
	public void setZoom(int zoom) {
		Camera.Parameters parameters = camera.getParameters();
		int maxZoom = parameters.getMaxZoom();
		if ((zoom >= 0) && (zoom <= maxZoom)) {
			parameters.setZoom(zoom);
			camera.setParameters(parameters);
		}
	}
	
	private void adjustPictureSize(Camera.Parameters parameters) {
		// gets the supported picture sizes of the camera and sorts the sizes in the descending order 
		List<Size> sizes = parameters.getSupportedPictureSizes();
		Collections.sort(sizes, new Comparator<Size>() {
			@Override
			public int compare(Camera.Size a, Camera.Size b) {
				int sizeOfA = a.width * a.height;
				int sizeOfB = b.width * b.height;
				
				if (sizeOfA > sizeOfB) {
					return -1;
				} else if (sizeOfA == sizeOfB) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		
		for (Size size : sizes) {
			int length = (size.width < size.height) ? size.width : size.height;
			if (length > MAX_SIZE_OF_PICTURE) {
				continue;
			} else {
				parameters.setPictureSize(size.width, size.height);
			}
		}
	}
}
