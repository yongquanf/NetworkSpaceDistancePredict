package edu.NUDT.PDL.BasicDistributedMMMF;

import edu.NUDT.PDL.optimization.Conjgrad_gradient;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;

public class CgLineSearch4_oneHost {

	
	int verbose=1;
	
	static  CgLineSearch4_oneHost self=null;
	public static  CgLineSearch4_oneHost getInstance(){
		if(self==null){
			self=new  CgLineSearch4_oneHost();
		}
		return self;
	}
	
	
	public void compute(Vector x0,double[] obj2,
			Vector dx0, Vector direction, M3fshc_ALPS ogfun,			
			double[]alpha2,int[]ogcalls, double alpha00){
	
	
	 int seciter = 5; //% maximum number of quadratic interpolation iterations
	 // double alpha0 = 1e-10;
	  double c1 = 1e-4; //% required decrease in objective (relative to gradient)
	  double  c2 = 1e-2;// % required decrease in directional derivative
	  int digits = 12; //% digits of precision to use for objective comparisons
	  double  gamma = 10;

	  
	  //copy the old obj
	  double[]obj={obj2[0]};
	  
	  
	  double[] oldalpha=new double[1];
	  double[] oldobj=new double[1];
	  
	  /**
	   * gradient
	   */
	  Vector dx=new DenseVector(dx0);
	  
	  Vector olddx=new DenseVector(dx.numDimensions());
	  
	  double[]preAlpha={0};
	  int []doBacktrack={0};
	  
	 
	  //double obj = obj0[0];
	  double etazero = dx0.dotProduct(direction);
	  double etaprev = etazero;

		//init alpha0
	  double []alpha={alpha00};
	  
	  ogcalls[0]=0;
	  findNonZeroAlpha(x0,obj,
				dx, direction,  ogfun,alpha,ogcalls,gamma,preAlpha);
	  //directly update the array
	  double[] dd2=new double[dx.numDimensions()];			  
	  obj[0]=ogfun.evaluate(x0.add(direction.scale(alpha[0])).asArray(),
			  dd2);
	  dx.copyFrom(dd2);
	  dd2=null;
	  
	  ogcalls[0]++;
	  saveAlpha(oldalpha, alpha,
		oldobj , obj,
		olddx , dx);
	 // % Make sure current step yields decrease in objective
	  backtrack(x0,obj,
				dx, direction,  ogfun,
				alpha,ogcalls,gamma,
				digits,c1, etazero,
				 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj2);
	  
	  double beta = alpha[0];
	  double eta = dx.dotProduct(direction);
	  int i=0;
	  
	  while( (Math.abs(eta) > c2*Math.abs(etazero)) && 
			  (i < seciter) &&
(Conjgrad_gradient.pround(obj[0],digits) <= Conjgrad_gradient.pround(obj2[0],digits)) &&
			   (etaprev != eta) &&
			   ((x0.add(direction.scale(alpha[0]))).diffItems(x0)>0)){
		  
		  beta=(eta*beta)/(etaprev-eta);
		  
		  saveAlpha(oldalpha, alpha,
					oldobj , obj,
					olddx , dx);
		  alpha[0]+=beta;
		  
		  if(alpha[0]<=0){
			 alpha[0]=1; 
		  }
		  
		  etaprev=eta;
		  i++;
		  double []dd=new double[dx.numDimensions()];
		  obj[0]=ogfun.evaluate((x0.add(direction.scale(alpha[0]))).asArray(), dd);
		  dx.copyFrom(dd);
		  
		  ogcalls[0]++;
		  eta=dx.dotProduct(direction);
 
	  }
	  //================================
	  
	  findNonZeroAlpha(x0,obj,
				dx, direction,  ogfun,
				alpha,ogcalls,gamma,preAlpha);
	  backtrack(x0,obj,
				dx, direction,  ogfun,
				alpha,ogcalls,gamma,
				digits,c1, etazero,
				 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj2);
	  
	  
	  //on return, we update  items,
	  //alpha0, obj0, dx0,ogcs
	  obj2[0]=obj[0];
	  dx.assignValue(dx0);
	  alpha2[0]=alpha[0];
}
	//on return, alpha0, obj0, dx0,ogcalls
	/**
	 * TODO: ogfun selection!
	 */
	public void compute1(int ind,Vector x0,double[] obj0,
			Vector dx0, Vector direction, M3fshc_ALPS ogfun,			
			double[]alpha2,int[]ogcalls, double alpha00){
		
		
		 int seciter = 5; //% maximum number of quadratic interpolation iterations
		 // double alpha0 = 1e-10;
		  double c1 = 1e-4; //% required decrease in objective (relative to gradient)
		  double  c2 = 1e-2;// % required decrease in directional derivative
		  int digits = 12; //% digits of precision to use for objective comparisons
		  double  gamma = 10;

		  
		  //copy the old obj
		  double[]obj={obj0[0]};
		  
		  
		  double[] oldalpha=new double[1];
		  double[] oldobj=new double[1];
		  
		  /**
		   * gradient
		   */
		  Vector dx=new DenseVector(dx0);
		  
		  Vector olddx=new DenseVector(dx.numDimensions());
		  
		  double[]preAlpha={0};
		  int []doBacktrack={0};
		  
		 
		  //double obj = obj0[0];
		  double etazero = dx0.dotProduct(direction);
		  double etaprev = etazero;

			//init alpha0
		  double []alpha={alpha00};
		  
		  ogcalls[0]=0;
		  findNonZeroAlpha(x0,obj,
					dx, direction,  ogfun,alpha,ogcalls,gamma,preAlpha);
		  //directly update the array
		  double[] dd2=new double[dx.numDimensions()];			  
		  obj[0]=ogfun.evaluate(x0.add(direction.scale(alpha[0])).asArray(),
				  dd2);
		  dx.copyFrom(dd2);
		  dd2=null;
		  
		  ogcalls[0]++;
		  saveAlpha(oldalpha, alpha,
			oldobj , obj,
			olddx , dx);
		 // % Make sure current step yields decrease in objective
		  backtrack(x0,obj,
					dx, direction,  ogfun,
					alpha,ogcalls,gamma,
					digits,c1, etazero,
					 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj0);
		  
		  double beta = alpha[0];
		  double eta = dx.dotProduct(direction);
		  int i=0;
		  
		  while( (Math.abs(eta) > c2*Math.abs(etazero)) && 
				  (i < seciter) &&
(Conjgrad_gradient.pround(obj[0],digits) <= Conjgrad_gradient.pround(obj0[0],digits)) &&
				   (etaprev != eta) &&
				   ((x0.add(direction.scale(alpha[0]))).diffItems(x0)>0)){
			  
			  beta=(eta*beta)/(etaprev-eta);
			  
			  saveAlpha(oldalpha, alpha,
						oldobj , obj,
						olddx , dx);
			  alpha[0]+=beta;
			  
			  if(alpha[0]<=0){
				 alpha[0]=1; 
			  }
			  
			  etaprev=eta;
			  i++;
			  double []dd=new double[dx.numDimensions()];
			  obj[0]=ogfun.evaluate((x0.add(direction.scale(alpha[0]))).asArray(), dd);
			  dx.copyFrom(dd);
			  
			  ogcalls[0]++;
			  eta=dx.dotProduct(direction);
	  
		  }
		  //================================
		  
		  findNonZeroAlpha(x0,obj,
					dx, direction,  ogfun,
					alpha,ogcalls,gamma,preAlpha);
		  backtrack(x0,obj,
					dx, direction,  ogfun,
					alpha,ogcalls,gamma,
					digits,c1, etazero,
					 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj0);
		  
		  
		  //on return, we update  items,
		  //alpha0, obj0, dx0,ogcs
		  obj0[0]=obj[0];
		  dx.assignValue(dx0);
		  alpha2[0]=alpha[0];
	}

