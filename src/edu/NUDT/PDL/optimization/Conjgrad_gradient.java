package edu.NUDT.PDL.optimization;

import edu.NUDT.PDL.BasicDistributedMMMF.CgLineSearch;
import edu.NUDT.PDL.BasicDistributedMMMF.CgLineSearch4_oneHost;
import edu.NUDT.PDL.BasicDistributedMMMF.M3fshc_ALPS;
import edu.NUDT.PDL.BasicDistributedMMMF.m3fshc;
import edu.NUDT.PDL.BiasMatrixApproximation.BiasMatrixRatingGradient;
import edu.NUDT.PDL.BiasMatrixApproximation.CgLineSearch4_oneHost_Bias;
import edu.NUDT.PDL.BiasMatrixApproximation.CgLineSearch_BiasLandmark;
import edu.NUDT.PDL.BiasMatrixApproximation.M3fshc_bias_Landmark;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;
import edu.harvard.syrah.prp.Log;

public class Conjgrad_gradient {

	private static Log log=new Log(Conjgrad_gradient.class);
	
	double tol=1e-3;
	int maxiter=100;
	
	static Conjgrad_gradient self=null;
	
	public static Conjgrad_gradient getInstance(){
		if(self==null){
			self=new Conjgrad_gradient();
		}
		return self;
	}
	
	/**
	 * conjugate gradient method
	 * @param x0, on return,!
	 * @param p
	 * @param l
	 * @param newd_out
	 * @param newd_in
	 * @param ogfun
	 * @return
	 */
	public Vector   compute(Vector x0, int dim, int level,M3fshc_bias_Landmark ogfun){
		

		 double tol = 1e-3; //% geometric decrease in gradient magnitude to declare minimum
		 int  maxiter = 100; //% stop after this many iterations (if no minimum found)
		 double  nu = 0.1;
		 double abstol = 0; //% stop if gradient magnitude goes below this
		 double allowNonDecrease = 0; //% don't stop if line search fails to find decrease
		 int digits = 12; //% digits of precision to use for objective comparisons
		 

		  long t0 = System.currentTimeMillis();
		  long t1 = t0;
		  int ogcalls = 0;
		  //coordinate
		  //Vector x=new DenseVector(x0);
		  
		  
		  int numiter = 0;
		  int j = 0;
		  double[] alpha = {1e-10};

		  ogcalls = ogcalls + 1;
		  double beta;
		  //copy of x0
		  Vector x = new DenseVector(x0);
		  double[]dx0=new double[x.numDimensions()];
		  
		  double[] obj = {ogfun.evaluate(x.asArray(), dx0)};
		  
		  double c2=1e-2;
		  
		  //gradient vector
		  Vector dx=new DenseVector(dx0);
		  
		  Vector r = dx.scale(-1);
		  Vector s = new DenseVector(r);
		  Vector d = new DenseVector(s);
		  double deltanew = r.dotProduct(d);
		  
		  double deltazero = deltanew;
		  
		  double prevobj=0,deltaold=0,deltamid=0;
		  
		 		  
		  //iteration
		  while( (numiter < maxiter) && 
				  (Math.abs(deltanew) > tol*tol*Math.abs(deltazero)) 
				  && (Math.abs(deltanew) > abstol)){
			  
			  //System.out.format("i=%d alpha=%1.3e delta=%1.3e obj=%1.3e Calls: %d\n",numiter,alpha[0],deltanew,obj[0],ogcalls);
				
			  	numiter = numiter + 1;
			    j = j + 1;
			    
			    prevobj = obj[0];
			    
			    if(alpha[0] < 1e-10){
			      alpha[0] = 1e-10;
			      }

			    
			    //line search
			    int[]ogc={0};
			    
			    //TODO: line search can not find better result! BUG!
			    double alpha00=alpha[0];
			    Vector x1 = new DenseVector(x);			    
			    DenseVector dd = new DenseVector(d);	
			    //obj,dx,alpha,ogc,
			    CgLineSearch_BiasLandmark.getInstance().compute(x1, obj, dx, dd, ogfun,
			    		alpha, ogc,alpha00);
			    dd.clear();			    
			    x1.clear();
			    
			    ogcalls+=ogc[0];
			    
			    
			   // oldObj=obj[0];
			    
			    x=x.add(d.scale(alpha[0]));
			    
			    if(j==1&& 
			      (pround(obj[0],digits) > pround(prevobj,digits))&&
			    		(allowNonDecrease==0)){
			    	//log.warn("Line search could not find decrease in direction of negative gradient \n");
			    	return x;
			    }
			    
			    r=dx.scale(-1);
			    deltaold = deltanew;
			    deltamid=r.dotProduct(s);
			    deltanew=r.dotProduct(r);
			    
			    beta = (deltanew-deltamid)/deltaold;
			    d=r.add(d.scale(Math.max(0, beta)));
			    
			    if((deltamid/deltanew) >= nu || d.dotProduct(dx) >= 0){
			    	//System.err.print("RESET\n");
			    	d=r.copy();
			    	j=0;
			    }
			    s=r.copy();			    
		  }
		 //System.out.format("T=%d i=%d delta=%1.3e obj=%1.3e Calls: %d\n",(System.currentTimeMillis()-t0)/1000,numiter,deltanew,obj[0],ogcalls);
		  String str="T="+(System.currentTimeMillis()-t0)/1000+" i="+numiter+" delta="+deltanew+" obj="+obj[0]+" Calls: "+ogcalls;
		  log.info(str);
		  //on return, copy x to x0
		 // x.assignValue(x0);
		  return x;
	}


