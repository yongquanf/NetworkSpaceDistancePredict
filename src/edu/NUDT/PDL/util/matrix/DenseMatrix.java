/*
 * LingPipe v. 3.9
 * Copyright (C) 2003-2010 Alias-i
 *
 * This program is licensed under the Alias-i Royalty Free License
 * Version 1 WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the Alias-i
 * Royalty Free License Version 1 for more details.
 *
 * You should have received a copy of the Alias-i Royalty Free License
 * Version 1 along with this program; if not, visit
 * http://alias-i.com/lingpipe/licenses/lingpipe-license-1.txt or contact
 * Alias-i, Inc. at 181 North 11th Street, Suite 401, Brooklyn, NY 11211,
 * +1 (718) 290-9170.
 */

package edu.NUDT.PDL.util.matrix;

import java.util.Arrays;
import java.util.List;

import edu.NUDT.PDL.BiasMatrixApproximation.RatingGradient;

/**
 * A <code>DenseMatrix</code> is a matrix implementation suitable for
 * matrices with primarily non-zero values.  The dimensionality of a
 * dense matrix is set at construction time and immutable afterwards.
 * Values may be specified at construction time or given default
 * values; they are mutable and may be reset after construction.
 *
 * <P><i>Implementation Note:</i> A dense matrix represents the values
 * with a two-dimensional array of primitive double values.
 *
 * @author Bob Carpenter
 * @version 2.4
 * @since   LingPipe2.0
 */
public class DenseMatrix extends AbstractMatrix {

    private  double[][] mValues;
    private final int mNumRows;
    private final int mNumColumns;

    private static final boolean IGNORE = true;

    /**
     * Construct a dense matrix with the specified positive number of
     * rows and columns.  All values are initially <code>0.0</code>.
     *
     * @param numRows Number of rows for this matrix.
     * @param numColumns Number of columns for this matrix.
     * @throws IllegalArgumentException If either the number of rows or
     * columns is not positive.
     */
    public DenseMatrix(int numRows, int numColumns) {
        this(zeroValues(numRows,numColumns),IGNORE);
    }

    /**
     * copy the val to each item
     * @param numRows
     * @param numColumns
     * @param val
     */
    public DenseMatrix(int numRows, int numColumns,double val) {
        this(sameValues(numRows,numColumns,val),IGNORE);
    }
    
    private static double[][] sameValues(int numRows, int numColumns, double val) {
		// TODO Auto-generated method stub
    	double[][] result = new double[numRows][numColumns];
        for (int i = 0; i < result.length; i++)
            Arrays.fill(result[i],val);
	return result;
	}

	/**
     * Construct a dense matrix with the specified values.  Row
     * dimensionality is determined by the dimensionality of the
     * specified array of values.  Column dimensionality is specified
     * as the maximum length of a row of specified values.  Shorter
     * rows in the specified values are filled with <code>0.0</code>.
     * All labels are initialized to <code>null</code>.
     *
     * @param values Two-dimensional array of values on which to base
     * this matrix.
     * @throws IllegalArgumentException If the either dimension of
     * the values array is 0.
     */
    public DenseMatrix(double[][] values) {
	this(copyValues(values),IGNORE);
    }

    public Matrix copyMatrix(){
    	return new DenseMatrix(this.mValues);
    }
    
    // package protected with dummy arg to distinguish
    DenseMatrix(double[][] values,
                boolean ignoreMe) {
        mValues = values;
	mNumRows = values.length;
	if (mNumRows < 1) {
	    String msg = "Require positive number of rows."
		+ " Found number of rows=" + mNumRows;
	    throw new IllegalArgumentException(msg);
	}
	mNumColumns = values[0].length; 
	if (mNumColumns < 1) {
	    String msg = "Require positive number of columns."
		+ " Found number of columns=" + mNumColumns;
	    throw new IllegalArgumentException(msg);
	}
    }

    @Override
    public final int numRows() {
        return mNumRows;
    }

    @Override
    public final int numColumns() {
        return mNumColumns;
    }

