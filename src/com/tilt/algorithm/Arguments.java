package com.tilt.algorithm;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import com.tilt.view.PictureView.ImageBoundary;

public class Arguments {
	Mat srcImage;
	Point[] initialPoints;
	Point center;
	
	// Optional Parameters 
	Mat initialTfmMatrix = Mat.eye(3, 3, CvType.CV_64FC1);	// 3-by-3 initialization matrix for tfm_matrix. If this is
																					// not set, tfm_matrix will be initialized to identity
//	boolean isBranch = true;							// true or false, true for turnning on branch-and-bound, but 
//																	// this is only allowed in the AFFINE case
//	boolean isBlur = true;								// true or false, true for turnning on BLUR
//	boolean isPyramid = true;							// true or false, true for turnning on pyramid
//	boolean isNoTranslation;							// true or false, true for no translation
	
//	double innerTol = 1e-4;								// positive real value, inner_loop threshold
//	double innerC = 1.0;									// positive real value, inner_loop lambda=C/sqrt(m)
//	double innerMu;										// positive real value, inner_loop for ALM mu
//	int innerMaxIter = Integer.MAX_VALUE;		// positive integer, maximum iteration for inner_loop
//	int innerDisplayPeriod = 100;						// positive integer, whether we display the results
	
//	double outerTol = 1e-4;								// positive real value, outer_loop threshold
//	int outerMaxIter = 50;								// positive integer, maximum iteration for ouer_loop
//	int OuterDisplayperiod = 1;						// positive integer, whether we display the results for the outer loop

	int focusThreshold = 50;							// positive integer, smallest edge length threshold in pyramid
//	double outerTolStep = 10;							// positive real value, We relax threshold for outer-loop each time 
//																	// we move downstairs in pyramid by tol=tol*OUTER_TOL_STEP

//	int blurKernelSizeK = 2;								// positive integer, size of effective blur neighbourhood
//	double blurKernelSigmaK = 2.0;					// positive real value, standard derivation of the blur kernel

//	int branchMaxIter = 10;									// positive integer, we need have extremely high accuracy
//																		// in branch-and=bound. So we separately set
//																		// branch_maxiter for branch_and_bound
	int branchAccuracy = 5;									// positive integer, we split the whole parameter region
																		// to search into 2*branch_accuracy+1 sub-region
	double branchMaxRotation = Math.PI / 9.0;		// positive real, by default pi/9, specifying how large to
																		// do branch-and-bound for rotation	
	double branchMaxSkew = 0.7;						// positive real, by default
	int pyramidMaxLevel = 2;								// positive integer, we only run TILT on the highest
																		// PYRAMID_MAXLEVEL levels in the pyramid
	
	Size focusSize;												// 1*2 row vector, forcing focus_size to be something
	
	public Arguments() {
		// do nothing
	}
	
	public Arguments(Mat srcImg, Point[] selectedPoints, ImageBoundary boundary) {
		// check the arguments
		Util.checkArgumentsAreNotNull(srcImg, selectedPoints, boundary);

		if (selectedPoints.length != 2) {
			throw new IllegalArgumentException("The size of the selectedPoints should be 2");
		}

		this.srcImage = srcImg;
		Point upperLeftPoint = selectedPoints[0];
		Point bottomRightPoint = selectedPoints[1];
		
		if (upperLeftPoint.x > bottomRightPoint.x) {
			double tempValue = upperLeftPoint.x;
			upperLeftPoint.x = bottomRightPoint.x;
			bottomRightPoint.x = tempValue;
		}
		
		if (upperLeftPoint.y > bottomRightPoint.y) {
			double tempValue = upperLeftPoint.y;
			upperLeftPoint.y = bottomRightPoint.y;
			bottomRightPoint.y = tempValue;
		}
		
		double widthOfImage = boundary.getRightBoundary()-boundary.getLeftBoundary();
		double heightOfImage = boundary.getBottomBoundary()-boundary.getTopBoundary();
		double upperLeftX = (upperLeftPoint.x-boundary.getLeftBoundary())/widthOfImage * srcImg.width();
		double upperLeftY = (upperLeftPoint.y-boundary.getTopBoundary())/heightOfImage * srcImg.height();
		double bottomRightX = (bottomRightPoint.x-boundary.getLeftBoundary())/widthOfImage * srcImg.width();
		double bottomRightY = (bottomRightPoint.y-boundary.getTopBoundary())/heightOfImage * srcImg.height();
		this.initialPoints = new Point[2];
		this.initialPoints[0] = new Point(Math.floor(upperLeftX), Math.floor(upperLeftY));				// the left-up point
		this.initialPoints[1] = new Point(Math.floor(bottomRightX), Math.floor(bottomRightY));		// the right-bottom point
		focusSize = new Size(Math.abs(this.initialPoints[0].x - this.initialPoints[1].x)+1, Math.abs(this.initialPoints[0].y - this.initialPoints[1].y)+1);
		center = new Point(Math.floor((this.initialPoints[0].x+this.initialPoints[1].x)/2.0), 
									Math.floor((this.initialPoints[0].y + this.initialPoints[1].y)/2.0));
//		System.out.println("Boundary[Left:" + boundary.getLeftBoundary() + " Right:" + boundary.getRightBoundary() + " Top:" + boundary.getTopBoundary() + " Bottom:" + boundary.getBottomBoundary() + "]");
//		System.out.println("Selected Points[UpperLeftPoint(" + upperLeftPoint.x + ", " + upperLeftPoint.y + ") BottomRightPoint(" + bottomRightPoint.x + ", " + bottomRightPoint.y + ")]");
//		System.out.println("Initial Points[UpperLeftPoint(" + upperLeftX + ", " + upperLeftY + ") BottomRightPoint(" + bottomRightX + ", " + bottomRightY + ")]");
//		System.out.println("Letf-Up:(" + initialPoints[0].x + initialPoints[0].y + ")");
//		System.out.println("Right-Bottom:(" + initialPoints[1].x + initialPoints[1].y + ")");
//		System.out.println("FocusSize[Width:" + focusSize.width + ", Height:" + focusSize.height + "]");
//		System.out.println("Center[" + center.x + ", " + center.y + "]");
	}
	
	@Override
	public Arguments clone() {
		Arguments newArgs = new Arguments();
		newArgs.srcImage = this.srcImage.clone();
		newArgs.initialPoints = this.initialPoints.clone();
		newArgs.center = this.center.clone();
		newArgs.initialTfmMatrix = this.initialTfmMatrix.clone();
		newArgs.focusSize = this.focusSize.clone();
		
		return newArgs;
	}
}
