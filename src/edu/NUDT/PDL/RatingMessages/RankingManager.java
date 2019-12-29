package edu.NUDT.PDL.RatingMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.NUDT.PDL.BasicDistributedMMMF.AbstractDistributedMMMF;
import edu.NUDT.PDL.BasicDistributedMMMF.DistributedMMMF;
import edu.NUDT.PDL.BiasMatrixApproximation.DistributedMMMF_bias;
import edu.NUDT.PDL.RatingFunc.RatingFunction;
import edu.NUDT.PDL.cluster.Statistics;
import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.main.RankingLoader;
import edu.NUDT.PDL.measurement.MeasureManager;
import edu.NUDT.PDL.util.Util;
import edu.NUDT.PDL.util.matrix.DenseMatrix;
import edu.NUDT.PDL.util.matrix.Matrix;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommRRCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

public class RankingManager {
	static final Log log = new Log(RankingManager.class);

	final int LandmarkThresholds = 15;

	static long[] sendStamp;

	/**
	 * communication agent
	 */
	final ObjCommIF comm;
	/**
	 * measurement agent
	 */
	final MeasureManager mProber;
	
	RankingClient localNode=null;
	HostClient hostNode=null;
	
	static AddressIF UpdateSource;

	// Number of dimensions, in version 1, we set the dimension manually,
	// TODO: in Version 2, we extend the adaptive dimension estimation process
	// public static int NC_NUM_DIMS = Integer.parseInt(Config.getProperty(
	// "ClusteringDimension", "2"));

	public static final String[] bootstrapList = Config.getProperty(
			"bootstraplist", "r1d15.pyxida.pdl").split("[\\s]");

	/**
	 * the node which updates the rating coordinates of landmarks
	 */
	public static final String[] UpdateSrc = Config.getProperty("updateSource",
			"").split("[\\s]");

	public static final boolean WATCH_NEIGHBORS = Boolean
			.parseBoolean(Config.getConfigProps().getProperty("watch_neighbors", "false"));

	public static  boolean IS_HEADER=
	Boolean.parseBoolean(Config.getConfigProps().getProperty(
	 "isHead", "false"));

	//public boolean IS_HEADER = false;

	// number of landmarks
	public static int NumberOfLandmarks = Integer.parseInt(Config
			.getConfigProps().getProperty("NumberOfLandmarks", "15"));
	
	//rating level
	public static final int RatingLevel = Integer.parseInt(Config
			.getConfigProps().getProperty("RatingLevel", "5"));
	//rating type
	public static final int RatingType = Integer.parseInt(Config
			.getConfigProps().getProperty("RatingType", "0"));
	//dimension
	public static final int coordDim = Integer.parseInt(Config
			.getConfigProps().getProperty("coordDim", "5"));
	//use bias based MMMF
	public static final boolean useBias=Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useBias","false"));

	
	public static final boolean useLandmark=Boolean.parseBoolean(Config
			.getConfigProps().getProperty("useLandmark","true"));

	
	// cache hosts that are seen so far, time out in t secs, if no cotaction is
	// received from that node
	Set<AddressIF> bootstrapAddrs;
	Set<AddressIF> pendingNeighbors;
	Set<AddressIF> upNeighbors = new HashSet<AddressIF>();
	Set<AddressIF> downNeighbors = new HashSet<AddressIF>();

	// cache the alive landmarks
	public final static Map<Long, HSHRecord> pendingHSHLandmarks = new ConcurrentHashMap<Long, HSHRecord>(
			1);
	public Map<AddressIF, RemoteState<AddressIF>> pendingLatency = new ConcurrentHashMap<AddressIF, RemoteState<AddressIF>>(
			1);
	
	long timeStamp4Clustering;		
	long versionNumber = -1;
	
	/**
	 * Time between gossip messages to coordinate neighbors. Default is 10
	 * seconds.
	 */
	public static final long UPDATE_DELAY = Long.parseLong(
			Config.getProperty("UPDATE_DELAY", "30000"));

	/**
	 * whether we separate the rating values by gossip methods
	 */
	public static final boolean useSimpleRatingSeparator= Boolean
	.parseBoolean(Config.getProperty(
			"useSimpleRatingSeparator", "true"));
	
	
	//update timer
	final CB0 updateTimer;
	
	void registerTimer() {
		double rnd = RankingLoader.random.nextGaussian();// What is the type of
														// RankingLoader?
		long delay = 10 * UPDATE_DELAY + (long) (30000 * rnd);
		log.debug("setting timer to " + delay);

		EL.get().registerTimerCB(delay, updateTimer);
	}
	

	/**
	 * computation engine
	 * @author ericfu
	 *
	 */
	
	 public  static AbstractDistributedMMMF<AddressIF> mmmf;
	 
	 
	// for HSH clustering
	class HSHRecord {
		final long timeStamp;

		List<AddressIF> IndexOfLandmarks;

		// completed the clustering process
		boolean alreadyComputedClustering = false;

		boolean readyForUpdate = false;
		// H & S are belong to core node. Core node sends them to host nodes
		Hashtable<AddressIF,ratingCoord<AddressIF>> ratingCache;
		int UnReceivedNodes;
		long lastUpdateTime;
		// ============================================
		// //for update source node
		HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> pendingLandmarkLatency = new HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>>(
				1);

		// latency matrix
		double[][] DistanceMatrix;
		long version;

		/*
		 * public HSHRecord(double[][] _H, double[][] _S, double[] _Coord, long
		 * _timeStamp){ H=_H; S=_S; Coord=_Coord; timeStamp=_timeStamp;
		 * 
		 * }
		 */
		public HSHRecord(long _timeStamp) {
			timeStamp = _timeStamp;
			IndexOfLandmarks = new ArrayList<AddressIF>(1);
			ratingCache=new Hashtable<AddressIF,ratingCoord<AddressIF>>(5);
		}

		public void addLandmarks(AddressIF landmark) {
			if (!IndexOfLandmarks.contains(landmark)) {
				IndexOfLandmarks.add(landmark);
			}
		}

		void clear() {
			IndexOfLandmarks.clear();
			IndexOfLandmarks = null;
			pendingLandmarkLatency.clear();
			DistanceMatrix = null;

		}
	}
	
	
	
	/**
	 * proximity ranking constructor
	 * @param _comm
	 */
	public RankingManager(ObjCommIF _comm){
		comm=_comm;
		mProber=new MeasureManager();
		
		if(!useBias){
			mmmf=new DistributedMMMF<AddressIF>(coordDim,RatingLevel,RatingType);
		}else{
			mmmf=new DistributedMMMF_bias<AddressIF>(coordDim,RatingLevel,RatingType);
		}
		//update our timer to standard time zone, based on the NTP
		Util.getNEWSTime();
		updateTimer=new CB0(){
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
			Util.getNEWSTime();	
			}
			
		};
		
