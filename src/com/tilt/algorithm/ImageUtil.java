package com.tilt.algorithm;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public final class ImageUtil {
	/**
	 * calculate the corners' positions after the affine transformation 
	 * @param image
	 * @param tfm
	 * @return
	 */
	public static List<Point> calTfmCorners(Mat image, Mat tfm) {
		Util.checkArgumentsAreNotNull(image, tfm);
		
		if ((tfm.rows() != 3) || (tfm.cols() != 3)) {
			throw new IllegalArgumentException("The size of the tfm should be 3x3");
		}
		
		final int SIZE = 4;
		List<Point> points = new ArrayList<Point>(SIZE);
		points.add(new Point(0, 0));
		points.add(new Point(image.cols()-1, 0));
		points.add(new Point(image.cols()-1, image.rows()-1));
		points.add(new Point(0, image.rows()-1));

		for (int ix = 0; ix < SIZE; ++ix) {
			Mat mat = new Mat(1, 3, CvType.CV_64FC1);
			Point point = points.get(ix);
			mat.put(0, 0, point.x);
			mat.put(0, 1, point.y);
			mat.put(0, 2, 1.0);
			Mat resMat = Util.multiply(tfm, mat.t());
			point.x = Math.ceil(resMat.get(0, 0)[0]);
			point.y = Math.ceil(resMat.get(1, 0)[0]);
		}
		
		return points;
	}
	
	/**
	 * calculate the size of the destination image
	 * @param image
	 * @param tfm
	 * @return
	 */
	public static Size calDstImageSize(Mat image, Mat tfm) {
		Util.checkArgumentsAreNotNull(image, tfm);
		
		// calculate the positions of the four corners after the affine transform
		List<Point> points = ImageUtil.calTfmCorners(image, tfm);
		int size = points.size();
		int bottom = Integer.MIN_VALUE;
		int top = Integer.MAX_VALUE;
		int left = Integer.MAX_VALUE;
		int right = Integer.MIN_VALUE;

		for (int ix = 0; ix < size; ++ix) {
			Point point = points.get(ix);
			
			if (point.x > right) {
				right = (int)point.x;
			}
			if (point.x < left) {
				left = (int)point.x;
			}
			if (point.y > bottom) {
				bottom = (int)point.y;
			}
			if (point.y < top) {
				top = (int)point.y;
			}
		}
		
		Size dstSize = new Size(right-left+1, bottom-top+1);
		return dstSize;
	}
	
	/**
	 * if the focus window covers the pixels outside the image, then enlarge the image
	 * @param image
	 * @param focusSize
	 * @return
	 */
	public static Mat enlargeTheImage(Mat image, Size focusSize) {
		Util.checkArgumentsAreNotNull(image, focusSize);
		
		// if the size of the source image is larger than the size of the focus window, return the source image
		Size srcSize = image.size();
		if ((srcSize.width >= focusSize.width) && (srcSize.height >= focusSize.height)) {
			return image;
		}
		
		// enlarge the srouce image to make sure the focus window is inside the image
		Size dstSize = new Size(srcSize.width, srcSize.height);
		boolean adjustWidth = false;
		boolean adjustHeight = false;
		int rowStart = 0;
		int rowEnd = image.rows();
		int colStart = 0;
		int colEnd = image.cols();
		
		// if the focus window is wider than the image
		if (srcSize.width < focusSize.width) {
			dstSize.width = focusSize.width;
			adjustWidth = true;
			colStart = (int)((focusSize.width-srcSize.width)/2.0);
			colEnd = colStart + (int)srcSize.width;
		}
		
		// if the focus window is higher than the image
		if (srcSize.height < focusSize.height) {
			dstSize.height = focusSize.height;
			adjustHeight = true;
			rowStart = (int)((focusSize.height-srcSize.height)/2.0);
			rowEnd = rowStart + (int)srcSize.height;
		}
		
		// put the source image into the destination image
		Mat dstImage = new Mat(dstSize, image.type());
		Mat subImage = dstImage.submat(rowStart, rowEnd, colStart, colEnd);
		image.copyTo(subImage);
//		for (int dr = rowStart, sr = 0; dr < rowEnd; ++dr, ++sr) {
//			for (int dc = colStart, sc = 0; dc < colEnd; ++dc, ++sc) {
//				double[] values = image.get(sr, sc);
//				dstImage.put(dr, dc, values);
//			}
//		}
		return dstImage;
	}
	
	/**
	 * make the affine transform of the source image
	 * @param srcImage
	 * @param tfm transform matrix(3x3)
	 * @param flags the method of interpolation(see the Imgproc.Inter_XXX)
	 * @return
	 */
	public static Mat imageTransform(Mat srcImage, Mat tfm, int flags) {
		Util.checkArgumentsAreNotNull(srcImage, tfm);
		
		Size dstSize = null;
		
		if ((flags&Imgproc.WARP_INVERSE_MAP) == 0) {
			dstSize = ImageUtil.calDstImageSize(srcImage, tfm);
		} else {
			Mat invertTfm = tfm.clone();
			Mat subInvertTfm = invertTfm.submat(0, 2, 0, 3);
			Imgproc.invertAffineTransform(tfm.submat(0, 2, 0, 3), subInvertTfm);
			dstSize = ImageUtil.calDstImageSize(srcImage, invertTfm);
		}
		
		Mat dstImage = new Mat();
		Mat mat = null;
				
		if ((ImageUtil.equals(tfm.get(0, 1)[0], 0.0)) && (ImageUtil.equals(tfm.get(1, 0)[0], 0.0))) {
			mat = tfm;
		} else {
			Point srcCenter = new Point(Math.floor(srcImage.width()/2.0), Math.floor(srcImage.height()/2.0));
			Point dstCenter = new Point(Math.floor(dstSize.width/2.0), Math.floor(dstSize.height/2.0));
			Mat rightMat = Mat.eye(3, 3, CvType.CV_64FC1);
			rightMat.put(0, 2, srcCenter.x);
			rightMat.put(1, 2, srcCenter.y);
			Mat leftMat = Mat.eye(3, 3, CvType.CV_64FC1);
			leftMat.put(0, 2, -dstCenter.x);
			leftMat.put(1, 2, -dstCenter.y);
			mat = Util.multiply(Util.multiply(rightMat, tfm), leftMat);
		}

		Imgproc.warpAffine(srcImage, dstImage, mat.submat(0, 2, 0, 3), dstSize, flags);
		return dstImage;
	}
	
	public static Mat imageTransform(Mat srcImage, Mat tfm, Size focusSize, int flags) {
		Util.checkArgumentsAreNotNull(srcImage, tfm, focusSize);
		Mat dstImage = imageTransform(srcImage, tfm, flags);
		dstImage = ImageUtil.enlargeTheImage(dstImage, focusSize);
		Size dstSize = dstImage.size();
		Point dstImageCenter = new Point(Math.floor(dstSize.width/2.0), Math.floor(dstSize.height/2.0));
		int rowStart = (int)Math.floor(dstImageCenter.y-focusSize.height/2.0);
		int rowEnd = (int)Math.floor(dstImageCenter.y+focusSize.height/2.0);
		int colStart = (int)Math.floor(dstImageCenter.x-focusSize.width/2.0);
		int colEnd = (int)Math.floor(dstImageCenter.x+focusSize.width/2.0);
		dstImage = dstImage.submat(rowStart, rowEnd, colStart, colEnd);
		return dstImage;
	}
	
	/**
	 * calculates the scale factor of the image (scale factor=dstSize/longer size of the picture)
	 * @param picture
	 * @param dstSize 
	 * @return
	 */
	public static float getScaleFactor(Bitmap picture, final int dstSize) {
		Util.checkArgumentsAreNotNull(picture);
		int width = picture.getWidth();
		int height = picture.getHeight();
		int length = (width > height) ? width : height;
		float scale = (float)dstSize / (float)length;
		return scale;
	}
	
	/**
	 * scales the picture 
	 * @param srcPicture the picture that needed to be scaled
	 * @param scaleWidth scale of the width 
	 * @param scaleHeight scale of the height
	 * @return
	 */
	public static Bitmap scalePicture(Bitmap srcPicture, final float scaleWidth, final float scaleHeight, boolean recycle) {
		Util.checkArgumentsAreNotNull(srcPicture);
		if ((scaleWidth < 0) || (scaleHeight <0)) {
			throw new IllegalArgumentException();
		}
		
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap newPicture = Bitmap.createBitmap(srcPicture, 0, 0, srcPicture.getWidth(), srcPicture.getHeight(), matrix, true);
		
		if (recycle && (!srcPicture.isRecycled())) {
			srcPicture.recycle();
		}
		
		srcPicture = null;
		return newPicture;
	}
	
	public static Bitmap rotatePicture(Bitmap srcPicture, final int degrees) {
		Util.checkArgumentsAreNotNull(srcPicture);
		Matrix matrix = new Matrix();
		matrix.setRotate(degrees, (float)srcPicture.getWidth()/2.0f, (float)srcPicture.getHeight()/2.0f);
		Bitmap picture = Bitmap.createBitmap(srcPicture, 0, 0, srcPicture.getWidth(), srcPicture.getHeight(), matrix, true);
		
		// recycle the source picture
		if (!srcPicture.isRecycled()) {
			srcPicture.recycle();
			srcPicture = null;
		}
		
		return picture;
	}
	
	/**
	 * judges whether a == b
	 * @param a
	 * @param b
	 * @return
	 */
	private static boolean equals(double a, double b) {
		final double RANGE = 1E-5;
		
		if (Math.abs(a-b) < RANGE) {
			return true;
		} else {
			return false;
		}
	}
}
