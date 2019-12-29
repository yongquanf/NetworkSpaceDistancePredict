/**
 * News Project
 *
 * File:         PingManager.java
 * RCS:          $Id: PingManager.java,v 1.9 2009/11/14 11:31:31 zsb739 Exp $
 * Description:  PingManager class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Apr 11, 2007 at 7:13:06 PM
 * Language:     Java
 * Package:      edu.northwestern.news.util
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2007, Northwestern University, all rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package edu.NUDT.PDL.measurement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import edu.NUDT.PDL.util.Pair;
import edu.NUDT.PDL.util.Util;
import edu.NUDT.PDL.util.Util.PingResponse;


/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The PingManager class is the place to go for getting ICMP ping data. It 
 * caches recent entries and limits the number of concurrent pings.
 */
public class PingManager {

	private static PingManager self;

	private int maxPings = 10;
	
	private static boolean stopped = false;
	
    /** cache of ping results seen recently */
    private HashMap<String, Pair<Long, Double>> pingCache;
    private int activePings = 0;
    
    private static final int MAX_ACTIVE_PINGS = 10;
    
    private static final long PING_CACHE_EXPIRE = 5 * 60 * 1000;

	protected static final boolean DEBUG = false;
	
	protected static Object rttSync = new Object();
	
	private double rttSum = 0;
	private int rttCount = 0;

	public PingManager(){
		 pingCache = 
		    	new HashMap<String, Pair<Long, Double>>();
	}
	
	public synchronized static PingManager getInstance(){
		if (self == null ){
			stopped = false;
			self = new PingManager();			
		}
		return self;
	}
	
