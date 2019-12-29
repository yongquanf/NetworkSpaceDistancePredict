package edu.NUDT.PDL.BiasMatrixApproximation;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.util.matrix.Matrix;

public class M3fshc_gradient<T>{

	static M3fshc_gradient self=null;
	 
public static M3fshc_gradient getInstance(){
	if(self==null){
		self=new M3fshc_gradient();
	}
	return self;
}
	
/**
 *  calculate the gradient for the MMMF with bias	
 * @param index
 * @param v
 * @param Y_in rating matrix, hosts to landmarks
 * @param Y_out, rating matrix, targets to hosts
 * @param U_l
 * @param V_l
 * @param B_l
 * @param theta_l
 * @param lambda
 * @param obj objective function
 * @param dx the gradient 
 */
public void compute(int index,Vec v, Matrix Y_in,Matrix Y_out,Matrix U_l,
		Matrix V_l,Matrix B_l,Matrix theta_l,double lambda,Vec obj, Vec dx){
	
	
	
}













}
