package edu.NUDT.PDL.optimization;

import edu.NUDT.PDL.gradient.MFWithGradient;
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
	
	//on return, alpha0, obj0, dx0,ogcalls
	/**
	 * TODO: ogfun selection!
	 */
	public void compute(Vector x,double[] obj,
			Vector dx0,Vector direction, MFWithGradient ogfun,			
			double[]alpha0,int[]ogcalls){
		
		 int seciter = 5; //% maximum number of quadratic interpolation iterations
		 // double alpha0 = 1e-10;
		  double c1 = 1e-4; //% required decrease in objective (relative to gradient)
		  double  c2 = 1e-2;// % required decrease in directional derivative
		  int digits = 12; //% digits of precision to use for objective comparisons
		  double  gamma = 10;
		  int verbose = 2;
		  
		  //copy the old obj
		  double[]obj0={obj[0]};
		  
		  
		  double[] oldalpha=new double[1];
		  double[] oldobj=new double[1];
		  
		  /**
		   * gradient
		   */
		  Vector dx=new DenseVector(dx0);
		  
		  Vector olddx=new DenseVector(dx);
		  
		  double[]preAlpha={0};
		  int []doBacktrack={0};
		  
		  if(alpha0!=null&&alpha0[0]<=0){
			  System.err.println("alpha0 must be greater than zero");
		  }
		  //double obj = obj0[0];
		  double etazero = dx.dotProduct(direction);
		  double etaprev = etazero;
		  //double alpha=alpha0[0];
		  
		  ogcalls[0]=0;
		  findNonZeroAlpha1(x,obj0,
					dx, direction,  ogfun,alpha0,ogcalls,gamma,preAlpha);
		  //directly update the array
		  obj0[0]=ogfun.evaluate(x.add(direction.scale(alpha0[0])).asArray(), dx.asArray());
		  
		  ogcalls[0]++;
		  saveAlpha(oldalpha, alpha0,
			oldobj , obj0,
			olddx , dx);
		 // % Make sure current step yields decrease in objective
		  backtrack1(x,obj0,
					dx, direction,  ogfun,
					alpha0,ogcalls,gamma,
					digits,c1, etazero,
					 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj);
		  
		  double beta = alpha0[0];
		  double eta = dx.dotProduct(direction);
		  int i=0;
		  
		  while( (Math.abs(eta) > c2*Math.abs(etazero)) && 
				  (i < seciter) &&
(Conjgrad_gradient.pround(obj0[0],digits) <= Conjgrad_gradient.pround(obj[0],digits)) &&
				   (etaprev != eta) &&
				   (x.add(direction.scale(alpha0[0])).diffItems(x)>0)){
			  
			  beta=(eta*beta)/(etaprev-eta);
			  saveAlpha(oldalpha, alpha0,
						oldobj , obj0,
						olddx , dx);
			  alpha0[0]+=beta;
			  
			  if(alpha0[0]<=0){
				 alpha0[0]=1; 
			  }
			  
			  etaprev=eta;
			  i++;
			  obj0[0]=ogfun.evaluate(x.add(direction.scale(alpha0[0])).asArray(), dx.asArray());
			  ogcalls[0]++;
			  eta=dx.dotProduct(direction);
	  
		  }
		  //================================
		  
		  findNonZeroAlpha1(x,obj0,
					dx, direction,  ogfun,
					alpha0,ogcalls,gamma,preAlpha);
		  backtrack1(x,obj0,
					dx, direction,  ogfun,
					alpha0,ogcalls,gamma,
					digits,c1, etazero,
					 oldalpha, oldobj, olddx,preAlpha,doBacktrack,obj);
		  
		  
		  //on return, we update  items,
		  //alpha0, obj0, dx0,ogcs
		  obj[0]=obj0[0];
		  dx.assignValue(dx0);
	}

	private void backtrack1(Vector x,double[] obj0,
			Vector dx,Vector direction, MFWithGradient ogfun,
			double[]alpha0,int[]ogcalls,double gamma,int digits,double c1,double etazero,
			double[] oldalpha,double[] oldobj,Vector olddx,double[]preAlpha,int []doBacktrack,double[]o_obj) {
		// TODO Auto-generated method stub
		doBacktrack[0]=0;
		preAlpha[0]=alpha0[0];
		
		while ((Conjgrad_gradient.pround(obj0[0],digits) > Conjgrad_gradient.pround(o_obj[0] + c1*alpha0[0]*etazero,digits)) &&
				(x.add(direction.scale(alpha0[0])).diffItems(x)>0)){
		 if(ogcalls[0]>1){
			 if ((oldalpha[0] > (alpha0[0]/gamma)) && 
				(oldobj[0] < o_obj[0] + c1*oldalpha[0]*etazero)){
				 alpha0[0] = oldalpha[0];
				 obj0[0] = oldobj[0];
				 DenseVector.copyVector(olddx, dx);
				 doBacktrack[0] = 1;
			 }
			
		 }
		 alpha0[0]=alpha0[0]/gamma;
		 obj0[0]=ogfun.evaluate(x.add(direction.scale(alpha0[0])).asArray(), dx.asArray());
		 ogcalls[0]++;
		 doBacktrack[0] = 1;
		}
		
		if(doBacktrack[0]>0&&verbose>3){
		  System.out.format("Backtracking preAlpha=%.2e alpha=%.2e\n",preAlpha[0],alpha0[0]);
		}
	}

	private void saveAlpha(double[] oldalpha,double[] alpha,
			double []oldobj,double[]obj,
			Vector olddx,Vector  dx) {
		// TODO Auto-generated method stub
		oldalpha[0] = alpha[0];
		oldobj[0] = obj[0];		
		DenseVector.copyVector(dx,olddx);

	}

	private void findNonZeroAlpha1(Vector x,double[] obj0,
			Vector dx,Vector direction, MFWithGradient ogfun,
			double[]alpha0,int[]ogcalls,double gamma,double[]preAlpha) {
		// TODO Auto-generated method stub
		//double preAlpha=1;
		if(x.add(direction.scale(alpha0[0])).diffItems(x)<=0){
			preAlpha[0]=alpha0[0];
			while(x.add(direction.scale(alpha0[0])).diffItems(x)<=0){
				alpha0[0]=alpha0[0]*gamma;				
			}
			obj0[0]=ogfun.evaluate(x.add(direction.scale(alpha0[0])).asArray(), dx.asArray());
			ogcalls[0]++;
		}
	}
}
