package com.tilt.util;

import android.content.Context;

public final class DisplayUtils {
	/**
	 * transform the px to dp 
	 * @param context
	 * @param pxValue
	 * @return
	 */
	public static int pxToDip(Context context, int pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue * scale + 0.5f);
	}
}