    @Override
    public double value(int row, int column) {
        return mValues[row][column];
    }

    @Override
    public void setValue(int row, int column, double value) {
        mValues[row][column] = value;
    }


    private static int numColumns(double[][] values) {
	int numColumns = 0;
	for (int i = 0; i < values.length; i++)
	    numColumns = Math.max(numColumns,values[i].length);
	return numColumns;
    }

    /**
     * assume each row, column has identical length
     * @param values
     * @return
     */
    private static double[][] copyValues(double[][] values) {
	int numRows = values.length;
	//int numColumns = numColumns(values);
	int numColumns =values[0].length;
	
	double[][] result = new double[numRows][numColumns];
        for (int i = 0; i < numRows; i++) {
        	for(int j=0;j<numColumns;j++){
        		result[i][j]=values[i][j];
        	}
/*            for (int j = 0; j < values[i].length; j++)
                result[i][j] = values[i][j];
            for (int j = values[i].length; j < numColumns; j++)
                result[i][j] = 0.0;*/
        }
	return result;
    }

    private static double[][] zeroValues(int numRows, int numColumns) {
	double[][] result = new double[numRows][numColumns];
        for (int i = 0; i < result.length; i++)
            Arrays.fill(result[i],0.0);
	return result;
    }
    
    //=======================================
    //matrix multiply,assume that the multiply is as follows:
    //this *mat'
    //=======================================
    public Matrix multiply(Matrix mat){
    	/*if(this.mNumColumns!=mat.numColumns()){
    		System.err.println("error  multiply!");
    		return null;
    	}*/
    	int rows=this.numRows();
    	int columns=mat.numRows();
    	Matrix out=new DenseMatrix(rows,columns);
    	for(int ii=0;ii<rows;ii++){
    		for(int jj=0;jj<columns;jj++){
    			double val=rowVector(ii).dotProduct(mat.rowVector(jj));
    			out.setValue(ii, jj, val);
    		}
    	}
    	return out;
    }
    
    /**
     * add two matrix, return the resultant matrix
     * @param mat
     * @return
     */
    public Matrix add(Matrix mat){
    	if(this.mNumColumns!=mat.numColumns()||this.numRows()!=mat.numRows()){
    		System.err.println("error add!");
    		return null;
    	}
    	
    	Matrix out=new DenseMatrix(this.mValues);
    	for(int i=0;i<mat.numRows();i++){
    		for(int j=0;j<mat.numColumns();j++){
    			out.setValue(i, j, value(i, j)+mat.value(i, j));
    		}
    		
    	}
    	return out;
    }
    
    /**
     * dot product two matrices
     */
    public Matrix dotProduct(Matrix mat){
    	if(this.mNumColumns!=mat.numColumns()||this.numRows()!=mat.numRows()){
    		System.err.println("error dotProduct!");
    		return null;
    	}
    	Matrix out=new DenseMatrix(this.numRows(),this.numColumns());
    	for(int i=0;i<mat.numRows();i++){
    		for(int j=0;j<mat.numColumns();j++){
    			out.setValue(i, j, value(i, j)*mat.value(i, j));
    		}
    		
    	}
    	return out;
    }
    
    /**
     * scale the matrix
     */
    public Matrix scale(double scalar){
    	Matrix out=new DenseMatrix(this.mValues);
    	for(int i=0;i<this.numRows();i++){
    		for(int j=0;j<this.numColumns();j++){
    			out.setValue(i, j, value(i, j)*scalar);
    		}
    		
    	}
    	return out;
    }
    
    /**
     * sum
     */
    public double sum(){
    	double out=0;
    	for(int i=0;i<this.numRows();i++){
    		for(int j=0;j<this.numColumns();j++){
    			out+=this.value(i, j);
    		}
    	}
    		return out;
    }
    
