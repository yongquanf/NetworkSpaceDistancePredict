package edu.NUDT.PDL.optimization;

import edu.NUDT.PDL.gradient.MFWithGradient;
import edu.NUDT.PDL.util.matrix.Vector;

public class sinAX implements MFWithGradient {

		/**
		 *  
		%    function [f,g]=example1(x,a)
		%    if nargin < 2, a = 1; end
		%    f = sin(a*x);
		%    g = a*cos(a*x);
		 * 
		 */
		double a=2;
		
		public sinAX(double _a){
			a=_a;
		}
		
		@Override
		public void computeGradient(double[] argument, double[] gradient) {
			// TODO Auto-generated method stub
			double f = Math.sin(a*argument[0]);
			double dd=a*Math.cos(a*argument[0]);
			if(gradient==null){
				gradient=new double[1];
			}
			gradient[0]=dd;
		}

		@Override
		public double evaluate(double[] argument, double[] gradient) {
			// TODO Auto-generated method stub
			double f = Math.sin(a*argument[0]);
			double dd=a*Math.cos(a*argument[0]);
			
			if(gradient==null){
				gradient=new double[1];
			}
			gradient[0]=dd;
			return f;
		}

		@Override
		public double evaluate(double[] argument) {
			// TODO Auto-generated method stub
			double f = Math.sin(a*argument[0]);
			return f;
		}

		@Override
		public double getLowerBound(int n) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getNumArguments() {
			// TODO Auto-generated method stub
			return 1;
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