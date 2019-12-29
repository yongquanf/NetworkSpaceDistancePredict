package edu.NUDT.PDL.BasicDistributedMMMF;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import edu.NUDT.PDL.BiasMatrixApproximation.GradientBasedMMMF;
import edu.NUDT.PDL.BiasMatrixApproximation.MyConvergeMMMF_bias;
import edu.NUDT.PDL.BiasMatrixApproximation.Pair;
import edu.NUDT.PDL.RatingFunc.RatingFunction;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

/**
 * (1) implements the biasMMMF
 * (2) add the weighted items to remove the effects of missing elements
 * 
 * @author Administrator
 *
 * @param <T>
 */
public class DistributedMMMF<T> implements AbstractDistributedMMMF<T>{

	
	 Hashtable<T,ratingCoord<T>> ratingCache;
	/**
	 * configuration parameters
	 */
	public static int ratingType=RatingFunction.HistogramPervervingRating; //rating function Number
	public static int dim=8;
	public static int level=5;
	
	
	/**
	 * single step movement
	 */
	double lambda= 1.4125;
	
	
	public DistributedMMMF(){
		ratingCache=new Hashtable<T,ratingCoord<T>>(2);
	}
	
	public DistributedMMMF(int _dim,int _level,int _ratingType){
		ratingCache=new Hashtable<T,ratingCoord<T>>(2);
		dim=_dim;
		level=_level;
		ratingType=_ratingType;
	}
	
	/**
	 * TODO: we compute the rating function, based on the measurement results
	 * problem: how to compute the 
	 * @param samples
	 */
	public List<Pair<T>> computeRating(List<Pair<T>> samples,int ratingType){
		return RatingFunction.getInstance().compute(samples, ratingType,level);
	}
	

	
	/**
	 * 
	 * 
	 * cache the coordinate vectors for each landmarks
	 * we request their coordinates
	 * 
	 * double directions
	 * A-B BA xx xx1;xx2 xx3;xx4 xx5
	 * A-C CA yy yy1;yy2 yy3;yy4 yy5
	 * @param landmarks
	 * @return
	 */
	public void getSample(List<T> landmarks){
		Iterator<T> ier = landmarks.iterator();
		while(ier.hasNext()){
			T tmp = ier.next();
			this.getSample(tmp);			
		}
		
	}
	
	/**
	 * get the rating coordinate vectors as well as the measurement results
	 * @param landmark
	 */
	public void getSample(T landmark){
		
	}
	
	
	/**
	 * for hosts, 
	 * we must cache the rating coordinate vectors of landmarks, NOW!
	 * @param Timer
	 * @param samples
	 */
	public Matrix  processRatingSamples_Host(long Timer,List<T>hosts,List<T> landmarks,List<Pair<T>> HostToLandmarkSamples,List<Pair<T>> LandmarkToHostSamples,double lambda){
		
		
		boolean isLM=false;
		boolean isHost2Landmark=true;
		Matrix host2Landmarks=this.getMatFromMeasurements(isLM, isHost2Landmark, hosts, ratingType, landmarks, HostToLandmarkSamples);
		isHost2Landmark=false;
		Matrix  landamrk2Host=this.getMatFromMeasurements(isLM, isHost2Landmark, hosts, ratingType, landmarks, LandmarkToHostSamples);
		
		//construct landmark coordinate matrix based on caches
	/*	List<ratingCoord<T>> coords = GradientBasedMMMF.getInstance().compute_basicMMMF(host2Landmarks, landamrk2Host,
				this.getU(landmarks), this.getV(landmarks), this.getTheta(landmarks), dim, level,lambda);
*/		
		
		Hashtable<T,ratingCoord<T>> coords = GradientBasedMMMF.getInstance().compute_basicMMMF(hosts,host2Landmarks, landamrk2Host,
				this.getU(landmarks), this.getV(landmarks), this.getTheta(landmarks), dim, level,lambda);
		
		
		Iterator<Entry<T, ratingCoord<T>>> ierH = coords.entrySet().iterator();	
		while(ierH.hasNext()){	
			Entry<T, ratingCoord<T>> tmp = ierH.next();			
				this.ratingCache.put(tmp.getKey(),tmp.getValue());
		}
		
		
		return host2Landmarks;
	}
	
