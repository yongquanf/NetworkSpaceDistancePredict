package edu.NUDT.PDL.BiasMatrixApproximation;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;

/**
 * implements the gradient function
 * @author Administrator
 *
 */	
public class BiasMatrixRatingGradient<T> implements MFWithGradient {
		 
		 /**
		  * configuration parameters for gradient computation
		  */
		//T address; /*the address of the host which calculates its dx and obj*/
		 int landmarkNum;
		 int dim;
		 int level;
		 public Matrix D_host2landmark;
		 Matrix D_landmark2host;
		 Vec v; // the coordinate vector of the host
		 
		 //landmarks coordinate matrix
		 Matrix x_Ul;
		 Matrix x_Vl;
		 Matrix x_thetal;
		 Matrix x_bl;
		 double lambda;
		 boolean isLandmark=false;
		 
		 public BiasMatrixRatingGradient(boolean _isLandmark, int dim2, int level2, Matrix d_host2landmark2, Matrix d_landmark2host2, double lambda2){
			 isLandmark=_isLandmark;
			 this.dim=dim2;
			 this.level=level2;
			 this.D_host2landmark=d_host2landmark2.copyMatrix();
			 this.D_landmark2host=d_landmark2host2.copyMatrix();
			 this.lambda=lambda2;
			 			 
		 }
		 
		 /**
		  * constructor
		  * @param _landmarkNum
		  * @param _dim
		  * @param _level
		  * @param _D_host2landmark
		  * @param _D_landmark2host
		  * @param _x_Ul
		  * @param _x_Vl
		  * @param _x_thetal
		  * @param _x_bl
		  * @param _lambda
		  */
		 public BiasMatrixRatingGradient(int _dim, int _level,Matrix _D_host2landmark,Matrix _D_landmark2host,
				Matrix _x_Ul,Matrix _x_Vl,	Matrix _x_thetal, Matrix _x_bl,double _lambda
		 	){
			 
			 isLandmark=false;
			 //address=_addr;
			// landmarkNum=_landmarkNum;
			 dim= _dim;
			 level=_level;
			 
			 D_host2landmark=_D_host2landmark;
			 D_landmark2host=_D_landmark2host;
			 x_Ul=_x_Ul;
			 x_Vl=_x_Vl;
			 x_thetal= _x_thetal;
			 x_bl=_x_bl;
			 lambda=_lambda;
		 }
		
		@Override
		public void computeGradient(double[] argument, double[] gradient) {
			// TODO Auto-generated method stub
			v=new Vec(argument,true);
			//number of hosts
			int n=this.D_host2landmark.numRows();
			int m=this.D_host2landmark.numColumns();
			
/*			if(n!=1){
				System.err.print("it support one point per round!\n");
				return ;
			}*/
			//no of landmarks
			
			int n1=this.x_Ul.numRows();
			int p=this.x_Ul.numColumns();
			int l2=this.x_thetal.numColumns();
			int l=l2+1;
			
			Matrix U=v.reshape(0, n, p);
			Matrix V=v.reshape(n*p, n, p);
			Matrix theta=v.reshape(n*p+n*p, n, l-1);
			Matrix b_me=v.reshape(n*p+n*p+n*(l-1), n,1);
			
			Matrix X1=U.multiply(x_Vl); //[n,m]
			Matrix X2=this.x_Ul.multiply(V); //[m,n]
			
			Matrix TotalBias1=new DenseMatrix(n,m,0); //[n,m]
			for(int i=0;i<n;i++){
				for(int j=0;j<m;j++){
					TotalBias1.setValue(i, j,b_me.value(i, 0)+x_bl.value(j, 0) );	
				}
			}
			Matrix TotalBias2=new DenseMatrix(m,n,0);
			for(int i=0;i<m;i++){
				for(int j=0;j<n;j++){
					TotalBias2.setValue(i, j,b_me.value(j, 0)+x_bl.value(i, 0) );	
				}
			}
			
			Matrix YY_ingt0=greaterIndicator(D_host2landmark,0); //[n,m]
			Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);//[m,n]
			
			//for each non-missing elements
			Matrix BX1=X1.add(TotalBias1).dotProduct(YY_ingt0);
			Matrix BX2=X2.add(TotalBias2).dotProduct(YY_outgt0);
			
			X1=null;
			X2=null;
			
