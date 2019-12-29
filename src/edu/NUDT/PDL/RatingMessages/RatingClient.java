package edu.NUDT.PDL.RatingMessages;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import edu.NUDT.PDL.BasicDistributedMMMF.DistributedMMMF;
import edu.NUDT.PDL.RatingFunc.RatingFunction;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.main.RankingLoader;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.comm.AddressIF;


public class RatingClient<T> {

	private static Log logger=new Log(RatingClient.class);
	boolean DEBUG=false;
	
	final T myAddress;
	
	/**
	 * coordinate vector
	 */
	ratingCoord<T> coord;
	
	/**
	 * rating separators
	 */
	public Vec separators;
	/**
	 * dimension
	 */
	int dim;
	/**
	 * level
	 */
	int level;
	
	/**
	 * performance statistics
	 */
	public EWMAStatistic RatingDriftResult;
	public EWMAStatistic absoluteDifferenceResult;
	public EWMAStatistic NMAEResult;
	public EWMAStatistic RMSEResult;
	public EWMAStatistic coordDrift;
	
	
	
	// keep the list of neighbors around for computing statistics
	
	
	protected final List<RemoteState<T>> neighbors;
	
	/**
	 * coordinates
	 */
	protected final Map<T, ratingCoord<T>> NeighborCoords;
	
	protected final Map<T, RemoteState<T>> rs_map;

	public Map<T, RemoteState<T>> getRs_map() {
		return rs_map;
	}
	
	protected final Set<T> hosts;
	
	
	
	// Note: for larger installations, use e.g. 512
	// Try to minimize our error between up to MAX_NEIGHBORS guys at once
	public static int MAX_NEIGHBORS = 512;

	// Toss remote state of nodes if we haven't heard from them for thirty
	// minutes
	// This allows us to keep around a list of RTTs for the node even if
	// we currently aren't using its coordinate for update
	public static long RS_EXPIRATION = 30 * 60 * 1000;

	final public static long MAINTENANCE_PERIOD = 5* 60 * 1000; // ten minutes

	/**
	 * last maintance timer
	 */
	long lastMaintance=0;
	
	final public static long MAX_PING_RESPONSE_TIME = 10 * 60 * 1000; // ten
	// minutes

	
	final public static double OUTRAGEOUSLY_LARGE_RTT = 20000.0;
	// target max number of remote states kept
	// set to be larger than MAX_NEIGHBORS
	public final static int MAX_RS_MAP_SIZE = 128;

	public final static double alpha_separator=.1;
	public static boolean hasInitialized=false;
	/*
	 * The minimum time to wait before kicking a neighbor off the list if that
	 * neighbor has not been pinged yet.
	 */
	public static long MIN_UPDATE_TIME_TO_PING = 2 * MAINTENANCE_PERIOD;

	/*
	 * The weight to be used in calculating the probability
	 */

	final static protected NumberFormat nf = NumberFormat.getInstance();

	final static protected int NFDigits = 2;

	// to indicate whether is likely to be high-err node
	int tick = 0;

	
	
	static {
		if (nf.getMaximumFractionDigits() > NFDigits) {
			nf.setMaximumFractionDigits(NFDigits);
		}
		if (nf.getMinimumFractionDigits() > NFDigits) {
			nf.setMinimumFractionDigits(NFDigits);
		}
		nf.setGroupingUsed(false);
	}
	
	

	//---------------------------------
	public double SystemError=0;
	
	public boolean canUpdateCoordByGossip=false;
	
	public static double COORD_ERROR = 0.20; // c_e parameter
	//-------------------
	
	 DistributedMMMF<T> mmmf;
	 