	public Matrix getU(List<T> landmarks){
		int n=landmarks.size();
		int p=dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!this.ratingCache.containsKey(tmp)){
				System.err.println("U: missing: "+tmp);
				continue;
			}
			Vec uu = this.ratingCache.get(tmp).p;
			
			for(int i=0;i<p;i++){
				mat.setValue(indexRow,i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;		
	}
	public Matrix getV(List<T> landmarks){
		int n=landmarks.size();
		int p=dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!this.ratingCache.containsKey(tmp)){
				System.err.println("V: missing: "+tmp);
				continue;
			}
			Vec uu = this.ratingCache.get(tmp).q;
			for(int i=0;i<p;i++){
				mat.setValue(indexRow, i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;	
		
	}
	public Matrix getTheta(List<T> landmarks){
		int n=landmarks.size();
		int p=level-1;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!this.ratingCache.containsKey(tmp)){
				System.err.println("theta: missing: "+tmp);
				continue;
			}
			
			Vec uu = this.ratingCache.get(tmp).thetaVec;
			for(int i=0;i<p;i++){
				mat.setValue(indexRow, i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;	
	}
	
	
	/**
	 * for landmarks
	 * @param Timer
	 * @param samples, raw measurements
	 */
	public Matrix processSamples_Landmarks(long Timer,List<T> landmarks,List<Pair<T>> samples){
		boolean isLM=true;
		boolean isHost2Landmark=false;
		List<T> hosts=null;
		Matrix mat=this.getMatFromMeasurements(isLM, isHost2Landmark, hosts, ratingType, landmarks, samples);
		//Matrix out=mat.copyMatrix();
		//print(mat);
		//List<ratingCoord<T>> coords = MyConvergeMMMF_bias.getInstance().compute_basicMMMF(landmarks, mat, dim, level);
	
		Hashtable<T,ratingCoord<T>> coords = MyConvergeMMMF_bias.getInstance().compute_basicMMMF(landmarks, mat, dim, level);
		//Iterator<ratingCoord<T>> ier = coords.iterator();
		Iterator<Entry<T, ratingCoord<T>>> pair = coords.entrySet().iterator();

		while(pair.hasNext()){
				Entry<T, ratingCoord<T>> tmp = pair.next();
				
				this.ratingCache.put(tmp.getKey(),tmp.getValue());
				
		}
		//===========================
		return mat;
	}
	
	private void print(Matrix mat) {
		// TODO Auto-generated method stub
		for(int i=0;i<mat.numRows();i++){
			for(int j=0;j<mat.numColumns();j++){
				System.out.print(mat.value(i, j)+" ");
			}
			System.out.print("\n");
		}
	}

	/**
	 * 
	 * @param samples
	 * @param direction, 0 from host to landmarks, 1 from landmark to host;
	 * @return
	 */
	public Matrix getMatFromMeasurements(boolean isLandmark,boolean isHost2Landmark,List<T>hosts,int ratingType,List<T> landmarks, List<Pair<T>> samples_raw){
		
		//TODO: how to deal with missing elements
		//if an element is missing, it is denoted as "<0"
		
		//transform to rating 
		List<Pair<T>> samples = this.computeRating(samples_raw, ratingType);
		
		//get rating matrix		
		int rows=1;
		int columns=1;
		if(isLandmark){
			rows=landmarks.size();	
			columns=landmarks.size();
		}else{
			if(isHost2Landmark){
				//host to landmark
				rows=1;
				columns=landmarks.size();
			}else{
				//landmark to host
				rows=landmarks.size();
				columns=1;
			}
		}
		
		
		//construct node id list according to the landmarks
		Matrix mat=new DenseMatrix(rows,columns,0);	
		
		//set value on each items
		if(isLandmark){
			//pairwise measurement between landmarks
			Iterator<Pair<T>> ier = samples.iterator();
			while(ier.hasNext()){
				Pair<T> tmp = ier.next();
				int from =landmarks.indexOf(tmp.from);
				int to=landmarks.indexOf(tmp.to);
				if(from<0||to<0){
					System.err.print("missing landmarks!\n");
					continue;
				}else{
					mat.setValue(from, to, tmp.sample);
				}
			}
			
		}else{
			//
			if(isHost2Landmark){
				Iterator<Pair<T>> ier = samples.iterator();
				while(ier.hasNext()){
					Pair<T> tmp = ier.next();
					int to=landmarks.indexOf(tmp.to);
					//int from=hosts.indexOf(tmp.from);
					int from=0;
					if(to<0||from<0){
						System.err.print("missing host or landmarks!\n");
						continue;
					}else{
						/*System.out.println("hostSize: "+hosts.size()+"from: "+from+",to: "+to+", ma,rows: "+mat.numRows()
								+", mat.columns: "+mat.numColumns());*/
						mat.setValue(from,to , tmp.sample);
					}					
				}
			}else{
				//landmark to hosts
				Iterator<Pair<T>> ier = samples.iterator();
				while(ier.hasNext()){
					Pair<T> tmp = ier.next();
					int from=landmarks.indexOf(tmp.from);
					//int to=hosts.indexOf(tmp.to);
					int to=0;
					if(to<0||from<0){
						System.err.print("missing host or landmarks!\n");
						continue;
					}else{
						mat.setValue(from,to , tmp.sample);
					}					
				}
				
				
			}
		}//end
		return mat;
		
		
	}

	/**
	 * process based on centralized rating
	 * @param timer
	 * @param landmarks
	 * @param centralizedRatingMat
	 * @return
	 */
	public Matrix processSamples_Landmarks(long timer, List<T> landmarks,
			Matrix centralizedRatingMat) {
		// TODO Auto-generated method stub
		Matrix mat=centralizedRatingMat.subMatrix(landmarks,landmarks);
		
		Hashtable<T,ratingCoord<T>> coords = MyConvergeMMMF_bias.getInstance().compute_basicMMMF(landmarks, mat, dim, level);
		//Iterator<ratingCoord<T>> ier = coords.iterator();
		Iterator<Entry<T, ratingCoord<T>>> pair = coords.entrySet().iterator();

		while(pair.hasNext()){
				Entry<T, ratingCoord<T>> tmp = pair.next();
				
				this.ratingCache.put(tmp.getKey(),tmp.getValue());
				
		}
		//===========================
		return mat;
		
	}

	/**
	 * process based on hosts
	 * @param timer
	 * @param hh
	 * @param landmarks
	 * @param centralizedRatingMat
	 */
	public void processRatingSamples_Host(long timer, List<T> hosts,
			List<T> landmarks, Matrix centralizedRatingMat,double lambda) {
		// TODO Auto-generated method stub
		Matrix host2Landmarks=centralizedRatingMat.subMatrix(hosts, landmarks);
		Matrix  landamrk2Host=centralizedRatingMat.subMatrix(landmarks,hosts);
		
		//construct landmark coordinate matrix based on caches
		Hashtable<T,ratingCoord<T>> coords = GradientBasedMMMF.getInstance().compute_basicMMMF(hosts,host2Landmarks, landamrk2Host,
				this.getU(landmarks), this.getV(landmarks), this.getTheta(landmarks), dim, level,lambda);
		
		
		Iterator<Entry<T, ratingCoord<T>>> ierH = coords.entrySet().iterator();	
		while(ierH.hasNext()){	
			Entry<T, ratingCoord<T>> tmp = ierH.next();			
				this.ratingCache.put(tmp.getKey(),tmp.getValue());
		}

	}
	
	
	/**
	 * process samples based on the samples from pairwise measurements between landmarks
	 * @param timer
	 * @param landmarks
	 * @param centralizedRatingMat
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> processSamplesLandmarks_(long timer, List<T> landmarks,
			Matrix centralizedRatingMat) {
		// TODO Auto-generated method stub
	//	Matrix mat=centralizedRatingMat.subMatrix(landmarks,landmarks);
		
		Hashtable<T,ratingCoord<T>> coords = MyConvergeMMMF_bias.getInstance().compute_basicMMMF(landmarks, centralizedRatingMat, dim, level);
		//Iterator<ratingCoord<T>> ier = coords.iterator();
		
		return coords;		
	}
	
	/**
	 * process samples based on hosts measuremetns
	 * identical matrix
	 * @param timer
	 * @param hosts
	 * @param landmarks
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param lambda
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> processRatingSamplesHost(long timer,List<T> hosts, 
			List<T> landmarks, Matrix host2Landmarks, Hashtable<T,ratingCoord<T>> landmark_x) {			
		//construct landmark coordinate matrix based on caches
		double lambda=2.4;
		Hashtable<T,ratingCoord<T>> coords = GradientBasedMMMF.getInstance().compute_basicMMMF(hosts,host2Landmarks, host2Landmarks.transpose(),
				this.getU(landmarks, landmark_x), this.getV(landmarks, landmark_x), this.getTheta(landmarks, landmark_x), dim, level,lambda);		
		return coords;

	}
	
	/**
	 * update host's coordinate
	 * @param timer
	 * @param x
	 * @param landmarks
	 * @param landmark_x
	 * @param host2Landmarks
	 * @return
	 */
	public ratingCoord<T> processRatingSamplesHost(long timer,T host,ratingCoord<T> x, 
			List<T> landmarks, Hashtable<T,ratingCoord<T>> landmark_x,Matrix host2Landmarks){
		
	
		
		ratingCoord<T> coord = GradientBasedMMMF.getInstance().compute_conjugateGradient(x,host2Landmarks, host2Landmarks.transpose(),
				this.getU(landmarks, landmark_x), this.getV(landmarks, landmark_x), this.getTheta(landmarks, landmark_x), dim, level,lambda);		
		return coord;
	}
	
	
	
	private Matrix  getTheta(List<T> landmarks,
			Hashtable<T, ratingCoord<T>> landmark_x) {
		// TODO Auto-generated method stub
		int n=landmarks.size();
		int p=level-1;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!landmark_x.containsKey(tmp)){
				System.err.println("theta: missing: "+tmp);
				continue;
			}
			
			Vec uu = landmark_x.get(tmp).thetaVec;
			for(int i=0;i<p;i++){
				mat.setValue(indexRow, i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;	
	}

	private Matrix  getV(List<T> landmarks,
			Hashtable<T, ratingCoord<T>> landmark_x) {
		// TODO Auto-generated method stub
		int n=landmarks.size();
		int p=dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!landmark_x.containsKey(tmp)){
				System.err.println("V: missing: "+tmp);
				continue;
			}
			Vec uu = landmark_x.get(tmp).q;
			for(int i=0;i<p;i++){
				mat.setValue(indexRow, i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;	
	}

	private Matrix  getU(List<T> landmarks,
			Hashtable<T, ratingCoord<T>> landmark_x) {
		// TODO Auto-generated method stub
		int n=landmarks.size();
		int p=dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<T> ier = landmarks.iterator();
		int indexRow=0;
		while(ier.hasNext()){
			T tmp = ier.next();
			if(!landmark_x.containsKey(tmp)){
				System.err.println("U: missing: "+tmp);
				continue;
			}
			Vec uu =landmark_x.get(tmp).p;
			
			for(int i=0;i<p;i++){
				mat.setValue(indexRow,i, uu.direction[i]);
			}
			indexRow++;
		}
		return mat;	
	}

	/**
	 * two different matrix
	 * @param timer
	 * @param hosts
	 * @param landmarks
	 * @param host2Landmarks
	 * @param landamrk2Host
	 * @param lambda
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> processRatingSamples_Host(long timer,List<T> hosts, 
			List<T> landmarks, Matrix host2Landmarks,Matrix  landamrk2Host,double lambda) {			
		//construct landmark coordinate matrix based on caches
		Hashtable<T,ratingCoord<T>> coords = GradientBasedMMMF.getInstance().compute_basicMMMF(hosts,host2Landmarks, landamrk2Host,
				this.getU(landmarks), this.getV(landmarks), this.getTheta(landmarks), dim, level,lambda);		
		return coords;

	}
	
}
