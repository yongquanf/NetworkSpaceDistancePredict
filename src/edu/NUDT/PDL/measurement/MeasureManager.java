package edu.NUDT.PDL.measurement;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.NUDT.PDL.RatingMessages.NodesPair;
import edu.NUDT.PDL.main.RankingLoader;
import edu.NUDT.PDL.util.MainGeneric;
import edu.NUDT.PDL.util.Util;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.sbon.async.Barrier;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;

public class MeasureManager {

	public static Log log=new Log(MeasureManager.class);
	/**
	 * ping
	 */
	PingManager pinger;
	/**
	 * hops, loss
	 */
	TraceRouteRunner tracer;
	
	
	int PingType;
	
	public MeasureManager(){
		pinger=new PingManager();
		tracer=TraceRouteRunner.getInstance();
	}
	
	public PingManager getPing(){
		return pinger;
	}
	public TraceRouteRunner getTracerouter(){
		return tracer;
	}
	
	
	/**
	 * collect RTTs from a set of nodes
	 * 
	 * @param nodes
	 * @param cbCoords
	 */
	public void collectRTTs(final Collection<AddressIF> landmarks,
			final CB2<Set<NodesPair>, String> cbCoords) {


		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(1);

		if (landmarks.size() == 0) {
			String errorString = "collectRTTs: no valid nodes";
			log.debug(errorString);
			cbCoords.call(CBResult.OK(), nodesPairSet, errorString);
			return;
		}

		final Barrier barrier = new Barrier(true); // Collect
		//remove myself
		if(landmarks.contains(RankingLoader.me)){
			landmarks.remove(RankingLoader.me);
			//add myself
			//nodesPairSet.add(new NodesPair(RankingLoader.me,RankingLoader.me,0));
		}
		//set up the barrier number
			barrier.setNumForks(landmarks.size());
		

		final StringBuffer errorBuffer = new StringBuffer();
		// System.out.println("$@ @ Collect from: "+pendingLandmarks.size()+" nodes "+pendingLandmarks.toString());
		// when "for" is run over, the local node's nodesPairSet will get the
		// rtt informations from itself to the other nodes
		
		double []latency =new double[1];
		latency[0]=-1;
		
		for (AddressIF addr : landmarks) {
			
			final AddressIF remNod=addr;
/*			//me
			if (RankingLoader.me.equals(remNod)) {
				continue;
			} else{
				*/
			//not cached
			//barrier.fork();
			doPing(remNod, new CB2<Double,Long>() {

				
				protected void cb(CBResult result, Double lat, Long timer) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {
						nodesPairSet.add(new NodesPair(RankingLoader.me, remNod, lat
								.doubleValue()));
						log.info("$: Collect: doPing OK!");
						break;
					}
					case ERROR:
					case TIMEOUT: {
						String error = remNod.toString(false)
								+ "has not received responses, as: "
								+ result.toString();
						log.info(error);
						break;
					}
					}

					barrier.join();
				}
			});	
			
