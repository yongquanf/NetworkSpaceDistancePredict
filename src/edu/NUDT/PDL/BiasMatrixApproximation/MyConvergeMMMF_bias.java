package edu.NUDT.PDL.BiasMatrixApproximation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;
import edu.harvard.syrah.prp.Log;

public class MyConvergeMMMF_bias<T> {

	private Log log=new Log(MyConvergeMMMF_bias.class);
	
	
	//public static double[] regvals={7.4989,    5.6234,    4.2170 ,   3.1623  ,  2.3714 ,   3.3598};
	public static double[] regvals={ 1.9953, 1.7783, 1.5849,1.4125,1.2589,1.1220,1.0593};
	public static double tol = 1e-3;
	public static int maxiter = 100; //index data cell
	
	
	static MyConvergeMMMF_bias self=null;
	public static MyConvergeMMMF_bias getInstance(){
		if(self==null){
			self=new MyConvergeMMMF_bias();
		}
		return self;
	}
	
	/**
	 * basic MMMF mechanism
	 * @param addrs
	 * @param mat
	 * @param dim
	 * @param level
	 * @return
	 */
	public  Hashtable<T,ratingCoord<T>> compute_basicMMMF(List<T> addrs,Matrix mat,int dim,int level){
		
		int n=addrs.size();
		int p=dim;
		int l=level;
		
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1)); 
	
		double max1=1000;
		
		Vec v=null;
		long t1,t2;
		double AMae=-1;
		int repeat=3;
		for(int indRepeat=0;indRepeat<repeat;indRepeat++){
			//x0=Vec.randVec(n*p+n*p+n*(l-1)); 
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
			//host2landmarks and landmarks to hosts
			log.debug("regvals: "+regvals[i3]);
			
			
			//t1=System.currentTimeMillis();
			x0=BiasConjgrad_gradientBias.getInstance().compute_MMMF(x0, dim, level, mat, regvals[i3], tol ,maxiter);
			//t2=System.currentTimeMillis()-t1;
			//System.out.println("X0 size: "+x0.num_dims);
			
			Matrix x_U=x0.reshape(0, n, p);
			Matrix x_V=x0.reshape(n*p, n, p);
			Matrix x_theta=x0.reshape(n*p+n*p, n, l-1);

			//==========================================
			Matrix X=x_U.multiply(x_V);
			
			Matrix y=m3fSoftmax(X,x_theta);
			
			
			 AMae=mae(y,mat);
		
			if(AMae<max1){
			   max1=AMae;
			   if(v!=null){
				   v.clear();
			   }
			  v=x0.copy();			 			
			}	
			
			//free space
			X.clear();
			y.clear();
			 x_U.clear();
			 x_V.clear();
			 x_theta.clear();	
			 
			 log.info("mae: "+AMae);
			}
			
		}
		
		
		//pause();
		
		Matrix x_U=v.reshape(0, n, p);
		Matrix x_V=v.reshape(n*p, n, p);
		Matrix x_theta=v.reshape(n*p+n*p, n, l-1);
		//====================================
		v.clear();
		Hashtable<T,ratingCoord<T>> info =new Hashtable<T,ratingCoord<T>>(2);
		 for(int i=0;i< n;i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector(x_U.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_V.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_theta.rowVector(i));
			 info.put(addrs.get(i), coord);
		 }
		 x_U.clear();
		 x_V.clear();
		 x_theta.clear();
		 
		 return info;
		
		
	}
	
	
