package edu.NUDT.PDL.BasicDistributedMMMF;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.NUDT.PDL.BiasMatrixApproximation.MyConvergeMMMF_bias;
import edu.NUDT.PDL.BiasMatrixApproximation.Pair;
import edu.NUDT.PDL.RatingFunc.RatingFunction;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.MatrixParser;

public class TestDistributedMMMF {

	/**
	 * index of landamrks and hosts
	 */
	List<Integer> landmarks=new ArrayList<Integer>(2);	
	List<Integer> hosts=new ArrayList<Integer>(2);	
	List<Integer> totalIndex=new ArrayList<Integer>(2);	
	
	
	int []landmarkSize={5, 10,20};
	
	/**
	 * read the measurement file
	 */
	MatrixParser parser;
	
	/**
	 * configuration parameter
	 */
	public static int level=5;
	public static int dim=8;
	public static int ratingType=RatingFunction.EqualLenRating;
	/**
	 * engine
	 */
	DistributedMMMF<Integer> mmmf;
	
	
	public static BufferedWriter logRanking = null;
	
	static String MMMFWriter="MMMFWriter";
	
	/**
	 * index of all nodes
	 */
	//Integer[] totalIndex ;
	boolean useBias=false;
	
	public TestDistributedMMMF(){
		parser=new MatrixParser();
	}
	
	/**
	 * select the landmarks
	 * default: randomized landmarks
	 * TODO: the landmarks are selected based on several rules
	 * 
	 * @param selectionType
	 * @return
	 */
	public void selectLandmarks(int size,int selectionType){
		landmarks= randomizedLandmarkSelection(size,hosts);
		
	}
	
	/**
	 * select landmarks based on randomized methods
	 * @param landmark_size
	 * @return
	 */
	List<Integer> randomizedLandmarkSelection(int landmark_size,List<Integer> hosts){
		
		//clear cached data
		landmarks.clear();
		totalIndex.clear();
		hosts.clear();
		
		
		int len=parser.rows;
				
		int[] indexPermuted=edu.NUDT.PDL.cluster.Statistics.permutation(len);
		//totalIndex = new Integer[ len];		
		//int index=0;
		List<Integer> landmarks=new ArrayList<Integer>(landmark_size);
		/**
		 * landmarks
		 */
		for(int i=0;i<landmark_size;i++){
			landmarks.add(Integer.valueOf(indexPermuted[i]));
			totalIndex.add(Integer.valueOf(indexPermuted[i]));
			//totalIndex[index]=Integer.valueOf(indexPermuted[i]);
			//index++;
		}
		/**
		 * config hosts
		 */
		for(int i=landmark_size;i<len;i++){
			hosts.add(Integer.valueOf(indexPermuted[i]));
			totalIndex.add(Integer.valueOf(indexPermuted[i]));
			//totalIndex[index]=Integer.valueOf(indexPermuted[i]);
			//index++;
		}
		
				
		return landmarks;
	}
	
	
	/**
	 * construct host to landmark measurement pairs
	 * @param host
	 * @return
	 */
	public List<Pair<Integer>> HostToLandmark(int host){
		
		double val;
		List<Pair<Integer>> samples=new ArrayList<Pair<Integer>>(2);
		if(landmarks.isEmpty()){
			System.err.println("landmarks is null!");
			return null;
		}else{
			Iterator<Integer> ier = landmarks.iterator();
			while(ier.hasNext()){
				Integer tmp = ier.next();
				val=parser.get(host, tmp);
				samples.add(new Pair<Integer>(Integer.valueOf(host),tmp,val));
			}
		}
		return samples;
		
	}
	
	/**
	 * construct landmark to host measurement pairs
	 * @param host
	 * @return
	 */
	public List<Pair<Integer>> LandmarkToHost(int host){
		double val;
		List<Pair<Integer>> samples=new ArrayList<Pair<Integer>>(2);
		if(landmarks.isEmpty()){
			return null;
		}else{
			Iterator<Integer> ier = landmarks.iterator();
			while(ier.hasNext()){
				Integer tmp = ier.next();
				val=parser.get(host, tmp);
				samples.add(new Pair<Integer>(tmp,Integer.valueOf(host),val));
			}
		}
		return samples;
	}
	