	private void backtrack(Vector x,double[] obj,
			Vector dx,Vector direction, M3fshc_ALPS ogfun,
			double[]alpha,int[]ogcalls,double gamma,int digits,double c1,double etazero,
			double[] oldalpha,double[] oldobj,Vector olddx,double[]preAlpha,int []doBacktrack,double[]o_obj) {
		// TODO Auto-generated method stub
		doBacktrack[0]=0;
		preAlpha[0]=alpha[0];
		
		while ((Conjgrad_gradient.pround(obj[0],digits) > Conjgrad_gradient.pround(o_obj[0] + c1*alpha[0]*etazero,digits)) &&
				((x.add(direction.scale(alpha[0]))).diffItems(x)>0)){
		 if(ogcalls[0]>1){
			 if ((oldalpha[0] > (alpha[0]/gamma)) && 
				(oldobj[0] < o_obj[0] + c1*oldalpha[0]*etazero)){
				 alpha[0] = oldalpha[0];
				 obj[0] = oldobj[0];
				 DenseVector.copyVector(olddx, dx);
				 doBacktrack[0] = 1;
				 break;
			 }
			
		 }
		 alpha[0]=alpha[0]/gamma;
		 double[]dd=new double[dx.numDimensions()];
		 obj[0]=ogfun.evaluate(x.add(direction.scale(alpha[0])).asArray(), dd);
		 dx.copyFrom(dd);
		 dd=null;
		 
		 ogcalls[0]++;
		 doBacktrack[0] = 1;
		}
/*			if(doBacktrack[0]>0&&verbose>3){
		  System.out.format("Backtracking preAlpha=%.2e alpha=%.2e\n",preAlpha[0],alpha[0]);
		}*/
	}

	private void saveAlpha(double[] oldalpha,double[] alpha,
			double []oldobj,double[]obj,
			Vector olddx,Vector  dx) {
		// TODO Auto-generated method stub
		oldalpha[0] = alpha[0];
		oldobj[0] = obj[0];		
		DenseVector.copyVector(dx,olddx);

	}

	private void findNonZeroAlpha(Vector x0,double[] obj0,
			Vector dx,Vector direction, M3fshc_ALPS ogfun,
			double[]alpha,int[]ogcalls,double gamma,double[]preAlpha) {
		// TODO Auto-generated method stub
		//double preAlpha=1;
		if((x0.add(direction.scale(alpha[0]))).diffItems(x0)==0){
			preAlpha[0]=alpha[0];
			while((x0.add(direction.scale(alpha[0]))).diffItems(x0)==0){
				alpha[0]=alpha[0]*gamma;				
			}
			double[] dd = new double[dx.numDimensions()];
			obj0[0]=ogfun.evaluate(x0.add(direction.scale(alpha[0])).asArray(), dd);
			dx.copyFrom(dd);
			dd=null;
			
			ogcalls[0]++;
		}
	}
}