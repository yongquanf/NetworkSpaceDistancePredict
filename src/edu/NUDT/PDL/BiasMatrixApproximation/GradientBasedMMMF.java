package edu.NUDT.PDL.BiasMatrixApproximation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.NUDT.PDL.BasicDistributedMMMF.CgLineSearch4_oneHost;
import edu.NUDT.PDL.BasicDistributedMMMF.M3fshc_ALPS;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;
import edu.harvard.syrah.prp.Log;

public class GradientBasedMMMF<T> {

	private static Log log=new Log(GradientBasedMMMF.class);
	//public static double[] regvals={ 7.4989,    5.6234 ,   4.2170 ,   3.1623  ,  2.3714 ,   3.3598 };
	//public static double[] regvals={ 7.5,5.6,4.2,3.4,3.2,2.4 };
	public static double[] regvals={1.5849,
	    1.4125,
	    1.2589,
	    1.1220,
	    1.0593};
	static GradientBasedMMMF self=null;
	
	public GradientBasedMMMF(){}
	/**
	 * get instance
	 * @return
	 */
	public static GradientBasedMMMF getInstance(){
		if(self==null){
			self=new GradientBasedMMMF();
		}
		return self;
	}
	
	
	/**
	 * for host, bias MMMF, randomized initial position
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_U
	 * @param x_V
	 * @param x_theta
	 * @param x_b
	 * @param dim
	 * @param level
	 * @return
	 */
	public  List<ratingCoord<T>> compute(Matrix host2Landmarks,Matrix landamrk2Host,Matrix x_U,Matrix x_V,
			Matrix x_theta,Matrix x_b,int dim,int level){
		int n=host2Landmarks.numRows();
		int p=dim;
		int l=level;
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n);
		return compute_BiasMMMF(x0, host2Landmarks, landamrk2Host, x_U, x_V,
				 x_theta, x_b, dim,level);
	}
	
	/**
	 * for host, bias MMMF,
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_U
	 * @param x_V
	 * @param x_theta
	 * @param x_b
	 * @param dim
	 * @param level
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>>  compute_biasMMMF(List<T> hosts,Matrix host2Landmarks,Matrix landamrk2Host,Matrix x_U,Matrix x_V,
			Matrix x_theta,Matrix x_b,int dim,int level){
		int n=host2Landmarks.numRows();
		int p=dim;
		int l=level;
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n);
		List<ratingCoord<T>> tt = compute_BiasMMMF(x0, host2Landmarks, landamrk2Host, x_U, x_V,
				 x_theta, x_b, dim,level);
		//save to the hashtable
		Hashtable<T,ratingCoord<T>> out=new Hashtable<T,ratingCoord<T>>(2);
		int index=0;
		if(hosts.size()!=tt.size()){
			log.warn("un-equal hosts and the returned rating coordinates!");
		}
		Iterator<ratingCoord<T>> ier = tt.iterator();
		while(ier.hasNext()){
			ratingCoord<T> rec = ier.next();
			out.put(hosts.get(index), rec.copy());			
			 index++;
		}
		return out;
	}
	
	/**
	 * update
	 * @param x0
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param x_b
	 * @param dim
	 * @param level
	 * @return
	 */
	private List<ratingCoord<T>> compute_BiasMMMF(Vec x0,
			Matrix host2Landmarks, Matrix landamrk2Host, Matrix x_u,
			Matrix x_v, Matrix x_theta, Matrix x_b, int dim, int level) {
		// TODO Auto-generated method stub
		//no of hosts
		int n=host2Landmarks.numRows();
		int NLandmarks=host2Landmarks.numColumns();
		int p=dim;
		int l=level;
		double max1=10000000;
		//Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n); 
		Matrix x_UL= x0.reshape(0, n, p);;
		Matrix x_VL=x0.reshape(n*p, n, p);
		Matrix x_thetaL=x0.reshape(n*p+n*p, n, l-1);
		Matrix x_bL=x0.reshape(n*p+n*p+n*(l-1), n, 1);
			
		Matrix TotalBias=new DenseMatrix(n,NLandmarks);

		
		Vec vv=null;
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
			
			//v=BiasConjgrad_gradientBias.getInstance().compute(x0, dim, level, host2Landmarks, landamrk2Host,x_u,x_v,x_theta,x_b,regvals[i3]);
			
			//update the coordinate
			x0=BiasConjgrad_gradientBias.getInstance().compute(x0, dim, level, host2Landmarks, landamrk2Host,x_u,
					x_v,x_theta,x_b,regvals[i3]);
			//t2=System.currentTimeMillis()-t1;
			//System.out.println("total cost: "+t2/1000+" Secs");
			//v=x0;
			
			Matrix x_U1=x0.reshape(0, n, p);
			Matrix x_V1=x0.reshape(n*p, n, p);
			Matrix x_theta1=x0.reshape(n*p+n*p, n, l-1);
			Matrix x_b1=x0.reshape(n*p+n*p+n*(l-1), n, 1);
			
			for(int i=0;i<n;i++){
				for(int j=0;j<NLandmarks;j++){
					TotalBias.setValue(i, j,  x_b1.value(i, 0)+x_b.value(j, 0));
				}
				
			}
			//==========================================
			//from host to landmarks
			Matrix X_h2landmarks=(x_U1.multiply(x_v)).add(TotalBias);
			if(X_h2landmarks==null){
				System.err.println("X_h2landmarks is null!");
			}
			//Matrix X_l2host=x_U.multiply(x_V1).add(TotalBias);
			
			Matrix y= MyConvergeMMMF_bias.m3fSoftmax(X_h2landmarks,x_theta1);
			
			
			double AMae=MyConvergeMMMF_bias.mae(y,host2Landmarks);
		
			if(AMae<max1){
			   max1=AMae;
			  vv=x0;				
			}			
		}
		//=====================================
		//reshape
		//=================================
		x_UL=  vv.reshape(0, n, p);;
		x_VL= vv.reshape(n*p, n, p);
		x_thetaL= vv.reshape(n*p+n*p, n, l-1);
		x_bL= vv.reshape(n*p+n*p+n*(l-1), n, 1);

		
		//====================================	
		 List<ratingCoord<T>> info =new ArrayList<ratingCoord<T>> (2);
		 for(int i=0;i<x_UL.numRows();i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector( x_UL.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_VL.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_thetaL.rowVector(i));
			 coord.Bias=Vec.getVecFromVector(x_bL.rowVector(i));
			 info.add(coord);
		 }
		 return info;
		 
		
	}
	
	
	/**
	 * update one's coordinate
	 * @param x
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param x_b
	 * @param dim
	 * @param level
	 * @param lambda
	 * @return
	 */
	public ratingCoord<T>  compute_biasConjugateGradient(ratingCoord<T> x,
			Matrix host2Landmarks, Matrix landamrk2Host, Matrix x_u, Matrix x_v,
			Matrix x_theta, Matrix x_b,int dim, int level, double lambda){
		
		int n=1;
		int p=dim;
		int l=level;
		
		Vec x0=new Vec(x.p.num_dims+x.q.num_dims+x.thetaVec.num_dims+x.Bias.num_dims);
		System.arraycopy(x.p.direction, 0, x0.direction, 0, x.p.num_dims);
		System.arraycopy(x.q.direction,0,x0.direction,x.p.num_dims,x.q.num_dims);
		System.arraycopy(x.thetaVec.direction,0,x0.direction,x.p.num_dims+x.q.num_dims,x.thetaVec.num_dims);
		System.arraycopy(x.Bias.direction,0,x0.direction,x.p.num_dims+x.q.num_dims+x.thetaVec.num_dims,x.Bias.num_dims);
		
		//gradient function
		
		BiasMatrixRatingGradient ogfun=new BiasMatrixRatingGradient(dim, level, host2Landmarks,landamrk2Host,
				x_u,x_v,	x_theta, x_b, lambda);
		
		  double[]dx00=new double[x0.num_dims];
		
		  //oject
		  Vector obj_1 = ogfun.evaluate_Vector(x0.direction,dx00);
		
		  //gradient
		  Vector dx=new DenseVector(dx00);
		  
		  //alpha
		  double []alpha ={ 1e-10};
		
		  Vector orig_x = x0.getVectorFromVec();
		
		    double alpha00=alpha[0];

		    Vector r = dx.scale(-1);
			  Vector d = new DenseVector(r);
			  
		    Vector dd = new DenseVector(d);	
		    
		    int[]ogc={0};
		    double[] obj={obj_1.value(0)};
		    //obj,dx,alpha,ogc,
		    CgLineSearch4_oneHost_Bias.getInstance().compute(orig_x , obj, dx, dd, ogfun,
		    		alpha, ogc,alpha00);
		    dd.clear();			    		    
		    orig_x=orig_x.add(d.scale(alpha[0]));
		    
		    ratingCoord<T> newCoord = x.copy_bias( orig_x,dim,level);
		    return newCoord;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * based on a host's position and the landmarks' positions, new coordinate is computed by ncg
	 * @param x0
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param dim
	 * @param x_U
	 * @param x_V
	 * @param x_theta
	 * @param x_b
	 * @param level
	 * @return
	 */
	public  List<ratingCoord<T>> compute(Vec x0,Matrix host2Landmarks,Matrix landamrk2Host,Matrix x_U,Matrix x_V,
			Matrix x_theta,Matrix x_b,int dim,int level){
		
		//no of hosts
		int n=host2Landmarks.numRows();
		int NLandmarks=host2Landmarks.numColumns();
		int p=dim;
		int l=level;
		double max1=1000;
		//Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n); 
		Matrix x_UL= x0.reshape(0, n, p);;
		Matrix x_VL=x0.reshape(n*p, n, p);
		Matrix x_thetaL=x0.reshape(n*p+n*p, n, l-1);
		Matrix x_bL=x0.reshape(n*p+n*p+n*(l-1), n, 1);
			
		Matrix TotalBias=new DenseMatrix(n,NLandmarks);

		
		Vec v;
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
			
			v=BiasConjgrad_gradientBias.getInstance().compute(x0, dim, level, host2Landmarks, landamrk2Host,x_U,x_V,x_theta,x_b,regvals[i3]);
			
			Matrix x_U1=v.reshape(0, n, p);
			Matrix x_V1=v.reshape(n*p, n, p);
			Matrix x_theta1=v.reshape(n*p+n*p, n, l-1);
			Matrix x_b1=v.reshape(n*p+n*p+n*(l-1), n, 1);
			
			for(int i=0;i<n;i++){
				for(int j=0;j<NLandmarks;j++){
					TotalBias.setValue(i, j,  x_b1.value(i, 0)+x_b.value(j, 0));
				}
				
			}
			//==========================================
			//from host to landmarks
			Matrix X_h2landmarks=x_U1.multiply(x_V).add(TotalBias);
			if(X_h2landmarks==null){
				System.err.println("X_h2landmarks is null!");
			}
			//Matrix X_l2host=x_U.multiply(x_V1).add(TotalBias);
			
			Matrix y= MyConvergeMMMF_bias.m3fSoftmax(X_h2landmarks,x_theta1);
			
			
			double AMae=MyConvergeMMMF_bias.mae(y,host2Landmarks);
		
			if(AMae<max1){
			   max1=AMae;
			   x_UL = x_U1;  //%[n,p]
			   x_VL = x_V1;// %[n,p]
			   x_thetaL = x_theta1;//% [n,l-1]
			   x_bL=x_b1;				
			}
			//=================================
			Vector xu1=x_UL.flat();
			Vector xv1=x_VL.flat();
			Vector xt1=x_thetaL.flat();
			Vector xb1=x_bL.flat();			
			
			x0=new Vec(xu1.numDimensions()+xv1.numDimensions()
					+xt1.numDimensions()+xb1.numDimensions());
			
			for(int i=0;i<x0.num_dims;i++){
				if(i<xu1.numDimensions()){
					x0.direction[i]=xu1.value(i);
				}else if(i>=xu1.numDimensions()&&
						(i<xu1.numDimensions()+xv1.numDimensions())){
					x0.direction[i]=xv1.value(i-xu1.numDimensions());
				}else if((i>=xu1.numDimensions()+xv1.numDimensions())&&
						(i<(xu1.numDimensions()+xv1.numDimensions()
								+xt1.numDimensions()))){
					x0.direction[i]=xt1.value(i-(xu1.numDimensions()+xv1.numDimensions()));
				}else {
					x0.direction[i]=xb1.value(i-
							(xu1.numDimensions()+xv1.numDimensions()+xt1.numDimensions()));
				}
				
			}
			
		}
		
		//====================================	
		 List<ratingCoord<T>> info =new ArrayList<ratingCoord<T>> (2);
		 for(int i=0;i<x_UL.numRows();i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector( x_UL.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_VL.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_thetaL.rowVector(i));
			 coord.Bias=Vec.getVecFromVector(x_bL.rowVector(i));
			 info.add(coord);
		 }
		 return info;
		 
		
	}
	
	/**
	 * basic method
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param u
	 * @param v
	 * @param theta
	 * @param dim
	 * @param level
	 * @return
	 */
	
	public List<ratingCoord<T>> compute_basicMMMF1(Matrix host2Landmarks,
			Matrix landamrk2Host, Matrix x_u, Matrix x_v, Matrix x_theta, 
			int dim, int level) {
		// TODO Auto-generated method stub
		
		int n=host2Landmarks.numRows();
		int NLandmarks=host2Landmarks.numColumns();
		int p=dim;
		int l=level;
		
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1));
		
		
		double max1=1000;
		//Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n); 
		Matrix x_U= x0.reshape(0, n, p);
		Matrix x_V=x0.reshape(n*p, n, p);
		Matrix x_T=x0.reshape(n*p+n*p, n, l-1);
			
		Vec v;
		Vec  VV=null;
		
		int repeat=3;
		for(int indRepeat=0;indRepeat<repeat;indRepeat++){
			 x0=Vec.randVec(n*p+n*p+n*(l-1));
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
		
			System.out.println("rept: "+indRepeat+", regvals: "+regvals[i3]);
			//v=BiasConjgrad_gradientBias.getInstance().compute(x0, dim, level, host2Landmarks, landamrk2Host,x_u,x_v,x_theta,x_b,regvals[i3]);
			
			//t1=System.currentTimeMillis();
			BiasConjgrad_gradientBias.getInstance().compute_MMMF(x0, dim, level, host2Landmarks, landamrk2Host,x_u,
					x_v,x_theta,regvals[i3]);
			//t2=System.currentTimeMillis()-t1;
			//System.out.println("total cost: "+t2/1000+" Secs");
			v=x0;
			
			Matrix x_U1=v.reshape(0, n, p);
			Matrix x_V1=v.reshape(n*p, n, p);
			Matrix x_theta1=v.reshape(n*p+n*p, n, l-1);
					
			//==========================================
			//from host to landmarks
			Matrix X_h2landmarks=x_U1.multiply(x_v);
			if(X_h2landmarks==null){
				System.err.println("X_h2landmarks is null!");
			}
			//Matrix X_l2host=x_U.multiply(x_V1).add(TotalBias);
			
			Matrix y= MyConvergeMMMF_bias.m3fSoftmax(X_h2landmarks,x_theta1);
			
			
			double AMae=MyConvergeMMMF_bias.mae(y,host2Landmarks);
		
			if(AMae<max1){
			   max1=AMae;
			   if(VV!=null){
				   VV.clear();
			   }
			   VV=v.copy();
			}
			//=================================			
		}
		
		}
		//====================================	
		x_U= VV.reshape(0, n, p);;
		x_V=VV.reshape(n*p, n, p);
		x_T=VV.reshape(n*p+n*p, n, l-1);
		
		
		 List<ratingCoord<T>> info =new ArrayList<ratingCoord<T>> (2);
		 for(int i=0;i<x_U.numRows();i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector( x_U.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_V.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_T.rowVector(i));
			 
			 info.add(coord);
		 }
		 
		 x_U.clear();
		 x_V.clear();
		 x_T.clear();
		 
		 return info;
		 
		
	}
	
	/**
	 * new version,
	 * only one host is permited once
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param x_u
	 * @param x_v
	 * @param x_theta
	 * @param dim
	 * @param level
	 * @param lambda
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> compute_basicMMMF(List<T> hosts,Matrix host2Landmarks,
			Matrix landamrk2Host, Matrix x_u, Matrix x_v, Matrix x_theta, 
			int dim, int level,double lambda) {
		// TODO Auto-generated method stub
		int n=host2Landmarks.numRows();
		int p=dim;
		int l=level;
		
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1));
		
		x0=BiasConjgrad_gradientBias.getInstance().compute_MMMF(x0, dim, level, host2Landmarks, landamrk2Host,x_u,
					x_v,x_theta, lambda);
			//t2=System.currentTimeMillis()-t1;
			//System.out.println("total cost: "+t2/1000+" Secs");
							
		//====================================	
			Matrix x_U= x0.reshape(0, n, p);;
			Matrix x_V=x0.reshape(n*p, n, p);
			Matrix x_T=x0.reshape(n*p+n*p, n, l-1);
		
		
		Hashtable<T,ratingCoord<T>> info =new Hashtable<T,ratingCoord<T>> (2);
		 for(int i=0;i<n;i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector( x_U.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_V.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_T.rowVector(i));
			 info.put(hosts.get(i),coord);
		 }
		 
		 x_U.clear();
		 x_V.clear();
		 x_T.clear();
		 
		 return info;
		 
		
	}
	
	/**
	 * update in one step
	 * @param host2Landmarks
	 * @param transpose
	 * @param u
	 * @param v
	 * @param theta
	 * @param dim
	 * @param level
	 * @param lambda
	 * @return
	 */
	public ratingCoord<T> compute_conjugateGradient(ratingCoord<T> x,
			Matrix host2Landmarks, Matrix landamrk2Host, Matrix u, Matrix v,
			Matrix theta, int dim, int level, double lambda) {
		// TODO Auto-generated method stub
		int n=1;
		int p=dim;
		int l=level;
		
		Vec x0=new Vec(x.p.num_dims+x.q.num_dims+x.thetaVec.num_dims);
		System.arraycopy(x.p.direction, 0, x0.direction, 0, x.p.num_dims);
		System.arraycopy(x.q.direction,0,x0.direction,x.p.num_dims,x.q.num_dims);
		System.arraycopy(x.thetaVec.direction,0,x0.direction,x.p.num_dims+x.q.num_dims,x.thetaVec.num_dims);
		
		
		//gradient function
		M3fshc_ALPS ogfun=new M3fshc_ALPS(dim,level,host2Landmarks,landamrk2Host,
				u,v, theta,lambda);
		
		
		  double[]dx00=new double[x0.num_dims];
		
		  //oject
		  Vector obj_1 = ogfun.evaluate_Vector(x0.direction,dx00);
		
		  //gradient
		  Vector dx=new DenseVector(dx00);
		  
		  //alpha
		  double []alpha ={ 1e-10};
		
		  Vector orig_x = x0.getVectorFromVec();
		
		    double alpha00=alpha[0];

		    Vector r = dx.scale(-1);
			  Vector d = new DenseVector(r);
			  
		    Vector dd = new DenseVector(d);	
		    
		    int[]ogc={0};
		    double[] obj={obj_1.value(0)};
		    //obj,dx,alpha,ogc,
		    CgLineSearch4_oneHost.getInstance().compute(orig_x , obj, dx, dd, ogfun,
		    		alpha, ogc,alpha00);
		    dd.clear();			    		    
		    orig_x=orig_x.add(d.scale(alpha[0]));
		    
		    ratingCoord<T> newCoord = x.copy( orig_x,dim,level);
		    return newCoord;
	}
}