			Matrix dU=U.scale(lambda);
			Matrix dV=V.scale(lambda);
			Matrix dtheta=new DenseMatrix(n,l-1,0);
			Matrix db=b_me.scale(lambda);
			
			
			Vector  regobj=new DenseVector(n,0);
			for(int i=0;i<n;i++){
				double val=U.rowVector(i).dotProduct(U.rowVector(i))+
				V.rowVector(i).dotProduct(V.rowVector(i))+
				b_me.value(i, 0)*b_me.value(i, 0);
				val=(val*lambda)/2;
				regobj.setValue(i,val );
			}
			
			Vector  lossobj=new DenseVector(n,0);
			
			for(int i=0;i<n;i++){
				//the loss value for each level
				for(int k=1;k<=l-1;k++){
					Matrix S1=YY_ingt0.add(greaterIndicator(D_host2landmark, k).scale(-2));
					Vector S_in = S1.rowVector(i);
					Matrix S2=YY_outgt0.add(greaterIndicator(this.D_landmark2host,k).scale(-2));
					Vector S_out = S2.columnVector(i);
					
					Vector tmp=new DenseVector(m,1);
					Vector tmp2 = tmp.scale(theta.value(i,k-1)).dotProductVec(S_in);					
					
					Vector tmp3=S_in.dotProductVec(BX1.rowVector(i));
					
					Vector BZ1 = tmp2.add(tmp3.scale(-1));
					
					Vector tmp4=x_thetal.columnVector(k-1).dotProductVec(S_out);
					Vector tmp5=BX2.columnVector(i).dotProductVec(S_out);
					Vector BZ2=tmp4.add(tmp5.scale(-1));
					
					lossobj.setValue(i,lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum() );
					
					Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
					tmp2=hprime(BZ2).dotProductVec(S_out);
					
					//==========================
					tmp3=dU.rowVector(i);
					Vector tmpdU = tmp3.add(tmp1.multiply(this.x_Vl).scale(-1));
					
					for(int ind=0;ind<tmpdU.numDimensions();ind++){
						dU.setValue(i, ind, tmpdU.value(ind));
					}
					tmp4=dV.rowVector(i);
					Vector tmpdV = tmp4.add(tmp2.multiply(this.x_Ul).scale(-1));
					for(int ind=0;ind<tmpdV.numDimensions();ind++){
						dV.setValue(i, ind, tmpdV.value(ind));
					}
					//dtheta					
					dtheta.setValue(i, k-1, tmp1.sum());
					//db
					db.setValue(i, 1, db.value(i, 1)-tmp1.sum()-tmp2.sum());					
				}
	
			}
			//=====================================
			Vector obj = regobj.add(lossobj);
			
			Vector tmp7 = dU.flat();
			Vector tmp8 = dV.flat();
			Vector tmp9 = dtheta.flat();
			Vector tmp10 = db.flat();
			
			Vector out=new DenseVector(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
					tmp10.numDimensions());
			for(int i=0;i<out.numDimensions();i++){
				if(i<tmp7.numDimensions()){
					out.setValue(i, tmp7.value(i));
				}else if(i<tmp7.numDimensions()+tmp8.numDimensions()){
					out.setValue(i, tmp8.value(i-tmp7.numDimensions()));
					
				}else if(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()){
					
					out.setValue(i, tmp9.value(i-(tmp7.numDimensions()+tmp8.numDimensions())));
				}else if(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
						tmp10.numDimensions()){
					out.setValue(i, tmp10.value(i-(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())));					
				}								
			}
			
