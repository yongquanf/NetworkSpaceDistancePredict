package edu.NUDT.PDL.BasicDistributedMMMF;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;

public class M3fshc_ALPS<T> implements MFWithGradient{

	
/**
 * configuration parameters for gradient computation
 */
//T address; /*the address of the host which calculates its dx and obj*/
int landmarkNum;
int dim;
int level;
public Matrix D_host2landmark;
public Matrix D_landmark2host;
Vec v; // the coordinate vector of the host

//landmarks coordinate matrix
public Matrix x_Ul;
public Matrix x_Vl;
public Matrix x_thetal;

double lambda;
boolean isLandmark=false;

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
public M3fshc_ALPS(int _dim, int _level,Matrix _D_host2landmark,Matrix _D_landmark2host,
		Matrix _x_Ul,Matrix _x_Vl,	Matrix _x_thetal,double _lambda
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
	 lambda=_lambda;
}

@Override
public void computeGradient(double[] argument, double[] gradient) {
	// TODO Auto-generated method stub
}

public double evaluate(double[] argument, double[] gradient){
	
	v=new Vec(argument,true);
	//number of hosts
	int n=D_host2landmark.numRows();
	//landmarks
	int m=D_host2landmark.numColumns();
	//System.out.println("n: "+n+", m: "+m);

	
	int n1=this.x_Ul.numRows();
	int p=this.x_Ul.numColumns();
	int l2=this.x_thetal.numColumns();
	int l=l2+1;
	
	Matrix U=v.reshape(0, n, p);
	Matrix V=v.reshape(n*p, n, p);
	Matrix theta=v.reshape(n*p+n*p, n, l-1);
	
	Matrix X1=U.multiply(x_Vl);
	Matrix X2=x_Ul.multiply(V);
	

	
	Matrix YY_ingt0=greaterIndicator(D_host2landmark,0);
	Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);
	
	//for each non-missing elements
	Matrix BX1=X1.dotProduct(YY_ingt0);
	Matrix BX2=X2.dotProduct(YY_outgt0);
	
	
	//X1=null;
	//X2=null;
	
	Matrix dU=U.scale(lambda);
	Matrix dV=V.scale(lambda);
	Matrix dtheta=new DenseMatrix(n,l-1,0);
	
	
	Vector  regobj=new DenseVector(n,0);
	for(int i=0;i<n;i++){
		double val=U.rowVector(i).dotProduct(U.rowVector(i))+
		V.rowVector(i).dotProduct(V.rowVector(i));
		//val=(val*lambda)/2;
		
		//System.out.println(i+" : "+val);
		
		regobj.setValue(i,(val*lambda)/2);
	}
	
	Vector  lossobj=new DenseVector(n,0);
	Vector tmp=new DenseVector(m,1);
	
