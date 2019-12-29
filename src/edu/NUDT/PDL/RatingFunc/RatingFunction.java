package edu.NUDT.PDL.RatingFunc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;


import edu.NUDT.PDL.BiasMatrixApproximation.Pair;
import edu.NUDT.PDL.cluster.FeatureExtractor;
import edu.NUDT.PDL.cluster.KMeansClusterer;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.util.Reporters;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;

public class RatingFunction<T> {

	//find the 5, and 95 percentile value
	MathUtil math=new MathUtil(10);
	
	static RatingFunction self=null;
	/**
	 * rating type
	 */
	public final static int EqualLenRating=1;
	public final static int  HistogramPervervingRating=0;
	public final static int  ClusteringBasedRating=2;
	
	/**
	 * compute rating based on the sample set
	 */
	public static int sampleSize=20;
	
	
	/**
	 * singleton instance
	 * @return
	 */
	public static RatingFunction getInstance(){
		if(self==null){
			self=new RatingFunction();
		}
		return self;
	}
	
	/**
	 * compute the rating values
	 * @param samples
	 * @param ratingType
	 * @return
	 */
	public List<Pair<T>> compute(List<Pair<T>> samples,int ratingType,int ratingLevel){
		switch(ratingType){
		case EqualLenRating:{
			return this.EqualLenRating(samples, ratingLevel);			
		}
		case HistogramPervervingRating:{
			return this.HistogramPervervingRating(samples, ratingLevel);
		}
		case ClusteringBasedRating:{
			return this.ClusteringBasedRating(samples, ratingLevel);
		}
		default:{
			System.err.print("Unsupported rating function!\n");
			return null;			
		}
		
		}
	}
	
	
	/**
	 * locally compute the rating, for each node
	 * @param asMatrix
	 * @param ratingType
	 * @param level
	 * @return
	 */
	public Matrix local_compute(Matrix samples, Vector<Double> rawForRatingSeparation,int ratingType,int ratingLevel,Vec outSeparators) {
		// TODO Auto-generated method stub
		switch(ratingType){
		case EqualLenRating:{
			return this.EqualLenRating(samples, rawForRatingSeparation,ratingLevel,outSeparators);			
		}
		case HistogramPervervingRating:{
			return this.HistogramPervervingRating(samples,rawForRatingSeparation, ratingLevel,outSeparators);
		}
		case ClusteringBasedRating:{
			return this.ClusteringBasedRating(samples, rawForRatingSeparation,ratingLevel,outSeparators);
		}
		default:{
			System.err.print("Unsupported rating function!\n");
			return null;			
		}
		
		}
	}
	


	private Matrix ClusteringBasedRating(Matrix samples,
			Vector rawForRatingSeparation, int ratingLevel,Vec outSeparators) {
		// TODO Auto-generated method stub
		//init K-means clustering
		 KMeansClusterer clusterer
       = new KMeansClusterer(ID_PARSER,ratingLevel,maxEpochs,false,minImprovement);

		 Random random = new Random(); // (747L);
		
		
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);		
		
		
		/**
		 * sampled matrix for rating
		 */
		//Vector<Double> sampleVec=new Vector<Double>(sampleSize);
		//int[] indexPermuted;
		//sample each columns, for each node, the same with the distributed mechanism
		double rating=-1;
		double p;
		
