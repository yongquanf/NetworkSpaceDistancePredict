package edu.NUDT.PDL.coordinate;

import java.util.Random;

import edu.NUDT.PDL.util.matrix.Vector;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class ratingCoord<T> implements Coordinate{
	
	int version=1;
	/**
	 * 
	 */
	private static final long serialVersionUID = 6025094688184514042L;
	
	public double INIT = .1;
	public T address;
	public Vec p;
	public Vec q=null;
	public Vec Bias=null;
	public int RANKS;
	public int LEVEL;
	public Vec thetaVec;
	public Random r;
	
	public ratingCoord(int _rank,int _level){
		RANKS=_rank;
		LEVEL=_level;
		r=new Random();
		init_matrixApproximation();
	}
	
	public void init_matrixApproximation(){
		p=new Vec(RANKS);
		q=new Vec(RANKS);
		thetaVec=new Vec(LEVEL-1);
		Bias=new Vec(1);
		
		for(int i=0;i<RANKS;i++){
			p.direction[i]=INIT*r.nextGaussian();
			q.direction[i]=INIT*r.nextGaussian();
		}
		for(int i=0;i<LEVEL-1;i++){
			thetaVec.direction[i]=INIT*r.nextGaussian();
		}
		Bias.direction[0]=0;
	}
	
	/**
	 * make a copy
	 * @return
	 */
	public ratingCoord<T> copy(){
		ratingCoord<T> tmp=new ratingCoord<T>(RANKS,LEVEL);
		tmp.p=new Vec(p);
		tmp.q=new Vec(q);
		if(this.Bias!=null){
			tmp.Bias=new Vec(this.Bias);
		}
		tmp.thetaVec=new Vec(thetaVec);
		return tmp;
	}

	@Override
	public double distanceTo(Coordinate B) {
		// TODO Auto-generated method stub
		ratingCoord<T> tmp = ((ratingCoord<T>)B);
		
		double dist1 = this.p.innerProduct(tmp.q);
		
		if(this.Bias!=null&&(tmp.Bias!=null)){
			dist1=dist1+this.Bias.direction[0]+tmp.Bias.direction[0];
		}

		return getRating(dist1,this.thetaVec);
	}
	
	/**
	 * get the rating estimate
	 * @param dist
	 * @param ratingTheta
	 * @return
	 */
	public static double getRating(double dist,Vec ratingTheta){
		double y=1;
		double tmp;
		int indicator=0;
		for(int i=0;i<ratingTheta.num_dims;i++){
			tmp=ratingTheta.direction[i];
			if(dist>=tmp){
				indicator=1;
			}else{
				indicator=0;
			}
			y=y+indicator;			
		}
		return y;
	}

	/**
	 * copy the coord
	 * @param coord
	 */
	public void copy(ratingCoord<T> coord) {
		// TODO Auto-generated method stub
		for(int i=0;i<RANKS;i++){
			p.direction[i]=coord.p.direction[i];
			q.direction[i]=coord.q.direction[i];
		}
		for(int i=0;i<LEVEL-1;i++){
			thetaVec.direction[i]=coord.thetaVec.direction[i];
		}
		Bias.direction[0]=coord.Bias.direction[0];
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb=new StringBuffer();
		
		sb.append(POut.toString(p.direction)+", "+POut.toString(q.direction)
				+", "+POut.toString(thetaVec.direction)+", "+POut.toString(Bias.direction));
		
		return sb.toString();
	}
	
	/**
	 * version control, and ensure the rank is identical
	 * @param coord
	 * @return
	 */
	public boolean isCompatible(ratingCoord<T> coord){
		/*if(this.version==coord.version){
			return true;
		}else{
			return false;
		}*/
		if(this.RANKS==coord.RANKS&&this.version==coord.version){
			return true;
		}else{
			return false;
		}
	}
	public int getVersion(){
		return this.version;
	}

	/**
	 * copy one node's new coordinate
	 * @param orig_x
	 * @param p
	 * @param l
	 * @return
	 */
	public ratingCoord<T>  copy(Vector orig_x,int p,int l) {
		// TODO Auto-generated method stub
		ratingCoord<T> coord=new ratingCoord<T>(p,l);
		
		 for(int indU=0;indU<p;indU++){
			 coord.p.setValue(indU, orig_x.value(indU) );
			 coord.q.setValue(indU, orig_x.value(p+indU));
		 }
		 for(int indT=0;indT<l-1;indT++){
			 coord.thetaVec.setValue(indT, orig_x.value(p*2+indT));
		 }
		return coord;
	}
	
	/**
	 * copy a bias rating coordinate
	 * @param orig_x
	 * @param p
	 * @param l
	 * @return
	 */
	public ratingCoord<T>  copy_bias(Vector orig_x,int p,int l) {
		// TODO Auto-generated method stub
		ratingCoord<T> coord=new ratingCoord<T>(p,l);
		
		 for(int indU=0;indU<p;indU++){
			 coord.p.setValue(indU, orig_x.value(indU) );
			 coord.q.setValue(indU, orig_x.value(p+indU));
		 }
		 for(int indT=0;indT<l-1;indT++){
			 coord.thetaVec.setValue(indT, orig_x.value(p*2+indT));
		 }
		 
		 coord.Bias.setValue(0, orig_x.value(p*2+(l-1)));
		 
		return coord;
	}
}
