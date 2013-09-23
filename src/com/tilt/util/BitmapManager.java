package com.tilt.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;


public final class BitmapManager {
	private static BitmapManager bitmapManager = null;
	private Map<String, Bitmap> bitmaps = null;

	private BitmapManager() {
		bitmaps = new HashMap<String, Bitmap>();
	}
	
	public static synchronized BitmapManager getInstance() {
		if (bitmapManager == null) {
			bitmapManager = new BitmapManager();
		}
		
		return bitmapManager;
	}
	
	public void putBitmap(String key, Bitmap bitmap) {
		clearBitmaps();
		bitmaps.put(key, bitmap);
	}
	
	public Bitmap getBitmap(String key) {
		return bitmaps.get(key);
	}
	
	private void clearBitmaps() {
		Iterator<String> iter = bitmaps.keySet().iterator();
		
		while (iter.hasNext()) {
			String key = iter.next();
			Bitmap picture = bitmaps.get(key);
			
			if ((picture != null) && (!picture.isRecycled())) {
				picture.recycle();
				picture = null;
			}
			
			bitmaps.remove(key);
		}
	}
}

