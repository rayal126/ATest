package com.tilt.algorithm;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

public final class InnerIALMConstraints {
	public static void innerIALMConstraints(Mat image, Mat jacobiMat, Mat constraints, Arguments args) {
//        tol=inner_para.tol;
//        c=inner_para.c;
//        mu=inner_para.mu;
//        display_period=inner_para.display_period;
//        max_iter=inner_para.max_iter;

		Util.checkArgumentsAreNotNull(image, jacobiMat, constraints, args);
		
		// prepare data
//	    [m n]=size(Dotau);
//	    E=zeros(m, n);
//	    A=zeros(m, n);
//	    p=size(J, 3);
//	    delta_tau=zeros(p, 1);
		Size size = image.size();
		
		int p = jacobiMat.channels();
		Mat deltaTau = Mat.zeros(p, 1, CvType.CV_64FC1);
		
//
//    J_vec=reshape(J, m*n, p);
//    Jo=J_vec;
//    J_vec=[J_vec; S_J];
//%     pinv_J_vec=pinv(J_vec);
//    pinv_J_vec=inv(J_vec);
//    inner_round=0;
//    rho=1.25;
//    lambda=c/sqrt(m);
//
//    Y_1=Dotau;
//    norm_two=norm(Y_1, 2);
//    norm_inf=norm(Y_1(:), inf)/lambda;
//    dual_norm=max(norm_two, norm_inf);
//    Y_1=Y_1/dual_norm;
//    Y_2=zeros(size(S_J, 1), 1);
//    d_norm=norm(Dotau, 'fro');
//    error_sign=0;
//    first_f=sum(svd(Dotau));
//catch
//    error_sign=1;
//    A=Dotau;
//    E=zeros(size(Dotau));
//    delta_tau=zeros(size(J, 3), 1);
//    f=inf;
//    return;
//end
//
//
//%% begin main loop
//while 1
//    try
//        inner_round=inner_round+1;
//        temp_0=Dotau+reshape(Jo*delta_tau, m, n)+Y_1/mu;
//        temp_1=temp_0-E;
//        [U S V]=svd(temp_1, 'econ');
//        A=U*((S>1/mu).*(S-1/mu))*V';
//        temp_2=temp_0-A;
//		E=(temp_2>lambda/mu).*(temp_2-lambda/mu)+(temp_2<-lambda/mu).*(temp_2+lambda/mu);
//        f=sum(sum(abs((S>1/mu).*(S-1/mu))))+lambda*sum(sum(abs(E)));
//        temp_3=A+E-Dotau-Y_1/mu;
//        temp_3=reshape(temp_3, m*n, 1);
//        temp_3=[temp_3; -Y_2/mu];
//        delta_tau=pinv_J_vec*temp_3;
//        derivative_Y_1=Dotau-A-E+reshape(Jo*delta_tau, m, n);
//        derivative_Y_2=S_J*delta_tau;
//        Y_1=Y_1+derivative_Y_1*mu;
//        Y_2=Y_2+derivative_Y_2*mu;
//    catch
//        error_sign=1;
//        A=Dotau;
//        E=zeros(size(Dotau));
//        delta_tau=zeros(size(J, 3), 1);
//        f=first_f;
//        return;
//    end
//    %% judge error
//    if f<first_f/3
//        error_sign=1;
//        A=Dotau;
//        E=0;
//        f=first_f;
//        delta_tau=zeros(size(J, 3), 1);
//        return;
//    end
//    %% display
//    stop_criterion=sqrt(norm(derivative_Y_1, 'fro')^2+norm(derivative_Y_2, 2)^2)/d_norm;
//    if stop_criterion<tol || inner_round >max_iter
//        break;
//    end
//    if mod(inner_round, display_period)==0
//        disp(['        inner round ', num2str(inner_round), ' stop_criterion=',num2str(stop_criterion), 'rank(A)=',num2str(rank(A_new)), '||E||_1=',num2str(sum(sum(abs(E_new))))]);
//    end
//
//    %% update
//    mu=mu*rho;
//end
	}
}