	/**
	 * Rounds number to a given number of significant digits.
	 * @param prevobj
	 * @param digits
	 * @return
	 */
	public static double pround(double x, double d) {
		// TODO Auto-generated method stub
		d=Math.round(d);
		if(d<1){
			System.err.println("Number of digits must be integer");
		}
		double y;
		if(x==0||Double.isNaN(x)||Double.isInfinite(x)){
			y=x;
		}else{
			double p = Math.floor(Math.log10(Math.abs(x)))+1;
			double factor=Math.pow(10, d-p);
			y=Math.round(x*factor)/factor;
		}
		return y;
	}

	/**
	 * basic MMMF computation process
	 * Conjugate Gradients minimization routine
	 * for landmarks
	 * @param vectorFromVec
	 * @param dim
	 * @param level
	 * @param g0
	 */
	public Vector  conjgrad(Vector x0, int dim, int level, m3fshc ogfun) {
		// TODO Auto-generated method stub

		 double tol = 1e-3; //% geometric decrease in gradient magnitude to declare minimum
		 int  maxiter = 100; //% stop after this many iterations (if no minimum found)
		 double  nu = 0.1;
		 double abstol = 0; //% stop if gradient magnitude goes below this
		 double allowNonDecrease = 0; //% don't stop if line search fails to find decrease
		 int digits = 12; //% digits of precision to use for objective comparisons
		 

		  long t0 = System.currentTimeMillis();
		  long t1 = t0;
		  int ogcalls = 0;
		  //coordinate
		  //Vector x=new DenseVector(x0);
		  
		  
		  int numiter = 0;
		  int j = 0;
		  double[] alpha = {1e-10};

		  ogcalls = ogcalls + 1;
		  double beta;
		  //copy of x0
		  Vector x = new DenseVector(x0);
		  double[]dx0=new double[x.numDimensions()];
		  
		  double[] obj = {ogfun.evaluate(x.asArray(), dx0)};
		  
		  double c2=1e-2;
		  
		  //gradient vector
		  Vector dx=new DenseVector(dx0);
		  
		  Vector r = dx.scale(-1);
		  Vector s = new DenseVector(r);
		  Vector d = new DenseVector(s);
		  double deltanew = r.dotProduct(d);
		  
		  double deltazero = deltanew;
		  
		  double prevobj=0,deltaold=0,deltamid=0;
		  
		 		  
		  //iteration
		  while( (numiter < maxiter) && 
				  (Math.abs(deltanew) > tol*tol*Math.abs(deltazero)) 
				  && (Math.abs(deltanew) > abstol)){
			  
			  System.out.format("i=%d alpha=%1.3e delta=%1.3e obj=%1.3e Calls: %d\n",numiter,alpha[0],deltanew,obj[0],ogcalls);
				
			  	numiter = numiter + 1;
			    j = j + 1;
			    
			    prevobj = obj[0];
			    
			    if(alpha[0] < 1e-10){
			      alpha[0] = 1e-10;
			      }

			    
			    //line search
			    int[]ogc={0};
			    
			    //TODO: line search can not find better result! BUG!
			    double alpha00=alpha[0];
			    Vector x1 = new DenseVector(x);			    
			    DenseVector dd = new DenseVector(d);	
			    //obj,dx,alpha,ogc,
			    CgLineSearch.getInstance().compute(x1, obj, dx, dd, ogfun,
			    		alpha, ogc,alpha00);
			    dd.clear();			    
			    x1.clear();
			    
			    ogcalls+=ogc[0];
			    
			    
			   // oldObj=obj[0];
			    
			    x=x.add(d.scale(alpha[0]));
			    
			    if(j==1&& 
			      (pround(obj[0],digits) > pround(prevobj,digits))&&
			    		(allowNonDecrease==0)){
			    	//log.warn("Line search could not find decrease in direction of negative gradient \n");
			    	return x;
			    }
			    
			    r=dx.scale(-1);
			    deltaold = deltanew;
			    deltamid=r.dotProduct(s);
			    deltanew=r.dotProduct(r);
			    
			    beta = (deltanew-deltamid)/deltaold;
			    d=r.add(d.scale(Math.max(0, beta)));
			    
			    if((deltamid/deltanew) >= nu || d.dotProduct(dx) >= 0){
			    	//System.err.print("RESET\n");
			    	d=r.copy();
			    	j=0;
			    }
			    s=r.copy();			    
		  }
		  //System.out.format("T=%d i=%d delta=%1.3e obj=%1.3e Calls: %d\n",(System.currentTimeMillis()-t0)/1000,numiter,deltanew,obj[0],ogcalls);
		 // String str="T="+(System.currentTimeMillis()-t0)/1000+" i="+numiter+" delta="+deltanew+" obj="+obj[0]+" Calls: "+ogcalls;
		  //log.info(str);
		  //on return, copy x to x0
		 // x.assignValue(x0);
		  return x;
			}
	
