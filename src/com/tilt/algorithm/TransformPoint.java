package com.tilt.algorithm;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

public final class TransformPoint {
	public static Point transformPoint(Point point, Mat tfmMatrix) {
		// check the arguments 
		if ((point == null) || (tfmMatrix == null)) {
			throw new IllegalArgumentException("The arguments should not be null!");
		}
		
		if ((tfmMatrix.rows() != 3) || (tfmMatrix.cols() != 3)) {
			throw new IllegalArgumentException("The size of the tfmMatrix should be 3x3");
		}
		
		Mat mat = new Mat(3, 1,CvType.CV_64FC1);
		mat.put(0, 0, point.x);
		mat.put(1, 0, point.y);
		mat.put(2, 0, 1.0);
		Mat resMat = Util.multiply(tfmMatrix, mat);
		double scale = resMat.get(2, 0)[0];
		Point res = new Point(resMat.get(0, 0)[0]/scale, resMat.get(1, 0)[0]/scale);
		
		return res;
	}
}
