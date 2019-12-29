package edu.NUDT.PDL.BiasMatrixApproximation;

import edu.NUDT.PDL.BasicDistributedMMMF.M3fshc_ALPS;
import edu.NUDT.PDL.BasicDistributedMMMF.m3fshc;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.optimization.Conjgrad_gradient;
import edu.NUDT.PDL.optimization.NonlinearConjugateGradient;
import edu.NUDT.PDL.optimization.outDS;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;

public class BiasConjgrad_gradientBias {

	  static int verbose = 2; //% print progress information if true
	  static double tol = 1e-3; //% geometric decrease in gradient magnitude to declare minimum
	  static int maxiter = 100; //% stop after this many iterations (if no minimum found)
	  static double nu = 0.1;
	  static double abstol = 0; //% stop if gradient magnitude goes below this
	  static double allowNonDecrease = 0; //% don't stop if line search fails to find decrease
	  static int  digits = 12; //% digits of precision to use for objective comparisons
	
	static BiasConjgrad_gradientBias self=null;
	
	public static BiasConjgrad_gradientBias getInstance(){
		if(self==null){
			self=new BiasConjgrad_gradientBias();
		}
		return self;
	}
	/**
	 * compute the coordinate vector for landmarks, the main entry for the conjugate gradient function
	 * @param x0
	 * @param dim
	 * @param level
	 * @param D_host2landmark
	 * @param D_host2landmark
	 * @param x_b 
	 * @param x_theta 
	 * @param x_v 
	 * @param x_u 
	 * @param tol
	 * @param tol 
	 * @param maxiter
	 * @param verbose
	 * @return
	 */
	public Vec compute1(Vec x0, int dim, int level,Matrix D_host2landmark, Matrix D_landmark2host, double lambda){
		
		 boolean isLandmark=true;
		 
		 BiasMatrixRatingGradient g0=new BiasMatrixRatingGradient(isLandmark, dim, level,D_host2landmark,D_landmark2host,lambda);
		 
		 NonlinearConjugateGradient test =new NonlinearConjugateGradient();
		 
		 //print(x0.direction,"x0");
		 
		 outDS out= test.optimize(g0, x0.direction);
		 
		 return new Vec(out.X,true);
		 
	}
	public static void print(double[] direction, String note) {
		// TODO Auto-generated method stub
		System.out.print(note+"\n");
		for(int i=0;i<direction.length;i++){
			System.out.print(direction[i]+" ");			
		}
		System.out.print("\n");
	}
	/**
	 * host 
	 * @param x0
	 * @param dim
	 * @param level
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param x_b
	 * @param _lambda
	 * @return
	 */
	public Vec compute(Vec x0, int dim, int level, Matrix host2Landmarks,
			Matrix landamrk2Host, Matrix x_u, Matrix x_v, Matrix x_theta,
			Matrix x_b,double _lambda) {
		// TODO Auto-generated method stub
		BiasMatrixRatingGradient g0=new BiasMatrixRatingGradient(dim, level, host2Landmarks,landamrk2Host,
				x_u,x_v,	x_theta, x_b, _lambda);
		
		
		Vec  tt;		
		tt=Conjgrad_gradient.getInstance().newHosts_MMMF(x0, dim, level, g0);
	    return tt;
	}

	/**
	 * based on our bias MMMF Matlab code
	 * @param x0
	 * @param dim
	 * @param level
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param x_b
	 * @param _lambda
	 * @return
	 */
	public Vec   compute_MMMF(Vec x0, int dim, int level, Matrix host2Landmarks,
			Matrix landamrk2Host,double _lambda) {
		// TODO Auto-generated method stub
		M3fshc_bias_Landmark g0=new M3fshc_bias_Landmark(dim, level, host2Landmarks,landamrk2Host,
				_lambda);
		
		Vector tt = Conjgrad_gradient.getInstance().compute(x0.getVectorFromVec(), dim, level, g0);
		
		return Vec.getVecFromVector(tt);
	}
	
	
	/**
	 * basic MMMF, landmarks
	 * @param x0
	 * @param dim
	 * @param level
	 * @param mat
	 * @param matTanspose
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param maxiter 
	 * @param d
	 */
	public Vec  compute_MMMF(Vec x0, int dim, int level, Matrix mat,
			double lambda,double tol, int maxiter) {
		// TODO Auto-generated method stub
		
		m3fshc g0=new m3fshc(dim,level,mat,lambda);
		Vector tt;		
		tt=Conjgrad_gradient.getInstance().conjgrad(x0.getVectorFromVec(), dim, level, g0);
		
		return Vec.getVecFromVector(tt);
		//tt.assignValue(x0);
		//tt.clear();
	}
	/**
	 * basic MMMF, hosts,
	 * @param x0
	 * @param dim
	 * @param level
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param lambda
	 */
	public Vec  compute_MMMF(Vec x0, int dim, int level, Matrix host2Landmarks,
			Matrix landamrk2Host, Matrix x_u, Matrix x_v, Matrix x_theta,
			double lambda) {
		// TODO Auto-generated method stub
		
		M3fshc_ALPS g0=new M3fshc_ALPS(dim,level,host2Landmarks,landamrk2Host,
				x_u,x_v, x_theta,lambda);	
		
		Vec  tt;		
		tt=Conjgrad_gradient.getInstance().newHosts_MMMF(x0, dim, level, g0);
	    return tt;
	}
	
}