		tickControl=new CB0(){
			@Override
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
				logControlTraffic();
			}		
		};
	}
	
	public void init(final CB0 cbDone) {
		
		
		registerControlTrafficTimer();
		
		/**
		 * init message types
		 */
		comm.registerMessageCB(ClustCollectRequestMsg.class,
				new CollectHandler());
		comm.registerMessageCB(ClustUpdateRequestMsg.class,
						new updateHandler());
		comm.registerMessageCB(CluDatRequestMsg.class, new DatHandler());
		// When a host node send its request for H & S, core node do the job in
		// ReleaseHandler()
		comm.registerMessageCB(ClustReleaseRequestMsg.class,
				new ReleaseHandler());
		// Core node send its H & S to all landmarks after updating the distance
		// matrix
		comm.registerMessageCB(ClustIssueRequestMsg.class, new IssueHandler());

		comm.registerMessageCB(LandmarkRequestMsg.class,new LandmarkRequestHandler());
		
		comm.registerMessageCB(CoordGossipRequestMsg.class, new GossipHandler());
		//---------------------------------------------------------------------------
		//bootstrap node
		bootstrapAddrs = new HashSet<AddressIF>();
		//pending neighbors
		pendingNeighbors = new HashSet<AddressIF>();

		// =====================================================
		AddressFactory.createResolved(Arrays.asList(UpdateSrc),
				RankingLoader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
					protected void cb(CBResult result,
							Map<String, AddressIF> addrMap) {
						switch (result.state) {
						case OK: {

							for (String node : addrMap.keySet()) {

								AddressIF remoteAddr = addrMap.get(node);
								UpdateSource = remoteAddr;
								log.warn("Update Source Addr='"+ UpdateSource + "'");
								break;
							}
							
							//use the landmark
							if(useLandmark){
								if (RankingLoader.me.equals(UpdateSource)) {
									IS_HEADER = true;
								}
							}
							// ====================================
							if (IS_HEADER) {
								log.warn(" We are ready to register the Update procedure");
								localNode = new RankingClient();
								hostNode = null;
							} else {
								localNode = null;
								// at the beginning, nodesList is empty. So we
								// couldn't make sure which node is host node
								hostNode = new HostClient();
							}
							// ====================================
							// Starts local coordinate timer
							if (localNode != null) {
								localNode.init();
							}
							// if the node is host, start the timer for release
							if (hostNode != null) {

								hostNode.init();
							}
							
							//-------------------------------------------------------------
							AddressFactory.createResolved(Arrays.asList(bootstrapList),
									RankingLoader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
										
										protected void cb(CBResult result,
												Map<String, AddressIF> addrMap) {
											switch (result.state) {
											case OK: {
												for (String remoteNode : addrMap.keySet()) {
													log.debug("remoteNode='" + remoteNode + "'");
													AddressIF remoteAddr = addrMap.get(remoteNode);
													// we keep these around in case we run out of
													// neighbors in the future
													if (!remoteAddr.equals(RankingLoader.me)) {
														bootstrapAddrs.add(remoteAddr);
														addPendingNeighbor(remoteAddr);
													}
												}

												// Starts local coordinate timer
					
												cbDone.callOK();
												break;
											}
											case TIMEOUT:
											case ERROR: {
												log.error("Could not resolve bootstrap list: "
														+ result.what);
												cbDone.callERROR();
												break;
											}
											}
										}
									});
							//------------------------------------------------------------
							break;

						}
						case TIMEOUT:
						case ERROR: {
							log.error("Could not resolve bootstrap list: "
									+ result.what);
							cbDone.call(result);
							break;
						}
						}
					}

				});

	}
	
	// define what to do once the different types of request messages have been
	// received
	abstract class ResponseObjCommCB<T extends ObjMessageIF> extends
			ObjCommCB<T> {
		void sendResponseMessage(final String handler,
				final AddressIF remoteAddr11, final ObjMessage response,
				long requestMsgId, final String errorMessage,
				final CB1<Boolean> cbHandled) {
			if (errorMessage != null) {
				log.warn(handler + " : " + errorMessage);
			}

			comm.sendResponseMessage(response, remoteAddr11, requestMsgId,
					new CB0() {
						protected void cb(CBResult sendResult) {
							switch (sendResult.state) {
							case TIMEOUT:
							case ERROR: {
								log.warn(handler + ": " + sendResult.what);
								return;
							}
							}
						}
					});
			cbHandled.call(CBResult.OK(), true);
		}
	}

	class LandmarkRequestHandler extends ResponseObjCommCB<LandmarkRequestMsg> {

		public void cb(CBResult result, LandmarkRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			log.warn("Landmark RequestHandler");

			//addUpNeighbor(msg.from);
			// response
			int K=2;
			Set<AddressIF> nodes=new HashSet<AddressIF>(K);
			getRandomNodes(nodes, K);
			
			//remove the update node
			if(UpdateSource!=null){
				nodes.remove(UpdateSource);
			}
			
			LandmarkResponseMsg msg1 = new LandmarkResponseMsg(nodes,RankingLoader.me);
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			// log.warn("$ @@ Maybe landmarkRequest from: "+remoteAddr1+", but from: "+msg.from);

			sendResponseMessage("LandmarkRequest", msg.from, msg1, msg
					.getMsgId(), null, cbHandled);

		}
	}

	class CollectHandler extends ResponseObjCommCB<ClustCollectRequestMsg> {

		public void cb(CBResult result, ClustCollectRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			log.warn("in CollectHandler cb");
			// response
			ClustCollectResponseMsg msg1 = new ClustCollectResponseMsg();
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());
			log.warn("$ @@ Maybe CollectRequest from: " + remoteAddr1
					+ ", but from: " + msg.from);

			sendResponseMessage("Collect", msg.from, msg1, msg.getMsgId(),
					null, cbHandled);

		}
	}

	class DatHandler extends ResponseObjCommCB<CluDatRequestMsg> {
		public void cb(CBResult result, CluDatRequestMsg msg33,
				AddressIF remoteAddr, Long ts, CB1<Boolean> cbHandled) {

			log.warn(RankingLoader.me + " has received rtts from "
					+ msg33.from);
			// coreNode.nodesPairSet <- UpdateResponseMsg.latencySet (viz
			// remNode.nodesPairSet);
			Iterator ier = msg33.latencySet.iterator();
			CluDatResponseMsg msg1 = new CluDatResponseMsg(RankingLoader.me);
			sendResponseMessage("Dat", msg33.from, msg1, msg33.getMsgId(),
					null, cbHandled);

			
			//has the index, and do not have computed
			if (pendingHSHLandmarks.containsKey(Long.valueOf(msg33.timeStamp))
					&&!pendingHSHLandmarks.get(Long.valueOf(msg33.timeStamp)).alreadyComputedClustering) {
				// HSHRecord list =
				// pendingHSHLandmarks.get(Long.valueOf(msg33.timeStamp));
				HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatList = pendingHSHLandmarks
						.get(Long.valueOf(msg33.timeStamp)).pendingLandmarkLatency;
				log.warn("\n\n TimeStamp: " + msg33.timeStamp
						+ "\n\n");
				AddressIF srcNode = msg33.from;
				while (ier.hasNext()) {

					NodesPair nxt = (NodesPair) ier.next();

					// log.warn("$: "+nxt.toString());

					if (nxt.value < 0) {
						continue;
					}

					// endNode
					if (!LatList.containsKey(srcNode)) {
						ArrayList<RemoteState<AddressIF>> tmp = new ArrayList<RemoteState<AddressIF>>(
								1);
						RemoteState<AddressIF> state = new RemoteState<AddressIF>((AddressIF)nxt.endNode);
						state.addSample(nxt.value );
						tmp.add(state);
						LatList.put(srcNode, tmp);

					} else {

						ArrayList<RemoteState<AddressIF>> tmp = LatList
								.get(srcNode);
						Iterator<RemoteState<AddressIF>> IER = tmp.iterator();

						// ========================================
						boolean found = false;
						RemoteState<AddressIF> S = null;
						while (IER.hasNext()) {
							RemoteState<AddressIF> state = IER.next();
							if (state.getAddress().equals(nxt.endNode)) {
								found = true;
								S = state;
								break;
							}
						}
						// ========================================
						if (found) {
							S.addSample(nxt.value );
							LatList.put(srcNode, tmp);
						} else {
							S = new RemoteState<AddressIF>((AddressIF)nxt.endNode);
							S.addSample(nxt.value );
							tmp.add(S);
							LatList.put(srcNode, tmp);
						}
					}
				}

				// test if we can start clustering computation
				HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> latRecords = pendingHSHLandmarks
						.get(Long.valueOf(msg33.timeStamp)).pendingLandmarkLatency;
				HSHRecord curLandmarks = pendingHSHLandmarks.get(Long.valueOf(msg33.timeStamp));

				if (latRecords != null
						&& !latRecords.isEmpty()
						&& curLandmarks != null
						&& curLandmarks.IndexOfLandmarks != null
						&& !curLandmarks.IndexOfLandmarks.isEmpty()
						&& latRecords.keySet().size() > 0.8 * curLandmarks.IndexOfLandmarks
								.size()
						&&! curLandmarks.alreadyComputedClustering) {
	
					final long timer=msg33.timeStamp;
					/**
					 * run the computation process
					 */
					RankingLoader.execNina.execute(new Runnable(){
						public void run() {
							// TODO Auto-generated method stub
							if(localNode!=null){
								log.info("localNode updateDistMatrix!");
								
								localNode.updateDistMatrix( timer);
								
							}else{
								log.warn("localNode is null!, when process <CluDatRequestMsg");
							}
						}
						
					});

				}

			}

		}
	}

	class IssueHandler extends ResponseObjCommCB<ClustIssueRequestMsg> {

		public void cb(CBResult result, ClustIssueRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {
			log.warn("in IssueHandler cb");
			//save the coordinate
			ClustIssueResponseMsg msg1 = new ClustIssueResponseMsg(
					RankingLoader.me);
			msg1.setResponse(true);
			msg1.setMsgId(msg.getMsgId());

			//null
			if( hostNode==null){
				log.warn("host Node is null @"+RankingLoader.me.toString());
				sendResponseMessage("Issue", msg.from, msg1, msg.getMsgId(), null,
						cbHandled);
				return;
			}
			
			hostNode.receiveMyCoord1(msg.ratingCache);
			
			// new version of clustering results
			versionNumber = msg.version;
			/*try {
				logHSHClusteringResults();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			log.warn(RankingLoader.me + "has acquired its coordinate");
		
			sendResponseMessage("Issue", msg.from, msg1, msg.getMsgId(), null,
					cbHandled);
		}
	}

	
	// update request message received, and then send corresponding response
	// back
	class updateHandler extends ResponseObjCommCB<ClustUpdateRequestMsg> {
		public void cb(CBResult result, ClustUpdateRequestMsg msg22,
				AddressIF remoteAddr2, Long ts, CB1<Boolean> cbHandled) {
			log.debug("in UpdateHandler cb");

			final AddressIF origin_remoteAddr = msg22.from;
			final long msgID22 = msg22.getMsgId();
			final long timer = msg22.timeStamp;

			// log.warn("$ @@ Maybe UpdateRequest from: "+remoteAddr2+" but is from "+origin_remoteAddr);

			ClustUpdateResponseMsg msg1 = new ClustUpdateResponseMsg(
					RankingLoader.me);
			log.warn("$ @@ Update acked to: " + origin_remoteAddr
					+ " With ID " + msgID22);

			sendResponseMessage("update", origin_remoteAddr, msg1, msgID22,
					null, cbHandled);

			// send collect request message to other landmarks
			if (msg22.landmarks == null || msg22.landmarks.size() == 0) {
				log.warn("$ empty landmarks from  "
						+ origin_remoteAddr.toString());
				return;
			} else {
				
				/**
				 * control traffic
				 */
				addControlTraffic(msg22.landmarks.size());
				
				mProber.collectRTTs(msg22.landmarks, new CB2<Set<NodesPair>, String>() {
					protected void cb(CBResult ncResult, Set<NodesPair> nps,
							String errorString) {
						// send data request message to the core node
						CluDatRequestMsg datPayload = new CluDatRequestMsg(
								RankingLoader.me, nps, timer);
						log.warn("$ @@ Dat is sent to: "
								+ origin_remoteAddr + " With ID " + msgID22);
						addControlTraffic(1);
						comm.sendRequestMessage(datPayload, origin_remoteAddr,
								new ObjCommRRCB<CluDatResponseMsg>() {
									protected void cb(
											CBResult result,
											final CluDatResponseMsg responseMsg22,
											AddressIF node, Long ts) {
										switch (result.state) {
										case OK: {
											log.info("$ ! Dat is ACKed from"
															+ responseMsg22.from);
											break;
										}
										case ERROR:
										case TIMEOUT: {
											String error = origin_remoteAddr
													.toString(false)
													+ "has not received responses from"
													+ origin_remoteAddr
															.toString()
													+ ", as: "
													+ result.toString();
											log.warn(error);
											break;
										}
										}
									}
								});
					}
				});

			}
		}
	}
	
	
	// core node send its H & S to the node which launches release request
	class ReleaseHandler extends ResponseObjCommCB<ClustReleaseRequestMsg> {
		public void cb(CBResult result, ClustReleaseRequestMsg msg,
				AddressIF remoteAddr1, Long ts, CB1<Boolean> cbHandled) {

			// addPendingNeighbor(msg.from);

			// TODO: test the initialization process
			ClustReleaseResponseMsg msg1 = null;

			log.warn("in ReleaseHandler cb");
			
			if(localNode!=null){
			
			if (localNode!=null&&!localNode.hasInitialized) {
				log.warn("pendingHSHLandmarks isEmpty()");
				msg1 = new ClustReleaseResponseMsg(UpdateSource, null, null,
						 -1,null);
				sendResponseMessage("Release", msg.from, msg1, msg.getMsgId(),
						null, cbHandled);
				return;
			}
			// not empty
			long timeStamp = Long.MIN_VALUE;
			Iterator<Long> ierTimer = pendingHSHLandmarks.keySet().iterator();
			while (ierTimer.hasNext()) {
				long tmp = ierTimer.next().longValue();
				if (timeStamp < tmp
						&& pendingHSHLandmarks.get(Long.valueOf(tmp)).alreadyComputedClustering) {
					timeStamp = tmp;
					break;
				}
			}

			HSHRecord record = pendingHSHLandmarks.get(Long.valueOf(timeStamp));

			if (record != null && record.alreadyComputedClustering) {

				log.info("have computed rating vectors");

				msg1 = new ClustReleaseResponseMsg(UpdateSource, record.ratingCache,
						record.IndexOfLandmarks, versionNumber,localNode.outRankingSeparators);
				msg1.setResponse(true);
				msg1.setMsgId(msg.getMsgId());
			} else {
				log.warn("record don't alreadyComputedClustering");
				msg1 = new ClustReleaseResponseMsg(UpdateSource, null,
						null, -1,null);
			}
			
			}else{
				//I am the landmark coordinate
				//send the rating coordinate
				
				if(hostNode.hasInitialized){
				Hashtable<AddressIF,ratingCoord<AddressIF>> myCoord=new 
												Hashtable<AddressIF,ratingCoord<AddressIF>>(1);
				myCoord.put(RankingLoader.me, hostNode.myCoord.getSystemCoord().copy());
				List<AddressIF> _landmarks=new ArrayList<AddressIF>(1);
				_landmarks.add(RankingLoader.me);
				msg1=new ClustReleaseResponseMsg(RankingLoader.me, myCoord, _landmarks, versionNumber,hostNode.myCoord.separators );
				
				msg1.setResponse(true);
				msg1.setMsgId(msg.getMsgId());
				}else{
					log.warn("I have not initialized my coordinate!");
					msg1 = new ClustReleaseResponseMsg(RankingLoader.me, null, null,
							 -1,null);
					sendResponseMessage("Release", msg.from, msg1, msg.getMsgId(),
							null, cbHandled);
					return;
				}
			}
		
		
			sendResponseMessage("Release", msg.from, msg1, msg.getMsgId(),
					null, cbHandled);
		}
	}

	
	class GossipHandler extends ResponseObjCommCB<CoordGossipRequestMsg> {

		public void cb(CBResult result, CoordGossipRequestMsg msg,
				AddressIF remoteAddr, Long ts, final CB1<Boolean> cbHandled) {
			log.debug("in GossipHandler cb");
			// we just heard from him so we know he is up
			
			if(hostNode==null){
				log.warn("GossipHandler, host node is null@ "+RankingLoader.me.toString());
				sendResponseMessage("Gossip", msg.from, new CoordGossipResponseMsg(
						null, null,
						RankingLoader.me,null,-1), msg.getMsgId(), null, cbHandled);
				return;
			}
			
			if(!msg.from.equals(UpdateSource)){
				addUpNeighbor(msg.from);
				addPendingNeighbors(msg.nodes);
			}
			
			if(hostNode.myCoord.containsHost(msg.from)){
				hostNode.myCoord.getRs_map().get(msg.from).last_coords=msg.Coord.copy();
			}
						
			long curr_time = System.currentTimeMillis();
			
			ratingCoord<AddressIF> coord=null;
			if(useLandmark){
			if(hostNode.hasInitialized){
				coord=hostNode.myCoord.getSystemCoord();
			}
			}else{
				//directly use the coordinate
				coord=hostNode.myCoord.getSystemCoord();
			}
			sendResponseMessage("Gossip", msg.from, new CoordGossipResponseMsg(
					coord, getUpNeighbors(),
					RankingLoader.me,hostNode.myCoord.separators,hostNode.myCoord.SystemError), msg.getMsgId(), null, cbHandled);

		}
	}
	
	
	public long lastUpdate = 0;

	AddressIF getUpNeighbor() {
		if (upNeighbors.size() == 0 && pendingNeighbors.size() == 0) {
			log.warn("we are lonely and have no one to gossip with");
			return null;
		}

		final double pctUsePendingNeighbor = 0.1;

		AddressIF upNeighbor;
		if (upNeighbors.size() == 0
				|| (pendingNeighbors.size() > 0 && RankingLoader.random
						.nextDouble() < pctUsePendingNeighbor)) {
			upNeighbor = PUtil.getRandomObject(pendingNeighbors);
			log.debug("getUpNeighbor using pending: " + upNeighbor);
		} else {
			upNeighbor = PUtil.getRandomObject(upNeighbors);
			log.debug("getUpNeighbor using up: " + upNeighbor);
		}

		return upNeighbor;
	}

	public Set<AddressIF> getUpNeighbors() {
		Set<AddressIF> nodes = new HashSet<AddressIF>();
		// LOWTODO add option of loop here
		AddressIF node = getUpNeighbor();
		if (node != null) {
			nodes.add(node);
		}
		return nodes;
	}

	public void addPendingNeighbors(Set<AddressIF> nodes) {
		for (AddressIF node : nodes) {
			addPendingNeighbor(node);
		}
	}

	// If this guy is in an unknown state add him to pending.
	public void addPendingNeighbor(AddressIF node) {
		assert node != null : "Pending neighbour is null?";

		if (node.equals(comm.getLocalAddress())||node.equals(RankingLoader.me))
			return;
		if (!pendingNeighbors.contains(node) && !upNeighbors.contains(node)
				&& !downNeighbors.contains(node)) {
			pendingNeighbors.add(node);
			log.debug("addPendingNeighbor: " + node);
		} else {
			log.debug("!addPendingNeighbor: " + node);
		}
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	public void addUpNeighbor(AddressIF node) {
		if (node.equals(comm.getLocalAddress()))
			return;
		downNeighbors.remove(node);
		pendingNeighbors.remove(node);
		upNeighbors.add(node);
		log.debug("addUpNeighbor: " + node);
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	public void addDownNeighbor(AddressIF node) {
		if (node.equals(comm.getLocalAddress()))
			return;
		pendingNeighbors.remove(node);
		upNeighbors.remove(node);
		downNeighbors.add(node);
		log.debug("addDownNeighbor: " + node);
		if (WATCH_NEIGHBORS)
			dumpNeighbors();
	}

	void dumpNeighbors() {
		log.debug(listNeighbors());
	}

	String listNeighbors() {
		StringBuffer sb = new StringBuffer();
		sb.append("pending:");
		for (AddressIF node : pendingNeighbors) {
			sb.append(" " + node);
		}
		sb.append(" up:");
		for (AddressIF node : upNeighbors) {
			sb.append(" " + node);
		}
		sb.append(" down:");
		for (AddressIF node : downNeighbors) {
			sb.append(" " + node);
		}
		return new String(sb);
	}

	String summarizeNeighbors() {
		StringBuffer sb = new StringBuffer();
		return new String("p= " + pendingNeighbors.size() + " u= "
				+ upNeighbors.size() + " d= " + downNeighbors.size());
	}

	public Set<AddressIF> getPendingNeighbours() {
		return pendingNeighbors;
	}

	public Set<AddressIF> getUpNeighbours() {
		return upNeighbors;
	}

	public Set<AddressIF> getDownNeighbours() {
		return downNeighbors;
	}

	/**
	 * get k random nodes from the list
	 * @param cache
	 * @param K
	 */
	public void getRandomNodes(Set<AddressIF> cache,int K){
		List<AddressIF> all=new ArrayList(getUpNeighbours());
		all.addAll(getPendingNeighbours());
		int N=all.size();
		int[] perms = Statistics.permutation(N);
		//double the size of the returned random nodes
		int realSize=(N>2*K)?2*K:N;
		for(int i=0;i<realSize;i++){
			cache.add(all.get(perms[i]));
		}
		
		perms=null;
		all.clear();
		all=null;
	}
	
	
	
	
	AddressIF pickGossipNode() {
		AddressIF neighbor;
		// ask our ncClient if it has a preferred gossip node
			// if not, use somebody from our neighbor set
			neighbor = getUpNeighbor();
			if (neighbor == null) {
				resetBootstrapNeighbors();
				neighbor = getUpNeighbor();
			}
		
		// assert (neighbor != null) :
		// "Could not find a bootstrap neighbour";
		int count=3;
		int i=0;
		while(neighbor!=null&&neighbor.equals(RankingLoader.me)){
		   if(i>3){
			   neighbor=null;
			   break;
		   }else{
			   i++;
			   neighbor = getUpNeighbor();
		   }
		   }
		return neighbor;
	}
	
	void resetBootstrapNeighbors() {
		for (AddressIF remoteAddr : bootstrapAddrs) {
			
				upNeighbors.remove(remoteAddr);
				downNeighbors.remove(remoteAddr);
				pendingNeighbors.add(remoteAddr);
			
		}
	}
	
	
	
	/**
	 * centralized ranking agent
	 * @author ericfu
	 *
	 */
	class RankingClient{
		
		final CB0 updateLandmarks;
		Vec outRankingSeparators=null;
		
		//have initialized coordinate
		boolean hasInitialized=false;
		
		
		public RankingClient(){
			
			// update landmarks
			updateLandmarks = new CB0() {

				@Override
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					updateLandmarks();
				}
			};
		}
		
		
		
		void registerUpdateLandmarksTimer() {
			double rnd = RankingLoader.random.nextGaussian();
			long delay = 5 * UPDATE_DELAY + (long) (5 * UPDATE_DELAY * rnd);
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, updateLandmarks);
		}
		
		/**
		 * init the centralized ranking process
		 */
		public void init(){
			registerUpdateLandmarksTimer();	
		}
		
		//lock the landmark update process
		private final Lock updateLandmarks_lock = new ReentrantLock();
		
		/**
		 * we update the set of landmarks, after each time epoch
		 */
		void updateLandmarks() {

			
			if(hasInitialized){
				log.warn("the source has initialized the coordinate, break!");
				return;
			}
			
			//if we have computed a set of landmarks, and several nodes that have computed their coordinates,
			//when we recompute, we need to find a set of nodes, that have initialized their coordinates
			
			
			registerUpdateLandmarksTimer();

			int m = 2;
			final int expectedLandmarks = NumberOfLandmarks;
			Iterator<Long> ier = pendingHSHLandmarks.keySet().iterator();

			Set<AddressIF> tmpList = new HashSet<AddressIF>(1);
			Long curRecord = null;

			// get a non-empty list of landmarks
			while (ier.hasNext()) {
				Long nxt = ier.next();
				if (pendingHSHLandmarks.get(nxt).IndexOfLandmarks != null
						&& pendingHSHLandmarks.get(nxt).IndexOfLandmarks.size() > 0) {
					tmpList.addAll(pendingHSHLandmarks.get(nxt).IndexOfLandmarks);
					curRecord = nxt;
					break;
				}else{
					//remove old cache
					ier.remove();
				}
			}

			// if empty, we need to update landmarks immediately
			if (tmpList.size() == 0) {
				// remove the record
				if (curRecord != null) {
					pendingHSHLandmarks.get(curRecord).clear();
					pendingHSHLandmarks.remove(curRecord);
				} else {
					// null curRecord
				}

			} else {
				final Long Timer = curRecord;
				// ping landmarks, if several of them fails, p percentage p=0.2
				// , we remove the records, and restart the landmark process
				// =================================================================
				final List<AddressIF> aliveLandmarks = new ArrayList<AddressIF>(
						1);

				/**
				 * control traffic
				 */
				addControlTraffic(tmpList.size());
				
				//test nodes whether they are online
				mProber.collectRTTs1(tmpList, new CB2<Set<NodesPair>, String>() {
					protected void cb(CBResult ncResult, Set<NodesPair> nps,
							String errorString) {
						// send data request message to the core node
						long timer=System.currentTimeMillis();
						
						if (nps != null && nps.size() > 0) {
							log.debug("\n==================\n Alive No. of landmarks: "
											+ nps.size()
											+ "\n==================\n");

							Iterator<NodesPair> NP = nps.iterator();
							while (NP.hasNext()) {
								NodesPair tmp = NP.next();
								if (tmp != null && tmp.value >= 0) {

									AddressIF peer = (AddressIF)tmp.endNode;
									
									//====================================================
								/*	if (!pendingLatency.containsKey(peer)) {
										pendingLatency.put(peer,
												new RemoteState<AddressIF>(peer));
									}
									pendingLatency.get(peer).addSample(tmp.value, timer);*/
									//=====================================================
									
									if (!pendingHSHLandmarks.containsKey(Timer)
											|| pendingHSHLandmarks.get(Timer).IndexOfLandmarks == null) {
										continue;
									}

									int index = pendingHSHLandmarks.get(Timer).IndexOfLandmarks
											.indexOf(peer);

									if (index < 0) {

										continue;
									} else {

										// found the element, and it is smaller
										// than
										// rank, i.e., it is closer to the
										// target
										aliveLandmarks.add(peer);

										continue;
									}
								} else {
									// wrong measurements
								}
							}
						} else {
							// empty
							// all nodes fail, so there are no alive nodes
							if (pendingHSHLandmarks.containsKey(Timer)) {
								pendingHSHLandmarks.get(Timer).clear();
								pendingHSHLandmarks.remove(Timer);
							}
						}
						// some landmarks are offline, we clear records and
						// start
						if (pendingHSHLandmarks.containsKey(Timer)) {

							if (aliveLandmarks.size() < 0.8 * expectedLandmarks) {
								pendingHSHLandmarks.get(Timer).clear();
								pendingHSHLandmarks.remove(Timer);

							} else {
								// the landmarks are healthy, so we can sleep
								// awhile
								// TODO: remove dead landmarks, and resize the
								// landmarks
								pendingHSHLandmarks.get(Timer).IndexOfLandmarks
										.clear();
								pendingHSHLandmarks.get(Timer).IndexOfLandmarks
										.addAll(aliveLandmarks);

								// pendingHSHLandmarks.get(Timer).readyForUpdate=false;
								final Set<AddressIF> nodes = new HashSet<AddressIF>(
										1);

								nodes.addAll(pendingHSHLandmarks.get(Timer).IndexOfLandmarks);
								pendingHSHLandmarks.get(Timer).readyForUpdate = true;

								//update the rtts
								 updateRTTs(Timer.longValue(),nodes, new
								  CB1<String>(){ protected void cb(CBResult
								 ncResult, String errorString){ switch
								 (ncResult.state) { case OK: {
								 log.warn("$: Update completed");
								
								 if(errorString.length()<=0){
								 pendingHSHLandmarks.get(Timer).readyForUpdate=true; }
								 
								 break; } case ERROR: case TIMEOUT: { break; }
								 } nodes.clear(); } });
								 

								return;
							}
						}
					}
				});

			}

			// ==================================================================

			// expected landmarks, K+m

			final Set<AddressIF> pendingNodes = new HashSet<AddressIF>(1);

			getRandomNodes(pendingNodes, expectedLandmarks);

			// remove myself
			if (pendingNodes.contains(RankingLoader.me)) {
				pendingNodes.remove(RankingLoader.me);
			}

			log.warn("$: HSH: Total number of landmarks are: "
					+ pendingNodes.size());

			if (pendingNodes.size() == 0) {
				String errorString = "$: HSH no valid nodes";
				log.warn(errorString);
				return;
			}
			Barrier barrierUpdate = new Barrier(true);
			final StringBuffer errorBuffer = new StringBuffer();

			final long TimeStamp = System.currentTimeMillis();

			for (AddressIF addr : pendingNodes) {
				seekLandmarks(TimeStamp, addr, barrierUpdate,
						pendingHSHLandmarks, errorBuffer);
			}

			EL.get().registerTimerCB(barrierUpdate, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					if (errorBuffer.length() == 0) {
						errorString = new String("Success");
					} else {
						errorString = new String(errorBuffer);
					}

					// finish the landmark seeking process

					if (!pendingHSHLandmarks.containsKey(Long
							.valueOf(TimeStamp))
							|| pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks == null) {
						log.warn("$: NULL elements! ");
						return;
					}
					if (pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks
							.size() < (0.7 * expectedLandmarks)) {
						pendingHSHLandmarks.get(Long.valueOf(TimeStamp))
								.clear();
						pendingHSHLandmarks.remove(Long.valueOf(TimeStamp));
						log.warn("$: not enough landmark nodes");
					} else {

						//
						pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).readyForUpdate = true;
						log.warn("$: enough landmark nodes");
						
						final Set<AddressIF> nodes = new HashSet<AddressIF>(1);
						
						////select only the real size
						//int realNSize=(pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks).size();
						//int selectedSize=realNSize>expectedLandmarks?expectedLandmarks:realNSize;
						nodes.addAll((pendingHSHLandmarks.get(Long.valueOf(TimeStamp)).IndexOfLandmarks));
						
						updateRTTs(Long.valueOf(TimeStamp), nodes, new CB1<String>() {
							protected void cb(CBResult ncResult, String errorString) {
								switch (ncResult.state) {
								case OK: {
									log.warn("$: Update completed");
								}
								case ERROR:
								case TIMEOUT: {
									break;
								}
								}
								nodes.clear();
							}
						});
					}

				}
			});
		}

		
		/**
		 * seek landmarks from pending neighbors
		 * 
		 * @param timeStamp
		 * @param addr
		 * @param barrier
		 * @param pendingHSHLandmarks
		 * @param errorBuffer
		 */
		private void seekLandmarks(final long timeStamp, final AddressIF addr,
				final Barrier barrier,
				final Map<Long, HSHRecord> pendingHSHLandmarks,
				StringBuffer errorBuffer) {
			// TODO Auto-generated method stub
			if (RankingLoader.me.equals(addr)) {
				
				return;
			}

			if(pendingHSHLandmarks.containsKey(Long.valueOf(timeStamp))
			&&pendingHSHLandmarks.get(Long.valueOf(timeStamp)).pendingLandmarkLatency!=null
			&&pendingHSHLandmarks.get(Long.valueOf(timeStamp)).pendingLandmarkLatency.containsKey(addr)){				
				log.info("the landmark has echoed!");
				return;
			}
			/**
			 * control traffic
			 */
			addControlTraffic(1);
			
			LandmarkRequestMsg msg = new LandmarkRequestMsg(RankingLoader.me,
					timeStamp);
			barrier.fork();
			comm.sendRequestMessage(msg, addr,
					new ObjCommRRCB<LandmarkResponseMsg>() {
						protected void cb(CBResult result,
								final LandmarkResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// log.warn("$Landmark Request is acked! We have received ACK from"+addr);
								if(responseMsg.nodes!=null&&!responseMsg.nodes.isEmpty()){
									addPendingNeighbors(responseMsg.nodes);
								}
								// we override previous landmarks
								if (!pendingHSHLandmarks.containsKey(Long
										.valueOf(timeStamp))) {
									pendingHSHLandmarks.put(Long
											.valueOf(timeStamp), new HSHRecord(
											Long.valueOf(timeStamp)));
								}
								pendingHSHLandmarks
										.get(Long.valueOf(timeStamp))
										.addLandmarks(addr);
								pendingHSHLandmarks
										.get(Long.valueOf(timeStamp)).alreadyComputedClustering = false;
								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = addr.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								log.warn(error);

								// remove pending neighbor
								// addDownNeighbor(addr);
								break;
							}
							}
							barrier.join();
						}
					});

		}
		
		
		void updateRTTs(final long timeStamp,
				final Set<AddressIF> LandmarkList, final CB1<String> cbCoords) {

			//registerUpdateTimer();

			if (LandmarkList.size() == 0) {
				String errorString = "getRemoteCoords: no valid nodes";
				log.warn(errorString);
				cbCoords.call(CBResult.OK(), errorString);
				return;
			}
			
			//remove myself
			if(LandmarkList!=null&&LandmarkList.contains(RankingLoader.me)){
				LandmarkList.remove(RankingLoader.me);			
			}
			
			//set up the barrier
			Barrier barrierUpdate = new Barrier(true);
			barrierUpdate.setNumForks(LandmarkList.size());
			
			
			final StringBuffer errorBuffer = new StringBuffer();

			for (AddressIF addr : LandmarkList) {
				updateRTT(timeStamp, addr, LandmarkList, barrierUpdate,
						errorBuffer);
			}

			EL.get().registerTimerCB(barrierUpdate, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					if (errorBuffer.length() == 0) {
						errorString = new String("Success");
					} else {
						errorString = new String(errorBuffer);
					}

					cbCoords.call(CBResult.OK(), errorString);
				}
			});
		}

		// send the update request
		void updateRTT(long timeStamp, final AddressIF remNode,
				Set<AddressIF> landmarkList, final Barrier barrier,
				final StringBuffer errorBuffer) {

			ClustUpdateRequestMsg msg = new ClustUpdateRequestMsg(
					RankingLoader.me, landmarkList, timeStamp);
			// log.warn("ask " + remNode + " to update rtt");

			//barrier.fork();
			/**
			 * control traffic
			 */
			addControlTraffic(1);
			
			comm.sendRequestMessage(msg, remNode,
					new ObjCommRRCB<ClustUpdateResponseMsg>() {
						protected void cb(CBResult result,
								final ClustUpdateResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								log.warn("$ ! We have received ACK from"
												+ responseMsg.from);
								break;
							}
							case ERROR:
							case TIMEOUT: {
								String error = remNode.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								log.warn(error);
								errorBuffer.append(error);
								break;
							}
							}
							barrier.join();
						}
					});
		}

		/**
		 * use lock to synchronize
		 * @param timeStamp
		 */
		void updateDistMatrix(long timeStamp){
			
			updateLandmarks_lock.lock();
			try{
				updateDistMatrix1(timeStamp);
			}finally{
				updateLandmarks_lock.unlock();
			}
			
		}
		// latency information from all other nodes has been received by core
		// node and recorded in its Set<NodesPair>
		// TODO: update latency
		void updateDistMatrix1(long timeStamp) {

			// for each alive landmark, when its ping results are received by
			// the source node,
			// we start the clustering computation process
			// TODO: remove landmarks that return a subset of results 10-12
			Long timer = Long.valueOf(timeStamp);

			if (!pendingHSHLandmarks.containsKey(timer)
					|| pendingHSHLandmarks.get(timer).IndexOfLandmarks == null) {
				log.warn("$: Null elements!");
				return;
			}

			HSHRecord CurList = pendingHSHLandmarks.get(timer);
			//int NoOfLandmarks = CurList.IndexOfLandmarks.size();
			/*if(NoOfLandmarks<=0){
				log.warn("$: Empty elements!");
				return;
			}*/

			HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatRecords = CurList.pendingLandmarkLatency;
			if (LatRecords == null) {
				log.warn("$: Null HashMap<AddressIF, ArrayList<RemoteState<AddressIF>>> LatRecords!");
				return;
			}
			
			List<AddressIF> receivedLandmarkList = new ArrayList<AddressIF>(LatRecords.keySet());
			
			
			int receivedLandmarks =CurList.IndexOfLandmarks.size();
			int NoOfLandmarks= receivedLandmarkList.size();
			// empty or incomplete records
			if (LatRecords.isEmpty() || receivedLandmarks < 0.8 * NoOfLandmarks) {
				log.warn("$: Null LatRecords!");
				return;
			}

			log.warn("$: HSH: total received landmarks: " + NoOfLandmarks
					+ ", total: " + receivedLandmarks);
			
			
			// use the IndexOfLandmarks as the index of nodes

			if (CurList.DistanceMatrix == null
					|| CurList.DistanceMatrix.length != NoOfLandmarks) {
				CurList.DistanceMatrix = null;
				CurList.DistanceMatrix = new double[NoOfLandmarks][NoOfLandmarks];
				for (int i = 0; i < NoOfLandmarks; i++) {
					for (int j = 0; j < NoOfLandmarks; j++) {
						CurList.DistanceMatrix[i][j] = 0;
					}
				}
			}

			// fill elements
			Iterator<Entry<AddressIF, ArrayList<RemoteState<AddressIF>>>> entrySet = LatRecords
					.entrySet().iterator();
			
			//record the sampled data
			//Vector<Double> sampledData=new Vector<Double>(2);
			
			while (entrySet.hasNext()) {
				Entry<AddressIF, ArrayList<RemoteState<AddressIF>>> tmp = entrySet
						.next();
				// empty lists
				if (tmp.getValue() == null || tmp.getValue().size() == 0) {
					log.warn("empty records!");
					continue;
				}
				// ===============================================================
				AddressIF srcNode = tmp.getKey();
				// find the index of landmarks
				//int from = CurList.IndexOfLandmarks.indexOf(srcNode);
				int from = receivedLandmarkList.indexOf(srcNode);
				
				Iterator<RemoteState<AddressIF>> ier = tmp.getValue()
						.iterator();
				if (from < 0) {
					// already removed landmarks!
					log.warn("already removed landmarks!");
					continue;
				}
				
				while (ier.hasNext()) {
					RemoteState<AddressIF> NP = ier.next();

					if (!receivedLandmarkList.contains(NP.getAddress())) {
						log.warn(" not landmarks in received list");
						continue;
					}
					
					//int to = CurList.IndexOfLandmarks.indexOf(NP.getAddress());
					int to = receivedLandmarkList.indexOf(NP.getAddress());
					
					double rtt = NP.getSample();
					if (to < 0 || rtt <= 0) {
						log.warn("$: failed to find the landmarks!");
						continue;
					} else {
						log.info(" <"+to+", "+from+", "+rtt+"> ");
						// Note: symmetric RTT
			/*			if (CurList.DistanceMatrix[to][from] > 0) {
							double avgRTT;
							if (rtt > 0) {
								avgRTT = (rtt + CurList.DistanceMatrix[to][from]) / 2;

							} else {
								avgRTT = CurList.DistanceMatrix[to][from];
							}
							CurList.DistanceMatrix[from][to] = avgRTT;
							CurList.DistanceMatrix[to][from] = avgRTT;
							
							//sampled data
							sampledData.add(avgRTT);
							
						} else {*/
							// TODO: missing elements
						if(rtt>0){
							CurList.DistanceMatrix[from][to] = rtt;
							if(	CurList.DistanceMatrix[to][from]<=0){
								CurList.DistanceMatrix[to][from]=CurList.DistanceMatrix[from][to];
							}
						}

					}
				}
			}

			// ======================================================

			boolean markZero = false;
			for (int i = 0; i < NoOfLandmarks; i++) {
				int sum = 0;
				for (int column = 0; column < NoOfLandmarks; column++) {
					if (i == column) {
						continue;
					}
					if (CurList.DistanceMatrix[i][column] > 0) {
						sum++;
					}
				}

				if (sum < 0.1 * NoOfLandmarks) {
					markZero = true;
					break;
				}
			}
			if (markZero) {
				// CurList.DistanceMatrix=null;
				log.warn("$: incomplete CurList.DistanceMatrix!");
				return;
			}

			log.info("\n\n$: HSH: Start HSH clustering computation process!");
			Matrix measuredValus;
			Matrix outRating=null;
			//if(useSimpleRatingSeparator){
				measuredValus= new DenseMatrix(CurList.DistanceMatrix);	
				
				log.info("measuredValus Matrix: "+measuredValus.toString());
				Vec separators=initSeparators();
				
				Vector<Double> sampledData = getPositiveValues(CurList.DistanceMatrix);
				
				//compute rating
				outRating=RatingFunction.getInstance().local_compute(measuredValus,sampledData, RatingType, RatingLevel,separators);
				
				log.info("ratingMatrix: "+outRating.toString());
				//clear
				sampledData.clear();
				
				if(localNode!=null){
					localNode.outRankingSeparators=new Vec(separators);
				}
	/*			
					if(hostNode!=null){
					separators=new Vec(RatingLevel-1);
					hostNode.myCoord.movingAverage(separators);
				}*/
			
			Hashtable<AddressIF, ratingCoord<AddressIF>> outRatingCoords = mmmf.processSamplesLandmarks_(timer, 
					receivedLandmarkList,outRating);
			
			print(outRatingCoords);
			/**
			 * already compute
			 */
			CurList.alreadyComputedClustering=true;
			/**
			 * if possible, replace existed computed results
			 */
			CurList.ratingCache.putAll(outRatingCoords);
			
			localNode.hasInitialized=true;
						
			// after centralized computation, the core node should send its H &
			// S to all the landmark nodes
/*			issueCoords(timeStamp, CurList.IndexOfLandmarks, new CB1<String>() {
				protected void cb(CBResult ncResult, String errorString) {
					
							log.debug("Core node has issued its H & S to all landmarks.");
				}
			});*/
		}
		
	}
	
	
	// after the centralized computation, the core node issue H & S to landmarks
	// forwardly
	void issueCoords(final long timeStamp, final List<AddressIF> nodes,
			final CB1<String> cbCoords) {

		if (nodes.size() == 0) {
			String errorString = "getRemoteCoords: no valid nodes";
			log.warn(errorString);
			cbCoords.call(CBResult.OK(), errorString);
			return;
		}
		//remove myself
		if(nodes.contains(RankingLoader.me)){
			nodes.remove(RankingLoader.me);
		}
		
		Barrier barrierIssue = new Barrier(true);
		barrierIssue.setNumForks(nodes.size());
		
		
		final StringBuffer errorBuffer = new StringBuffer();

		for (AddressIF addr : nodes) {
			issueCoord(timeStamp, addr, barrierIssue, errorBuffer);
		}

		EL.get().registerTimerCB(barrierIssue, new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				cbCoords.call(CBResult.OK(), errorString);
			}
		});
	}

	/**
	 * print the coordinate
	 * @param outRatingCoords
	 */
	public static void print(
			Hashtable<AddressIF, ratingCoord<AddressIF>> outRatingCoords) {
		// TODO Auto-generated method stub
		log.info("\n\npint Hashtable<AddressIF, ratingCoord<AddressIF>>");
		if(outRatingCoords!=null){
			Iterator<AddressIF> ier = outRatingCoords.keySet().iterator();
			while(ier.hasNext()){
				AddressIF tmp = ier.next();
				log.info("addr: "+tmp.toString()+", coord: "+ outRatingCoords.get(tmp).toString());		
			}			
		}
		//
	}

	public static void print1(
			Hashtable<Integer, ratingCoord<Integer>> outRatingCoords) {
		// TODO Auto-generated method stub
		log.info("\n\npint Hashtable<AddressIF, ratingCoord<AddressIF>>");
		if(outRatingCoords!=null){
			Iterator<Integer> ier = outRatingCoords.keySet().iterator();
			while(ier.hasNext()){
				Integer tmp = ier.next();
				log.info("addr: "+tmp.toString()+", coord: "+ outRatingCoords.get(tmp).toString());		
			}			
		}
		//
	}
	
	void issueCoord(long timeStamp, final AddressIF remNode,
			final Barrier barrier, final StringBuffer errorBuffer) {
		if (RankingLoader.me.equals(remNode))
			return;

		HSHRecord curRecord = pendingHSHLandmarks.get(Long.valueOf(timeStamp));
	
		ClustIssueRequestMsg msg = new ClustIssueRequestMsg(RankingLoader.me, curRecord.ratingCache,timeStamp, curRecord.version);
		log.warn("Core node issues H & S to " + remNode);

		//barrier.fork();
		
		comm.sendRequestMessage(msg, remNode,
				new ObjCommRRCB<ClustIssueResponseMsg>() {
					protected void cb(CBResult result,
							final ClustIssueResponseMsg responseMsg,
							AddressIF node, Long ts) {
						switch (result.state) {
						case OK: {
							log.info("$ ! H & S have been received from"
											+ responseMsg.from);
							break;
						}
						case ERROR:
						case TIMEOUT: {
							String error = remNode.toString(false)
									+ "has not received requirement, as: "
									+ result.toString();
							log.warn(error);
							break;
						}
						}
						barrier.join();
					}
				});
	}
	
	
	/**
	 * measure the control message
	 */
	private volatile double controlTraffic=0;
	
	/**
	 * get the control traffic, and reset the counter
	 * @return
	 */
	public double getControlTraffic(){
		
		double traffic=controlTraffic;
		controlTraffic=0;
		//dual direction
		return traffic*2;
	}
	/**
	 * add the control traffic
	 * @param val
	 */
	public void addControlTraffic(double val){
		controlTraffic+=val;
	}
	
	
	public static int logControl_DELAY=120000;
	
	//tick control
	final CB0 tickControl;
	
	
	public void logControlTraffic(){
		
		registerControlTrafficTimer();
		try {
			RankingLoader.logControlTraffic.append(RankingLoader.getUptimeStr()+" "+this.getControlTraffic()+"\n");
			RankingLoader.logControlTraffic.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * register the control traffic timer
	 */
	public void registerControlTrafficTimer() {
		// LOWTODO adaptive delay
		long delay = logControl_DELAY ;

		// log.debug("setting timer to " + delay);
		EL.get().registerTimerCB(delay, tickControl);

	}
	
	
	/**
	 * 
	 * 
	 * [nudt_nina@kc-sce-plab1 ~]$ cat coordinate.log 
Timer: 1684.466
(-0.00 0.00 0.00 -0.00 0.00), (0.00 0.00 0.00 0.00 0.00), (-1.00 -1.00 -1.20 -2.01), (0.00)
0.0
Timer: 1696.895
(0.00 0.00 -0.00 0.00 0.00), (0.00 0.00 -0.00 0.00 0.00), (-1.00 -1.06 -1.00 -1.00), (0.00)
0.0
Timer: 1769.784
(0.00 -0.00 -0.00 0.00 0.00), (-0.00 0.00 0.00 0.00 -0.00), (-2.71 -2.17 -2.09 1.14), (0.00)
0.0
Timer: 1773.732
(0.00 -0.00 -0.00 0.00 0.00), (-0.00 0.00 0.00 0.00 0.00), (-1.58 -2.30 -1.82 2.15), (0.00)
0.0

	 * 
	 * host ranking agent
	 * @author ericfu
	 *
	 */
	class HostClient{
		
		final CB0 updateCB;
		final CB0 releaseCB;
		final CB0 UpdatePerformanceLogCB;
		
		
		final RatingClient<AddressIF> myCoord;
		final Hashtable<AddressIF,ratingCoord<AddressIF>> cachedLandmarks;
		
		//initialized my coordinate
		boolean hasInitialized=false;
		
		public HostClient(){
			 myCoord=new RatingClient<AddressIF>(RankingLoader.me,coordDim,RatingLevel);
			 cachedLandmarks=new Hashtable<AddressIF,ratingCoord<AddressIF>>(2);
				releaseCB = new CB0() {
					protected void cb(CBResult result) {
						askRelease(UpdateSource, new CB1<String>() {
							protected void cb(CBResult cbResult, String errorString) {
								switch (cbResult.state) {
								case OK: {
									log.debug("$: ask for release completed");
									break;
								}
								case ERROR:
								case TIMEOUT: {
									log.warn("$: ask for release failed");
									break;
								}
								}
							}
						});
					}
				};
				
				updateCB = new CB0() {
					protected void cb(CBResult result) {
						GossipUpdate();
					}
				};
				
				UpdatePerformanceLogCB= new CB0() {
					protected void cb(CBResult result) {
						UpdatePerformanceLog();
					}
				};
		}
		
		/**
		 * receive the coordinate from the centralized node, e.g., for landmarks,
		 * @param coord
		 */
		public void receiveMyCoord1(Hashtable<AddressIF,ratingCoord<AddressIF>> coords){
			if( coords!=null){
				if(coords.containsKey(RankingLoader.me)){
					myCoord.getSystemCoord().copy(coords.get(RankingLoader.me));
					log.info("receive the coordinate: \n"+myCoord.getSystemCoord().toString());
				}
				cachedLandmarks.putAll(coords);
			}
		}
		
		/**
		 * copy the coordinate
		 * @param coord
		 */
		public void receiveMyCoord(ratingCoord<AddressIF> coord){
			if( coord!=null){				
					myCoord.getSystemCoord().copy(coord);
					log.info("receive the coordinate: \n"+myCoord.getSystemCoord().toString());
		}
		}
		
		// call askRelease() periodically
		void registerTimer() {
			double rnd = RankingLoader.random.nextGaussian();// What is the type of
															// RankingLoader?
			long delay = 2*UPDATE_DELAY + (long) (2*UPDATE_DELAY * rnd);
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, releaseCB);
		}
		/**
		 * gossip timer
		 */
		void registerGossipTimer(){
			// LOWTODO adaptive delay
			double rnd = RankingLoader.random.nextGaussian();
			long delay = UPDATE_DELAY + (long) (2*UPDATE_DELAY * rnd);

			log.debug("setting timer to " + delay);
			EL.get().registerTimerCB(delay, updateCB);
	
		}

		void registerUpdatePerformanceLogTimer() {
		
			long delay = 60000;
			log.debug("setting timer to " + delay);

			EL.get().registerTimerCB(delay, UpdatePerformanceLogCB);
		}
		
		public void init(){
			
			if(useLandmark){
				registerTimer();
			}
			registerGossipTimer();
			
			registerUpdatePerformanceLogTimer();
		}
		
		/**
		 * update the performance log
		 */
		void UpdatePerformanceLog(){
			
			registerUpdatePerformanceLogTimer();
			
			if(hasInitialized){
				try {
					logHSHClusteringResults();
				} catch (IOException e) {
					// TODO Auto-generated
					// catch block
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * gossip process
		 */
		void GossipUpdate() {
			
			
			registerGossipTimer();
			
			//construct message
			final AddressIF neighbor = pickGossipNode();

			if (neighbor == null||neighbor.equals(UpdateSource)) {
				log.warn("Nobody to gossip with. Waiting...");
				return;
			}
			
			/**
			 * add the control traffic
			 */
			addControlTraffic(1);
			//
			// send him a gossip msg
			// LOWTODO could bias which nodes are sent based on his coord
			CoordGossipRequestMsg msg = new CoordGossipRequestMsg(
					myCoord.coord, getUpNeighbors(),
					RankingLoader.me);
			log.info("CoordGossipRequestMsg: "+neighbor);
			final long sendStamp = System.nanoTime();
			comm.sendRequestMessage(msg, neighbor,
					new ObjCommRRCB<CoordGossipResponseMsg>() {

						protected void cb(CBResult result,
								final CoordGossipResponseMsg responseMsg,
								AddressIF remoteAddr, Long ts) {

							switch (result.state) {
							case OK: {
								if(responseMsg.remoteCoord==null||responseMsg.nodes==null||responseMsg.nodes.isEmpty()){
									log.info("remoteCoord is null!");
									break;
								}else{
									addControlTraffic(1);	
								//do probe
								mProber.doPing(neighbor, new CB2<Double,Long>(){
									@Override
									protected void cb(CBResult result,
											Double arg1, Long arg2) {
										// TODO Auto-generated method stub
																													
										double rtt = arg1.doubleValue();
										
										if(rtt>0){
											//update the neighbor records											
											boolean finished=myCoord.processSample(neighbor, rtt, responseMsg.remoteCoord,responseMsg.ratingError, System.currentTimeMillis());
											if(finished){											
												myCoord.aggregate(responseMsg.separators);
											}
											/*try {
												logHSHClusteringResults();
											} catch (IOException e) {
												// TODO Auto-generated
												// catch block
												e.printStackTrace();
											}*/
										}
									
									}									
								});	
					
								
								break;
								}
							}
							case ERROR:
							case TIMEOUT:{							
								log.warn("time, out!");
								
								
								
								break;
							}
							}
							}
						
			});
		}

		/**
		 * ask the source node to release the landmark's coordinate
		 * @param coreNode
		 * @param cbCoords
		 */
		void askRelease(final AddressIF coreNode, final CB1<String> cbCoords) {
			
			//once we have initialized the coordinate, we do not ask release again
			if(hasInitialized){
				myCoord.canUpdateCoordByGossip=true;
				return;
			}
			

			/**
			 * traffic
			 */
			addControlTraffic(1);
			
			ClustReleaseRequestMsg msg = new ClustReleaseRequestMsg(
					RankingLoader.me);
			log.warn("ask " + coreNode + " to release H & S");

			comm.sendRequestMessage(msg, coreNode,
					new ObjCommRRCB<ClustReleaseResponseMsg>() {
						protected void cb(CBResult result,
								final ClustReleaseResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch (result.state) {
							case OK: {
								// the host node records the release response
								// message
								// hostNode._H <- coreNode.responseMsg.H
								// hostNode._S <- coreNode.responseMsg.S

								if (responseMsg.ratingCache==null||responseMsg.ratingCache.isEmpty()) {
									
									registerTimer();
									log.warn("Failed due to empty H or S");
									cbCoords.call(CBResult.ERROR(),
											"Failed due to empty H or S");
									break;
								} else {
									// else
								
									// Use IsReady to judge if the H & S have
									// already been computed.
									final List<AddressIF> nodes = new ArrayList<AddressIF>(
											1);
									//has landmarks
									if(responseMsg.landmarks!=null&&!responseMsg.landmarks.isEmpty()){
										nodes.addAll(responseMsg.landmarks);
									}
									log.debug("$ ! ReleaseResponse from"
											+ responseMsg.from+", landmarks: "+nodes.size());
									
									if(nodes.contains(RankingLoader.me)){
										
										log.info("is landmark!");
										//save the coordinate in me
										receiveMyCoord1(responseMsg.ratingCache);
										//save the rating separator
										if(hostNode!=null){
											hostNode.myCoord.movingAverage(responseMsg.outSeparators);
										}
										
										//we have initialized successfully
										hasInitialized=true;
										
										
									}else{

									// Send collect message to all the landmark
									// nodes.
									// Record the rtts and call the D_N_S_NMF()
									// to compute the Coord

									//update my coordinate
									//updateMatrix(nodes,responseMsg.outSeparators,responseMsg.ratingCache);
										if(nodes.isEmpty()){
											
											registerTimer();
											
											String error = coreNode.toString(false)
											+ "has not received requirement, as: "
											+ result.toString();
											log.warn(error);
											cbCoords.call(CBResult.ERROR(), error);											
											return;
											
										}else{
											
											askRelease(nodes,new CB2<Vec,Hashtable<AddressIF,ratingCoord<AddressIF>>>(){
												protected void cb(
													CBResult result2,
													Vec arg1,
													Hashtable<AddressIF, ratingCoord<AddressIF>> arg2) {
												// TODO Auto-generated method stub
												switch(result2.state){
												case OK:{
													//update my rating coordinate, in case of not 
													
													updateMatrix(nodes,arg1,arg2);
													
													break;
												}
												case TIMEOUT:
												case ERROR:{
													
													registerTimer();
													cbCoords.call(CBResult.ERROR(),
													"Failed due to unresloved landmarks");
													break;
												}	
												}
											}												
										});
										}
										
									}
									
									cbCoords.call(CBResult.OK(), "Success");
								}

								break;
							}
							case ERROR:
							case TIMEOUT: {
								//register the timeout timer
								registerTimer();
								
								String error = coreNode.toString(false)
										+ "has not received requirement, as: "
										+ result.toString();
								log.warn(error);
								cbCoords.call(CBResult.ERROR(), error);
							}
							}
							
						}
					});
		}
		
		/**
		 * ask the landmarks to release the rating coordinate, as well as the rating separators
		 * @param nodes
		 * @param cbDone
		 */
		void askRelease(final List<AddressIF> nodes,final CB2<Vec,Hashtable<AddressIF,ratingCoord<AddressIF>>> cbDone){
			
			
			int size=nodes.size();
			
			final 	Barrier barrierIssue = new Barrier(true);
			barrierIssue.setNumForks(nodes.size());
			
			
			final Vec[] outVec=new Vec[1];
			final Hashtable<AddressIF,ratingCoord<AddressIF>> outTable=new Hashtable<AddressIF,ratingCoord<AddressIF>>(1);

			for (AddressIF addr : nodes) {
				askRelease(addr, barrierIssue, outVec,outTable);
			}

			EL.get().registerTimerCB(barrierIssue, new CB0() {
				protected void cb(CBResult result) {
					String errorString;
					
					
					//updateMatrix(nodes,responseMsg.outSeparators,responseMsg.ratingCache);
					
					cbDone.call(CBResult.OK(), outVec[0],outTable);
				}
			});
			
		}
		/**
		 * ask the landmark to release its coordinate
		 * @param addr
		 * @param barrierIssue
		 * @param outVec
		 * @param outTable
		 */
		private void askRelease(final AddressIF coreNode,final Barrier barrierIssue,
				final Vec[]  outVec,final Hashtable<AddressIF,ratingCoord<AddressIF>> outTable) {
			// TODO Auto-generated method stub
			
			
			ClustReleaseRequestMsg msg = new ClustReleaseRequestMsg(
					RankingLoader.me);
			log.warn("ask landmark " + coreNode + " to release rating coordinate!");

			comm.sendRequestMessage(msg, coreNode,
					new ObjCommRRCB<ClustReleaseResponseMsg>() {
						protected void cb(CBResult result,
								final ClustReleaseResponseMsg responseMsg,
								AddressIF node, Long ts) {
							switch(result.state){
							case OK:{
								if(responseMsg.outSeparators!=null){
									outVec[0]=responseMsg.outSeparators.copy();
								}
								if(responseMsg.ratingCache!=null&&!responseMsg.ratingCache.isEmpty()){
									outTable.putAll(responseMsg.ratingCache);
								}
								break;
							}
							case TIMEOUT:
							case ERROR:{
								log.warn("ask landmark to release has failed, @: "+coreNode.getHostname());
								break;
							}
							}
							barrierIssue.join();
						}
			});
		}

		/**
		 * update my coordinate, for intialization
		 * @param nodes
		 * @param outSeparators
		 */
		public void updateMatrix(final List<AddressIF> nodes,final Vec outSeparators,final Hashtable<AddressIF,ratingCoord<AddressIF>> ratingCache){
			

			
			/**
			 * control traffic
			 */
			addControlTraffic(nodes.size());
			
			mProber.collectRTTs(nodes,
					new CB2<Set<NodesPair>, String>() {
						protected void cb(
								CBResult ncResult,
								Set<NodesPair> nps,
								String errorString) {
							
							

							// A host to all the
							// landmarks, it's a row
							// vector
							Matrix host2Landmark = new DenseMatrix(1,nodes.size(),0);
							// After call
							// D_N_S_NMF(host2Landmark),
							// the result returned as
							// coord
							Iterator ier = nps.iterator();
							while (ier.hasNext()) {
								NodesPair tmp = (NodesPair) ier.next();
								int to = nodes.indexOf(tmp.endNode);
								if (to < 0) {
									log.warn("non-existed item, total "+nodes.size());
									continue;
								} else if(to<host2Landmark.numColumns()){
									host2Landmark.setValue(0, to, tmp.value);
								}else{
									log.warn("to: "+to+" exceeds "+nodes.size());
								}
							}
							
							List<AddressIF> host=new ArrayList<AddressIF>(1);
							host.add(RankingLoader.me);
							long timer=System.currentTimeMillis()/1000;
							//rating matrix
							Matrix outRating=null;
							
							//init the separators
							Vec separators=initSeparators();
							//if(useSimpleRatingSeparator){
								//separators=new Vec(RatingLevel-1);
								//use the hosts' separators
							
							log.info("host2Landmark Matrix: "+host2Landmark.toString());
							if(outSeparators==null){
								outRating=RatingFunction.getInstance().local_compute(host2Landmark, 
										myCoord.getRawMeasurementsFromNeighbors(), RatingType, RatingLevel, separators);
								if(hostNode!=null&&separators!=null){
									hostNode.myCoord.movingAverage(separators);
								}
							}else{
								outRating=RatingFunction.computeRating(host2Landmark,outSeparators, RatingLevel);
								if(hostNode!=null){
									hostNode.myCoord.movingAverage(outSeparators);
								}
							}
							
							/**
							 * save the neighbors
							 */
							myCoord.saveNeighbors(nps,ratingCache);
							
							
							log.info("ratingMatrix: "+outRating.toString());
							//compute my coordinate
							Hashtable<AddressIF, ratingCoord<AddressIF>> coord = mmmf.processRatingSamplesHost(timer, host, 
									nodes, outRating,ratingCache);
							//save the coordinate
							if(coord.containsKey(RankingLoader.me)){
								receiveMyCoord(coord.get(RankingLoader.me));
							}else{
								log.warn("do not have the coordinate!");														
							}
							
							//versionNumber = responseMsg.version;
							/*try {
								logHSHClusteringResults();
							} catch (IOException e) {
								// TODO Auto-generated
								// catch block
								e.printStackTrace();
							}*/

							nodes.clear();
							
							//we have initialized successfully
							hasInitialized=true;
						}
						
					});
			
			
			
			
		}
	}
	

	
	/**
	 * init the separators
	 * @param level
	 * @return
	 */
	public static  Vec initSeparators(){
		Vec vv;
		if(RatingType==RatingFunction.ClusteringBasedRating){
			vv=new Vec(RatingLevel);
			return vv;
		}else{
			vv=new Vec(RatingLevel-1);
			return vv;
		}
		
	}
	
	
	
	 void logHSHClusteringResults() throws IOException {

		if(hostNode!=null){
			//double timer = (System.currentTimeMillis() - RankingLoader.StartTime) / 1000d;
			RankingLoader.logCluster.write("Timer: " + Util.currentGMTTime() + "\n");			
			RankingLoader.logCluster.write(hostNode.myCoord.getSystemCoord().toString() + "\n");
			
			double[] err=hostNode.myCoord.getErrorWithNeighbor();
			RankingLoader.logCluster.write(err[0]+"\n");
			RankingLoader.logCluster.write(err[1]+"\n");
			RankingLoader.logCluster.write(hostNode.myCoord.RatingDriftResult.get()+"\n");
			RankingLoader.logCluster.write(hostNode.myCoord.NeighborCoords.size()+"\n");
			RankingLoader.logCluster.flush();
			
			
		}
	}
	
	/**
	 * main entry
	 * @param args
	 */
	public static void main(String[] args){
	
		DistributedMMMF<Integer> mmmf1=new DistributedMMMF<Integer>(coordDim,RatingLevel,RatingType);
		
		int numRows=8;
		int numcolumns=8;
		int []ids={0, 1, 2 ,3 ,4 ,5, 6, 7};
		String vals="";
		
		double[][]DistanceMatrix={
				{ 0,   22.7390,   22.8220,  160.2300 , 194.8730 , 180.5290  ,299.8540  ,284.0700},
				{22.7640   ,      0 ,   0.1370 , 181.9340 , 220.8690,  268.6630 , 325.0220 , 309.1530},
				{22.8880  ,  0.5150 ,        0 , 181.2340 , 220.8870 , 260.1410 , 325.0940 , 309.2090},
				{164.3860 , 182.9360 , 181.0970 ,        0 ,  65.7670 ,  37.0950,  160.3560 , 152.8710},
				{195.0100 , 220.9100 , 220.8670 ,  63.6640 ,        0  , 52.6180 , 124.2280 , 110.0100},
				{180.5290,  268.6630 , 260.1410  , 37.0950 ,  52.6180 ,        0 , 167.7530,  151.0880},
				{ 299.9350 , 325.1380 , 325.1400 , 156.6080,  124.2900 , 167.7530      ,   0  , 26.2210},
				{284.1430,  309.1630 , 309.1170 , 152.2020  ,109.9720,  151.0880 ,  26.0850   ,      0}};

		Vector<Double> sampledData=getPositiveValues(DistanceMatrix);
		log.info("\n\n$: HSH: Start HSH clustering computation process!");
		Matrix measuredValus;
		Matrix outRating=null;
		//if(useSimpleRatingSeparator){
			measuredValus= new DenseMatrix(DistanceMatrix);	
			
			log.info("measuredValus Matrix: \n"+measuredValus.toString());
			Vec separators=initSeparators();
			//compute rating
			outRating=RatingFunction.getInstance().local_compute(measuredValus,sampledData, RatingType, RatingLevel,separators);
			
			log.info("ratingMatrix: \n"+outRating.toString());
			log.info("rating separators: "+separators.toString());
			//clear
	
			long timer=1;
		    List<Integer> landmarks=new ArrayList<Integer>(2);
		    for(int i=0;i<ids.length;i++){
		    	landmarks.add(Integer.valueOf(ids[i]));
		    }
		    
		Hashtable<Integer, ratingCoord<Integer>> outRatingCoords = mmmf1.processSamplesLandmarks_(timer, 
				landmarks,outRating);
		
		print1(outRatingCoords);
		
	}

	/**
	 * get positive values
	 * @param distanceMatrix
	 * @return
	 */
	private static Vector<Double> getPositiveValues(double[][] distanceMatrix) {
		// TODO Auto-generated method stub
		Vector<Double> out=new Vector<Double>(2);
		for(int i=0;i<distanceMatrix.length;i++){
			for(int j=0;j<distanceMatrix[0].length;j++){
				out.add(distanceMatrix[i][j]);
			}
		}
		return out;
	}
}
