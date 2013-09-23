package com.tilt.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TiltKernel {
	public static void tiltKernel(Mat image, Point center, Size focusSize, Mat initialTfmMatrix, Arguments para) {
		// parse parameters
		//original_image=input_image;
//		if size(input_image, 3)>1
//	    input_image=input_image(:, :, 1)*0.299+input_image(:, :, 2)*0.587+input_image(:, :, 3)*0.144;
//	end
		// input_image=double(input_image);
		Mat originalImage = image.clone();
		System.out.println("In the tiltKernel:");
		System.out.println("Image channels:" + image.channels());
		
		if (image.channels() > 1) {
			Imgproc.cvtColor(originalImage, image, Imgproc.COLOR_BGRA2GRAY);
		}
		
		image.convertTo(image, CvType.CV_64FC1);
		
		// make base_points and focus_size integer, the effect of this operations remains to be tested
		// center=floor(center);
		// focus_size=floor(focus_size);
		center = Util.floor(center);
		focusSize = Util.floor(focusSize);

//		outer_tol=para.outer_tol;
//		outer_max_iter=para.outer_max_iter;
//		outer_display_period=para.outer_display_period;
//		inner_para=[];
//		inner_para.tol=para.inner_tol;
//		inner_para.c=para.inner_c;
//		inner_para.mu=para.inner_mu;
//		inner_para.display_period=para.inner_display_period;
//		inner_para.max_iter=para.inner_max_iter;
		
		// decide origin of the two axes
//		image_size=size(input_image);
//		image_center=floor(center);
//		focus_center=zeros(2, 1);
//		focus_center(1)=floor((1+focus_size(2))/2);
//		focus_center(2)=floor((1+focus_size(1))/2);
//		UData=[1-image_center(1) image_size(2)-image_center(1)];
//		VData=[1-image_center(2) image_size(1)-image_center(2)];
//		XData=[1-focus_center(1) focus_size(2)-focus_center(1)];
//		YData=[1-focus_center(2) focus_size(1)-focus_center(2)];
//		A_scale=1;
//		Dotau_series={};
		double aScale = 1;
		
		// prepare initial data
//		input_du=imfilter(input_image, -fspecial('sobel')'/8);
//		input_dv=imfilter(input_image, -fspecial('sobel')/8);
		Mat duMat = new Mat(3, 3, CvType.CV_64FC1);
		Util.initMat(duMat, new double[][]{{-0.125, 0.0, 0.125}, {-0.25, 0.0, 0.25}, {-0.125, 0.0, 0.125}});
		Mat dvMat = duMat.t();
		Mat inputDu = new Mat(image.size(), CvType.CV_64FC1);
		Mat inputDv = new Mat(image.size(), CvType.CV_64FC1);
		Imgproc.filter2D(image, inputDu, -1, duMat, new Point(1, 1), 0.0, Imgproc.BORDER_CONSTANT);
		Imgproc.filter2D(image, inputDv, -1, dvMat, new Point(1, 1), 0.0, Imgproc.BORDER_CONSTANT);
//		tfm_matrix=initial_tfm_matrix;
//		inv_1 = inv(tfm_matrix')
//		tfm=maketform('affine', inv_1);
//		Dotau=imtransform(input_image, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);
//		Dotau_series{1}=Dotau;
//		initial_image=Dotau;
		Mat tfmMatrix = initialTfmMatrix.clone();
		Mat invTfmMatrix = tfmMatrix.t().inv();
		Mat dstImage = ImageUtil.imageTransform(image, invTfmMatrix, focusSize, Imgproc.INTER_LINEAR);
		Mat initialImage = dstImage.clone();
//		du=imtransform(input_du, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);
//		dv=imtransform(input_dv, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);

		Mat du = ImageUtil.imageTransform(inputDu, invTfmMatrix, focusSize, Imgproc.INTER_LINEAR);
		Mat dv = ImageUtil.imageTransform(inputDv, invTfmMatrix, focusSize, Imgproc.INTER_LINEAR);
		double norm = Core.norm(dstImage, Core.NORM_L2);
//		du= du/norm(Dotau, 'fro')-(sum(sum(Dotau.*du)))/(norm(Dotau, 'fro')^3)*Dotau;		
//		dv= dv/norm(Dotau, 'fro')-(sum(sum(Dotau.*dv)))/(norm(Dotau, 'fro')^3)*Dotau;		
//		A_scale=norm(Dotau, 'fro');
//		Dotau=Dotau/norm(Dotau, 'fro');		
		du = formular(du, dstImage, norm);
		dv = formular(dv, dstImage, norm);
		aScale = norm;
		Core.divide(dstImage, new Scalar(norm), dstImage);
 
//		tau=tfm2para(tfm_matrix, XData, YData, mode);
//		J=jacobi(du, dv, XData, YData, tau, mode);
//		S=constraints(tau, XData, YData, mode);
		Point focusCenter = new Point(Math.floor(focusSize.width/2.0), Math.floor(focusSize.height/2.0));
		int beginX = 1 - (int)focusCenter.x;
		int endX = (int)(focusSize.width - focusCenter.x);
		int beginY = 1 - (int)focusCenter.y;
		int endY = (int)(focusSize.height - focusCenter.y);
		Mat tau = TiltKernel.tfmToPara(tfmMatrix);
		Mat jacobiMat = TiltKernel.jacobi(du, dv, beginX, endX, beginY, endY, tau);
		Mat constraints = TiltKernel.constraints(tau);
//		 
//		outer_round=0;
//		pre_f=0;
//		while 1
		// begin main loop
		int outerRound =0;
		while(true) {
//			   outer_round=outer_round+1;
//			   [A, E, delta_tau, f, error_sign]=inner_IALM_constraints(Dotau, J, S, inner_para);
//			   if error_sign==1
//			       return;
//			   end
			++outerRound;
			
			// update Dotau
//			   tau=tau+delta_tau;
//			   tfm_matrix=para2tfm(tau, XData, YData, mode);
//			   inv_2 = inv(tfm_matrix');
//			   tfm=maketform('affine', inv_2);
//			   Dotau=imtransform(input_image, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);
//			   Dotau_series{outer_round+1}=Dotau;
//			   %% judge convergence
//			   if outer_round>=outer_max_iter || abs(f-pre_f)<outer_tol
//			       break;
//			   end
//			   %% record data and prepare for the next round.
//			   pre_f=f;
//			   du=imtransform(input_du, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);
//			   dv=imtransform(input_dv, tfm, 'bilinear', 'UData', UData, 'VData', VData, 'XData', XData, 'YData', YData);
//			   du= du/norm(Dotau, 'fro')-(sum(sum(Dotau.*du)))/(norm(Dotau, 'fro')^3)*Dotau;
//			   dv= dv/norm(Dotau, 'fro')-(sum(sum(Dotau.*dv)))/(norm(Dotau, 'fro')^3)*Dotau;
//			   A_scale=norm(Dotau, 'fro');
//			   Dotau=Dotau/norm(Dotau, 'fro');
//			   J=jacobi(du, dv, XData, YData, tau, mode);
//			   S=constraints(tau, XData, YData, mode);
		}


	}
	
	/**
	 * convert the tfmMatrix(3x3) into a 4x1 column vector
	 * @param tfmMatrix
	 * @return
	 */
	private static Mat tfmToPara(Mat tfmMatrix) {
		Util.checkArgumentsAreNotNull(tfmMatrix);
		
		if ((tfmMatrix.rows() != 3) || (tfmMatrix.cols() != 3)) {
			throw new IllegalArgumentException("The size of the tfmMatrix shoubld be 3x3");
		}

		Mat res = Mat.zeros(4, 1, tfmMatrix.type());
		res.put(0, 0, tfmMatrix.get(0, 0));
		res.put(1, 0, tfmMatrix.get(0, 1));
		res.put(2, 0, tfmMatrix.get(1, 0));
		res.put(3, 0, tfmMatrix.get(1, 1));
		
		return res;
	}
	
	private static Mat jacobi(Mat du, Mat dv, int beginX, int endX, int beginY, int endY, Mat tau) {
//		[m n]=size(du);
//		[X0 Y0]=meshgrid(XData(1):XData(2), YData(1):YData(2));
		Util.checkArgumentsAreNotNull(du, dv, tau);
		List<Mat> mats = TiltKernel.meshgrid(beginX, endX, beginY, endY);
		Mat x = mats.get(0);
		Mat y = mats.get(1);
		
		// generate jacobi matrix
		// translation is just ambiguity, so we discard it in affine mode
		int rows = du.rows();
		int cols = du.cols();
		Mat res = Mat.zeros(rows, cols, CvType.CV_64F);
		Mat xu = x.mul(du);
		Mat yu = y.mul(du);
		Mat xv = x.mul(dv);
		Mat yv = y.mul(dv);
		
//		J=zeros(m, n, 4);
//        J(:, :, 1)=X0.*du;
//        J(:, :, 2)=Y0.*du;
//        J(:, :, 3)=X0.*dv;
//        J(:, :, 4)=Y0.*dv;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				double[] value = new double[4];
				value[0] = xu.get(i, j)[0];
				value[1] = yu.get(i, j)[0];
				value[2] = xv.get(i, j)[0];
				value[3] = yv.get(i, j)[0];
				res.put(i, j, value);
			}
		}
		
		return res;
	}
	
	private static Mat constraints(Mat tau) {
//        S=zeros(2, 4);
//        vec1=[tau(1);tau(3)];
//        vec2=[tau(2);tau(4)];
		Util.checkArgumentsAreNotNull(tau);
		
		int rows = tau.rows();
		int cols = tau.cols();
		Mat conMat = Mat.zeros(2, 4, CvType.CV_64FC1);
		Mat vec1 = new Mat(2, 1, CvType.CV_64FC1);
		Mat vec2 = new Mat(2, 1, CvType.CV_64FC1);
		double tau1 = tau.get(0, 0)[0];
		double tau2 =tau.get(1%rows, 1/rows)[0];
		double tau3 = tau.get(2%rows, 2/rows)[0];
		double tau4 = tau.get(3%rows, 3/rows)[0];
		Util.initMat(vec1, new double[][]{{tau1}, {tau3}});
		Util.initMat(vec2, new double[][]{{tau2}, {tau4}});
		
//      V=sqrt(norm(vec1)^2*norm(vec2)^2-(vec1'*vec2)^2);
//      S(1, 1)=(tau(1)*norm(vec2)^2-vec1'*vec2*tau(2))/V;
//      S(1, 2)=(tau(2)*norm(vec1)^2-vec1'*vec2*tau(1))/V;
//      S(1, 3)=(tau(3)*norm(vec2)^2-vec1'*vec2*tau(4))/V;
//      S(1, 4)=(tau(4)*norm(vec1)^2-vec1'*vec2*tau(3))/V;
		Mat s1 = new Mat();
		Mat s2 = new Mat();
		Core.SVDecomp(vec1, s1, new Mat(), new Mat(), Core.SVD_NO_UV);
		Core.SVDecomp(vec2, s2, new Mat(), new Mat(), Core.SVD_NO_UV);
		double norm1 = Util.min(s1).value;
		double norm2 = Util.min(s2).value;
		norm1 *= norm1;
		norm2 *= norm2;
		double product = Util.multiply(vec1.t(), vec2).get(0, 0)[0];
		double v = Math.sqrt(norm1*norm2-product*product);
		conMat.put(0, 0, (tau1*norm2-product*tau2)/v);
		conMat.put(0, 1, (tau2*norm1-product*tau1)/v);
		conMat.put(0, 2, (tau3*norm2-product*tau4)/v);
		conMat.put(0, 3, (tau4*norm1-product*tau3)/v);
//        S(2, 1)=2*tau(1);
//        S(2, 2)=-2*tau(2);
//        S(2, 3)=2*tau(3);
//        S(2, 4)=-2*tau(4);
		conMat.put(1, 0, 2*tau1);
		conMat.put(1, 1, 2*tau2);
		conMat.put(1, 2, 2*tau3);
		conMat.put(1, 3, 2*tau4);
        
		return conMat;
	}
	
	private static List<Mat> meshgrid(int beginX, int endX, int beginY, int endY) {
		if ((beginX > endX) || (beginY > endY)) {
			throw new IllegalArgumentException();
		}
		
		// initial the matrixs
		int cols = endX-beginX+1;
		int rows = endY-beginY+1;
		Size size = new Size(cols, rows);
		Mat x = new Mat(size, CvType.CV_32SC1);
		Mat y = new Mat(size, CvType.CV_32SC1);
		
		int value = beginX;
		for (int j = 0; j < cols; ++j) {
			for (int i = 0; i < rows; ++i) {
				x.put(i, j, value);
			}
			++value;
		}
		
		value = beginY;
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < cols; ++j) {
				y.put(i, j, value);
			}
			++value;
		}
		
		List<Mat> mats = new ArrayList<Mat>(2);
		mats.set(0, x);
		mats.set(1, y);
		return mats;
	}
	
	/**
	 * calculate d/norm-(sum(sum(dotau.*d)))/norm^3)*dotau;	
	 * @param d
	 * @param dotau
	 * @param norm
	 * @return
	 */
	private static Mat formular(Mat d, Mat dotau, double norm) {
		if ((d.rows() != dotau.rows()) || (d.cols() != dotau.cols())) {
			throw new IllegalArgumentException("The matrix d and the matrix dotau should have the same size");
		}
		
		Mat left = new Mat(d.size(), CvType.CV_64FC1);
		Core.divide(d, new Scalar(norm), left);
		Mat mulMat = dotau.mul(d);
		double scale = Core.sumElems(mulMat).val[0];
		scale /= (Math.pow(norm, 3.0));
		Mat right = new Mat(dotau.size(), CvType.CV_64FC1);
		Core.multiply(dotau, new Scalar(scale), right);
		Mat res = new Mat(d.size(), CvType.CV_64FC1);
		Core.subtract(left, right, res);
		left.release();
		right.release();
		
		return res;
	}
}
