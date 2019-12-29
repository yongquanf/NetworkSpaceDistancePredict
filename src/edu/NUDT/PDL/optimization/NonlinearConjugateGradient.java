package edu.NUDT.PDL.optimization;

import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.optimization.Mcsrch;
public class NonlinearConjugateGradient {

	
	
	/**
	 * update choice
	 */
	public final static int FR=0;
	public final static int PR=1;
	public final static int HS=2;
	public final static int SD=3;
	
	
	public static int MaxIters=1000;
	public static int MaxFuncEvals=1000;
	public static double StopTol=1e-5;
	public static double RelFuncTol=1e-6;
	public static String LineSearch_method="more-thuente";
	public static int LineSearch_initialstep=1;
	public static double LineSearch_xtol= 1e-15; 
	public static double LineSearch_ftol= 1e-4; 
	public static double LineSearch_gtol= 1e-2; 
	public static double LineSearch_stpmin= 1e-15; 
	public static double LineSearch_stpmax= 1e15; 
	public static int LineSearch_maxfev= 500; 
	
	
	public static int Update=PR; //default choice
	public static int RestartIters=1000;
	public static boolean RestartNW=false;
	public static double RestartNWTol=.01;
	/**
	 * optimize the function, based on the initial input value
	 * @param f
	 * @param x
	 */
	public outDS optimize( MFWithGradient f, double[] x){
		double[] xk = x;
		
		double[]gk=new double[x.length];
		double fk = f.evaluate(x, gk);
		outDS out=outDS.constructInstace();
		out.config(xk, fk, gk, 1);
		
		double []pk =new double[gk.length];
		double ak=-1;
		double gkTgk=-1;
		double bk=0;
		double gkTgkold=0;
		double []  gkMgkold =new double[gk.length];
		double [] gkold =new double[gk.length];
		double [] pkold =new double[gk.length];
		
		double []xkold=xk;
		double fkold=fk;
		
		double[] tmp1 = new double[gk.length];
		double[] tmp2= new double[gk.length];
		
		//not terminated
		while(out.ExitFlag==-1){
			if(out.Iters==0){
				
				MathUtil_optimization.copyArray(gk, pk);
				MathUtil_optimization.multiArray(pk, -1);
				ak=1;
				gkTgk=MathUtil_optimization.dotProduct(gk, gk);
								
			}else{
				if(out.Iters%RestartIters==0){
					bk=0;
					MathUtil_optimization.copyArray(gk, pk);
					MathUtil_optimization.multiArray(pk, -1);
				}else{
					switch(Update){
					case FR:{
						gkTgk=MathUtil_optimization.dotProduct(gk, gk);
						if (gkTgkold >0){
							bk=gkTgk/gkTgkold;
						}else{
							bk=0;
						}
						break;
					}
					case PR:{
						gkTgk=MathUtil_optimization.dotProduct(gk, gk);
						gkMgkold=MathUtil_optimization.minus(gk, gkold);
						if(gkTgkold>0){
							bk=MathUtil_optimization.dotProduct(gk,gkMgkold )/gkTgkold;
						}else{
							bk=0;
						}
							
						break;
					}
					case HS:{
						gkMgkold=MathUtil_optimization.minus(gk, gkold);
						double denom=MathUtil_optimization.dotProduct(pkold, gkMgkold);
						if(denom>0){
							bk=MathUtil_optimization.dotProduct(gk, gkMgkold)/denom;
						}else{
							bk=0;
						}
						break;
					}
					case SD:{
						bk=0;
						break;
					}
					}//end switch
					
					//do not allow negative conjugate direction weights
					if(bk<0){
						bk=Math.max(0,bk);						
					}
					// restart method from Nocedal and Wright
					if(RestartNW){
						double v=RestartNWTol;
						if(MathUtil_optimization.dotProduct(gk, gkold)/
								(Math.pow(gkTgkold,2))>=v){
							bk=0;
						}
					}
					//new direction
					//MathUtil.multiArray(gk, -1);
					//MathUtil.multiArray(pkold, bk);

					
					MathUtil_optimization.copyArray(gk, tmp1);
					MathUtil_optimization.multiArray(tmp1, -1);
					
					
					MathUtil_optimization.copyArray(pkold, tmp2);
					MathUtil_optimization.multiArray(tmp2, bk);
					
					pk=MathUtil_optimization.add(tmp1,tmp2);
					
					
				}
				
			}
			//===============================
			xkold=xk;
			fkold=fk;
			gkold=gk;
			pkold=pk;
			gkTgkold=gkTgk;
			
			//xk,fk,gk,ak,lsinfo,nfev
			double []fkk={fk}; //pass the result on return
			double []akk={ak}; //pass the result on return
			int []nfevv={0};
			linesearch(f,xk,fkk,gk,akk,pk,nfevv);
			
			//update counts, check exit conditions, etc.
			out.config(xk, fkk[0], gk, nfevv[0]);
		}
		return out;
	}
	