	 /**
	  * for localAggregation
	  */
	private int localSeparatorAggrationThreshold=15;
	
	
	private boolean useLocalRankingAggregator= Boolean
	.parseBoolean(Config.getConfigProps().getProperty("useLocalRankingAggregator", "false"));
	
	
	public RatingClient(T _myAddress,int _dim,int _level){
		
		myAddress= _myAddress;
		
		mmmf=new DistributedMMMF<T>(RankingManager.coordDim,RankingManager.RatingLevel,RankingManager.RatingType);
		
		dim=_dim;
		level=_level;
		coord=new ratingCoord<T>(dim,level);
		//rating separators
		if(RankingManager.RatingType!=RatingFunction.ClusteringBasedRating){
			separators=new Vec(level-1);
		}else{
			separators=new Vec(level);
		}
		separators.zeroInit();
		
		RatingDriftResult=new EWMAStatistic();
		NMAEResult=new EWMAStatistic();
		RMSEResult=new EWMAStatistic();
		
		absoluteDifferenceResult=new EWMAStatistic();
		
		neighbors = new ArrayList<RemoteState<T>>();
		NeighborCoords=new ConcurrentHashMap<T, ratingCoord<T>>();
		rs_map = new ConcurrentHashMap<T, RemoteState<T>>();
		hosts = Collections.unmodifiableSet(rs_map.keySet());

	}
	
	/**
	 * process new samples, when we are in the gossip mode
	 * @param addr
	 * @param sample_rtt
	 * @param curr_time
	 * @return
	 */
	synchronized public boolean processSample(T addr, double sample_rtt, ratingCoord<T> _r_coord,double remoteError,long curr_time){
		
		if (sample_rtt > OUTRAGEOUSLY_LARGE_RTT) {
			return false;
		}
		
		
		if(_r_coord==null){
			logger.warn("remote node "+addr+" has not been initialized!");
			return false;
		}
		int id = getIdFromAddr(addr);
		
		if (!this.coord.isCompatible(_r_coord)) {
			if (DEBUG)
				logger.info("INVALID " + id + " s " + sample_rtt
						+ " NOT_COMPAT " + _r_coord.getVersion());
			return false;
		}

		//save the coordinates
		NeighborCoords.put(addr, _r_coord);
				
		RemoteState<T> addr_rs = rs_map.get(addr);
		if (addr_rs == null) {
			addHost(addr);
			addr_rs = rs_map.get(addr);
		}
		//save the coordinate, raw rtt
		addr_rs.addSample(sample_rtt, _r_coord.copy(),curr_time);
		
		//test the rating value drift
		testRatingDrift(addr,sample_rtt);
		
		//TODO: update the coordinate 
		if(this.canUpdateCoordByGossip){
			updateSysCoord(curr_time);
									
			//update weight
			double weight=SystemError/(SystemError+remoteError);
			if(Double.isInfinite(weight)||Double.isNaN(weight)){
				weight=0.5;
			}
			
			updateSystemError(sample_rtt, _r_coord, weight);
			

		}
		
		//TODO: update the neighbor nodes, when they are aged, or can not be contacted, we remove them
		if((lastMaintance-curr_time)>MAINTENANCE_PERIOD){
			updateNeighbor(curr_time);
		}
		return true;
	}
	
	/**
	 * test the drift of the rating
	 * @param addr
	 * @param sample_rtt
	 */
	private void testRatingDrift(T addr, double sample_rtt){
		
		double lastRating=rs_map.get(addr).oldRating;
		
		double newRating=computeRating(sample_rtt);
		
		RatingDriftResult.add(Math.abs(lastRating-newRating));
		
	}
	/**
	 * iterate from the neighbor list, remove timed out neighbors
	 * @param curr_time
	 */
	private void updateNeighbor(long curr_time) {
		// TODO Auto-generated method stub
		Iterator<Entry<T, RemoteState<T>>> ier = rs_map.entrySet().iterator();
		while(ier.hasNext()){
			Entry<T, RemoteState<T>> rec = ier.next();
			T addr=rec.getKey();
			RemoteState<T> val = rec.getValue();
			if((val.getLastPingTime()-curr_time)>RS_EXPIRATION){
				NeighborCoords.remove(addr);
				ier.remove();
			}
		}
		
		lastMaintance=System.currentTimeMillis();
		
	}

