/**
 * News Project
 *
 * File:         TraceRouteRunner.java
 * RCS:          $Id: TraceRouteRunner.java,v 1.8 2009/11/19 01:19:59 zsb739 Exp $
 * Description:  TraceRouteRunner class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Feb 25, 2007 at 1:22:39 PM
 * Language:     Java
 * Package:      edu.northwestern.news.experiment
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;

import edu.NUDT.PDL.util.HashSetCache;
import edu.NUDT.PDL.util.Util;



/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The TraceRouteRunner class handles asynchronous recording of 
 * traceroute data.
 */
public class TraceRouteRunner implements Runnable {
	

	

	
	private class TraceRouteRun{
		public String ip;
		public boolean recordInDB = true;
		
		public TraceRouteRun(String ip){
			this.ip = ip;
		}
		
		public TraceRouteRun(String ip, boolean record){
			this.ip = ip;
			recordInDB = record;
		} 
		@Override
		public boolean equals(Object obj) {

			return obj!=null && obj instanceof TraceRouteRun && ip.equals(((TraceRouteRun)obj).ip);
		}
		@Override
		public int hashCode() {

			return ip.hashCode();
		}
		
		
	}



	private static final boolean DEBUG = false;
	

	
    static BufferedReader in;
    static Calendar myCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    
	private static TraceRouteRunner self;
	private static Process p;

	private static boolean active = true;
    TraceResult tr;
    HashSetCache<TraceRouteRun> pendingRuns;
    HashSetCache<TraceRouteRun> pendingRunsPrio;
    
    private Object lossSync = new Object();
    private double lossSum = 0;
    private double lossCount = 0;

    private TraceRouteRunner(){
    	pendingRuns = new HashSetCache<TraceRouteRun>(200);
    	pendingRunsPrio = new HashSetCache<TraceRouteRun>(50);
    	Util.createThread("TraceRouteRunner", this);
    	
    }
    
    public static TraceRouteRunner getInstance(){
    	if (self==null ){
    		active = true;
    		self = new TraceRouteRunner();
    	}
    	return self;
    }
    