	/**
	 * bias MMMF
	 * @param x
	 * @param dim
	 * @param level
	 * @param ogfun
	 * @return
	 */
	public Vec newHosts_MMMF(Vec x, int dim, int level,
			BiasMatrixRatingGradient ogfun) {
		// TODO Auto-generated method stub
		
		 double tol = 1e-3; //% geometric decrease in gradient magnitude to declare minimum
		 int  maxiter = 100; //% stop after this many iterations (if no minimum found)
		 double  nu = 0.1;
		 double abstol = 0; //% stop if gradient magnitude goes below this
		 double allowNonDecrease = 0; //% don't stop if line search fails to find decrease
		 int digits = 12; //% digits of precision to use for objective comparisons
		 

		  long t0 = System.currentTimeMillis();
		  long t1 = t0;
		  int ogcalls = 0;
		  //coordinate
		  //Vector x=new DenseVector(x0);
		  int n=ogfun.D_host2landmark.numRows();
		  int p=dim;
		  int l=level;
		  
		  Matrix x_U = x.reshape(0, n, p);
		  Matrix x_V=x.reshape(n*p, n, p);
		  Matrix x_theta=x.reshape(n*p+n*p, n, l-1);
		  Matrix x_bias=x.reshape(n*p+n*p+n*(l-1), n, 1);
		  
		  double[]dx00=new double[x.num_dims];
		  
		  Vector obj_all = ogfun.evaluate_Vector(x.direction,dx00);
		  Vec dx_all=new Vec(dx00,true);
		  
		  Matrix dU_dx = dx_all.reshape(0, n, p);
		  Matrix dV_dx = dx_all.reshape(n*p, n, p);
		  Matrix dtheta_dx =dx_all.reshape(n*p+n*p, n, l-1);
		  Matrix db_dx=dx_all.reshape(n*p+n*p+n*(l-1), n, 1);
		  
		  int numiter = 0;
		  int j = 0;
		  double[] alpha = {1e-10};
		  
		  double beta=0;
		  Vector dx;
		  Vec dx0;
		  
		//iterate
		  for(int i=0;i<n;i++){
			  ogcalls = 0;
			  numiter = 0;
			  j = 0;
			  alpha[0] = 1e-10;
			  ogcalls = ogcalls + 1;
			  
			  Vec xx = concat(x_U.rowVector(i),x_V.rowVector(i),
					  x_theta.rowVector(i), x_bias.rowVector(i));
			  dx0=concat(dU_dx.rowVector(i),dV_dx.rowVector(i),
					  dtheta_dx.rowVector(i),db_dx.rowVector(i));
			  
			  Vector orig_x = xx.getVectorFromVec();
			  
			  double[] obj={obj_all.value(i)};
			//gradient vector
			  dx=dx0.getVectorFromVec();
			  Vector r = dx.scale(-1);
			  Vector s = new DenseVector(r);
			  Vector d = new DenseVector(s);
			  double deltanew = r.dotProduct(d);
			  
			  double deltazero = deltanew;
			  
			  //---------------------------------------------
			  //stop condition
			  
			  
			  double prevobj,deltaold,deltamid;
			  
			  //
			  //int repeatedMAX=10;
			  ///double oldObj=10000000000d;
			   
			   
			   //iteration
				  while( (numiter < maxiter) && 
						  (Math.abs(deltanew) > tol*tol*Math.abs(deltazero)) 
						  && (Math.abs(deltanew) > abstol)){
					  
					  	numiter = numiter + 1;
					    j = j + 1;
					    
					    prevobj = obj[0];
					    
					    if(alpha[0] < 1e-10){
					      alpha[0] = 1e-10;
					      }
					 /*   if( curRepeat>=repeatedMAX){
					    	System.err.print("too many repeated process with identical obj value\n");
					    	break;
					    }*/
					    
					    //line search
					    int[]ogc={0};
					    
					    //directly work the original array
					    double alpha00=alpha[0];
					    Vector x1 = new DenseVector( orig_x);			    
					    Vector dd = new DenseVector(d);	
					    //obj,dx,alpha,ogc,
					    CgLineSearch4_oneHost_Bias.getInstance().compute(x1, obj, dx, dd, ogfun,
					    		alpha, ogc,alpha00);
					    dd.clear();			    
					    x1.clear();
					    
					    
					    ogcalls+=ogc[0];
			    
					    orig_x=orig_x.add(d.scale(alpha[0]));
					    
					    if(j==1&& (pround(obj[0],digits) > pround(prevobj,digits))&&(allowNonDecrease==0)){
					    	//System.err.print("Line search could not find decrease in direction of negative gradient \n");
					    	return  Vec.getVecFromVector(orig_x);
					    }
					    r=dx.scale(-1);
					    
					    
					    deltaold = deltanew;
					    deltamid=r.dotProduct(s);
					    deltanew=r.dotProduct(r);
					    
					    beta = (deltanew-deltamid)/deltaold;
					    d=r.add(d.scale(Math.max(0, beta)));
					    if((deltamid/deltanew) >= nu || d.dotProduct(dx) >= 0){
					    	//System.err.print("RESET\n");
					    	d=r.copy();
					    	j=0;
					    }
					    s=r.copy();			    
				  }
				  
				  //copy the data back to x!
				 for(int indU=0;indU<p;indU++){
					 x_U.setValue(i, indU, orig_x.value(indU) );
					 x_V.setValue(i, indU, orig_x.value(p+indU));
				 }
				 for(int indT=0;indT<l-1;indT++){
					 x_theta.setValue(i, indT, orig_x.value(p*2+indT));
				 }
				 //bias
				 x_bias.setValue(i, 0, orig_x.value(p*2+(l-1)));
				  //
				  //on return, copy x to x0
				// System.out.format("T=%d i=%d delta=%1.3e obj=%1.3e Calls: %d\n",(System.currentTimeMillis()-t0)/1000,numiter,deltanew,obj[0],ogcalls);  
		  }
		//update x	  
		x=concat(x_U.flat(),x_V.flat(),x_theta.flat(),x_bias.flat());  
	
		return x;  				
	}