	for(int i=0;i<n;i++){
		//the loss value for each level
		for(int k=1;k<=l-1;k++){
			Matrix S1=YY_ingt0.add((greaterIndicator(D_host2landmark, k)).scale(-2));
			Vector S_in = new DenseVector(S1.rowVector(i));
			
			Matrix S2=YY_outgt0.add((greaterIndicator(this.D_landmark2host,k)).scale(-2));
			Vector S_out = new DenseVector(S2.columnVector(i));
			
		
			Vector tmp22 = (tmp.scale(theta.value(i,k-1))).dotProductVec(S_in);					
			
			/*if(S_in==null){
				System.err.println("S_in null!");
			}*/					
			Vector tt2=new DenseVector(BX1.rowVector(i));
			
		/*	if(tt2==null){
				System.err.println("tt2 null!");
			}*/
			
			Vector tmp333 = (tt2.dotProductVec(S_in)).scale(-1);			
		/*	if(tmp333==null){
				System.err.println("tmp3 null!");
			}*/
			
			//Vector tt=tmp333.scale(-1);
			
			
			Vector BZ1 = tmp22.add(tmp333);
			
			DenseVector tt5 = new DenseVector(x_thetal.columnVector(k-1));
/*			if(tt5 ==null){
				System.out.println("theta is null!");
			}
			if(S_out==null){
				System.out.println("S_out is null!");						
			}*/
						
			Vector tmp4=tt5.dotProductVec(S_out);
			
			DenseVector tt6 = new DenseVector(BX2.columnVector(i));					
			Vector tmp5=tt6.dotProductVec(S_out);
			
			Vector BZ2=tmp4.add(tmp5.scale(-1));
			double ddddd = lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum();
			lossobj.setValue(i,ddddd);
			
			Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
			Vector tmp2=hprime(BZ2).dotProductVec(S_out);
			
			//==========================
			Vector tmp3 = dU.rowVector(i);
			Vector tmpdU = tmp3.add((tmp1.multiply(x_Vl)).scale(-1));
			
			for(int ind=0;ind<tmpdU.numDimensions();ind++){
				dU.setValue(i, ind, tmpdU.value(ind));
			}
			tmp4=dV.rowVector(i);
			Vector tmpdV = tmp4.add((tmp2.multiply(x_Ul)).scale(-1));
			for(int ind=0;ind<tmpdV.numDimensions();ind++){
				dV.setValue(i, ind, tmpdV.value(ind));
			}
			//dtheta					
			dtheta.setValue(i, k-1, tmp1.sum());
								
		}

	}
	//=====================================
	Vector obj = regobj.add(lossobj);
	
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
			
		}else if((i>=len7+len8)
				&&
				(i<len7+len8+len9)){
			
			out.setValue(i, tmp9.value(i-(len7+len8)));
		}							
	}
	tmp7.clear();
	tmp8.clear();
	tmp9.clear();
			
	double[] dd88 = out.asArray();
	if(gradient.length!=dd88.length){
		System.err.println("gradient size do not coinside!");
		System.exit(-1);
		
	}
	//assign value
	for(int i=0;i<dd88.length;i++){
		gradient[i]=dd88[i];
	}
	out.clear();
	//BiasConjgrad_gradientBias.print(gradient,"gradient");
	/**
	 * all elements in terms of sum
	 */
	if(obj.numDimensions()==1){
		return obj.value(0);
	}else{
		System.err.println("only one item permitted");
		return obj.value(0);
	}
	
	
	
}
 
/**
 * for hosts to landmarks
 * @param argument
 * @param gradient
 * @return
 */