		for(int i=0;i<N;i++){
			//find the sampled measurements
			
			
			//compute  separation points
			 Set<double[]> eltSet = config1dCoord(rawForRatingSeparation);
	  		  
			  Set<Set<double[]>> clustering
	          = clusterer.cluster(eltSet,random,Reporters.stdOut());
			  
			  double[]centroids=new double[ratingLevel];
			  //1d coordinate for each rating level
			  for(int ii=0;ii<ratingLevel;ii++){
				  centroids[ii]=clusterer.centroids_out[ii][0];
			  }
			  //sort the centroids
			  Arrays.sort(centroids);
			  
			  if(outSeparators!=null){
				  //add separators
				  outSeparators.add(centroids);
			  }
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				//============================
				sample=samples.value(i, j);
				 rating=getClusteringIndex(sample,centroids);												
				//============================
				out.setValue(i, j, rating);
			}
		}
		
		return out;
		
		
	}

	private Matrix HistogramPervervingRating(Matrix samples,
			Vector<Double> rawForRatingSeparation, int ratingLevel,Vec outSeparators) {
		// TODO Auto-generated method stub
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);				
		/**
		 * sampled matrix for rating
		 */
		//Vector sampleVec=new Vector(sampleSize);
		//int[] indexPermuted;
		//sample each columns, for each node, the same with the distributed mechanism
		
		for(int i=0;i<N;i++){
			
			//find the sampled measurements
		
			//compute  separation points
			double d10prt = math.percentile(rawForRatingSeparation, 0.1);
			double d90prt=math.percentile(rawForRatingSeparation, .9);			
			double step=(d90prt-d10prt)/ratingLevel;
			//rawForRatingSeparation.clear();
			/**
			 * separators
			 */
			if(outSeparators!=null){
			for(int j=0;j<outSeparators.num_dims;j++){
				outSeparators.direction[j]=(j+1)*step;
			}
			}
				
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				
				sample=samples.value(i, j);
				double estimate=0;				
				if(sample<=0){
					estimate=1;
				}else{
					if(step>0){
						estimate=Math.ceil(sample/step);
					}else{
						//step size is 0
						estimate=1;
					}
				}
				if(estimate>ratingLevel){
					estimate=ratingLevel;
				}
				
				out.setValue(i, j, estimate);
			}
		}
		
		return out;
	}

	private Matrix EqualLenRating(Matrix samples,
			Vector rawForRatingSeparation, int ratingLevel,Vec outSeparators) {
		// TODO Auto-generated method stub
		
		
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);				
		/**
		 * sampled matrix for rating
		 */
		//sample each columns, for each node, the same with the distributed mechanism
		double rating=-1;
		double p;
		
		for(int i=0;i<N;i++){
			//find the sampled measurements
			
			//compute  separation points
			double point;
			Vector<Double> percentiles=new Vector<Double>(2);
			for(int indL=0;indL<ratingLevel-1;indL++){
				point=((indL+1)/(ratingLevel+0.0));
				percentiles.add(point);
			}
			Vector<Double> chosenPoint=math.percentile(rawForRatingSeparation, percentiles);
			
			removeRedundantItems(chosenPoint);
			
			if(outSeparators!=null){
				outSeparators.add(chosenPoint);
			}
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				//============================
				sample=samples.value(i, j);
				
				if(sample>=chosenPoint.lastElement()){
					rating=chosenPoint.size();
					if(chosenPoint.size()<outSeparators.num_dims){
						rating++;
					}
				}else{
				/* search over the chosen point */
					for(int indC=0;indC<chosenPoint.size();indC++){
						p=chosenPoint.get(indC);
						if(sample<=p){
							rating=indC+1;
							break;
						}
					}
					if(rating<0){
						rating=0;
					}
				}
								
				//============================
				out.setValue(i, j, rating);
			}
		}
		
		return out;
		
		
		
	}

	/**
	 * compute based on the raw measurement matrix
	 * @param samples
	 * @param ratingType
	 * @param ratingLevel
	 * @return
	 */
	public Matrix compute(Matrix samples,int ratingType,int ratingLevel){
		switch(ratingType){
		case EqualLenRating:{
			return this.EqualLenRating(samples, ratingLevel);			
		}
		case HistogramPervervingRating:{
			return this.HistogramPervervingRating(samples, ratingLevel);
		}
		case ClusteringBasedRating:{
			return this.ClusteringBasedRating(samples, ratingLevel);
		}
		default:{
			System.err.print("Unsupported rating function!\n");
			return null;			
		}
		
		}
	}
	
	
	/**
	 * clustering based rating
	 * @param samples
	 * @param ratingLevel
	 * @return
	 */
	private Matrix ClusteringBasedRating(Matrix samples, int ratingLevel) {
		// TODO Auto-generated method stub
		
		//init K-means clustering
		 KMeansClusterer clusterer
        = new KMeansClusterer(ID_PARSER,ratingLevel,maxEpochs,false,minImprovement);

		 Random random = new Random(); // (747L);
		
		
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);		
		
		
		/**
		 * sampled matrix for rating
		 */
		Vector<Double> sampleVec=new Vector<Double>(sampleSize);
		int[] indexPermuted;
		//sample each columns, for each node, the same with the distributed mechanism
		double rating=-1;
		double p;
		
		for(int i=0;i<N;i++){
			//find the sampled measurements
			indexPermuted=edu.NUDT.PDL.cluster.Statistics.permutation(N);
			for(int indPerm=0;indPerm<sampleSize;indPerm++){
				sampleVec.add(samples.value(i, indexPermuted[indPerm]));
			}
			//compute  separation points
			 Set<double[]> eltSet = config1dCoord(sampleVec);
			  
	  		  
			  Set<Set<double[]>> clustering
	          = clusterer.cluster(eltSet,random,Reporters.stdOut());
			  
			  double[]centroids=new double[ratingLevel];
			  //1d coordinate for each rating level
			  for(int ii=0;ii<ratingLevel;ii++){
				  centroids[ii]=clusterer.centroids_out[ii][0];
			  }
			  //sort the centroids
			  Arrays.sort(centroids);
			  						
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				//============================
				sample=samples.value(i, j);
				 rating=getClusteringIndex(sample,centroids);												
				//============================
				out.setValue(i, j, rating);
			}
		}
		
		return out;
		
	}

	/**
	 * histogram preserving based rating
	 * @param samples
	 * @param ratingLevel
	 * @return
	 */
	private Matrix HistogramPervervingRating(Matrix samples, int ratingLevel) {
		// TODO Auto-generated method stub
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);				
		/**
		 * sampled matrix for rating
		 */
		Vector sampleVec=new Vector(sampleSize);
		int[] indexPermuted;
		//sample each columns, for each node, the same with the distributed mechanism
		
		for(int i=0;i<N;i++){
			//find the sampled measurements
			indexPermuted=edu.NUDT.PDL.cluster.Statistics.permutation(N);
			for(int indPerm=0;indPerm<sampleSize;indPerm++){
				sampleVec.add(samples.value(i, indexPermuted[indPerm]));
			}
			//compute  separation points
			double d10prt = math.percentile(sampleVec, 0.1);
			double d90prt=math.percentile(sampleVec, .9);			
			double step=(d90prt-d10prt)/ratingLevel;
			sampleVec.clear();
			
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				
				sample=samples.value(i, j);
				double estimate=0;				
				if(sample<=0){
					estimate=0;
				}else{
					estimate=Math.ceil(sample/step);
				}
				if(estimate>ratingLevel){
					estimate=ratingLevel;
				}
				
				out.setValue(i, j, estimate);
			}
		}
		
		return out;
	}

	/**
	 * equivalent length
	 * @param samples
	 * @param ratingLevel
	 * @return
	 */
	private Matrix EqualLenRating(Matrix samples, int ratingLevel) {
		// TODO Auto-generated method stub
		
		int N=samples.numRows();
		int M=samples.numColumns();
		Matrix out=new DenseMatrix(N,M,0);				
		/**
		 * sampled matrix for rating
		 */
		Vector sampleVec=new Vector(sampleSize);
		int[] indexPermuted;
		//sample each columns, for each node, the same with the distributed mechanism
		double rating=-1;
		double p;
		
		for(int i=0;i<N;i++){
			//find the sampled measurements
			indexPermuted=edu.NUDT.PDL.cluster.Statistics.permutation(N);
			for(int indPerm=0;indPerm<sampleSize;indPerm++){
				sampleVec.add(samples.value(i, indexPermuted[indPerm]));
			}
			//compute  separation points
			double point;
			Vector<Double> percentiles=new Vector<Double>(2);
			for(int indL=0;indL<ratingLevel-1;indL++){
				point=((indL+1)/(ratingLevel+0.0));
				percentiles.add(point);
			}
			Vector<Double> chosenPoint=math.percentile(sampleVec, percentiles);
			
			removeRedundantItems(chosenPoint);
			
			//compute ratings for each raw measurements
			double sample;
			for(int j=0;j<M;j++){
				//============================
				sample=samples.value(i, j);
				
				if(sample>=chosenPoint.lastElement()){
					rating=chosenPoint.size();
				}else{
				/* search over the chosen point */
					for(int indC=0;indC<chosenPoint.size();indC++){
						p=chosenPoint.get(indC);
						if(sample<=p){
							rating=indC+1;
							break;
						}
					}
					if(rating<0){
						rating=0;
					}
				}
								
				//============================
				out.setValue(i, j, rating);
			}
		}
		
		return out;
		
	}

	/**
	 * equally divided percentage of nodes
	 * @param samples, can not contain negative values
	 * @param ratingLevel
	 * @return
	 */
	public List<Pair<T>> EqualLenRating(List<Pair<T>> samples, int ratingLevel){
		int len=samples.size();
		//min, max,
		Vector dd=new Vector(len);
		for(int i=0;i<len;i++){
			dd.add(samples.get(i).sample);
		}
		if(ratingLevel>len){
			System.err.println("level exceeds sample size!");
			return null;
		}
		double point;
		Vector<Double> percentiles=new Vector<Double>(2);
		for(int i=0;i<ratingLevel-1;i++){
			point=((i+1)/(ratingLevel+0.0));
			percentiles.add(point);
		}
		Vector<Double> chosenPoint=math.percentile(dd, percentiles);
		
		removeRedundantItems(chosenPoint);
		
		
		List<Pair<T>> ratings=new ArrayList<Pair<T>>(2);
		double p;
		double rating=-1;
		
		
		for(int index=0;index<samples.size();index++){
			Pair<T> element = samples.get(index);
			if(element.sample>=chosenPoint.lastElement()){
				rating=ratingLevel;
			}else{
			/* search over the chosen point */
				for(int i=0;i<chosenPoint.size();i++){
					p=chosenPoint.get(i);
					if(element.sample<=p){
						rating=i+1;
						break;
					}
				}
			}
			ratings.add(new Pair<T>(element.from,element.to,rating));
		}
		return ratings;
	}
	
	/**
	 * first compute the histogram, then compute the rating results
	 * 
	 * @param samples, can not contain negative values
	 * @return
	 */
	public List<Pair<T>> HistogramPervervingRating(List<Pair<T>> samples, int ratingLevel){
		int len=samples.size();
		//min, max,
		Vector dd=new Vector(len);
		for(int i=0;i<len;i++){
			dd.add(samples.get(i).sample);
			//System.out.println(samples.get(i).sample);
		}
	
		
		double d10prt = math.percentile(dd, 0.1);
		double d90prt=math.percentile(dd, .9);
		
		double step=(d90prt-d10prt)/ratingLevel;
		
		dd.clear();
		
		//System.out.print("d10prt: "+d10prt+", d90prt: "+d90prt+", step: "+step+", level: "+ratingLevel+"\n");
		
		List<Pair<T>> ratings=new ArrayList<Pair<T>>(2);
		
		Iterator<Pair<T>> ier = samples.iterator();
		while(ier.hasNext()){
			Pair<T> tmp = ier.next();
			double estimate=0;
			if(tmp.sample<=0){
				estimate=1;
			}else{
				estimate=Math.ceil(tmp.sample/step);
			}
			if(estimate>ratingLevel){
				estimate=ratingLevel;
			}
			//System.out.print(estimate);
			ratings.add(new Pair<T>(tmp.from,tmp.to,estimate));
			
		}
		return ratings;
	}
	
	
	/**
	 * construct the clustering based rating 
	 * @param samples, can not contain negative values
	 * @param ratingLevel
	 * @return
	 */
	public List<Pair<T>> ClusteringBasedRating(List<Pair<T>> samples, int ratingLevel){
		
		//init K-means clustering
		 KMeansClusterer clusterer
         = new KMeansClusterer(ID_PARSER,ratingLevel,maxEpochs,false,minImprovement);

		  Random random = new Random(); // (747L);
		  //Hashtable<double[],Pair<T>> tab=new Hashtable<double[],Pair<T>>(2);
		  
		  //tab contains the 1d coordinate to pair
		  Set<double[]> eltSet = config1dCoordUseList( samples);
		  
		  		  
		  Set<Set<double[]>> clustering
          = clusterer.cluster(eltSet,random,Reporters.stdOut());
		  
		  double[]centroids=new double[ratingLevel];
		  //1d
		  for(int i=0;i<ratingLevel;i++){
			  centroids[i]=clusterer.centroids_out[i][0];
		  }
		  //sort the centroids
		  Arrays.sort(centroids);
		  
		  //fore each cluster, we determine the relative position of all nodes, by assigning the clustering index with samples
		  List<Pair<T>> ratings=new ArrayList<Pair<T>>(samples.size());
		  double rating=-1;
		  Iterator<Pair<T>> ier = samples.iterator();
		  while(ier.hasNext()){
			  Pair<T> tmp = ier.next();
			  rating=getClusteringIndex(tmp.sample,centroids);
			  if(rating>0){
				  Pair<T> tp=new Pair<T>(tmp.from,tmp.to,rating);
				  ratings.add(tp);
			  }
			  
		  }
		 
           //print(clustering);
		  clustering.clear();
		  centroids=null;
		  
		  
		  return ratings;

	}
	
	/**
	 * compute rating based on separators
	 * @param sample
	 * @param separators
	 * @param level
	 * @return
	 */
	public static double getRatingFromSeparators(double sample,Vec separators,int level){
		int L=separators.num_dims;
		if(sample<=0){
			return 0;
		}
		//positive
		if(L==level){
			//clustering
			return getClusteringIndex(sample, separators.direction);
		}else{
			double min;
			int outL=-1;
			for(int i=0;i<L;i++){
				//come to the end of the rating
				if(separators.direction[i]<=0){
					outL=i;
					break;
				}
				if(sample<=separators.direction[i]){
					outL=i+1;
					break;
				}				
			}
			if(outL<0){
				outL=(L+1);
			}
			return outL;
		}
	}
	
	public static Matrix computeRating(Matrix host2Landmark,Vec _separators,int level) {
		// TODO Auto-generated method stub
		int landmarks=host2Landmark.numColumns();
		Matrix out=new DenseMatrix(1,landmarks,0);
		double rating=0;
		for(int i=0;i<landmarks;i++){
			 rating=getRatingFromSeparators(host2Landmark.value(0, i), _separators, level);
			out.setValue(0, i, rating );
		}
		return out;
	}
	/**
	 * find the closest centroid, centroids have been sorted in ascending order
	 * @param sample
	 * @param centroids
	 * @return
	 */
    private static double getClusteringIndex(double sample, double[] centroids) {
		// TODO Auto-generated method stub
		int index=-1;
		
		double minD=Double.MAX_VALUE;
		double d;
		for(int i=0;i<centroids.length;i++){
			d=Math.abs(sample-centroids[i]);
			if(d<minD){
				minD=d;
				index=i+1;
			}
		}
		//=================================
		return index;
	}

	private void print(Set<Set<double[]>> clustering) {
		// TODO Auto-generated method stub
		Iterator<Set<double[]>> ier = clustering.iterator();
		int index=0;
		int total=0;
		while(ier.hasNext()){
			Set<double[]> tmp = ier.next();
			System.out.println(index+" th cluster: "+tmp.size());
			total+=tmp.size();
			index++;
		}
		System.out.println("Total: "+total);
	}
	
	/**
	 * config the 1d coordinate based on the measurement
	 * @param samples
	 * @return
	 */
    private Set<double[]> config1dCoordUseList(List<Pair<T>> samples) {
		// TODO Auto-generated method stub
		int len=samples.size();
		 Set<double[]> inputSet = new HashSet<double[]>(len*2);
		//double []d=new double[1];
		
		for(int i=0;i<len;i++){
			double []dd=getArray(samples.get(i).sample);
			//tab.put(dd, samples.get(i));
			inputSet.add(dd);
			
		}
		return inputSet;
	}

    /**
     * transform to 1d coordinates
     * @param samples
     * @return
     */
    private Set<double[]> config1dCoord(Vector samples) {
		// TODO Auto-generated method stub
		int len=samples.size();
		 Set<double[]> inputSet = new HashSet<double[]>(len*2);
		//double []d=new double[1];
		
		for(int i=0;i<len;i++){
			double []dd=getArray((Double) samples.get(i));
			//tab.put(dd, samples.get(i));
			inputSet.add(dd);
			
		}
		return inputSet;
	}
    
    
	private double[] getArray(double sample) {
		// TODO Auto-generated method stub
		double [] d=new double[1];
		d[0]=sample;
		return d;
	}

	static FeatureExtractor<double[]> ID_PARSER
    = new FeatureExtractor<double[]>() {
    public Map<String,? extends Number> features(double[] xs) {
        Map<String,Double> result = new HashMap<String,Double>();
        for (int i = 0; i < xs.length; ++i)
            result.put(Integer.toString(i), Double.valueOf(xs[i]));
        return result;
    }
   };
   int maxEpochs = 200;
   double minImprovement = 0.0001;


   /**
    * remove redundant items
    * @param chosenPoint
    * @return
    */
   	public static void removeRedundantItems(Vector<Double> chosenPoint){
   		Iterator<Double> ier = chosenPoint.iterator();
   		Double previous=Double.MAX_VALUE;
   		while(ier.hasNext()){
   			Double item = ier.next();
   			if(previous.equals(item)){
   				ier.remove();
   			}else{
   				previous=item;
   			}	
   		}
   		//============================
   		
   	}
   
	
	
	//(1) the rating function is computed based on the sample inputs only
	//(2) use the centralized pool to cache the samples, so as to collect enough results, to provide sound
	// statistical results
	//distribution estimation
}