public  Hashtable<T,ratingCoord<T>> compute_biasMMMF(List<T> addrs,Matrix mat,int dim,int level){
		
		int n=addrs.size();
		int p=dim;
		int l=level;
		
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n); 
	
		double max1=1000;
		
		Vec v=null;
		long t1,t2;
		double AMae=-1;
		int repeat=3;
		
		Matrix TotalBias=new DenseMatrix(n,n,0);
		
		for(int indRepeat=0;indRepeat<repeat;indRepeat++){
			//x0=Vec.randVec(n*p+n*p+n*(l-1)); 
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
			//host2landmarks and landmarks to hosts
			log.info("regvals: "+regvals[i3]);
			
			
			//t1=System.currentTimeMillis();
			x0=BiasConjgrad_gradientBias.getInstance().compute_MMMF(x0, dim, level, mat, mat.transpose(),regvals[i3]);
			//t2=System.currentTimeMillis()-t1;
			//System.out.println("X0 size: "+x0.num_dims);
			
			Matrix x_U=x0.reshape(0, n, p);
			Matrix x_V=x0.reshape(n*p, n, p);
			Matrix x_theta=x0.reshape(n*p+n*p, n, l-1);
			Matrix x_bias=x0.reshape(n*p+n*p+n*(l-1), n, 1);

			//==========================================
			for(int i=0;i<n;i++){
				for(int j=0;j<n;j++){
					TotalBias.setValue(i, j,  x_bias.value(i, 0)+ x_bias.value(j, 0));
				}
				
			}
			//==========================================
			
			Matrix X=(x_U.multiply(x_V)).add(TotalBias);
			
			Matrix y=m3fSoftmax(X,x_theta);
			
			
			 AMae=mae(y,mat);
		
			if(AMae<max1){
			   max1=AMae;
			  v=x0.copy();			 			
			}	
			
			//free space
			X.clear();
			y.clear();
			 x_U.clear();
			 x_V.clear();
			 x_theta.clear();	
			 x_bias.clear();
			 log.info("mae: "+AMae);
			}
			
		}
		
		
		//pause();
		
		Matrix x_U=v.reshape(0, n, p);
		Matrix x_V=v.reshape(n*p, n, p);
		Matrix x_theta=v.reshape(n*p+n*p, n, l-1);
		Matrix x_bias=v.reshape(n*p+n*p+n*(l-1), n, 1);
		//====================================
		v.clear();
		Hashtable<T,ratingCoord<T>> info =new Hashtable<T,ratingCoord<T>>(2);
		 for(int i=0;i< n;i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector(x_U.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_V.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_theta.rowVector(i));
			 coord.Bias=Vec.getVecFromVector(x_bias.rowVector(i));
			 info.put(addrs.get(i), coord);
		 }
		 x_U.clear();
		 x_V.clear();
		 x_theta.clear();
		 
		 return info;
		
		
	}
	
	
	
	private void pause() {
		// TODO Auto-generated method stub
		System.exit(-1);
	}

	/**
	 * centralized MMMF procedure 
	 * @param addrs
	 * @param mat
	 * @param dim
	 * @param level
	 * @return
	 */
	public  List<ratingCoord<T>> compute11(List<T> addrs,Matrix mat,int dim,int level){
		int n=addrs.size();
		int p=dim;
		int l=level;
		
		Vec x0=Vec.randVec(n*p+n*p+n*(l-1)+n); 
		
		Matrix x_U=x0.reshape(0, n, p);
		Matrix x_V=x0.reshape(n*p, n, p);
		Matrix x_theta=x0.reshape(n*p+n*p, n, l-1);
		Matrix x_b=x0.reshape(n*p+n*p+n*(l-1), n, 1);
	
		Matrix TotalBias=new DenseMatrix(n,n,0);
		
		double max1=10000000;
		Matrix matTanspose=mat.transpose();
		Vec vv=null;
		long t1,t2;
		
		int repeat=5;
		for(int indRepeat=0;indRepeat<repeat;indRepeat++){
		//repeat
		for(int i3=0;i3<regvals.length;i3++){
			//host2landmarks and landmarks to hosts
			System.out.println("regvals: "+regvals[i3]);
			//t1=System.currentTimeMillis();
			BiasConjgrad_gradientBias.getInstance().compute_MMMF(x0, dim, level, mat, matTanspose,regvals[i3]);
			//t2=System.currentTimeMillis()-t1;
			//log.info("total cost: "+t2/1000+" Secs");
			//v=x0;
			Matrix x_U1=x0.reshape(0, n, p);
			Matrix x_V1=x0.reshape(n*p, n, p);
			Matrix x_theta1=x0.reshape(n*p+n*p, n, l-1);
			Matrix x_b1=x0.reshape(n*p+n*p+n*(l-1), n, 1);
			
			for(int i=0;i<n;i++){
				for(int j=0;j<n;j++){
					TotalBias.setValue(i, j,  x_b1.value(i, 0)+x_b1.value(j, 0));
				}
				
			}
			//==========================================
			Matrix X=x_U1.multiply(x_V1).add(TotalBias);
			for(int i=0;i<X.numRows();i++){
				X.setValue(i, i, 0);
			}
			
			Matrix y=m3fSoftmax(X,x_theta1);
			X.clear();
			
			double AMae=mae(y,mat);
		
			if(AMae<max1){
			   max1=AMae;
			   vv=x0.copy();				
			}		
			System.out.println("MAE: "+AMae);
		}
		
		}
		//====================================
		x_U=vv.reshape(0, n, p);
		x_V=vv.reshape(n*p, n, p);
		x_theta=vv.reshape(n*p+n*p, n, l-1);
		x_b=vv.reshape(n*p+n*p+n*(l-1), n, 1);
		//====================================
		 List<ratingCoord<T>> info =new ArrayList<ratingCoord<T>> (2);
		 for(int i=0;i<x_U.numRows();i++){
			 ratingCoord<T> coord=new ratingCoord<T>(dim,l);
			 coord.p=Vec.getVecFromVector(x_U.rowVector(i));
			 coord.q=Vec.getVecFromVector(x_V.rowVector(i));
			 coord.thetaVec=Vec.getVecFromVector(x_theta.rowVector(i));
			 coord.Bias=Vec.getVecFromVector(x_b.rowVector(i));
			 info.add(coord);
		 }
		 return info;
	}

	/**
	 * Calculation of predicted labels for MMMF given soft outputs and thresholds 
	 * @param x
	 * @param x_theta1
	 * @return
	 */
	public static Matrix m3fSoftmax(Matrix x, Matrix x_theta1) {
		// TODO Auto-generated method stub
		int n=x.numRows();
		int m=x.numColumns();
		int n1= x_theta1.numRows();
		int l1= x_theta1.numColumns();
		
/*		if(x.numRows()!=n1){
			System.err.println("error! m3fSoftmax");
			return null;
		}*/
		Matrix tmp1=new DenseMatrix(n,1);
		Matrix tmp2=new DenseMatrix(m,1,1);
		
		Matrix y=new DenseMatrix(n,m,1);
		
		for(int i=0;i<l1;i++){
			
			
			for(int ir=0;ir<n;ir++){
				tmp1.setValue(ir, 0, x_theta1.value(ir, i));
			}
			Matrix tmp = tmp1.multiply(tmp2);
			Matrix tmp3 = x.greaterEqualIndicator(tmp,true);
			y=y.add(tmp3);
		}
		return y;
	}

	/**
	 * 
	 * @param x0 estimated matrix
	 * @param x1 real matrix
	 * @return
	 */
	public static double mae(Matrix x0,Matrix x1){
	
		double threshold=0;
		double sum=0;
		int counter=0;
		for(int i=0;i<x0.numRows();i++){
			for(int j=0;j<x0.numColumns();j++){
				//valid measurement
				if(x1.value(i, j)>threshold){
					sum+=Math.abs(x0.value(i, j)-x1.value(i, j));
					counter++;
				}
			}
		}
		return sum/counter;
	}
	
	/**
	 * rmse computation
	 * @param estimated
	 * @param real
	 * @return
	 */
	public static double RMSE(Matrix estimated,Matrix real){
		
		double threshold=0;
		//valid measurement
		Matrix gt0=estimated.dotProduct(RatingGradient.greaterIndicator(real, threshold));
		Matrix err1=gt0.add(real.scale(-1));
		abs(err1);
		 double dd;
		 //
		Vector vv=new DenseVector(err1.numColumns());
		int N1=err1.numRows();
		int N2=err1.numColumns();
		for(int i=0;i<N2;i++){
			dd = (err1.columnVector(i).dotProduct(err1.columnVector(i)))/N1;		 
			vv.setValue(i,dd);
		}
		//===================
		double out = Math.sqrt(vv.sum()/N2);	
		vv=null;
		gt0=null;
		err1=null;
		return out;
	}
	
	/**
	 * absolute value
	 * @param mat
	 */
	public static void abs(Matrix mat){
		for(int i=0;i<mat.numRows();i++){
			for(int j=0;j<mat.numColumns();j++){
				mat.setValue(i, j, Math.abs(mat.value(i, j)));
			}
		}
	}

	public static double nmae(Matrix estimated, Matrix real) {
		// TODO Auto-generated method stub
		double threshold=0;
		//valid measurement
		Matrix gt0=estimated.dotProduct(RatingGradient.greaterIndicator(real, threshold));
		Matrix err1=gt0.add(real.scale(-1));
		
		double d1=0;
		double d2=0;
		for(int i=0;i<real.numRows();i++){
			for(int j=0;j<real.numColumns();j++){
				d1+=Math.abs(err1.value(i, j));
				d2+=Math.abs(real.value(i, j));
			}
		}
		err1.clear();
		gt0.clear();
		return d1/d2;
	}
	
	
	
	
	
	
	
	
	
	
	
}
