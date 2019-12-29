/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.NUDT.PDL.coordinate;
import java.io.Serializable;
import java.util.Random;

import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.DenseVector;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.Vector;


/*
 * A vector in the Euclidian space.
 */
public class Vec implements  java.io.Serializable {
  
	/**
	 * 
	 */
	private static final long serialVersionUID = -2175900503066970566L;



	//public final  static int CLASS_HASH = Vec.class.hashCode();
	
	public double[] direction;
	public final  int num_dims;
  	static Random random = new Random(System.currentTimeMillis());
  /*
	public static Vec add(Vec lhs, Vec rhs) {
		Vec sum = new Vec(lhs);
		sum.add(rhs);
		return sum;
	}
	
	public static Vec subtract(Vec lhs, Vec rhs) {
		Vec diff = new Vec(lhs);
		diff.subtract(rhs);
		return diff;
	}
	*/
  
	public static Vec scale(Vec lhs, double k) {
		Vec scaled = new Vec(lhs);
		scaled.scale(k);
		return scaled;
	}
	
  /*
	public static Vec makeUnit(Vec v) {
		Vec unit = new Vec(v);
		unit.makeUnit();
		return unit;
	}
	*/
  
  /*
  public static Vec makeRandomUnit(int num_dims) {
    final Vec v = makeRandom (num_dims, 1.); 
    v.makeUnit();
    return v;
  }
  */

  public static Vec makeRandom(int num_dims, double axisLength) {
    final Vec v = new Vec(num_dims);
    for (int i = 0; i < num_dims; ++i) {
      double length = random.nextDouble() * axisLength;

      v.direction[i] = length;
    }
    return v;
  }
  
  
	public Vec(int _num_dims) {
		direction = new double[_num_dims];		for(int i=0;i<_num_dims;i++){
			direction[i]=0;
		}
		
    num_dims = _num_dims;
  }
	
	public Vec(Vec v) {
		this(v.direction, true);
	}

	
	public Vec(double[] init_dir, boolean make_copy) {
		if (make_copy) {
			final int num_dims = init_dir.length;
			direction = new double[num_dims];
			System.arraycopy(init_dir, 0, direction, 0, num_dims);
		}
		else {
			direction = init_dir;
		}
    int _num_dims = init_dir.length;
    num_dims = _num_dims;
	}
		/**
	 * return the subVec starts from idx1 and ends at idx2
	 * @param vv
	 * @param idx1
	 * @param idx2
	 */
	public Vec(Vec vv, int idx1, int idx2) {
		// TODO Auto-generated constructor stub
		num_dims=Math.abs(idx2-idx1);
		direction=new double[num_dims];
		System.arraycopy(vv.direction, idx1, direction, 0, num_dims);
	}



	public int getNumDimensions() {
	  // keep num_dimensions internal
    return direction.length;
	}
	
	public double[] getComponents() {
		final double[] dir_copy = new double[direction.length];
		System.arraycopy(direction, 0, dir_copy, 0, direction.length);
		return dir_copy;
	}
	
	/**
	 * inner product
	 * @param v
	 * @return
	 */	public double innerProduct(Vec v){
		double d=0;
		for (int i = 0; i < direction.length; ++i) {
			d+=v.direction[i]*this.direction[i];		
		}
		return d;
		
	}
	
	
	
	//Same regardless of using height
	public void add(Vec v) {
		for (int i = 0; i < direction.length; ++i) {
			direction[i] += v.direction[i];
		}
	}

  // only done with gravity, ignores height
  public void subtract(Vec v) {
    for (int i = 0; i < direction.length; ++i) {
      direction[i] -= v.direction[i];
    }
  }
	public void scale(double k) {
		for (int i = 0; i < direction.length; ++i) {
			direction[i] *= k;
		}
	}
	
	public boolean isUnit() {
		return (getLength() == 1.0);
	}
	
  public double getLength() {
    double sum = getPlanarLength();
    return sum;
  }

  double getPlanarLength() {
    double sum = 0;
    for (int i = 0; i < num_dims; ++i) {
      sum += (direction[i] * direction[i]);
    }
    return Math.sqrt(sum);
  }
  
