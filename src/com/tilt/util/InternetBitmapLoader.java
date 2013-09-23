package com.tilt.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * locals the bitmap from the Internet
 * @author Rayal
 *
 */
public class InternetBitmapLoader implements BitmapLoader {
	@Override
	public Bitmap load(final String url) {
		Bitmap image = null;
		
		try {
			URL imgUrl = new URL(url);
			InputStream is = imgUrl.openConnection().getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			image = BitmapFactory.decodeStream(bis);
			bis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return image;
	}
}