	 /* (non-Javadoc)
	 * @see edu.northwestern.news.util.IPingManager#doPing(java.lang.String, edu.northwestern.news.util.PluginInterface, edu.northwestern.news.util.Util.PingResponse)
	 */
    public void doPing(final String ipToPing, 
    		final PingResponse callback) {
    
    	if (stopped || maxPings <=0){
    		callback.response(-1);
    		return;
    	}
    	// check cache first
    	synchronized (pingCache){
    	
	    	if (pingCache.get(ipToPing)!=null){
	    		Pair<Long, Double> pair = pingCache.get(ipToPing);
	    		
	    		while (pair.getValue()==null && (pair.getKey() + PING_CACHE_EXPIRE) >= System.currentTimeMillis()){
	    			// already scheduled, do nothing
	    			return;
//	    			try {
//						pingCache.wait();
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
	    		}
	    		if ( (pair.getKey() + PING_CACHE_EXPIRE) >= System.currentTimeMillis()){    			
	    			callback.response(pair.getValue());
	    			return;
	    		} else {
	    			pingCache.put(ipToPing, new Pair<Long, Double>(System.currentTimeMillis(), 
	    					null));
	    		}
	    	} else {
	    		pingCache.put(ipToPing, new Pair<Long, Double>(System.currentTimeMillis(), 
    					null));
	    	}
	    	
	    	if (activePings < 0){
	    		if (DEBUG) System.err.println("Bad active pings value!");
	    		activePings = 0;
	    	}
	    	
	    	while (activePings > this.maxPings 
	    			&& !stopped){
	    		try {
	    			if (DEBUG) System.out.println("Waiting... Active pings: " + activePings);
					pingCache.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	if (DEBUG) System.out.println("(" + ipToPing + ")Active pings: " + activePings);
	    	activePings++;
    	}
    	
    	
    	 
    	if (stopped ){
    		activePings--;
    		callback.response(-2);
    		return;
    	}
    	Util.createThread("Ping-" +ipToPing, new Runnable(){

			public void run() {
				double rtt = -1;
				if (stopped) {
					callback.response(-2);
					return;
				}
				
				boolean isV6 = Util.isV6Address(ipToPing);
            	String ping = "ping";
				
		        try {
		            if (Util.isWindows) {
		                Process p;

		                if(isV6) ping += " -6 ";
		                
		                
		                p = Runtime.getRuntime().exec(ping+" -n 3 -w 3000 " + ipToPing);
		                
		                
		                BufferedReader in = new BufferedReader(new InputStreamReader(
		                            p.getInputStream()));

		                String line;
		                
		                int notReadyCount = 0;
		                boolean notDone = true;
		                
		                while (notDone){
		                	while (!in.ready()){
		                		try {
		                			Thread.sleep(1*1000);
		                			notReadyCount++;
		                			if (notReadyCount>3) {
		                				notDone=false;
		                				break;
		                			}
		                		} catch (InterruptedException e) {
		                			// TODO Auto-generated catch block
		                			e.printStackTrace();
		                		}

		                	}
		                	notReadyCount = 0;
		                	if ((line = in.readLine()) == null){
		                		notDone = false;
		                		break;
		                	}
		                	// find RTT
		                	if (line.contains("Average =")) {
		                		int beginIndex = line.lastIndexOf("= ") + 2;
		                		int endIndex = line.lastIndexOf("ms");
		                		String val = line.substring(beginIndex, endIndex);
		                		rtt = Double.parseDouble(val);
		                		notDone=false;
		                		break;
		                	}

		                }
		                // then destroy
		                p.destroy();
		            } else if (Util.isLinux || Util.isOSX) {
		                Process p;
		                if (isV6) ping = "ping6 ";
		                p = Runtime.getRuntime().exec(ping+" -q -c 3 " + ipToPing);

		                BufferedReader in = new BufferedReader(new InputStreamReader(
		                            p.getInputStream()));
		                String line;

		                while ((line = in.readLine()) != null) {
		                    // find RTT
		                    if (line.contains("rtt") || line.contains("round-trip")) {
		                        String[] vals = line.split("/");
		                        rtt = Double.parseDouble(vals[4]);

		                        break;
		                    }
		                }

		                // then destroy
		                p.destroy();
		            }
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		        
		        if (PingManager.getInstance() == null || 
		        		PingManager.getInstance().getPingCache()==null) return;
		        synchronized (PingManager.getInstance().getPingCache()){
		        	//if (!MainGeneric.isShuttingDown()){
			        	if (rtt>-1){
				        	PingManager.getInstance().getPingCache().put(ipToPing, new Pair<Long, Double>(
				        			System.currentTimeMillis(), rtt));
			        	}
			        	if (PingManager.getInstance().activePings == 0){
			        		if (DEBUG) System.err.println("Bad active value for " +
			        				ipToPing +"!");
			        	}
			        	if (PingManager.getInstance().activePings>0) PingManager.getInstance().activePings--;
			        	
			        	if (DEBUG) System.out.println("Done with " +
			        			ipToPing +"... Active pings: " + PingManager.getInstance().activePings);
			        //	}
			       /*  else {
			        	activePings--;
			        	if (activePings < 0){
			        		if (DEBUG) System.err.println("Invalid ping value!");
			        		activePings = 0;
			        	}
			        }*/
		        	PingManager.getInstance().getPingCache().notifyAll();
		        } // end synch
		        //System.out.println(ipToPing+"\t"+rtt);
		        synchronized(rttSync){
		        	rttSum+=rtt;
		        	rttCount++;
		        }
		        callback.response(rtt);
				
			}
			
    	});
        
    }

	public static void stop() {
		stopped  = true;
		synchronized (getInstance().getPingCache()){
			getInstance().getPingCache().notifyAll();
		}
		//self = null;
		
	}
	
	public static void activate(){
		stopped = false;
	}
	
	/* (non-Javadoc)
	 * @see edu.northwestern.news.util.IPingManager#getPingCache()
	 */
	public HashMap<String, Pair<Long, Double>> getPingCache(){
		return pingCache;
	}

	public double getRttSample() {
		synchronized(rttSync){
			double toReturn = rttCount==0?0:rttSum/rttCount;
			rttSum = 0;
			rttCount = 0;
			return toReturn;
		}
		
	}
	
}