	/**
	 * find the landmarks to landmarks measurement pairs
	 * @param samples
	 * @return
	 */
	public List<Pair<Integer>> Landmarks2landmarks(){
		double val;
		List<Pair<Integer>> samples=new ArrayList<Pair<Integer>>(2);
		if(landmarks.isEmpty()){
			return null;
		}else{
			int totalLandmarks=landmarks.size();
			int from,to;
			
			for(int i=0;i<totalLandmarks;i++){
				for(int j=0;j<totalLandmarks;j++){
					from=landmarks.get(i);
					to=landmarks.get(j);
					if(from==to){
						continue;
					}else{
						samples.add(new Pair<Integer>(from,to,parser.get(from, to)));			
					}
				}
			}
		}
		return samples;
	}
		
	/**
	 * read the measurement file
	 * @param inputFile
	 * @param format
	 * @param starts0
	 */
	public void readMeasurementFile(String inputFile,int format,int starts0){
		parser.setFormat(format);
		parser.starts0=starts0;		
		File file = new File(inputFile);
		System.out.println(file.getAbsolutePath());
		
		parser.readlatencyMatrix(file);
		
		if(parser.getColums()<=0){
			System.err.println("parse failed!");
			return;
		}
	}
	/**
	 * test the bias based matrix approximation
	 */
	public void test(){
		
		//engine
		mmmf=new DistributedMMMF<Integer>();
		mmmf.level=level;
		mmmf.dim=dim;
		mmmf.ratingType=this.ratingType;
		
		/**
		 * timer 
		 */
		long Timer=1;
		
		for(int indLandmarkSize=0;indLandmarkSize<this.landmarkSize.length;indLandmarkSize++){
			
			
			
			int LMSize=this.landmarkSize[indLandmarkSize];
			
			RatingFunction.sampleSize=LMSize;
			
			int selectionType=1;
			//select landmarks,default randomized
			this.selectLandmarks(LMSize, selectionType);
			
			
			//landmark to landmark
			List<Pair<Integer>> landmarkSamples=this.Landmarks2landmarks();
			
			//System.out.println("landmark size: "+this.landmarks.size()+", measurement pairs: "+ landmarkSamples.size());
			
			
			double [] regvals={ 7.5,5.6,4.2,3.4,3.2,2.4 };		
			double lambda;
			for(int indREGVALs=0; indREGVALs<regvals.length; indREGVALs++){
				lambda=regvals[indREGVALs];
			
			//process the landmarks' rating coordinate vectors
			Matrix ratingLandmark2Landmarks=mmmf.processSamples_Landmarks(Timer, landmarks, landmarkSamples);
			
			//test host
			boolean testHost=true;
			
			if(testHost){
			for(int indHost=0;indHost<this.hosts.size();indHost++){
				//id of the hosts
				Integer host = this.hosts.get(indHost);
				List<Integer> hh=new ArrayList<Integer>(1);
				hh.add(host);
				//System.out.println("Host: "+host);
				List<Pair<Integer>> HostToLandmarks=this.HostToLandmark(host.intValue());
				List<Pair<Integer>> LandmarkToHosts=this.LandmarkToHost(host.intValue());
				mmmf.processRatingSamples_Host(Timer,hh , landmarks,
						HostToLandmarks, LandmarkToHosts,lambda);
								
			}
			}
		
			//RMSE
			//for landmarks only
			//TODO, change the evaluation rule!, since in a decentralized environment, we only see the local rating
			//testExpResults(totalIndex,RatingFunction.getInstance().local_compute(parser.asMatrix(),ratingType , level),level);
			
			}
		}//end of the coordinate computation
		
	}
	