	public void addIp( String dest ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest));
			pendingRuns.notifyAll();
		}
	}
	
	public void addIp( String dest, boolean recordInDB ){
		synchronized(pendingRuns){
			pendingRuns.add(new TraceRouteRun(dest, recordInDB));
			pendingRuns.notifyAll();
		}
	}
	
	public static void stop(){
		active = false;
		if (self==null || 
				(self.pendingRuns==null && self.pendingRunsPrio==null)) return;
		if (p!=null){
			p.destroy();
		}
		if (self.pendingRuns!=null){
			synchronized(self.pendingRuns){
				self.pendingRuns.notifyAll();
			}
		}

//		if (in!=null){
//			try {
//				in.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		self = null;
		//in = null; // will be closed already
		myCalendar = null;
		p = null;
		
		
		
	}

	/* if the destination list is not empty, then iterate the list, to find the traceroute results
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/** the ip address to trace routes on */
		String dest;
		
		TraceRouteRun trr = null;
		while (true){
		/*	if(Util.isTestingIPv6()){
				String[] ipv6Addr = new String[]{"2001:4860:b002::68", 
						"2001:a18:1:20::22","2001:4978:265::1"};
				trr = new TraceRouteRun(ipv6Addr[0]);
			} else {*/		
				synchronized(pendingRuns){
					while (pendingRuns.size()==0 && pendingRunsPrio.size()==0 
							){
						try {
							pendingRuns.wait(30*1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					Iterator<TraceRouteRun> it;
					if (pendingRunsPrio.size()>0){
						
						it = pendingRunsPrio.iterator();
						trr = it.next();
						it.remove();
						if (DEBUG) System.out.println("High priority traceroute to "+trr.ip);
					} else {
						it = pendingRuns.iterator();
						if (!it.hasNext()){
							continue;
						}
						trr = it.next();
						it.remove();
						if (DEBUG) System.out.println("Normal priority traceroute to "+trr.ip);
					}
				}
			//}
			if (!active) return;
			tr = new TraceResult();
			dest = trr.ip;
			
		ArrayList<TraceEntry> data = new ArrayList<TraceEntry>();
		 String line = null;
		try {
            
			if (Util.isWindows) {
				boolean isV6 = Util.isV6Address(dest);
				String v6Text = "";
				if (isV6) v6Text = "-6 ";
                p = Runtime.getRuntime().exec("tracert -d -w 3000 " + v6Text + dest);
                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));

               

                while ((line = in.readLine()) != null) {                	
                    // parse data
                	String[] entries = line.split("[\\s]+");
                	if (entries.length<=1) continue;
                	try {
                		Integer.parseInt(entries[1]);
                	} catch (NumberFormatException e){
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int i = 2;
                	int numEntries = 0;
                	while (i < entries.length-1){
                        try{
                		te.rtt[numEntries] = getValue(entries[i]);
                        } catch (NumberFormatException e){
                            // failure during traceroute
                            te.router[te.numRouters++] = getRouterIp(entries[i]);
                            break;
                        }
                		if (te.rtt[numEntries++]<0) i++;
                		else i+=2;
                		if (numEntries==te.rtt.length) break;
                		
                	}
                	
                	te.router[te.numRouters++] = getRouterIp(entries[entries.length-1]);  
                	data.add(te);
                	if (data.size()>=30) break;
                }

                // then destroy
                if (p!=null) p.destroy();         
                
            } else if (Util.isLinux || Util.isOSX) {
            	boolean isV6 = Util.isV6Address(dest);
            	String v6Text = "";
            	String commandText = "";
				if (isV6){
					//v6Text = "-A inet6 ";
					commandText = "traceroute6";
				}else{
					commandText = "traceroute";
				}
				
            	p = Runtime.getRuntime().exec(commandText+" -n -w 3 " + v6Text + dest);

                in = new BufferedReader(new InputStreamReader(
                            p.getInputStream()));
                

                while (in!=null && (line = in.readLine()) != null) {                	
                    // parse data
                	
                	String[] entries = line.split("[\\s]+");
                	//System.out.println(Arrays.toString(entries));
                	if (entries.length<=1) continue;
                	int index = 0;
                	while (entries[index].equals("")) index++;
                	try {
                		Integer.parseInt(entries[index]);
                	} catch (NumberFormatException e){
                		// not an entry we care about
                		continue;
                	}
                	TraceEntry te = new TraceEntry();
                	int rttCount = 0;
                	for (int i = index+1; i < entries.length; i++){
//                		System.out.println("i:"+i);
                		if (entries[i].contains("*")){
                			te.router[te.numRouters++] = getRouterIp(entries[i]);
                			te.rtt[rttCount++] = -1;
                			//System.out.println("Found asterisk!");
                		}
                		else {
                			if(isValidIP(entries[i])){
                				// if code reaches this point, this is an ip
                				te.router[te.numRouters++] = getRouterIp(entries[i]);
                				//System.out.println("Found router!");
                			}
                			else{
                				// not an ip, so it must be a value
                				try {
	                				te.rtt[rttCount] = Float.parseFloat(entries[i++]);
	                				rttCount++;
                				} catch (NumberFormatException e ){
                					
                					continue; // ignore the garbage
                				}
                				
                				//System.out.println("Found rtt!");
                				// i++ skips the "ms" entry
                				
                			}
                		}
                	}
                 
                	data.add(te);
                	if (data.size()>30) break;
                }

                // then destroy
                if (p!=null) p.destroy();
            }
            
            if (trr.recordInDB){
	            tr.dest = dest;
	            tr.source = Util.getPublicIpAddress();
	            tr.entries = data;
	            MeasurementStatistics.getInstance().addTraceRouteResult(tr);
	            synchronized(lossSync){
	            	double dropped = 0;
	            	double count = 0;
	            	for (TraceEntry te : data){
	            		int myDropped = 0;
	            		boolean valid = false;
	            		for (int i = 0; i < te.rtt.length; i++){
	            			if (te.rtt[i]>0) valid = true;
	            			else myDropped++;
	            		} // end each entry
	            		if (valid) {
	            			dropped+=myDropped;
	            			count+=te.rtt.length;
	            		}
	            	} // end all tr data
	            	if (count>0){
		            	lossSum+=dropped/count;
		            	lossCount++;
	            	}	
	            } // end sync
	            
            }
            if (in!=null) in.close();
            if (p!=null) p.destroy();            
        } catch (IOException e) {
        	if (p!=null)p.destroy();
            //e.printStackTrace();
        } catch (Exception e){
        	try {
				if (in !=null ) in.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	if (p!=null) p.destroy();
            if (active){

            }
        }
			
		} // end while active

	}

	public static String getRouterIp(String maybeIp) {
		if(maybeIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
			// if code reaches this point, this is an ip	
			MeasurementStatistics.getInstance().addRouterForLookup(maybeIp);
			return maybeIp;
			//System.out.println("Found router!");
		} else if (maybeIp.matches("\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))" +
				"|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2" +
				"[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}(" +
				"(:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3}" +
				")?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})" +
				"{0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}" +
				")){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}" +
				"){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})" +
				"?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((" +
				":[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)" +
				"|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|" +
				"[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4})" +
				"{1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}))" +
				"{3})))(%.+)?\\s*")){
			MeasurementStatistics.getInstance().addRouterForLookup(maybeIp);
			return maybeIp;
		}
		MeasurementStatistics.getInstance().addRouterForLookup("unknown");
		return "unknown";
	}
	
	public static boolean isValidIP(String maybeIp){
		if(maybeIp.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")){
			// if code reaches this point, this is an ip	
			return true;
			//System.out.println("Found router!");
		} else if (maybeIp.matches("\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))" +
				"|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2" +
				"[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}(" +
				"(:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3}" +
				")?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4})" +
				"{0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}" +
				")){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}" +
				"){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})" +
				"?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((" +
				":[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}" +
				"((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)" +
				"|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|" +
				"[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4})" +
				"{1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2}))" +
				"{3})))(%.+)?\\s*")){
			return true;
		}
		return false;
	}

	private float getValue(String val) {
		if (val.contains("<"))return 0;
		else if (val.contains("*")) return -1;
    	else return Float.parseFloat(val);
	}

	public void addIp(String dest, boolean recordInDB, boolean sameCluster) {
		if (!sameCluster){
			synchronized(pendingRuns){
				pendingRuns.add(new TraceRouteRun(dest, recordInDB));
				pendingRuns.notifyAll();
			}
		} else {
			synchronized(pendingRuns){
				pendingRunsPrio.add(new TraceRouteRun(dest, recordInDB));

				pendingRuns.notifyAll();
			}
		}
		
	}

	/**
	 * total loss rates, 
	 * TODO: for each client, compute the loss rates
	 * @return
	 */
	public double getLossSample() {
		synchronized(lossSync){
			double toReturn = lossCount==0?0:lossSum/lossCount;
			lossSum = 0;
			lossCount = 0;
			return toReturn;
		}
	}

}
