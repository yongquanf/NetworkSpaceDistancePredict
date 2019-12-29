package edu.NUDT.PDL.BasicDistributedMMMF;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;

public class m3fshc<T> implements MFWithGradient{

	
	/**
	 * configuration parameters for gradient computation
	 */
	//T address; /*the address of the host which calculates its dx and obj*/
	int landmarkNum;
	int dim;
	int level;
	Matrix D_host2landmark;
	//Matrix D_landmark2host;
	Vec v; // the coordinate vector of the host

	double lambda;
	
	public m3fshc(int dim2, int level2, Matrix d_host2landmark2,double lambda2){
		 
		 this.dim=dim2;
		 this.level=level2;
		 this.D_host2landmark=d_host2landmark2.copyMatrix();
		// this.D_landmark2host=d_landmark2host2.copyMatrix();
		 this.lambda=lambda2;
		 			 
	}


	@Override
	public void computeGradient(double[] argument, double[] gradient) {
		// TODO Auto-generated method stub
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

		int p=dim;

		int l=level;
		
		Matrix U=v.reshape(0, n, p);
		Matrix V=v.reshape(n*p, n, p);
		Matrix theta=v.reshape(n*p+n*p, n, l-1);
		
		Matrix X=U.multiply(V);
		
		
		Matrix Ygt0=greaterIndicator(D_host2landmark,0);
		
		//for each non-missing elements
		Matrix BX=X.dotProduct(Ygt0);
		
		
		//X1=null;
		//X2=null;
		
		Matrix dU=U.scale(lambda);
		Matrix dV=V.scale(lambda);
		Matrix dtheta=new DenseMatrix(n,l-1,0);
		
		//compute the regobj!
		//Ericfu 2010-7-21
		double regobj=0;
		double val=0;
		for(int i=0;i<n;i++){
			val=U.rowVector(i).dotProduct(U.rowVector(i))+
			V.rowVector(i).dotProduct(V.rowVector(i));
		}
		val=(val*lambda)/2;		
		regobj+=val;
		
		
		
		double  lossobj=0;
			//ones matrix, M^T, for convenience of matrix multiplication,
			Matrix onesMat=new DenseMatrix(m,1,1);
			//the loss value for each level
			for(int k=1;k<=l-1;k++){
				Matrix S=Ygt0.add(greaterIndicator(D_host2landmark, k).scale(-2));
				
				Matrix tmp1=new DenseMatrix(n,1,0);
				for(int i=0;i<n;i++){
					tmp1.setValue(i, 0, theta.value(i, k-1));
				}
				
				Matrix BZ = ((tmp1.multiply(onesMat)).dotProduct(S)).add((BX.dotProduct(S)).scale(-1));	
				
				lossobj=lossobj+h(BZ).sum();	
				
				Matrix tmp = hprime(BZ).dotProduct(S);//[n,m]
				
				//==========================
				dU=dU.add(tmp.multiply(V.transpose()).scale(-1));
				dV=dV.add(((tmp.transpose()).multiply(U.transpose())).scale(-1));
							
				//dtheta	
				Matrix tt = tmp.multiply(onesMat.transpose());
				for(int i=0;i<n;i++){
					dtheta.setValue(i, k-1, tt.value(i, 0));
				}
			}

		
		//=====================================
		double obj = regobj+lossobj;
		
		Vector tmp7 = dU.flat();
		Vector tmp8 = dV.flat();
		Vector tmp9 = dtheta.flat();
		
		int len7=n*p;
		int len8=n*p;
		int len9=n*(l-1);
		
		Vector out=new DenseVector(len7+len8+len9);
		for(int i=0;i<out.numDimensions();i++){
			if(i<len7){
				out.setValue(i, tmp7.value(i));
			}else if(i>=len7
					&&(i<len7+len8)){
				out.setValue(i, tmp8.value(i-len7));
				
			}else{
				
				out.setValue(i, tmp9.value(i-(len7+len8)));
			}							
		}
		tmp7.clear();
		tmp8.clear();
		tmp9.clear();
		

		//assign value
		for(int i=0;i<out.numDimensions();i++){
			gradient[i]=out.value(i);
		}
		out.clear();
		//BiasConjgrad_gradientBias.print(gradient,"gradient");
		/**
		 * all elements in terms of sum
		 */
		return obj;
		
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
	
	Matrix h(Matrix vec){
		Matrix zin01 = vec.greaterEqualIndicator(0, false).add((vec.greaterEqualIndicator(1, true)).scale(-1));
		
		Matrix zle0 = vec.smallerIndicator(0, false);
		
		Matrix tmp1 = zin01.scale(0.5).add(zin01.dotProduct(vec).scale(-1));
		
		Matrix tmp2 = (zle0.scale(0.5)).add((zle0.dotProduct(vec)).scale(-1));
		
		Matrix tmp3 = (zin01.dotProduct(vec.dotProduct(vec))).scale(0.5);
		
		Matrix out = (tmp1.add(tmp2)).add(tmp3);
		
		return out;
		
	}

	Vector hprime(Vector vec){
		Vector zin01 = (vec.greaterIndicator(0, false)).add((vec.greaterIndicator(1, true)).scale(-1));
		Vector zle0 = vec.smallerIndicator(0, false);
						
		Vector out = ((zin01.dotProductVec(vec)).add(zin01.scale(-1))).add(zle0.scale(-1));
		return out;
		
	}

	
	Matrix hprime(Matrix vec){
		Matrix zin01 = (vec.greaterEqualIndicator(0, false)).add((vec.greaterEqualIndicator(1, true)).scale(-1));
		Matrix zle0 = vec.smallerIndicator(0, false);
						
		Matrix out = ((zin01.dotProduct(vec)).add(zin01.scale(-1))).add(zle0.scale(-1));
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
		return -1;	
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
		return -1;
	}

	@Override
	public double getUpperBound(int n) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public Vector evaluate_Vector(double[] argument, double[] gradient) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
