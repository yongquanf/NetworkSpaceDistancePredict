package edu.NUDT.PDL.optimization;


public class outDS {

	    	
	     public	outDS(){
	    	 this.Display="iter";
	    	 
	     }
	    
	     
	    
		static outDS self=null;		
		static outDS getInstance(){
		if(self==null){
			self=new outDS();
		}
		return self;
		}
		
		public static outDS constructInstace(){
			return new outDS();
		}
		/**
		 * %   Standard ouptut parameters for Poblano are as follows:
%
%     OUT.X        : solution (i.e., best point found so far)
%     OUT.F        : function value at solution
%     OUT.G        : gradient at solution
%     OUT.Params   : input parameters
%     OUT.FuncEvals: number of function evaluations 
%     OUT.Iters    : number of iterations 
%     OUT.ExitFlag : termination flag
%      -1 = no termination condition has been statisfied
%       0 = successful termination (based on StopTol)
%       1 = maximum number of iterations exceeded
%       2 = maximum number of function values exceeded
%       3 = relative change in function value < RelFuncTol
%       4 = NaNs found in f, g, or norm(g)
		 * @param xk
		 * @param fk
		 * @param gk
		 * @param nfev
		 */
		public void config(double[]xk,double fk,double[]gk,int nfev){
			double relfit=-1;
			if(!config){
				Iters=0;
				ExitFlag=-1;
				X=xk;
				F=fk;
				G=gk;
				FuncEvals=nfev;
				config=true;
			}else{
				 relfit=-1;
				double oldf = F;
				if(oldf<=F){
					X=xk;
					F=fk;
					G=gk;
				}
				if(Math.abs(oldf)<eps){
					relfit=Math.abs(fk-oldf);
				}else{
					relfit=Math.abs((fk-oldf)/oldf);
				}
				FuncEvals+=nfev;
				Iters++;
			}
			
			//================================
			nx=xk.length;
			g2norm=NonlinearConjugateGradient.norm(gk);
			//System.out.println("\n\n============================\n\n"+"nx: "+nx+", g2norm: "+g2norm+
			//		"\n\n============================\n\n");
			g2normnx=g2norm/nx;
			
			if(g2normnx<NonlinearConjugateGradient.StopTol){
				 ExitFlag = 0;
			}else if(this.Iters>=NonlinearConjugateGradient.MaxIters){
				 ExitFlag = 1;
			}else if(this.FuncEvals>= NonlinearConjugateGradient.MaxFuncEvals){
				this.ExitFlag=2;
			}else if(this.Iters>0&&(relfit<=NonlinearConjugateGradient.RelFuncTol)){
				this.ExitFlag=3;
			}else if(Double.isNaN(fk)||gk==null||Double.isNaN(g2norm)){
				this.ExitFlag=4;
			}
			
			 print();
		}
		public String Display="";
		public int Iters =0;  //iterations
		public int ExitFlag=0; //- exit
		public double[]X;
		public double F;
		public double []G;
		public boolean config=false; //true after firstly visit
		public int FuncEvals=-1;
		public double eps=2.2204e-016;
		
		public int nx;
		public double g2norm;
		public double g2normnx;
		
		
		
		public void print(){
			
			if(this.Iters==0&&!this.Display.equalsIgnoreCase("off")){
				System.out.format(" Iter  FuncEvals       F(X)          ||G(X)||/N        \n");
				System.out.format("------ --------- ---------------- ----------------\n");
			}
			if(this.Display.equalsIgnoreCase("iter")){
				System.out.format("%6d %9d %16.8f %16.8f\n", Iters, FuncEvals,F, g2normnx);
			}
			if(this.ExitFlag>=0&&this.Display.equalsIgnoreCase("final")){
				System.out.format("%6d %9d %16.8f %16.8f\n", Iters,FuncEvals,F,g2normnx);
			}
		}
		
		
}
