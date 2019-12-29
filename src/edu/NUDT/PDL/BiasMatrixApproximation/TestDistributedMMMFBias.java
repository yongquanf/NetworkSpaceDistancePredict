package edu.NUDT.PDL.BiasMatrixApproximation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.NUDT.PDL.BasicDistributedMMMF.DistributedMMMF;
import edu.NUDT.PDL.RatingFunc.RatingFunction;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.NUDT.PDL.util.matrix.MatrixParser;

public class TestDistributedMMMFBias {

	/**
	 * index of landamrks and hosts
	 */
	List<Integer> landmarks=new ArrayList<Integer>(2);	
	List<Integer> hosts=new ArrayList<Integer>(2);	
	
	int []landmarkSize={10,15, 20,25};
	
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
	DistributedMMMF_bias<Integer> mmmf;
	
	/**
	 * index of all nodes
	 */
	List<Integer> totalIndex=new ArrayList<Integer>(2);	
	
	
	public static BufferedWriter logRanking = null;
	
	static String BiasMMMFWriter="BiasMMMFWriter";
	
	
	
	
	public TestDistributedMMMFBias(){
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
			
		int index=0;
		List<Integer> landmarks=new ArrayList<Integer>(landmark_size);
		/**
		 * landmarks
		 */
		for(int i=0;i<landmark_size;i++){
			landmarks.add(Integer.valueOf(indexPermuted[i]));
			totalIndex.add(Integer.valueOf(indexPermuted[i]));
			index++;
		}
		/**
		 * config hosts
		 */
		for(int i=landmark_size;i<len;i++){
			hosts.add(Integer.valueOf(indexPermuted[i]));
			totalIndex.add(Integer.valueOf(indexPermuted[i]));
			index++;
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
		mmmf=new DistributedMMMF_bias<Integer>();
		mmmf.level=level;
		mmmf.dim=dim;
		mmmf.ratingType=this.ratingType;
		
		
		try {
			logRanking= new BufferedWriter(new FileWriter(new File(
					BiasMMMFWriter +"N_"+parser.rows+"dim_"+dim+ ".log")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			
			System.out.println("landmark size: "+this.landmarks.size()+", measurement pairs: "+ landmarkSamples.size());
			
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
				System.out.println("Host: "+host);
				List<Pair<Integer>> HostToLandmarks=this.HostToLandmark(host.intValue());
				List<Pair<Integer>> LandmarkToHosts=this.LandmarkToHost(host.intValue());
				if(HostToLandmarks==null||LandmarkToHosts==null){
					System.err.println("NULL measurement pairs");
					System.exit(-1);
				}
				mmmf.processRatingSamples_Host(Timer,hh , landmarks,
						HostToLandmarks, LandmarkToHosts);
								
			}
			}
		
			//RMSE
			//for landmarks only
			try {
				testExpResults(totalIndex,RatingFunction.getInstance().compute(parser.asMatrix(),ratingType , level),level);
				logRanking.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}//end of the coordinate computation
		
	}
	
	/**
	 * centralized rating
	 */
	public void testWithCentralizedRating(){
		

		boolean useDirectRatingResult=false;
		
		//engine
		mmmf=new DistributedMMMF_bias<Integer>();
		
		
		//levels
		int[]levels={5,10,15,20};
		//rating funtion
		int[]ratingTypes={RatingFunction.EqualLenRating,RatingFunction.HistogramPervervingRating,
				RatingFunction.ClusteringBasedRating};
		
		for(int indR=0;indR<ratingTypes.length;indR++){
		
			this.ratingType=ratingTypes[indR];
			
		for(int indL=0;indL<levels.length;indL++){
			level=levels[indL];
			mmmf.level=level;
			mmmf.dim=dim;
			mmmf.ratingType=this.ratingType;
			
			try {
				logRanking= new BufferedWriter(new FileWriter(new File(
						BiasMMMFWriter +"N_"+parser.rows+"level_"+level+ ".log")));
				logRanking.append("\n#ratingType: "+ratingType+"\n");
				logRanking.append("\n#dim: "+dim+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/**
			 * timer 
			 */
			long Timer=1;
			
			for(int indLandmarkSize=0;indLandmarkSize<this.landmarkSize.length;indLandmarkSize++){
				
				//clear
				mmmf.ratingCache.clear();
				
				int LMSize=this.landmarkSize[indLandmarkSize];
				
				try {
					logRanking.append("Landmark: "+LMSize+"\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
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
				
				
				//double [] regvals={ 7.5,5.6,4.2,3.4,3.2,2.4};		
				//double lambda;
				int rept=5;
				for(int indREGVALs=0; indREGVALs<rept; indREGVALs++){
					//lambda=regvals[indREGVALs];
				
							
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

					
					mmmf.processRatingSamples_Host(Timer,hh , landmarks,centralizedRatingMat);
									
				}
				}
			
				//RMSE
				//for landmarks only
				try {
					testExpResults(totalIndex,centralizedRatingMat ,level);
					logRanking.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}//end of the coordinate computation
						
			}
			
			try {
				logRanking.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		}
	}
	
	/**
	 * test the rese result between estimated and real 
	 * @throws IOException 
	 */
	public void testExpResults(List<Integer> allNodes,Matrix ratingMat,int level) throws IOException{
		
		//logRanking.append("total: "+parser.rows+" computed: "+mmmf.ratingCache.size()+"\n");
		
		//int len=mmmf.ratingCache.size();
		int n=parser.rows;
		
		Matrix U=getU(n);
		Matrix V=getV(n);
		Matrix theta=getTheta(n,level);
		Matrix Bias=getBias(n);
		
		Matrix totalBias=new DenseMatrix(n,n,0);
		Matrix X=null;
		
			
			for(int i=0;i<n;i++){
				 for(int j=i+1;j<n;j++){
					 totalBias.setValue(i, j, Bias.value(i, 0)+Bias.value(j, 0));
					 totalBias.setValue(j, i, Bias.value(i, 0)+Bias.value(j, 0));
				 }
				}		
		X=(U.multiply(V)).add(totalBias);
	
		
			
		for(int i=0;i<X.numRows();i++){
			X.setValue(i, i, 0);
		}
		
		Matrix y = MyConvergeMMMF_bias.m3fSoftmax(X, theta);
		//==================================
		//compute the rating
		logRanking.append("RMSE: "+MyConvergeMMMF_bias.RMSE(y, ratingMat )+"\n");
		logRanking.append("MAE: "+MyConvergeMMMF_bias.mae(y, ratingMat)+"\n");
		//logRanking.append("RMSE_original: "+MyConvergeMMMF_bias.RMSE(X, ratingMat )+"\n");
		//logRanking.append("MAE_original: "+MyConvergeMMMF_bias.mae(X, ratingMat)+"\n");
		logRanking.append("NMAE: "+MyConvergeMMMF_bias.nmae(y, ratingMat)+"\n\n");
		//logRanking.append("NMAE_original: "+MyConvergeMMMF_bias.nmae(X, ratingMat)+"\n");
	}
	

	private Matrix getBias(int n) {
		// TODO Auto-generated method stub
		int p=1;
		Matrix mat=new DenseMatrix(n,1,0);
		
		
		for(int i=0;i<n;i++){
			
			 Integer tmp = Integer.valueOf(i);
			if(!mmmf.ratingCache.containsKey(tmp)){
				System.err.println("U: missing: "+tmp);
				continue;
			}
			Vec uu = mmmf.ratingCache.get(tmp).Bias;
			
			for(int j=0;j<p;j++){
				mat.setValue(tmp.intValue(),j, uu.direction[j]);
			}
			//indexRow++;
		}
		return mat;
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
	 * main entry
	 * @param args
	 */
	public static void main(String[] args){
		
		TestDistributedMMMFBias tester=new TestDistributedMMMFBias();
		
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
		

	}
	
}
