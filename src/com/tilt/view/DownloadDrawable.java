package com.tilt.view;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.tilt.util.AsyncImageLoader;

public class DownloadDrawable extends ColorDrawable {
	private WeakReference<AsyncImageLoader.LoadImageTask> taskRef;
	
	public DownloadDrawable(final AsyncImageLoader.LoadImageTask task) {
		//super(context.getResources(), bitmap);
		super(Color.BLACK);
		taskRef = new WeakReference<AsyncImageLoader.LoadImageTask>(task);
	}
	
	public AsyncImageLoader.LoadImageTask getLoadImageTask() {
		if (taskRef != null) {
			return taskRef.get();
		}
		
		return null;
	}
}
