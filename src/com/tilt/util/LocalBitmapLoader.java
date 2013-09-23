package com.tilt.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * locals the bitmap from the local file
 * @author Rayal
 *
 */
public class LocalBitmapLoader implements BitmapLoader {
	@Override
	public Bitmap load(final String url) {
		return BitmapFactory.decodeFile(url);
	}
}