	/**
	 * centralized rating
	 */
	public void testWithCentralizedRating(){
		

		boolean useDirectRatingResult=false;
		
		//engine
		mmmf=new DistributedMMMF<Integer>();
		mmmf.level=level;
		mmmf.dim=dim;
		mmmf.ratingType=this.ratingType;
		
		try {
			logRanking= new BufferedWriter(new FileWriter(new File(
					MMMFWriter +"N_"+parser.rows+"dim_"+dim+ ".log")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
		/**
		 * timer 
		 */
		long Timer=1;
		
		for(int indLandmarkSize=0;indLandmarkSize<this.landmarkSize.length;indLandmarkSize++){
			
			
			
			int LMSize=this.landmarkSize[indLandmarkSize];
			
			RatingFunction.sampleSize=Math.round(parser.rows*0.7f);
			
			int selectionType=1;
			//select landmarks,default randomized
			this.selectLandmarks(LMSize, selectionType);
			
			/**
			 * centralized matrix
			 */
			Matrix centralizedRatingMat ;
			if(!useDirectRatingResult){
				 centralizedRatingMat = RatingFunction.getInstance().compute(parser.asMatrix(),ratingType , level);
			}else{
				 centralizedRatingMat =parser.asMatrix();
			}
			
			
			double [] regvals={ 1.9953, 1.7783, 1.5849,1.4125,1.2589,1.1220,1.0593};		
			double lambda;

				
			for(int indREGVALs=0; indREGVALs<regvals.length; indREGVALs++){
				lambda=regvals[indREGVALs];
				logRanking.append("lambda: "+lambda+"\n");
			//landmark to landmark
			//List<Pair<Integer>> landmarkSamples=this.Landmarks2landmarks();
			
			//process the landmarks' rating coordinate vectors
			Matrix ratingLandmark2Landmarks=mmmf.processSamples_Landmarks(Timer, landmarks, centralizedRatingMat);
			
			//test host
			boolean testHost=true;
			
			if(testHost){
			for(int indHost=0;indHost<this.hosts.size();indHost++){
				//id of the hosts
				Integer host = this.hosts.get(indHost);
				List<Integer> hh=new ArrayList<Integer>(1);
				hh.add(host);
				//System.out.println("Host: "+host);
				//List<Pair<Integer>> HostToLandmarks=this.HostToLandmark(host.intValue());
				//List<Pair<Integer>> LandmarkToHosts=this.LandmarkToHost(host.intValue());

				
				mmmf.processRatingSamples_Host(Timer,hh , landmarks,centralizedRatingMat,lambda);
								
			}
			}
		
			//RMSE
			//for landmarks only
		
				testExpResults(totalIndex,centralizedRatingMat ,level,logRanking);
				logRanking.flush();
		
			

			
			}
		

			
		}//end of the coordinate computation
		
			logRanking.flush();
			logRanking.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * get the real measurement matrix
	 * @param allNodes
	 * @return
	 */
	public Matrix getMat(List<Integer> allNodes){
		int len=allNodes.size();
		
		Matrix mat=new DenseMatrix(len,len,0);
		for(int i=0;i<len;i++){
			for(int j=0;j<len;j++){
				mat.setValue(i, j, parser.get(allNodes.get(i).intValue(), allNodes.get(j).intValue()));
			}
		}
		return mat;
	}
	/**
	 * test the rese result between estimated and real 
	 * @param logRanking2 
	 * @throws IOException 
	 */
	public void testExpResults(List<Integer> allNodes,Matrix ratingMat,int level, BufferedWriter logRanking2) throws IOException{
		
		//System.out.println("total: "+parser.rows+" computed: "+mmmf.ratingCache.size());
		
		int len=mmmf.ratingCache.size();
		int n=allNodes.size();
		
		Matrix U=getU(n);
		Matrix V=getV(n);
		Matrix theta=getTheta(n,level);
		Matrix totalBias;
		Matrix Bias;
		Matrix X=null;
		if(useBias){
			totalBias=new DenseMatrix(len,len,0);
			Bias=getBias(allNodes);
			for(int i=0;i<len;i++){
				 for(int j=i+1;j<len;j++){
					 totalBias.setValue(i, j, Bias.value(i, 0)+Bias.value(j, 0));
					 totalBias.setValue(j, i, Bias.value(i, 0)+Bias.value(j, 0));
				 }
				}		
				X=U.multiply(V).add(totalBias);
		}else{
			X=U.multiply(V);
		}
		
			
		for(int i=0;i<X.numRows();i++){
			X.setValue(i, i, 0);
		}
		
		Matrix y = MyConvergeMMMF_bias.m3fSoftmax(X, theta);
		//==================================
		//compute the rating
		logRanking2.append("RMSE: "+MyConvergeMMMF_bias.RMSE(y, ratingMat )+"\n");
		logRanking2.append("MAE: "+MyConvergeMMMF_bias.mae(y, ratingMat)+"\n");
		logRanking2.append("NMAE: "+MyConvergeMMMF_bias.nmae(y, ratingMat)+"\n");
		
	}
	
	
	public Matrix getU(int n){
		
		int p=mmmf.dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		
		for(int i=0;i<n;i++){
			
			 Integer tmp = Integer.valueOf(i);
			if(!mmmf.ratingCache.containsKey(tmp)){
				System.err.println("U: missing: "+tmp);
				continue;
			}
			Vec uu = mmmf.ratingCache.get(tmp).p;
			
			for(int j=0;j<p;j++){
				mat.setValue(tmp.intValue(),j, uu.direction[j]);
			}
			//indexRow++;
		}
		return mat;		
	}
	public Matrix getV(int n){
	
		int p=mmmf.dim;
		Matrix mat=new DenseMatrix(n,p,0);
		
		for(int id=0;id<n;id++){
			
			 Integer tmp = Integer.valueOf(id);
			if(!mmmf.ratingCache.containsKey(tmp)){
				System.err.println("V: missing: "+tmp);
				continue;
			}
			Vec uu = mmmf.ratingCache.get(tmp).q;
			for(int i=0;i<p;i++){
				mat.setValue(tmp.intValue(), i, uu.direction[i]);
			}
			//indexR++;
		}
		return mat;	
		
	}
	
	public Matrix getTheta(int n,int level){

		int p=level-1;
		Matrix mat=new DenseMatrix(n,p,0);
		
		for(int id=0;id<n;id++){
			
			 Integer tmp = Integer.valueOf(id);
			if(!mmmf.ratingCache.containsKey(tmp)){
				System.err.println("Bias: missing: "+tmp);
				continue;
			}
			
			Vec uu = mmmf.ratingCache.get(tmp).thetaVec;			
			for(int i=0;i<p;i++){
				mat.setValue(tmp.intValue(), i, uu.direction[i]);
			}
				
				//indexR++;
		}
		return mat;	
	}

	public Matrix getBias(List<Integer> landmarks){
		int n=landmarks.size();
		int p=1;
		Matrix mat=new DenseMatrix(n,p,0);
		
		Iterator<Integer> ier = landmarks.iterator();
		//int indexR=0;
		while(ier.hasNext()){
			Integer tmp = ier.next();
			if(!mmmf.ratingCache.containsKey(tmp)){
				System.err.println("Bias: missing: "+tmp);
				continue;
			}
			
			Vec uu = mmmf.ratingCache.get(tmp).Bias;			
				mat.setValue(tmp.intValue(), 0, uu.direction[0]);
				
				//indexR++;
		}
		return mat;	
	}
	
	
	/**
	 * main entry
	 * @param args
	 */
	public static void main(String[] args){
		
		TestDistributedMMMF tester=new TestDistributedMMMF();
		
		String inputFile=args[0];
		int format=Integer.parseInt(args[1]);
		int starts0=Integer.parseInt(args[2]);
		
		System.out.println(inputFile+", format "+format+", start0 "+starts0);
		System.out.println();
		/* read the measurement file*/
		tester.readMeasurementFile(inputFile, format, starts0);
		
		//decentralized rating
		//tester.test();
		
		tester.testWithCentralizedRating();
		
		//int [] ex={200};
		//tester.changeArray(ex);
		//System.out.println(ex[0]);
	}
	
	
	public void changeArray(int[] ex){
		for(int i=0;i<ex.length;i++){
			ex[i]=-1;
		}
	}
	
}