	private Vec concat(Vector v1, Vector v2, Vector v3,
			Vector v4) {
		// TODO Auto-generated method stub
		int len1=v1.numDimensions();
		int len2=v2.numDimensions();
		int len3=v3.numDimensions();
		int len4=v4.numDimensions();
		
		Vec out=new Vec(len1+len2+len3+ len4);
		
		for(int i=0;i<out.num_dims;i++){
			if(i<len1){
				out.direction[i]=v1.value(i);
			}else if(i>=len1&&i<(len1+len2)){
				out.direction[i]=v2.value(i-len1);
			}else if((i>=(len1+len2))&&i<(len1+len2+len3)){
				out.direction[i]=v3.value(i-len1-len2);
			}else{
				out.direction[i]=v4.value(i-len1-len2-len3);
			}
		}
		return out;
	}

	/**
	 * new hosts based MMMF
	 * @param vectorFromVec
	 * @param dim
	 * @param level
	 * @param g0
	 */
	public Vec newHosts_MMMF(Vec x, int dim, int level,
			M3fshc_ALPS ogfun) {
		// TODO Auto-generated method stub
		
		 double tol = 1e-3; //% geometric decrease in gradient magnitude to declare minimum
		 int  maxiter = 100; //% stop after this many iterations (if no minimum found)
		 double  nu = 0.1;
		 double abstol = 0; //% stop if gradient magnitude goes below this
		 double allowNonDecrease = 0; //% don't stop if line search fails to find decrease
		 int digits = 12; //% digits of precision to use for objective comparisons
		 

		  long t0 = System.currentTimeMillis();
		  long t1 = t0;
		  int ogcalls = 0;
		  //coordinate
		  //Vector x=new DenseVector(x0);
		  int n=ogfun.D_host2landmark.numRows();
		  int p=dim;
		  int l=level;
		  
		  Matrix x_U = x.reshape(0, n, p);
		  Matrix x_V=x.reshape(n*p, n, p);
		  Matrix x_theta=x.reshape(n*p+n*p, n, l-1);
		  
		  double[]dx00=new double[x.num_dims];
		  
		  Vector obj_all = ogfun.evaluate_Vector(x.direction,dx00);
		  Vec dx_all=new Vec(dx00,true);
		  
		  Matrix dU_dx = dx_all.reshape(0, n, p);
		  Matrix dV_dx = dx_all.reshape(n*p, n, p);
		  Matrix dtheta_dx =dx_all.reshape(n*p+n*p, n, l-1);
		  
		  int numiter = 0;
		  int j = 0;
		  double[] alpha = {1e-10};
		  
		  double beta=0;
		  Vector dx;
		  Vec dx0;
		  
		//iterate
		  for(int i=0;i<n;i++){
			  ogcalls = 0;
			  numiter = 0;
			  j = 0;
			  alpha[0] = 1e-10;
			  ogcalls = ogcalls + 1;
			  
			  Vec xx = concat(x_U.rowVector(i),x_V.rowVector(i),x_theta.rowVector(i));
			  dx0=concat(dU_dx.rowVector(i),dV_dx.rowVector(i),dtheta_dx.rowVector(i));
			  
			  Vector orig_x = xx.getVectorFromVec();
			  
			  double[] obj={obj_all.value(i)};
			//gradient vector
			  dx=dx0.getVectorFromVec();
			  Vector r = dx.scale(-1);
			  Vector s = new DenseVector(r);
			  Vector d = new DenseVector(s);
			  double deltanew = r.dotProduct(d);
			  
			  double deltazero = deltanew;
			  
			  //---------------------------------------------
			  //stop condition
			  
			  
			  double prevobj,deltaold,deltamid;
			  
			  //
			  //int repeatedMAX=10;
			  ///double oldObj=10000000000d;
			   
			   
			   //iteration
				  while( (numiter < maxiter) && 
						  (Math.abs(deltanew) > tol*tol*Math.abs(deltazero)) 
						  && (Math.abs(deltanew) > abstol)){
					  
					  	numiter = numiter + 1;
					    j = j + 1;
					    
					    prevobj = obj[0];
					    
					    if(alpha[0] < 1e-10){
					      alpha[0] = 1e-10;
					      }
					 /*   if( curRepeat>=repeatedMAX){
					    	System.err.print("too many repeated process with identical obj value\n");
					    	break;
					    }*/
					    
					    //line search
					    int[]ogc={0};
					    
					    //directly work the original array
					    double alpha00=alpha[0];
					    Vector x1 = new DenseVector( orig_x);			    
					    Vector dd = new DenseVector(d);	
					    //obj,dx,alpha,ogc,
					    CgLineSearch4_oneHost.getInstance().compute(x1, obj, dx, dd, ogfun,
					    		alpha, ogc,alpha00);
					    dd.clear();			    
					    x1.clear();
					    
					    
					    ogcalls+=ogc[0];
			    
					    orig_x=orig_x.add(d.scale(alpha[0]));
					    
					    if(j==1&& (pround(obj[0],digits) > pround(prevobj,digits))&&(allowNonDecrease==0)){
					    	//System.err.print("Line search could not find decrease in direction of negative gradient \n");
					    	return  Vec.getVecFromVector(orig_x);
					    }
					    r=dx.scale(-1);
					    
					    
					    deltaold = deltanew;
					    deltamid=r.dotProduct(s);
					    deltanew=r.dotProduct(r);
					    
					    beta = (deltanew-deltamid)/deltaold;
					    d=r.add(d.scale(Math.max(0, beta)));
					    if((deltamid/deltanew) >= nu || d.dotProduct(dx) >= 0){
					    	//System.err.print("RESET\n");
					    	d=r.copy();
					    	j=0;
					    }
					    s=r.copy();			    
				  }
				  
				  //copy the data back to x!
				 for(int indU=0;indU<p;indU++){
					 x_U.setValue(i, indU, orig_x.value(indU) );
					 x_V.setValue(i, indU, orig_x.value(p+indU));
				 }
				 for(int indT=0;indT<l-1;indT++){
					 x_theta.setValue(i, indT, orig_x.value(p*2+indT));
				 }
				  //
				  //on return, copy x to x0
				 //System.out.format("T=%d i=%d delta=%1.3e obj=%1.3e Calls: %d\n",(System.currentTimeMillis()-t0)/1000,numiter,deltanew,obj[0],ogcalls);  
		  }
		//update x	  
		x=concat(x_U.flat(),x_V.flat(),x_theta.flat());  
	
		return x;  				
	}

	/**
	 * connect three vectors
	 * @param v1
	 * @param v2
	 * @param v3
	 * @return
	 */
	private Vec concat(Vector v1, Vector v2, Vector v3) {
		// TODO Auto-generated method stub
		
		int len1=v1.numDimensions();
		int len2=v2.numDimensions();
		int len3=v3.numDimensions();
		Vec out=new Vec(len1+len2+len3);
		for(int i=0;i<len1+len2+len3;i++){
			if(i<len1){
				out.direction[i]=v1.value(i);
			}else if(i>=len1&&i<(len1+len2)){
				out.direction[i]=v2.value(i-len1);
			}else{
				out.direction[i]=v3.value(i-len1-len2);
			}		
		}
		return out;
	}
	
	
}
