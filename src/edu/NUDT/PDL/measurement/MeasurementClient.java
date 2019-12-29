package edu.NUDT.PDL.measurement;

import java.io.BufferedWriter;
import java.util.List;

import edu.NUDT.PDL.main.RankingLoader;
import edu.NUDT.PDL.util.MainGeneric;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;

public class MeasurementClient {
/**
 * collect measurement results;
 */
	
	

	
	
	final CB0 updateMeasurement;
	
	private List<String> ipList=null;
	private BufferedWriter bw=null;
	
	//round of each measurements
	public static long MeasurementUPDATE_DELAY =Long.parseLong(Config
			.getConfigProps().getProperty("MeasurementUPDATE_DELAY", "600000"));
	
	public MeasurementClient(){
		
		updateMeasurement= new CB0() {
			protected void cb(CBResult result) {
				performOneRoundMeasurement();
			}

		};
				
	}
	
	/**
	 * register
	 * @param ipList_
	 * @param bw_
	 */
	public void init(List<String> ipList_,BufferedWriter bw_){
		this.ipList=ipList_;
		this.bw=bw_;
		//measurement clock
		//registerMeasurementClock();
		performOneRoundMeasurement();
	}
	
	public void registerMeasurementClock(){
		
		double rnd = RankingLoader.random.nextDouble();
		long delay = MeasurementUPDATE_DELAY + (long) (5*60*1000 * rnd);

		// log.debug("setting timer to " + delay);
		EL.get().registerTimerCB(delay, updateMeasurement);
		
	}
	
	/**
	 * perform measurements
	 */
	public void performOneRoundMeasurement(){
		registerMeasurementClock();
		MainGeneric.collectMeasurements2Nodes(ipList, bw);
		
	}
	
}
