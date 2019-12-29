package edu.NUDT.PDL.measurement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import edu.NUDT.PDL.util.HashSetCache;




public class MeasurementStatistics {

	private static final int THRESHOLD_ENTRIES = 200;
    private static MeasurementStatistics self;
    public final static HashMap<String, Integer> peerMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> peerClientMap = new HashMap<String, Integer>();
    public final static HashMap<String, Integer> routerMap = new HashMap<String, Integer>();
	private static final int MAX_VIVALDI_RESULTS = 200;
	private static final int MAX_TRACE_RESULTS = 200;
	private static final int MAX_PING_RESULTS = 200;

    private final long RATIO_UPDATE_INTERVAL = 30 * 60 *1000;
    private long nextRatioUpdateTime = 0;
    private long nextUpdateInterval;
    /**
     * 
     * 
     * 
     */
    /** list of all peers */
    static HashSet<String> peers;
    /** list of all trace route results*/
    static HashSetCache<TraceResult> traceResults;
	Random r;
    
    
    
    private MeasurementStatistics(){
    	if (r==null){
    		r = new Random();
    	}
    	
    	
    }
	
    public static MeasurementStatistics getInstance(){
    	if(self==null){
    		self=new MeasurementStatistics();
    	}
    	return self;
    	
    }
    
	public void addTraceRouteResult(TraceResult tr) {
        synchronized (traceResults) {
        	if (traceResults.size()>MAX_TRACE_RESULTS){
        		//reportTraces();
        	}
        	traceResults.add(tr);
        }		
	}
	
	/**
	 * Register peer client name for DB lookup
	 * @param peerClient
	 */
	public void addRouterForLookup(String routerIp) {
		synchronized (routerMap) {
            if (routerMap.get(routerIp) == null) {
            	routerMap.put(routerIp, routerMap.size());
            }
		}
		
	}
}
