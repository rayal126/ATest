package com.tilt.barcode;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.HybridBinarizer;
import com.tilt.R;
import com.tilt.algorithm.ImageUtil;

public class BarcodeDecodeThread extends Thread {
	private static final int RUNS = 10;
	private static final int MAX_SIZE_OF_PICTURE = 640;
	private Handler handler = null;
	private Bitmap picture = null;
	private MultiFormatReader multiFormatReader;

	public BarcodeDecodeThread(Handler handler, final Bitmap picture) {
		this.handler = handler;
		float scale = ImageUtil.getScaleFactor(picture, MAX_SIZE_OF_PICTURE);
		this.picture = ImageUtil.scalePicture(picture, scale, scale, false);
	}

	@Override
	public void run() {
	    multiFormatReader = new MultiFormatReader();
	    multiFormatReader.setHints(null);
	    // Try to get in a known state before starting the benchmark
	    System.gc();

	    Barcode barcode = decode();
	    Message msg = new Message();
	    msg.what = R.id.barcode_decoded;
	    msg.obj = barcode;
	    handler.sendMessage(msg);
	}
	
	private Barcode decode() {
		// get the pixels of the source image
	    int width = picture.getWidth();
	    int height = picture.getHeight();
	    int[] pixels = new int[width * height];
	    picture.getPixels(pixels, 0, width, 0, 0, width, height);
	    RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
	    Barcode barcode = new Barcode();
	    boolean isSuccessful = false;
	    Result rawResult = null;
	    
	    for (int ix = 0; ix < RUNS; ++ix) {
	    	try {
	    		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
	    		rawResult = multiFormatReader.decodeWithState(bitmap);
	    		
	    		if (rawResult != null) {
	    			isSuccessful = true;
	    			break;
	    		}
	    	} catch (Exception e) {

	    	} finally {
	    		multiFormatReader.reset();
	    	}
	    }
	    
	    if (isSuccessful) {
    		barcode.setDecoded(true);
    		ParsedResult result = ResultParser.parseResult(rawResult);
    		barcode.setContent(result.getDisplayResult());
	    } else {
    		barcode.setDecoded(false);
    		barcode.setContent("Failed");
	    }
	   
	    return barcode;
	}
}