			//}
		}

		//============================================================
		//final long interval=3*1000;
		EL.get().registerTimerCB(barrier, new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				log.info("$@ @ Collect Completed");
				cbCoords.call(result, nodesPairSet, errorString);
			}
		});
	}
	
	/**
	 * directly use the icmp ping
	 * @param target
	 * @param cbDone
	 */
	public void doPing(final AddressIF target, final CB2<Double, Long> cbDone) {

		final long timer = System.currentTimeMillis();
		
		final double[] lat = new double[1];
		lat[0] = -1;
	
		//=========================================

		//if (RankingLoader.USE_ICMP) {
			//icmp Ericfu
			MainGeneric.doPing(NetUtil.byteIPAddrToString(((NetAddress)target).getByteIPAddr()), new CB1<double[]>(){

				
				protected void cb(CBResult result, double[] arg1) {
					// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
						cbDone.call(result, arg1[0],Long.valueOf(timer));
						
						//log the ping record
						try {
							if(MainGeneric.pingType!=2){
							RankingLoader.logRanking.append(Util.currentGMTTime()+ " "+RankingLoader.me.getHostname()
									+" "+target.getHostname()+" "+arg1[0]+"\n");}else{
										RankingLoader.logRanking.append(Util.currentGMTTime()+ " "+RankingLoader.me.getHostname()
												+" "+target.getHostname()+" "+arg1[0]+" "+arg1[1]+"\n");			
										
									}
							
							RankingLoader.logRanking.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						break;
					}
					case ERROR:
					case TIMEOUT:{
						cbDone.call(result, arg1[0],Long.valueOf(timer));
						break;
					}
					
					}
					
				}
			
			});
			
		//} 
		
	}
	
	
	
	public void collectRTTs1(final Collection<AddressIF> landmarks,
			final CB2<Set<NodesPair>, String> cbCoords) {


		final Set<NodesPair> nodesPairSet = new HashSet<NodesPair>(1);

		if (landmarks.size() == 0) {
			String errorString = "collectRTTs: no valid nodes";
			log.debug(errorString);
			cbCoords.call(CBResult.OK(), nodesPairSet, errorString);
			return;
		}

		final Barrier barrier = new Barrier(true); // Collect
		//remove myself
		if(landmarks.contains(RankingLoader.me)){
			landmarks.remove(RankingLoader.me);
			//add myself
			//nodesPairSet.add(new NodesPair(RankingLoader.me,RankingLoader.me,0));
		}
		//set up the barrier number
			barrier.setNumForks(landmarks.size());
		

		final StringBuffer errorBuffer = new StringBuffer();
		// System.out.println("$@ @ Collect from: "+pendingLandmarks.size()+" nodes "+pendingLandmarks.toString());
		// when "for" is run over, the local node's nodesPairSet will get the
		// rtt informations from itself to the other nodes
		
		double []latency =new double[1];
		latency[0]=-1;
		
		for (AddressIF addr : landmarks) {
			
			final AddressIF remNod=addr;
/*			//me
			if (RankingLoader.me.equals(remNod)) {
				continue;
			} else{
				*/
			//not cached
			//barrier.fork();
			doPing1(remNod, new CB2<Double,Long>() {

				
				protected void cb(CBResult result, Double lat, Long timer) {
					// TODO Auto-generated method stub

					switch (result.state) {
					case OK: {
						nodesPairSet.add(new NodesPair(RankingLoader.me, remNod, lat
								.doubleValue()));
						log.info("$: Collect: doPing OK!");
						break;
					}
					case ERROR:
					case TIMEOUT: {
						String error = remNod.toString(false)
								+ "has not received responses, as: "
								+ result.toString();
						log.info(error);
						break;
					}
					}

					barrier.join();
				}
			});	
			
			//}
		}

		//============================================================
		//final long interval=3*1000;
		EL.get().registerTimerCB(barrier, new CB0() {
			protected void cb(CBResult result) {
				String errorString;
				if (errorBuffer.length() == 0) {
					errorString = new String("Success");
				} else {
					errorString = new String(errorBuffer);
				}
				log.info("$@ @ Collect Completed");
				cbCoords.call(result, nodesPairSet, errorString);
			}
		});
	}
	
	
	public void doPing1(final AddressIF target, final CB2<Double, Long> cbDone) {

		final long timer = System.currentTimeMillis();
		
		final double[] lat = new double[1];
		lat[0] = -1;
	
		//=========================================

		//if (RankingLoader.USE_ICMP) {
			//icmp Ericfu
			MainGeneric.doPing_RTT(NetUtil.byteIPAddrToString(((NetAddress)target).getByteIPAddr()), new CB1<double[]>(){

				
				protected void cb(CBResult result, double[] arg1) {
					// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
						cbDone.call(result, arg1[0],Long.valueOf(timer));
						
						//log the ping record
						try {
							if(MainGeneric.pingType!=2){
							RankingLoader.logRanking.append(Util.currentGMTTime()+ " "+RankingLoader.me.getHostname()
									+" "+target.getHostname()+" "+arg1[0]+"\n");}else{
										RankingLoader.logRanking.append(Util.currentGMTTime()+ " "+RankingLoader.me.getHostname()
												+" "+target.getHostname()+" "+arg1[0]+" "+arg1[1]+"\n");			
										
									}
							RankingLoader.logRanking.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						break;
					}
					case ERROR:
					case TIMEOUT:{
						cbDone.call(result, arg1[0],Long.valueOf(timer));
						break;
					}
					
					}
					
				}
			
			});
			
		//} 
		
	}
}