	/**
	 * update my rating coordinate
	 * @param curr_time
	 */
	private synchronized void updateSysCoord(long curr_time) {
		// TODO Auto-generated method stub
		if(NeighborCoords.isEmpty()){
			logger.warn("no neighbor yet!");
			return;
		}
						
		//logger.info("updateSysCoord "+curr_time);
		List<T> landmarks=new ArrayList<T>(NeighborCoords.keySet());
		
		int k=NeighborCoords.size();
		//set the rating value
		Matrix host2Landmarks=new DenseMatrix(1,k,0);
		for(int i=0;i<k;i++){
			if(getRs_map().containsKey(landmarks.get(i))){
				host2Landmarks.setValue(0, i,computeRating(getRs_map().get(landmarks.get(i)).getSample()));
			}
		}
		
		Hashtable<T, ratingCoord<T>> copyNeighborCoords=new Hashtable<T, ratingCoord<T>>();
		copyNeighborCoords.putAll(NeighborCoords);
		
		ratingCoord<T> x = mmmf.processRatingSamplesHost(curr_time, myAddress, getSystemCoord(), 
				landmarks,copyNeighborCoords, host2Landmarks);
		//update coordinate
		getSystemCoord().copy(x);
		
		//clear
		landmarks.clear();
		copyNeighborCoords.clear();
		host2Landmarks.clear();
	}

