package com.tilt.algorithm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.os.Environment;

public final class Tilt {
	private final static String dirPath = Environment.getExternalStorageDirectory().getPath() + "/TILT_Pictures/";
	
	public static Mat Tilt(Arguments args) {
		// back up
		Arguments originalArgs = args.clone();
		Timer timer = new Timer();
		
		// cut the image
//		expand_rate=0.8;
//		initial_points=args.initial_points;
//		left_bound=ceil(max(initial_points(1, 1)-expand_rate*(initial_points(1, 2)-initial_points(1, 1)), 1));
//		right_bound=floor(min(initial_points(1, 2)+expand_rate*(initial_points(1, 2)-initial_points(1, 1)), size(args.input_image, 2)));
//		top_bound=ceil(max(initial_points(2, 1)-expand_rate*(initial_points(2, 2)-initial_points(2, 1)), 1));
//		bottom_bound=floor(min(initial_points(2, 2)+expand_rate*(initial_points(2, 2)-initial_points(2, 1)), size(args.input_image, 1)));
//		new_image=zeros(bottom_bound-top_bound+1, right_bound-left_bound+1,  size(args.input_image, 3));
//		for c=1:size(args.input_image, 3)
//		    new_image(:, :, c)=args.input_image(top_bound:bottom_bound, left_bound:right_bound, c);
//		end
//		args.input_image=uint8(new_image);
//		args.center=args.center+[1-left_bound; 1-top_bound];
		timer.begin();
		double expandRate = 0.8;
		int leftBound = (int)Math.ceil(Math.max(args.initialPoints[0].x-expandRate*(args.focusSize.width-1), 0));
		int rightBound = (int)Math.floor(Math.min(args.initialPoints[1].x+expandRate*(args.focusSize.width-1), args.srcImage.width()));
		int topBound = (int)Math.ceil(Math.max(args.initialPoints[0].y-expandRate*(args.focusSize.height-1), 0));
		int bottomBound = (int)Math.floor(Math.min(args.initialPoints[1].y+expandRate*(args.focusSize.height-1), args.srcImage.height()));
		args.srcImage = args.srcImage.submat(topBound, bottomBound, leftBound, rightBound);
		originalArgs.srcImage = args.srcImage.clone();
		
//		args.center.x -= leftBound;
//		args.center.y -= topBound;	
//		timer.stop();
//		System.out.println("[cut the image]:" + timer.getTotalTime());
//		System.out.println("Cut the image int to Width:" + args.srcImage.width() + " Height:" + args.srcImage.height());
//		System.out.println("LeftBound:" + leftBound + " RightBound:" + rightBound + " TopBound:" + topBound + " BottomBound:" + bottomBound);
//		System.out.println("New Image Center:(" + args.center.x + ", " + args.center.y + ")");
		
		// down-sample the image if the focus is too large
//		pre_scale_matrix=eye(3);
//		focus_threshold=200; % The max length of the focus is 100, if too large downsample to matrix of this size.
//		min_length=min(original_args.focus_size);
		timer.begin();
		Mat preScaleMat = Mat.eye(3, 3, CvType.CV_64FC1);
		double focusThreshold = 200;
		double minLength = Math.min(args.focusSize.width, args.focusSize.height);	
		if (minLength > focusThreshold) {
//		    s=min_length/focus_threshold;
//		    dst_pt_size=round([bottom_bound-top_bound+1, right_bound-left_bound+1]/s);
//		    args.input_image=imresize(args.input_image, dst_pt_size);
//		    pre_scale_matrix=diag([s s 1]);
//			args.focus_size=round(args.focus_size/pre_scale_matrix(1, 1));
//			args.center=args.center/pre_scale_matrix(1, 1);
			double scale = minLength / focusThreshold;
			double dstWidth = Math.round(((double)(rightBound-leftBound))/scale); 
			double dstHeight = Math.round(((double)(bottomBound-topBound))/scale);
			Mat dstImage = new Mat();
			Imgproc.resize(args.srcImage, dstImage, new Size(dstWidth, dstHeight));
			args.srcImage = dstImage.clone();
			preScaleMat.put(0, 0, scale);
			preScaleMat.put(1, 1, scale);
			args.focusSize.width = Math.round(args.focusSize.width/scale);
			args.focusSize.height =  Math.round(args.focusSize.height/scale);
		}
		timer.stop();
		System.out.println("[down-sample the image]:" + timer.getTotalTime());
		
		// adjust the parameters 
//		initial_tfm_matrix=args.initial_tfm_matrix;
//		initial_tfm_matrix=inv(pre_scale_matrix)*initial_tfm_matrix*pre_scale_matrix;
//		args.initial_tfm_matrix=initial_tfm_matrix;
		Mat initialTfmMatrix = Util.multiply(Util.multiply(preScaleMat.inv(), args.initialTfmMatrix), preScaleMat);
		args.initialTfmMatrix = initialTfmMatrix.clone();
		
		/**
		 * do blur
		 */
		// step 1: prepare data for the lowest resolution
		timer.begin();
		double totalScale = minLength/(double)args.focusThreshold;
		double log2Value = Math.log(totalScale)/Math.log(2.0);
		totalScale = Math.floor(log2Value);
		Mat downsampleMat = new Mat(3, 3, CvType.CV_64FC1);
		Util.initMat(downsampleMat, new double[][]{{0.5, 0, 0},{0, 0.5, 0},{0, 0, 1}});
		Mat scaleMat = Util.power(downsampleMat, (int)totalScale);
		Mat srcImage = args.srcImage.clone();
		Mat dstImage = ImageUtil.imageTransform(srcImage, scaleMat, Imgproc.INTER_CUBIC);
		Highgui.imwrite(dirPath + "scale.jpg", dstImage);
		srcImage = dstImage.clone();
		
		if (srcImage.channels() > 1) {
			Imgproc.cvtColor(srcImage, dstImage, Imgproc.COLOR_BGRA2GRAY);
		}	
		
		//initial_tfm_matrix=scale_matrix*args.initial_tfm_matrix*inv(scale_matrix);
	    //center=floor(transform_point(args.center, scale_matrix));
//	    focus_size=floor(args.focus_size/2^total_scale);
//	    f_branch=zeros(3, 2*args.branch_accuracy+1);
//	    Dotau_branch=cell(3, 2*args.branch_accuracy+1);
//	    result_tfm_matrix=cell(3, 2*args.branch_accuracy+1);
		dstImage.convertTo(srcImage, CvType.CV_64FC1);
		initialTfmMatrix = Util.multiply(Util.multiply(scaleMat, args.initialTfmMatrix), scaleMat.inv());
		double powTotalScale = Math.pow(2.0, totalScale);
		Size focusSize = new Size(Math.floor(args.focusSize.width/powTotalScale), Math.floor(args.focusSize.height /powTotalScale));
		timer.stop();
		System.out.println("[step 1]:" + timer.getTotalTime());

		// step 2: design branch-and-bound method
//	    f_branch=zeros(3, 2*args.branch_accuracy+1);
//	    Dotau_branch=cell(3, 2*args.branch_accuracy+1);
//	    result_tfm_matrix=cell(3, 2*args.branch_accuracy+1);
//        max_rotation=args.branch_max_rotation;
//        max_skew=args.branch_max_skew;
//        level=3;
//        candidate_matrix=cell(3, 2*args.branch_accuracy+1);
		timer.begin();
		int branchAcc = 2*args.branchAccuracy+1;
		Mat fBranch = Mat.zeros(3, branchAcc, CvType.CV_64FC1);
		Cells resultTfmMatrix = new Cells(3, branchAcc);
		Cells candidateMatrix = new Cells(3, branchAcc);
		double maxRotation = args.branchMaxRotation;
		double maxSkew = args.branchMaxSkew;
		for (int ix = 0; ix < branchAcc; ++ix) {
			candidateMatrix.putCell(0, ix, Mat.eye(3, 3, CvType.CV_64FC1));
			double theta = -maxRotation+ix*maxRotation/args.branchAccuracy;
			double sinTheta = Math.sin(theta);
			double cosTheta = Math.cos(theta);
			candidateMatrix.setValue(0, ix, 0, 0, cosTheta);
			candidateMatrix.setValue(0, ix, 0, 1, -sinTheta);
			candidateMatrix.setValue(0, ix, 1, 0, sinTheta);
			candidateMatrix.setValue(0, ix, 1, 1, cosTheta);		
			double value = -maxSkew+ix*maxSkew/args.branchAccuracy;
			candidateMatrix.putCell(1, ix, Mat.eye(3, 3, CvType.CV_64FC1));
			candidateMatrix.setValue(1, ix, 0, 1, value);		
			candidateMatrix.putCell(2, ix, Mat.eye(3, 3, CvType.CV_64FC1));
			candidateMatrix.setValue(2, ix, 1, 0, value);
		}
		
		timer.stop();
		System.out.println("[step 2]:" + timer.getTotalTime());
		
		// step 3: begin branch-and-bound
		timer.begin();
		int level = 3;
		for (int i = 0; i < level; ++i) {
			for (int j = 0; j < branchAcc; ++j) {
				//tfm_matrix=inv( candidate_matrix{i, j}*inv(initial_tfm_matrix) );
				//inv_tfm_matrix = inv(tfm_matrix');
	            //tfm=maketform('affine', inv_tfm_matrix);
	            //Dotau=imtransform(input_image, tfm, 'bilinear', 'XData', XData, 'YData', YData, 'UData', UData, 'VData', VData);
				Mat tfmMatrix = Util.multiply(candidateMatrix.getCell(i, j), initialTfmMatrix.inv());
				tfmMatrix = tfmMatrix.inv();				
				dstImage = ImageUtil.imageTransform(srcImage, tfmMatrix, focusSize, Imgproc.INTER_LINEAR | Imgproc.WARP_INVERSE_MAP);
//				Highgui.imwrite(dirPath + _index + ".jpg", dstImage);
//				++_index; 
				
//	            Dotau=Dotau/norm(Dotau, 'fro');
//	            [U S V]=svd(Dotau);
//	            f=sum(sum(S));
//	           f_branch(i, j)=f;
//	           Dotau_branch{i, j}=Dotau;
//	           result_tfm_matrix{i, j}=tfm_matrix;				
				Mat resMat = new Mat(dstImage.size(), CvType.CV_64FC1);
				double norm = Core.norm(dstImage, Core.NORM_L2);
				Core.divide(dstImage, new Scalar(norm), resMat);
				
				if (resMat.type() != CvType.CV_64FC1) {
					resMat.convertTo(resMat, CvType.CV_64FC1);
				} 
				
				Mat s = new Mat();
				Core.SVDecomp(resMat, s, new Mat(), new Mat(), Core.SVD_NO_UV);		
				double f = Core.sumElems(s).val[0];
				fBranch.put(i, j, f);
				resultTfmMatrix.putCell(i, j, tfmMatrix);
				s.release();
			}
			
//	        [value index]=min(f_branch(i, :));
//	        initial_tfm_matrix=result_tfm_matrix{i, index};
			Util.MinValue minValue = Util.min(fBranch.submat(i, i+1, 0, branchAcc));
			int index = minValue.col;
			initialTfmMatrix = resultTfmMatrix.getCell(i, index);
		}
		
		timer.stop();
		System.out.println("[step 3]:" + timer.getTotalTime());
		
		// step 4: adapt initial_tfm_matrix to highest-resolution
		//initial_tfm_matrix=inv(scale_matrix)*initial_tfm_matrix*scale_matrix;
		initialTfmMatrix = Util.multiply(Util.multiply(scaleMat.inv(), initialTfmMatrix), scaleMat);
		
		/**
		 * do pyramid
		 */
		// define parameters
		// downsample_matrix=[0.5 0 0; 0 0.5 0; 0 0 1];
		// total_scale=ceil(max(log2(min(args.focus_size)/args.focus_threshold), 0));
		Util.initMat(downsampleMat, new double[][]{{0.5, 0, 0},{0, 0.5, 0},{0, 0, 1}});
		minLength = Math.min(args.focusSize.width, args.focusSize.height);
		log2Value = Math.log(minLength/(double)args.focusThreshold) / Math.log(2);
		totalScale = Math.ceil(Math.max(log2Value, 0));
		
		for (int scale = (int)totalScale; scale >= 0; --scale) {
			// begin each level of the pyramid
			if (totalScale-scale >= args.pyramidMaxLevel) {
				break;
			}
			
			// prepare initial tfm_matrix
//	        scale_matrix=downsample_matrix^scale;
//	        tfm_matrix=scale_matrix*initial_tfm_matrix*inv(scale_matrix);
//			initial_tfm_matrix=inv(scale_matrix)*tfm_matrix*scale_matrix;	
			scaleMat = Util.power(downsampleMat, scale);
			Mat tfmMatrix = Util.multiply(Util.multiply(scaleMat, initialTfmMatrix), scaleMat.inv());
			
			// update tfmMatrix of the highest-resolution level
			initialTfmMatrix = Util.multiply(Util.multiply(scaleMat.inv(), tfmMatrix), scaleMat);
		}
		
		Mat tfmMatrix = initialTfmMatrix;
		args = originalArgs;
		tfmMatrix = Util.multiply(Util.multiply(preScaleMat, tfmMatrix), preScaleMat.inv());
		//Mat resImage = ImageUtil.imageTransform(args.srcImage, tfmMatrix, Imgproc.INTER_CUBIC | Imgproc.WARP_INVERSE_MAP);
		Mat resImage = ImageUtil.imageTransform(args.srcImage, tfmMatrix, args.focusSize, Imgproc.INTER_CUBIC | Imgproc.WARP_INVERSE_MAP);

		return resImage;
	}
}