public Vector evaluate_Vector(double[] argument, double[] gradient) {
	// TODO Auto-generated method stub
	v=new Vec(argument,true);
	//number of hosts
	int n=D_host2landmark.numRows();
	//landmarks
	int m=D_host2landmark.numColumns();
	//System.out.println("n: "+n+", m: "+m);

	
	int n1=this.x_Ul.numRows();
	int p=this.x_Ul.numColumns();
	int l2=this.x_thetal.numColumns();
	int l=l2+1;
	
	Matrix U=v.reshape(0, n, p);
	Matrix V=v.reshape(n*p, n, p);
	Matrix theta=v.reshape(n*p+n*p, n, l-1);
	
	Matrix X1=U.multiply(x_Vl);
	Matrix X2=x_Ul.multiply(V);
	

	
	Matrix YY_ingt0=greaterIndicator(D_host2landmark,0);
	Matrix YY_outgt0=greaterIndicator(D_landmark2host,0);
	
	//for each non-missing elements
	Matrix BX1=X1.dotProduct(YY_ingt0);
	Matrix BX2=X2.dotProduct(YY_outgt0);
	
	
	//X1=null;
	//X2=null;
	
	Matrix dU=U.scale(lambda);
	Matrix dV=V.scale(lambda);
	Matrix dtheta=new DenseMatrix(n,l-1,0);
	
	
	Vector  regobj=new DenseVector(n,0);
	for(int i=0;i<n;i++){
		double val=U.rowVector(i).dotProduct(U.rowVector(i))+
		V.rowVector(i).dotProduct(V.rowVector(i));
		//val=(val*lambda)/2;
		
		//System.out.println(i+" : "+val);
		
		regobj.setValue(i,(val*lambda)/2);
	}
	
	Vector  lossobj=new DenseVector(n,0);
	Vector tmp=new DenseVector(m,1);
	
	for(int i=0;i<n;i++){
		//the loss value for each level
		for(int k=1;k<=l-1;k++){
			Matrix S1=YY_ingt0.add((greaterIndicator(D_host2landmark, k)).scale(-2));
			Vector S_in = new DenseVector(S1.rowVector(i));
			
			Matrix S2=YY_outgt0.add((greaterIndicator(this.D_landmark2host,k)).scale(-2));
			Vector S_out = new DenseVector(S2.columnVector(i));
			
		
			Vector tmp22 = (tmp.scale(theta.value(i,k-1))).dotProductVec(S_in);					
			
			/*if(S_in==null){
				System.err.println("S_in null!");
			}*/					
			Vector tt2=new DenseVector(BX1.rowVector(i));
			
		/*	if(tt2==null){
				System.err.println("tt2 null!");
			}*/
			
			Vector tmp333 = (tt2.dotProductVec(S_in)).scale(-1);			
		/*	if(tmp333==null){
				System.err.println("tmp3 null!");
			}*/
			
			//Vector tt=tmp333.scale(-1);
			
			
			Vector BZ1 = tmp22.add(tmp333);
			
			DenseVector tt5 = new DenseVector(x_thetal.columnVector(k-1));
/*			if(tt5 ==null){
				System.out.println("theta is null!");
			}
			if(S_out==null){
				System.out.println("S_out is null!");						
			}*/
						
			Vector tmp4=tt5.dotProductVec(S_out);
			
			DenseVector tt6 = new DenseVector(BX2.columnVector(i));					
			Vector tmp5=tt6.dotProductVec(S_out);
			
			Vector BZ2=tmp4.add(tmp5.scale(-1));
			
			lossobj.setValue(i,lossobj.value(i)+h(BZ1).sum()+h(BZ2).sum() );
			
			Vector tmp1 = hprime(BZ1).dotProductVec(S_in);
			Vector tmp2=hprime(BZ2).dotProductVec(S_out);
			
			//==========================
			Vector tmp3 = dU.rowVector(i);
			Vector tmpdU = tmp3.add((tmp1.multiply(x_Vl)).scale(-1));
			
			for(int ind=0;ind<tmpdU.numDimensions();ind++){
				dU.setValue(i, ind, tmpdU.value(ind));
			}
			tmp4=dV.rowVector(i);
			Vector tmpdV = tmp4.add((tmp2.multiply(x_Ul)).scale(-1));
			for(int ind=0;ind<tmpdV.numDimensions();ind++){
				dV.setValue(i, ind, tmpdV.value(ind));
			}
			//dtheta					
			dtheta.setValue(i, k-1, tmp1.sum());
								
		}

	}
	//=====================================
	Vector obj = regobj.add(lossobj);
	
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
			
		}else if((i>=len7+len8)
				&&
				(i<len7+len8+len9)){
			
			out.setValue(i, tmp9.value(i-(len7+len8)));
		}							
	}
	tmp7.clear();
	tmp8.clear();
	tmp9.clear();
			
	double[] dd88 = out.asArray();
	if(gradient.length!=dd88.length){
		System.err.println("gradient size do not coinside!");
		System.exit(-1);
		
	}
	//assign value
	for(int i=0;i<dd88.length;i++){
		gradient[i]=dd88[i];
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
	
	Vector tmp3 = ( zle0.scale(0.5)).add((zle0.dotProductVec(vec)).scale(-1));
	
	Vector tmp2 = (zin01.dotProductVec(vec.dotProductVec(vec))).scale(0.5);
	
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


}