	/**
	 * compute samples
	 * @return
	 */
	public Vector getRawMeasurementsFromNeighbors(){
		Vector out=new Vector();
		Iterator<edu.NUDT.PDL.RatingMessages.RemoteState<T>> ier = this.rs_map.values().iterator();
		while(ier.hasNext()){
			edu.NUDT.PDL.RatingMessages.RemoteState<T> tmp = ier.next();
			out.add(tmp.getSample());						
		}
		
		return out;
	}
	
	
	
	
	
	
	
	
	
	
	// If remote nodes are already represented by ints, just use them
	// otherwise, translate into more easily read-able ID
	protected int getIdFromAddr(T addr) {

		return (0);
	}
	
	
	/**
	 * get the coord
	 * @return
	 */
	public ratingCoord<T> getSystemCoord(){
		return coord;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#addHost(T)
	 */
	synchronized public boolean addHost(T addr) {
		if (rs_map.containsKey(addr)) {
			return false;
		}

		RemoteState<T> rs = new RemoteState<T>(addr);
		rs_map.put(addr, rs);
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#removeHost(T)
	 */

	synchronized public boolean removeHost(T addr) {
		if (rs_map.containsKey(addr)) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#containsHost(T)
	 */
	synchronized public boolean containsHost(T addr) {
		return rs_map.containsKey(addr);
	}
	
	synchronized public Set<T> getHosts() {
		return hosts;
	}

	
	protected boolean addNeighbor(RemoteState<T> guy) {
		boolean added = false;
		if (!neighbors.contains(guy)) {
			neighbors.add(guy);
			added = true;
		}
		if (neighbors.size() > MAX_NEIGHBORS) {
			RemoteState<T> neighbor = neighbors.remove(0);
		}
		return added;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#printNeighbors()
	 */
	public String printNeighbors() {
		String toPrint = "";
		for (Iterator<RemoteState<T>> i = neighbors.iterator(); i.hasNext();) {
			RemoteState<T> A_rs = i.next();
			toPrint = toPrint + "," + A_rs.getAddress();
		}
		return toPrint;
	}

	protected boolean removeNeighbor(RemoteState<T> guy) {
		if (neighbors.contains(guy)) {
			neighbors.remove(guy);
			return true;
		}
		return false;
	}

	/*
	 * Pick a "random" neighbor from the neighbors list to send a ping request
	 * For each neighbor, calculate the weight (probability that it will be sent
	 * a ping) If, on the off chance, that no neighbor was picked, then randomly
	 * pick one.
	 */

	/*
	 * JL Note: Eric used a weight here which might be nicer...
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.harvard.syrah.pyxida.nc.lib.NCClientIF#getNeighborToPing(long)
	 */
	synchronized public T getNeighborToPing(long curr_time) {
		List<RemoteState<T>> grayingNeighbors = new ArrayList<RemoteState<T>>();

		double avgTimeSincePing = 0.;
		int pingedNeighborCount = 0, nonPingedNeighborCount = 0;
		for (RemoteState<T> neighbor : neighbors) {

			if (curr_time - neighbor.getLastPingTime() > MIN_UPDATE_TIME_TO_PING) {
				grayingNeighbors.add(neighbor);
			}

			if (neighbor.getLastPingTime() > 0) {
				avgTimeSincePing += (curr_time - neighbor.getLastPingTime());
				pingedNeighborCount++;
			} else {
				nonPingedNeighborCount++;
			}
		}
		logger.debug("getNeighborToPing: pinged/non " + pingedNeighborCount
				+ "/" + nonPingedNeighborCount + " avgTimeSincePing "
				+ (avgTimeSincePing / pingedNeighborCount) + " minTime "
				+ MIN_UPDATE_TIME_TO_PING);

		if (grayingNeighbors.size() > 0) {
			Collections.shuffle(grayingNeighbors);
			RemoteState<T> neighbor = grayingNeighbors.get(0);
			neighbor.setLastPingTime(curr_time);

			// reduce the likely size of graying neighbors
			// if it is (relatively) too big, but only if it is (absolutely) too
			// big
			if (grayingNeighbors.size() > neighbors.size() / 8
					&& grayingNeighbors.size() > 3) {
				MIN_UPDATE_TIME_TO_PING /= 2;
				if (MIN_UPDATE_TIME_TO_PING <= 1)
					MIN_UPDATE_TIME_TO_PING = 1;
				logger
						.info("getNeighborToPing: lowered MIN_UPDATE_TIME_TO_PING "
								+ MIN_UPDATE_TIME_TO_PING);
			}

			logger.info("getNeighborToPing: picking from grayingNeighbors "
					+ grayingNeighbors.size() + "/" + neighbors.size() + " "
					+ neighbor.getAddress());

			return neighbor.getAddress();
		}

		// we do want to get some nodes in grayingNeighbors
		MIN_UPDATE_TIME_TO_PING *= 2;
		logger
				.info("getNeighborToPing: returning null, increased MIN_UPDATE_TIME_TO_PING "
						+ MIN_UPDATE_TIME_TO_PING);
		return null;
	}

	/**
	 * get a random neighbor
	 * @return
	 */
	public synchronized T getRandomNeighbor(){
		
		if(neighbors.isEmpty()){
			return null;
		}else{
			int index=RankingLoader.random.nextInt(neighbors.size());
			return neighbors.get(index).addr;
		}
		
		
	}

	/**
	 * aggregate local separators with remote separators,
	 * (a+b)/2
	 * @param separators2
	 */
	public void aggregate(Vec separators2) {
		// TODO Auto-generated method stub

		if(separators2==null||separators==null){
			logger.warn("aggregate,separators2 is NULL!");
			return;
		}
		
		if(useLocalRankingAggregator){
		//compute local rating separators
		 Vector ve=getRawMeasurementsFromNeighbors();
		 	if(ve.size()>localSeparatorAggrationThreshold){
		 	Vec separator_new=RankingManager.initSeparators();
		 	//if(useSimpleRatingSeparator){
			//separators=new Vec(RatingLevel-1);
			//use the hosts' separators
		    Matrix host2Landmark=new DenseMatrix(1,1,10);		   
			RatingFunction.getInstance().local_compute(host2Landmark, 
					ve,RankingManager.RatingType, RankingManager.RatingLevel, separator_new);
			movingAverage(separator_new);		
			separator_new.clear();
		 	}
			ve.clear();
		}
		//aggregate the separators
/*		for(int i=0;i<this.separators.num_dims;i++){
			separators.direction[i]=(separators.direction[i]+separators2.direction[i])/2;
		}*/
		//if(hasInitialized){
			movingAverage(separators2);
		//}
		
	}

	/**
	 * moving average, for separators
	 * @param separators2
	 */
	public void movingAverage(Vec separators2) {
		
		 if(separators2==null){
			 logger.warn("movingAverage,separators2 is NULL!");
				return;
		 }
		 
		// TODO Auto-generated method stub
		if(!hasInitialized){
			for(int i=0;i<separators2.num_dims;i++){
				separators.direction[i]=separators2.direction[i];
			}
			hasInitialized=true;
		}else{
		for(int i=0;i<separators.num_dims;i++){
			separators.direction[i]=(1-alpha_separator)*separators.direction[i]+separators2.direction[i]*alpha_separator;
		}
		}
	}

	
	/**
	 * update the system error
	 * @param _r_coord
	 * @param weight
	 */
	void updateSystemError(double sample, ratingCoord<T> _r_coord,double weight){
		
		double ratingReal=computeRating(sample);
		if(ratingReal<=0){
			return;
		}
		
		double estimatedRating=getSystemCoord().distanceTo(_r_coord);
		double diff=Math.abs(ratingReal-estimatedRating);
		if(Double.isInfinite(diff)||Double.isNaN(diff)){
			logger.warn(ratingReal+" "+estimatedRating);
		}else{
			absoluteDifferenceResult.add(diff);
		}
		//absolute error
		double newError=Math.abs(ratingReal-estimatedRating)/ratingReal;
		//invalid parameters
		if(Double.isInfinite(newError)||Double.isNaN(newError)||Double.isNaN(weight)||Double.isInfinite(weight)){
			return;
		}		
		updateSystemError(newError,weight);
	}
	
	/**
	 * update system error
	 * @param newError
	 * @param alpha
	 */
	public void updateSystemError(double newError,double weight){
		
		double alpha = weight*COORD_ERROR;		
		SystemError=( newError)*alpha+(1-alpha)*SystemError;
	}
	
	/**
	 * compute rating based on the separators
	 * @param sample_measurement
	 * @return
	 */
	double computeRating(double sample_measurement){
		if(separators!=null){
			return RatingFunction.getRatingFromSeparators(sample_measurement, separators, level);
		}else{
			return 0;
		}
		
	}
	
	/**
	 * clear a neighbor
	 * @param node
	 */
	public void clearNeighbor(T node){
		
	}

	/**
	 * compute the rating based on the local rating process
	 * @param host2Landmark
	 * @return
	 */
	public Matrix computeRating(Matrix host2Landmark) {
		// TODO Auto-generated method stub
		int landmarks=host2Landmark.numColumns();
		Matrix out=new DenseMatrix(1,landmarks,0);
		for(int i=0;i<landmarks;i++){
			out.setValue(0, i, this.computeRating(host2Landmark.value(0, i)));
		}
		return out;
	}

	/**
	 * save the nodes
	 * @param nps
	 * @param ratingCache
	 */
	public void saveNeighbors(Set<NodesPair> nps,
			Hashtable<T, ratingCoord<T>> ratingCache) {
		// TODO Auto-generated method stub
		
		long curr_time=System.currentTimeMillis();
		
		Iterator<NodesPair> ier = nps.iterator();
		ratingCoord<T> _r_coord;
		double sample_rtt;
		
		while(ier.hasNext()){
			
			NodesPair pair = ier.next();
			T addr = (T)pair.endNode;
			sample_rtt=pair.value;
			
			/**
			 * find the matched rating coordinate node
			 */
			if(ratingCache.containsKey(addr)){
				_r_coord=ratingCache.get(addr);
				//save the coordinates
				NeighborCoords.put(addr, _r_coord);
						
				RemoteState<T> addr_rs = rs_map.get(addr);
				if (addr_rs == null) {
					addHost(addr);
					addr_rs = rs_map.get(addr);
				}
				//save the coordinate, raw rtt
				addr_rs.addSample(sample_rtt, _r_coord.copy(),curr_time);				
				}
	
			}

		}

	/**
	 * get the relative error
	 * @return
	 */
	public double[] getErrorWithNeighbor() {
		// TODO Auto-generated method stub
		double [] error={0,0};
		double RelativeError=0;
		double absoluteError=0;
		int index=0;
		Iterator<Entry<T, ratingCoord<T>>> ier = NeighborCoords.entrySet().iterator();
		while(ier.hasNext()){
			Entry<T, ratingCoord<T>> rec = ier.next();
			T addr=rec.getKey();
			ratingCoord<T> _r_coord=rec.getValue();
			if(rs_map.containsKey(addr)){
				double sample_rtt=rs_map.get(addr).getSample();
				double ratingReal=computeRating(sample_rtt);
				if(ratingReal<=0){
					continue;
				}
				
				double estimatedRating=getSystemCoord().distanceTo(_r_coord);
				double diff=Math.abs(ratingReal-estimatedRating);
				if(Double.isInfinite(diff)||Double.isNaN(diff)){
					continue;
				}
				 absoluteError+=diff;
				RelativeError+=diff/ratingReal;
				index++;
			}
		}
		if(index>0){
			error[0]=RelativeError/index;
			error[1]=absoluteError/index;
			return error;		
		}else{
			return error;
		}
	}
}
