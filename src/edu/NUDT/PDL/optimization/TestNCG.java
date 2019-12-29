package edu.NUDT.PDL.optimization;

public class TestNCG {

		
	static sinAX FUN;
	
	public static void main(String[] args){
		
		NonlinearConjugateGradient test =new NonlinearConjugateGradient();
		
		 double[] x={Math.PI/4};
		 FUN=new sinAX(16);
		 outDS out=test.optimize(FUN, x);
		 
	}
}