    /**
     * compute M(:)
     */
    public Vector flat(){
    	Vector vec=new DenseVector(this.numRows()*this.numColumns());
    	int index=0;
    	for(int column=0;column<numColumns();column++){
    		for(int row=0;row<numRows();row++){
    			vec.setValue(index, value(row, column));
    			index++;
    		}    		
    	}
    	return vec;
    }
    /**
     * transpose of the original matrix
     */
    public Matrix transpose(){
    	Matrix ma=new DenseMatrix(this.numColumns(),this.numRows());
    	for(int r=0;r<ma.numRows();r++){
    		for(int c=0;c<ma.numColumns();c++){
    			ma.setValue(r, c, this.value(c, r));
    		}
    	}
    	return ma;
    }
    
	/**
	 * compare two matrix at different position
	 * @param mat
	 * @param mat2
	 * @return
	 */
	public Matrix greaterEqualIndicator(Matrix matA,Matrix matB){
		int n=matA.numRows();
		int m=matA.numColumns();
		
		Matrix out=new DenseMatrix(n,m,0);
		for(int i=0;i<n;i++){
			for(int j=0;j<m;j++){
				if(matA.value(i, j)>=matB.value(i, j)){
					out.setValue(i, j, 1);
				}
			}
			
		}
		
		return out;
	}
	/**
	 * 
	 * @param matA
	 * @param matB
	 * @param hasEquality
	 * @return
	 */
	public Matrix greaterEqualIndicator(Matrix matB,boolean hasEquality){
		int n=numRows();
		int m=numColumns();
		
		Matrix out=new DenseMatrix(n,m,0);
		for(int i=0;i<n;i++){
			for(int j=0;j<m;j++){
				if(value(i, j)>matB.value(i, j)){
					out.setValue(i, j, 1);
				}else if(hasEquality&&value(i, j)==matB.value(i, j)){
					out.setValue(i, j, 1);
				}
				
			}
			
		}
		
		return out;
	}
	/**
	 * greater or equal
	 * @param val
	 * @param hasEquality
	 * @return
	 */
	public Matrix greaterEqualIndicator(double val,boolean hasEquality){
		int n=numRows();
		int m=numColumns();
		
		Matrix out=new DenseMatrix(n,m,0);
		for(int i=0;i<n;i++){
			for(int j=0;j<m;j++){
				if(value(i, j)>val){
					out.setValue(i, j, 1);
				}else if(hasEquality&&value(i, j)==val){
					out.setValue(i, j, 1);
				}
				
			}
			
		}
		
		return out;
	}
	
	public Matrix smallerIndicator(double val, boolean hasEquality){
		int n=numRows();
		int m=numColumns();
		
		Matrix out=new DenseMatrix(n,m,0);
		for(int i=0;i<n;i++){
			for(int j=0;j<m;j++){
				if(value(i, j)<val){
					out.setValue(i, j, 1);
				}else if(hasEquality&&value(i, j)==val){
					out.setValue(i, j, 1);
				}
				
			}
			
		}
		
		return out;
	}
	
	
	/**
	 * mae
	 */
	public  double mae(Matrix x0,Matrix x1){
		
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
	 * RMSE
	 * @param estimated
	 * @param real
	 * @return
	 */
	public  double RMSE(Matrix estimated,Matrix real){
		
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
	 * get sub-matrix
	 */
	public Matrix subMatrix(List froms, List tos){
		int r=froms.size();
		int c=tos.size();
		Matrix out=new DenseMatrix(r,c,0);
		for(int i=0;i<r;i++){
			for(int j=0;j<c;j++){
				out.setValue(i, j, this.value(((Integer)(froms.get(i))).intValue(), 
						((Integer)(tos.get(j))).intValue()));
			}
		}
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
	
	/**
	 * clear the data structure
	 */
	public void clear(){
		mValues=null;
		
	}
	
	/**
	 * return the matrix
	 */
	public String toString(){
		StringBuffer buf=new StringBuffer();
		for(int i=0;i<this.numRows();i++){
			for(int j=0;j<this.numColumns();j++){
				buf.append(this.value(i, j)+" ");
			}
			buf.append("\n");
		}
		return buf.toString();
	
	}
}