	/**
	 * the line search process, it performs a
%  line search to find a point X such that FUN(X0+A*D0) is minimized. The
%  method returns X = X0+A*D0 with the function (F) and gradient (G) at
%  that point, along with the step length (A) found by the line search.
	 * @param f
	 * @param xk
	 * @param fk
	 * @param gk
	 * @param ak, 1 element
	 * @param pk
	 * @param nfevv, 1 element
	 */
    private void linesearch(MFWithGradient FUN, double[] x0, double[] f0,
			double[] g0, double[] a0, double[] d0, int[] nfevv) {
		// TODO Auto-generated method stub
		if(LineSearch_initialstep>0){
			a0[0]=LineSearch_initialstep;
		}
		
		/**
		 * %     The purpose of cvsrch is to find a step which satisfies 
%     a sufficient decrease condition and a curvature condition.
%     The user must provide a subroutine which calculates the
%     function and the gradient.
%
%     At each stage the subroutine updates an interval of
%     uncertainty with endpoints stx and sty. The interval of
%     uncertainty is initially chosen so that it contains a 
%     minimizer of the modified function
%
%          f(x+stp*s) - f(x) - ftol*stp*(gradf(x)'s).
%
%     If a step is obtained for which the modified function 
%     has a nonpositive function value and nonnegative derivative, 
%     then the interval of uncertainty is chosen so that it 
%     contains a minimizer of f(x+stp*s).
%
%     The algorithm is designed to find a step which satisfies 
%     the sufficient decrease condition 
%
%           f(x+stp*s) <= f(x) + ftol*stp*(gradf(x)'s),
%
%     and the curvature condition
%
%           abs(gradf(x+stp*s)'s)) <= gtol*abs(gradf(x)'s).
%
%     If ftol is less than gtol and if, for example, the function
%     is bounded below, then there is always a step which satisfies
%     both conditions. If no step can be found which satisfies both
%     conditions, then the algorithm usually stops when rounding
%     errors prevent further progress. In this case stp only 
%     satisfies the sufficient decrease condition.
		 * 
		 */
		
	 int n=x0.length;
	 double xtol=LineSearch_xtol;
	 double ftol=LineSearch_ftol;
	 double gtol = LineSearch_gtol;
	 double stpmin = LineSearch_stpmin;
	 double stpmax = LineSearch_stpmax;
	 int maxfev = LineSearch_maxfev;
	 //int[] nfev={0};
	 int [] info={1};
	 double []wa=new double[x0.length];
	 int is0=0;//index from 0

	 
	 Mcsrch.mcsrch(FUN, n, x0, f0, g0, d0, is0, a0, ftol, xtol, maxfev, info, nfevv, wa,stpmin, stpmax);
	}

	public static double norm(double ar[]) {
        double v = 0;
        for (int f = 0; f < ar.length; f++)
            v += ar[f] * ar[f];
        return Math.sqrt(v);
    }
    
    
    
    
   

}