			gradient=out.asArray();
		}

		/**
		 * compute the gradient and the function value, for a point
		 */
		public double evaluate(double[] argument, double[] gradient) {
			// TODO Auto-generated method stub
			v=new Vec(argument,true);
			//number of hosts
			int n=D_host2landmark.numRows();
			//landmarks
			int m=D_host2landmark.numColumns();
			//System.out.println("n: "+n+", m: "+m);
/*			if(n!=1){
				System.err.print("it support one point per round!\n");
				return ;
			}*/
			//no of landmarks
			
			/**
			 * configure the landmark rating coordinate matrix
			 * based on the rating coordinate vector itself
			 */
			if(false){				
				this.x_Ul=v.reshape(0, n, this.dim);
				this.x_Vl=v.reshape(n*dim, n, this.dim);
				this.x_thetal=v.reshape(n*dim+n*dim, n, level-1);
				this.x_bl=v.reshape(n*dim+n*dim+n*(level-1), n, 1);
				
			}
			//otherwise the coordinate has been assigned
			
			int n1=this.x_Ul.numRows();
			int p=this.x_Ul.numColumns();
			int l2=this.x_thetal.numColumns();
			int l=l2+1;
			
			Matrix U=v.reshape(0, n, p);
			Matrix V=v.reshape(n*p, n, p);
			Matrix theta=v.reshape(n*p+n*p, n, l-1);
			Matrix b_me=v.reshape(n*p+n*p+n*(l-1), n,1);
			
			Matrix X1=U.multiply(x_Vl);
			Matrix X2=x_Ul.multiply(V);
			
			Matrix TotalBias1=new DenseMatrix(n,m,0);
			
			//System.out.println(b_me.numRows()+", x_bl: "+x_bl.numRows());
			
			for(int i=0;i<n;i++){
				for(int j=0;j<m;j++){
					//System.out.println("i: "+i+", j: "+j);
					TotalBias1.setValue(i, j,b_me.value(i, 0)+x_bl.value(j, 0) );	
				}
			}
			Matrix TotalBias2=new DenseMatrix(m,n,0);
			for(int i=0;i<m;i++){
				for(int j=0;j<n;j++){
					TotalBias2.setValue(i, j,b_me.value(j, 0)+x_bl.value(i, 0) );	
				}
			}
			if(this.isLandmark){
				for(int i=0;i<n;i++){
					TotalBias2.setValue(i, i, 0);
					TotalBias1.setValue(i, i, 0);
				}
			}
			
			Matrix YY_ingt0=greaterIndicator(D_host2landmark,0);
			Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);
			
			//for each non-missing elements
			Matrix BX1=X1.add(TotalBias1).dotProduct(YY_ingt0);
			Matrix BX2=X2.add(TotalBias2).dotProduct(YY_outgt0);
			
			
			//X1=null;
			//X2=null;
			
			Matrix dU=U.scale(lambda);
			Matrix dV=V.scale(lambda);
			Matrix dtheta=new DenseMatrix(n,l-1,0);
			Matrix db=b_me.scale(lambda);
			
			
			Vector  regobj=new DenseVector(n,0);
			for(int i=0;i<n;i++){
				double val=U.rowVector(i).dotProduct(U.rowVector(i))+
				V.rowVector(i).dotProduct(V.rowVector(i))+
				b_me.value(i, 0)*b_me.value(i, 0);
				val=(val*lambda)/2;
				
				//System.out.println(i+" : "+val);
				
				regobj.setValue(i,val );
			}
			
			Vector  lossobj=new DenseVector(n,0);
			
			for(int i=0;i<n;i++){
				//the loss value for each level
				for(int k=1;k<=l-1;k++){
					Matrix S1=YY_ingt0.add(greaterIndicator(D_host2landmark, k).scale(-2));
					Vector S_in = new DenseVector(S1.rowVector(i));
					
					Matrix S2=YY_outgt0.add(greaterIndicator(this.D_landmark2host,k).scale(-2));
					Vector S_out = new DenseVector(S2.columnVector(i));
					
					Vector tmp=new DenseVector(m,1);
					Vector tmp2 = tmp.scale(theta.value(i,k-1)).dotProductVec(S_in);					
					
					if(S_in==null){
						System.err.println("S_in null!");
					}					
					Vector tt2=new DenseVector(BX1.rowVector(i));
					if(tt2==null){
						System.err.println("tt2 null!");
					}
					
					Vector tmp333 = tt2.dotProductVec(S_in);
					

					if(tmp333==null){
						System.err.println("tmp3 null!");
					}
					Vector tt=tmp333.scale(-1);
					
					
					Vector BZ1 = tmp2.add(tt);
					
					DenseVector tt5 = new DenseVector(x_thetal.columnVector(k-1));
					if(tt5 ==null){
						System.out.println("theta is null!");
					}
					if(S_out==null){
						System.out.println("S_out is null!");						
					}
					
					
					Vector tmp4=tt5.dotProductVec(S_out);
					
					DenseVector tt6 = new DenseVector(BX2.columnVector(i));					
					Vector tmp5=tt6.dotProductVec(S_out);
					
					Vector BZ2=tmp4.add(tmp5.scale(-1));
					
					lossobj.setValue(i,lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum() );
					
					Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
					tmp2=hprime(BZ2).dotProductVec(S_out);
					
					//==========================
					Vector tmp3 = dU.rowVector(i);
					Vector tmpdU = tmp3.add(tmp1.multiply(this.x_Vl).scale(-1));
					
					for(int ind=0;ind<tmpdU.numDimensions();ind++){
						dU.setValue(i, ind, tmpdU.value(ind));
					}
					tmp4=dV.rowVector(i);
					Vector tmpdV = tmp4.add(tmp2.multiply(this.x_Ul).scale(-1));
					for(int ind=0;ind<tmpdV.numDimensions();ind++){
						dV.setValue(i, ind, tmpdV.value(ind));
					}
					//dtheta					
					dtheta.setValue(i, k-1, tmp1.sum());
					//db
					db.setValue(i, 0, db.value(i, 0)-tmp1.sum()-tmp2.sum());					
				}
	
			}
			//=====================================
			Vector obj = regobj.add(lossobj);
			
			Vector tmp7 = dU.flat();
			Vector tmp8 = dV.flat();
			Vector tmp9 = dtheta.flat();
			Vector tmp10 = db.flat();
			
			Vector out=new DenseVector(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
					tmp10.numDimensions());
			for(int i=0;i<out.numDimensions();i++){
				if(i<tmp7.numDimensions()){
					out.setValue(i, tmp7.value(i));
				}else if(i>=tmp7.numDimensions()
						&&(i<tmp7.numDimensions()+tmp8.numDimensions())){
					out.setValue(i, tmp8.value(i-tmp7.numDimensions()));
					
				}else if((i>=tmp7.numDimensions()+tmp8.numDimensions())
						&&
						(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())){
					
					out.setValue(i, tmp9.value(i-(tmp7.numDimensions()+tmp8.numDimensions())));
				}else if((i>=tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())
						&&
						(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
						tmp10.numDimensions())){
					out.setValue(i, tmp10.value(i-(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())));					
				}								
			}
					
			double[] dd88 = out.asArray();
			if(gradient.length!=dd88.length){
				System.err.println("gradient size do not coinside!");
				System.exit(-1);
				
			}
			//assign value
			for(int i=0;i<dd88.length;i++){
				gradient[i]=dd88[i];
			}
			//BiasConjgrad_gradientBias.print(gradient,"gradient");
			/**
			 * all elements in terms of sum
			 */
			return obj.sum();
			
		}

		/**
		 * Return the hinge loss value
		 * @param vec
		 * @return
		 */
		Vector h(Vector vec){
			Vector zin01 = vec.greaterIndicator(0, false).add((vec.greaterIndicator(1, true)).scale(-1));
			Vector zle0 = vec.smallerIndicator(0, false);
			
			Vector tmp1 = zin01.scale(0.5).add(zin01.dotProductVec(vec).scale(-1));
			
			Vector tmp2 = (zle0.scale(0.5)).add((zle0.dotProductVec(vec)).scale(-1));
			
			Vector tmp3 = (zin01.dotProductVec(vec.dotProductVec(vec))).scale(0.5);
			
			Vector out = (tmp1.add(tmp2)).add(tmp3);
			return out;
			
		}
		
		Vector hprime(Vector vec){
			Vector zin01 = (vec.greaterIndicator(0, false)).add((vec.greaterIndicator(1, true)).scale(-1));
			Vector zle0 = vec.smallerIndicator(0, false);
							
			Vector out = ((zin01.dotProductVec(vec)).add(zin01.scale(-1))).add(zle0.scale(-1));
			return out;
			
		}
		
		/**
		 * indicator matrix for the input matrix
		 * @param mat
		 * @return
		 */
		public static Matrix greaterIndicator(Matrix mat,double val){
			int n=mat.numRows();
			int m=mat.numColumns();
			
			Matrix mat2=new DenseMatrix(n,m,0);
			for(int i=0;i<n;i++){
				for(int j=0;j<m;j++){
					if(mat.value(i, j)>val){
						mat2.setValue(i, j, 1);
					}
				}
				
			}
			
			return mat2;
		}
		
		
		@Override
		public double evaluate(double[] argument) {
			// TODO Auto-generated method stub
			v=new Vec(argument,true);
			//number of hosts
			int n=this.D_host2landmark.numRows();
			int m=this.D_host2landmark.numColumns();
			
/*			if(n!=1){
				System.err.print("it support one point per round!\n");
				return ;
			}*/
			//no of landmarks
			
			int n1=this.x_Ul.numRows();
			int p=this.x_Ul.numColumns();
			int l2=this.x_thetal.numColumns();
			int l=l2+1;
			
			Matrix U=v.reshape(0, n, p);
			Matrix V=v.reshape(n*p, n, p);
			Matrix theta=v.reshape(n*p+n*p, n, l-1);
			Matrix b_me=v.reshape(n*p+n*p+n*(l-1), n,1);
			
			Matrix X1=U.multiply(x_Vl);
			Matrix X2=this.x_Ul.multiply(V);
			
			Matrix TotalBias1=new DenseMatrix(n,m,0);
			for(int i=0;i<n;i++){
				for(int j=0;j<m;j++){
					TotalBias1.setValue(i, j,b_me.value(i, 1)+x_bl.value(j, 1) );	
				}
			}
			Matrix TotalBias2=new DenseMatrix(m,n,0);
			for(int i=0;i<m;i++){
				for(int j=0;j<n;j++){
					TotalBias2.setValue(i, j,b_me.value(j, 1)+x_bl.value(i, 1) );	
				}
			}
			
			Matrix YY_ingt0=greaterIndicator(D_host2landmark,0);
			Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);
			
			//for each non-missing elements
			Matrix BX1=X1.add(TotalBias1).dotProduct(YY_ingt0);
			Matrix BX2=X2.add(TotalBias2).dotProduct(YY_outgt0);
			
			X1=null;
			X2=null;
			
			Matrix dU=U.scale(lambda);
			Matrix dV=V.scale(lambda);
			Matrix dtheta=new DenseMatrix(n,l-1,0);
			Matrix db=b_me.scale(lambda);
			
			
			Vector  regobj=new DenseVector(n,0);
			for(int i=0;i<n;i++){
				double val=U.rowVector(i).dotProduct(U.rowVector(i))+
				V.rowVector(i).dotProduct(V.rowVector(i))+
				b_me.value(i, 1)*b_me.value(i, 1);
				val=(val*lambda)/2;
				regobj.setValue(i,val );
			}
			
			Vector  lossobj=new DenseVector(n,0);
			
			for(int i=0;i<n;i++){
				//the loss value for each level
				for(int k=1;k<=l-1;k++){
					Matrix S1=YY_ingt0.add(greaterIndicator(D_host2landmark, k).scale(-2));
					Vector S_in = S1.rowVector(i);
					Matrix S2=YY_outgt0.add(greaterIndicator(this.D_landmark2host,k).scale(-2));
					Vector S_out = S2.columnVector(i);
					
					Vector tmp=new DenseVector(m,1);
					Vector tmp2 = tmp.scale(theta.value(i,k-1)).dotProductVec(S_in);					
					
					Vector tmp3=S_in.dotProductVec(BX1.rowVector(i));
					
					Vector BZ1 = tmp2.add(tmp3.scale(-1));
					
					Vector tmp4=this.x_thetal.columnVector(k-1).dotProductVec(S_out);
					Vector tmp5=BX2.columnVector(i).dotProductVec(S_out);
					Vector BZ2=tmp4.add(tmp5.scale(-1));
					
					lossobj.setValue(i,lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum() );
					
					Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
					tmp2=hprime(BZ2).dotProductVec(S_out);
					
					//==========================
					tmp3=dU.rowVector(i);
					Vector tmpdU = tmp3.add(tmp1.multiply(this.x_Vl).scale(-1));
					
					for(int ind=0;ind<tmpdU.numDimensions();ind++){
						dU.setValue(i, ind, tmpdU.value(ind));
					}
					tmp4=dV.rowVector(i);
					Vector tmpdV = tmp4.add(tmp2.multiply(this.x_Ul).scale(-1));
					for(int ind=0;ind<tmpdV.numDimensions();ind++){
						dV.setValue(i, ind, tmpdV.value(ind));
					}
					//dtheta					
					dtheta.setValue(i, k-1, tmp1.sum());
					//db
					db.setValue(i, 1, db.value(i, 1)-tmp1.sum()-tmp2.sum());					
				}
	
			}
			//=====================================
			Vector obj = regobj.add(lossobj);
			double d=obj.sum();
			obj=null;
			regobj=null;
			lossobj=null;
			return d;
			
			
		}

		@Override
		public double getLowerBound(int n) {
			// TODO Auto-generated method stub
			return 0;
		}

		/**
		 * the number of arguments
		 */
		public int getNumArguments() {
			// TODO Auto-generated method stub
			int n=this.D_host2landmark.numRows();
			int p=this.x_Ul.numColumns();
			int l2=this.x_thetal.numColumns();
			int l=l2+1;
			return n*p+n*p+n*(l-1)+n;
		}

		@Override
		public double getUpperBound(int n) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Vector evaluate_Vector(double[] argument, double[] gradient) {
			// TODO Auto-generated method stub
			// TODO Auto-generated method stub
			v=new Vec(argument,true);
			//number of hosts
			int n=D_host2landmark.numRows();
			//landmarks
			int m=D_host2landmark.numColumns();
			//System.out.println("n: "+n+", m: "+m);
/*			if(n!=1){
				System.err.print("it support one point per round!\n");
				return ;
			}*/
			//no of landmarks
			
			/**
			 * configure the landmark rating coordinate matrix
			 * based on the rating coordinate vector itself
			 */
			if(false){				
				this.x_Ul=v.reshape(0, n, this.dim);
				this.x_Vl=v.reshape(n*dim, n, this.dim);
				this.x_thetal=v.reshape(n*dim+n*dim, n, level-1);
				this.x_bl=v.reshape(n*dim+n*dim+n*(level-1), n, 1);
				
			}
			//otherwise the coordinate has been assigned
			
			int n1=this.x_Ul.numRows();
			int p=this.x_Ul.numColumns();
			int l2=this.x_thetal.numColumns();
			int l=l2+1;
			
			Matrix U=v.reshape(0, n, p);
			Matrix V=v.reshape(n*p, n, p);
			Matrix theta=v.reshape(n*p+n*p, n, l-1);
			Matrix b_me=v.reshape(n*p+n*p+n*(l-1), n,1);
			
			Matrix X1=U.multiply(x_Vl);
			Matrix X2=x_Ul.multiply(V);
			
			Matrix TotalBias1=new DenseMatrix(n,m,0);
			
			//System.out.println(b_me.numRows()+", x_bl: "+x_bl.numRows());
			
			for(int i=0;i<n;i++){
				for(int j=0;j<m;j++){
					//System.out.println("i: "+i+", j: "+j);
					TotalBias1.setValue(i, j,b_me.value(i, 0)+x_bl.value(j, 0) );	
				}
			}
			Matrix TotalBias2=new DenseMatrix(m,n,0);
			for(int i=0;i<m;i++){
				for(int j=0;j<n;j++){
					TotalBias2.setValue(i, j,b_me.value(j, 0)+x_bl.value(i, 0) );	
				}
			}
			if(this.isLandmark){
				for(int i=0;i<n;i++){
					TotalBias2.setValue(i, i, 0);
					TotalBias1.setValue(i, i, 0);
				}
			}
			
			Matrix YY_ingt0=greaterIndicator(D_host2landmark,0);
			Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);
			
			//for each non-missing elements
			Matrix BX1=X1.add(TotalBias1).dotProduct(YY_ingt0);
			Matrix BX2=X2.add(TotalBias2).dotProduct(YY_outgt0);
			
			
			//X1=null;
			//X2=null;
			
			Matrix dU=U.scale(lambda);
			Matrix dV=V.scale(lambda);
			Matrix dtheta=new DenseMatrix(n,l-1,0);
			Matrix db=b_me.scale(lambda);
			
			
			Vector  regobj=new DenseVector(n,0);
			for(int i=0;i<n;i++){
				double val=U.rowVector(i).dotProduct(U.rowVector(i))+
				V.rowVector(i).dotProduct(V.rowVector(i))+
				b_me.value(i, 0)*b_me.value(i, 0);
				val=(val*lambda)/2;
				
				//System.out.println(i+" : "+val);
				
				regobj.setValue(i,val );
			}
			
			Vector  lossobj=new DenseVector(n,0);
			
			for(int i=0;i<n;i++){
				//the loss value for each level
				for(int k=1;k<=l-1;k++){
					Matrix S1=YY_ingt0.add(greaterIndicator(D_host2landmark, k).scale(-2));
					Vector S_in = new DenseVector(S1.rowVector(i));
					
					Matrix S2=YY_outgt0.add(greaterIndicator(this.D_landmark2host,k).scale(-2));
					Vector S_out = new DenseVector(S2.columnVector(i));
					
					Vector tmp=new DenseVector(m,1);
					Vector tmp2 = tmp.scale(theta.value(i,k-1)).dotProductVec(S_in);					
					
					if(S_in==null){
						System.err.println("S_in null!");
					}					
					Vector tt2=new DenseVector(BX1.rowVector(i));
					if(tt2==null){
						System.err.println("tt2 null!");
					}
					
					Vector tmp333 = tt2.dotProductVec(S_in);
					

					if(tmp333==null){
						System.err.println("tmp3 null!");
					}
					Vector tt=tmp333.scale(-1);
					
					
					Vector BZ1 = tmp2.add(tt);
					
					DenseVector tt5 = new DenseVector(x_thetal.columnVector(k-1));
					if(tt5 ==null){
						System.out.println("theta is null!");
					}
					if(S_out==null){
						System.out.println("S_out is null!");						
					}
					
					
					Vector tmp4=tt5.dotProductVec(S_out);
					
					DenseVector tt6 = new DenseVector(BX2.columnVector(i));					
					Vector tmp5=tt6.dotProductVec(S_out);
					
					Vector BZ2=tmp4.add(tmp5.scale(-1));
					
					lossobj.setValue(i,lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum() );
					
					Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
					tmp2=hprime(BZ2).dotProductVec(S_out);
					
					//==========================
					Vector tmp3 = dU.rowVector(i);
					Vector tmpdU = tmp3.add(tmp1.multiply(this.x_Vl).scale(-1));
					
					for(int ind=0;ind<tmpdU.numDimensions();ind++){
						dU.setValue(i, ind, tmpdU.value(ind));
					}
					tmp4=dV.rowVector(i);
					Vector tmpdV = tmp4.add(tmp2.multiply(this.x_Ul).scale(-1));
					for(int ind=0;ind<tmpdV.numDimensions();ind++){
						dV.setValue(i, ind, tmpdV.value(ind));
					}
					//dtheta					
					dtheta.setValue(i, k-1, tmp1.sum());
					//db
					db.setValue(i, 0, db.value(i, 0)-tmp1.sum()-tmp2.sum());					
				}
	
			}
			//=====================================
			Vector obj = regobj.add(lossobj);
			
			Vector tmp7 = dU.flat();
			Vector tmp8 = dV.flat();
			Vector tmp9 = dtheta.flat();
			Vector tmp10 = db.flat();
			
			Vector out=new DenseVector(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
					tmp10.numDimensions());
			for(int i=0;i<out.numDimensions();i++){
				if(i<tmp7.numDimensions()){
					out.setValue(i, tmp7.value(i));
				}else if(i>=tmp7.numDimensions()
						&&(i<tmp7.numDimensions()+tmp8.numDimensions())){
					out.setValue(i, tmp8.value(i-tmp7.numDimensions()));
					
				}else if((i>=tmp7.numDimensions()+tmp8.numDimensions())
						&&
						(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())){
					
					out.setValue(i, tmp9.value(i-(tmp7.numDimensions()+tmp8.numDimensions())));
				}else if((i>=tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())
						&&
						(i<tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions()+
						tmp10.numDimensions())){
					out.setValue(i, tmp10.value(i-(tmp7.numDimensions()+tmp8.numDimensions()+tmp9.numDimensions())));					
				}								
			}
					
			double[] dd88 = out.asArray();
			if(gradient.length!=dd88.length){
				System.err.println("gradient size do not coinside!");
				System.exit(-1);
				
			}
			//assign value
			for(int i=0;i<dd88.length;i++){
				gradient[i]=dd88[i];
			}
			//BiasConjgrad_gradientBias.print(gradient,"gradient");
			/**
			 * all elements in terms of sum
			 */
			return obj;
			
		}
		
	 }
	