	public void makeUnit() {
		final double length = getLength();
		if (length != 1.0) {
      scale (1./length);
    }
	}
  
	
	public boolean equals(Object obj) {
		if (obj instanceof Vec) {
			Vec v = (Vec) obj;
			final int num_dims = direction.length;
			for (int i = 0; i < num_dims; ++i) {
				if (direction[i] != v.direction[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		final int num_dims = direction.length;
		int hc = Vec.class.hashCode();
		for (int i = 0; i < num_dims; ++i) {
			hc ^= new Double(direction[i]).hashCode();
		}
		return hc;
	}
	
	public String toString() {
		final StringBuffer sbuf = new StringBuffer(1024);
		sbuf.append("[");
		final int num_dims = direction.length;
		for (int i = 0; true; ) {
  
			sbuf.append(direction[i]+" ");
			if (++i < num_dims) {
				sbuf.append(",");
			}
			else {
				break;
			}
		}
		sbuf.append("]");
		return sbuf.toString();
	}
	
	/**
	 * transform a vector to a matrix, construct each column first
	 * @param fromIndex
	 * @param toIndex
	 * @param rowNum
	 * @param columnNum
	 * @return
	 */
	public Matrix reshape(int fromIndex,int rowNum,int columnNum){
		Matrix mat=new  DenseMatrix(rowNum, columnNum);
		int index=0;
		for(int column=0;column<columnNum;column++){
			for(int row=0;row<rowNum;row++){
				mat.setValue(row, column, this.direction[fromIndex+index]);
				index++;
			}
			
		}
		
		return mat;
		
	}
	/*
	 * transform a matrix to a vector
	 * 	
	 */
   public static Vec toVector(Matrix mat){
	   int totalSize=mat.numRows()*mat.numColumns();
	   Vec v=new Vec(totalSize);
	   int index=0;
	   for(int rows=0;rows<mat.numRows();rows++){
		   for(int columns=0;columns<mat.numColumns();columns++){
			   
			   v.direction[index]=mat.value(rows, columns);
			   index++;
		   }
	   }
	   return v;
	   
   }
   
   /**
    * normal distribution, mean 0, variance delta^2=1
    * @param size
    * @return
    */
   public static Vec randVec(int size){
	   Vec v=new Vec(size);
	   Random r=new Random(System.currentTimeMillis());
	   for(int i=0;i<size;i++){
		   v.direction[i]=r.nextGaussian();
	   }
	   return v;
   }
   
   /**
    * transform from Vector class to Vec
    * @param vv
    * @return
    */
   public static Vec getVecFromVector(Vector vv){
	   Vec v=new Vec(vv.numDimensions());
	   for(int i=0;i<vv.numDimensions();i++){
		   v.direction[i]=vv.value(i);
	   }
	   return v;
   }
   
   /**
    * share identical storage array
    * @return
    */
   public Vector getVectorFromVec(){
	   Vector vv=new DenseVector(this.direction);
	   return vv;
   }

   public static Vector getVectorFromVec(Vec x0){
	   Vector vv=new DenseVector(x0.direction);
	   return vv;
   }
/**
 * make copy
 * @return
 */
public Vec copy() {
	// TODO Auto-generated method stub
	return new Vec(this);
}



public void clear() {
	// TODO Auto-generated method stub
	direction=null;
}


/**
 * keep the vec as zero
 */
public void zeroInit() {
	// TODO Auto-generated method stub
	for(int i=0;i<this.num_dims;i++){
		this.direction[i]=0;
	}
}


/**
 * set the chosen point into the Vec
 * @param chosenPoint
 */
public void add(java.util.Vector<Double> chosenPoint) {
	// TODO Auto-generated method stub
	int realSize=Math.min(this.num_dims, chosenPoint.size());
	
	for(int i=0;i< realSize;i++){
		this.direction[i]=chosenPoint.get(i);
	}
}


/**
 * set the  chosen centroids into the Vec
 * @param centroids
 */
public void add(double[] centroids) {
	// TODO Auto-generated method stub
	int realSize=Math.min(this.num_dims,centroids.length);
	for(int i=0;i<this.num_dims;i++){
		this.direction[i]=centroids[i];
	}
}


/**
 * set the value in the specified position
 * @param indT
 * @param value
 */
public void setValue(int indT, double value) {
	// TODO Auto-generated method stub
	direction[indT]= value;
}
}
