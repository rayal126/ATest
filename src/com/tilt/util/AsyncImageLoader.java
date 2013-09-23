package com.tilt.util;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.tilt.algorithm.Util;
import com.tilt.view.DownloadDrawable;

public class AsyncImageLoader {
	private static AsyncImageLoader instance;	
	private Map<String, SoftReference<Bitmap>> imageCache;			// <Key: the url of the image, Value: the image>
	
	/**
	 * private constructor to prevent user defining this class
	 */
	private AsyncImageLoader() {
		imageCache = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	/**
	 * gets the instance of AsyncImageLoader
	 * @return
	 */
	public synchronized static AsyncImageLoader getInstance() {
		if (instance == null) {
			instance = new AsyncImageLoader();
		}
		
		return instance;
	}
	
	/**
	 * loads the image from the Internet
	 * @param itemID
	 * @param imgUrl
	 * @param imgView
	 */
	public void loadImage(final String imgUrl, final ImageView imgView, final BitmapLoader loader) {
		Util.checkArgumentsAreNotNull(imgUrl, imgView);		
		
		boolean flag = true;
		if (imageCache.containsKey(imgUrl)) {
			SoftReference<Bitmap> ref = imageCache.get(imgUrl);
			if ((ref != null) && (ref.get() != null)) {
				imgView.setImageBitmap(ref.get());
				flag = false;
			}
		} else if (flag && cancelPotentialDownload(imgUrl, imgView)) {
			LoadImageTask task = new LoadImageTask(imgView, loader);
			DownloadDrawable drawable = new DownloadDrawable(task);
			imgView.setImageDrawable(drawable);
			task.execute(imgUrl);
		}
	}
	
	private static boolean cancelPotentialDownload(final String url, final ImageView imgView) {
		LoadImageTask task = getLoadImageTask(imgView);
		
		if (task != null) {
			String urlOfTask = task.getImageURL();
			if ((urlOfTask == null) || (!urlOfTask.equals(url))) {
				task.cancel(true);
			} else {
				// the same URL is already being downloaded
				return false;
			}
		}
		
		return true;
	}
	
	private static LoadImageTask getLoadImageTask(ImageView imgView) {
		if (imgView != null) {
			Drawable drawable = imgView.getDrawable();
			if (drawable instanceof DownloadDrawable) {
				DownloadDrawable dd = (DownloadDrawable)drawable;
				return dd.getLoadImageTask();
			}
		}
		
		return null;
	}
	
	public static class LoadImageTask extends AsyncTask<String, Integer, Bitmap> {
		private WeakReference<ImageView> imgViewRef;
		private BitmapLoader loader;
		private String imageURL = null;
		private int widthOfImgView;
		private int heightOfImgView;
		
		public LoadImageTask(final ImageView imgView, final BitmapLoader loader) {
			this.imgViewRef = new WeakReference<ImageView>(imgView);
			this.loader = loader;
			this.widthOfImgView = imgView.getLayoutParams().width;
			this.heightOfImgView = imgView.getLayoutParams().height;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			if ((params == null) || (params.length < 1)) {
				throw new IllegalArgumentException();
			}
			
			imageURL = params[0];
			Bitmap image = null;
			
			if (instance.imageCache.containsKey(imageURL)) {
				image = instance.imageCache.get(imageURL).get();
			}
			
			if (image == null) {
				Bitmap img = loader.load(imageURL);
				
				if (img == null) {
					return null;
				}
				
				int imgWidth = img.getWidth();
				int imgHeight = img.getHeight();
				double scaleOfWidth =  (double)this.widthOfImgView / (double)imgWidth;
				double scaleOfHeight  = (double)this.heightOfImgView / (double)imgHeight;
				double scale = (scaleOfWidth < scaleOfHeight) ? scaleOfWidth : scaleOfHeight;
				imgWidth *= scale;
				imgHeight *= scale;
				
				image = Bitmap.createScaledBitmap(img, imgWidth, imgHeight, true);
				SoftReference<Bitmap> reference = new SoftReference<Bitmap>(image);
				instance.imageCache.put(imageURL, reference);
			}
		
			return image;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (this.isCancelled()) {
				if ((result != null) && (!result.isRecycled())) {
					result.recycle();
					result = null;
				}
			}

			if (imgViewRef != null) {
				ImageView imgView = imgViewRef.get();
				if ((imgView != null) && (this == getLoadImageTask(imgView))) {
					imgView.setImageBitmap(result);
				}
			}
		}
		
		public String getImageURL() {
			return this.imageURL;
		}
	}
}
