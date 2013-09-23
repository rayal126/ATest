package com.tilt.algorithm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;

import android.os.Environment;

public final class Util {
	/**
	 * initial the matrix by the array
	 * @param mat
	 * @param arr
	 */
	public static void initMat(Mat mat, double[][] arr) {
		checkArgumentsAreNotNull(mat, arr);
		
		int rows = mat.rows();
		int cols = mat.cols();

		if ((rows != arr.length) || (cols != arr[0].length)) {
			throw new IllegalArgumentException("The matrix should have the same size of the array");
		}
		
		// initial the matrix by the arraay
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				mat.put(i, j, arr[i][j]);
			}
		}
	}
	
	public static MinValue min(Mat mat) {
		Util.checkArgumentsAreNotNull(mat);
		
		int rows = mat.rows();
		int cols = mat.cols();
		MinValue min = new MinValue();
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				double value = mat.get(i, j)[0];
				if (min.value > value) {
					min.value = value;
					min.row = i;
					min.col = j;
				}
			}
		}
		
		return min;
	}
	/**
	 * calculate the power of the matrix
	 * @param srcMat
	 * @param power should be greater than or equal to 1
	 * @return
	 */
	public static Mat power(Mat srcMat, int power) {
		checkArgumentsAreNotNull(srcMat);
		
		if ((power < 1) || (srcMat.rows() != srcMat.cols())) {
			throw new IllegalArgumentException();
		}
		
		if (power == 1) {
			return srcMat;
		}
		
		Mat dstMat = power(srcMat, power>>1);
		dstMat = Util.multiply(dstMat, dstMat);
	
		if ((power&1) == 1) {
			dstMat = Util.multiply(dstMat, srcMat);		
		} 
		
		return dstMat;
	}
	
	/**
	 * floor the value of x and y of the point
	 * @param point
	 * @return
	 */
	public static Point floor(Point point) {
		checkArgumentsAreNotNull(point);
		Point res = new Point(Math.floor(point.x), Math.floor(point.y));
		return res;
	}
	
	/**
	 * floor the width and the height of the size 
	 * @param size
	 * @return
	 */
	public static Size floor(Size size) {
		checkArgumentsAreNotNull(size);
		Size dstSize = new Size(Math.floor(size.width), Math.floor(size.height));
		return dstSize;
	}
	
	/**
	 * floor every element in the matrix
	 * @param srcMat
	 * @return
	 */
	public static Mat floor(Mat srcMat) {
		checkArgumentsAreNotNull(srcMat);
		
		int rows = srcMat.rows();
		int cols = srcMat.cols();
		Mat dstMat = new Mat(rows, cols, CvType.CV_32SC1);
		
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				double srcValue = srcMat.get(i, j)[0];
				int value = (int)Math.floor(srcValue);
				dstMat.put(i, j, value);
			}
		}
		
		return dstMat;
	}
	
	/**
	 * print the elements in the matrix
	 * @param mat
	 */
	public static void printMat(Mat mat) {
		checkArgumentsAreNotNull(mat);
		
		// print every element in the matrix
		int rows = mat.rows();
		int cols = mat.cols();
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols-1; ++j) {
				sb.append(mat.get(i, j)[0] + ",");
			}
			if (i == rows-1) {
				sb.append(mat.get(i, cols-1)[0]);
			} else {
				sb.append(mat.get(i, cols-1)[0] + ";");
			}
		}	
		sb.append("]");
		System.out.println(sb.toString());
	}
	
	public static Mat multiply(Mat leftMat, Mat rightMat) {
		// check the arguments 
		if ((leftMat == null) || (rightMat == null)) {
			throw new IllegalArgumentException("The arguments should not be null");
		}
		
		if (leftMat.cols() != rightMat.rows()) {
			throw new IllegalArgumentException("The number of the columns in leftMat should be the same as the number of rows in rightMat");
		}
		
		if (leftMat.type() != rightMat.type()) {
			throw new IllegalArgumentException("The type of the leftMat should be the same as the rightMat");
		}
		
		int rows = leftMat.rows();
		int cols = rightMat.cols();
		Mat res = new Mat(rows, cols, leftMat.type());
		Core.gemm(leftMat, rightMat, 1.0, Mat.zeros(rows, cols, leftMat.type()), 0.0, res);

		return res;
	}
	
	
	/**
	 * check the arguments to make sure that the arguments are not null
	 * @param args
	 */
	public static void checkArgumentsAreNotNull(Object... args) {
		for (Object arg : args) {
			if (arg == null) {
				throw new IllegalArgumentException("The arguments should not be null");
			}
		}
	}
	
	public static class MinValue {
		int row;
		int col;
		double value;
		
		public MinValue() {
			row = col = 0;
			value = Double.MAX_VALUE;
		}
	}
}
