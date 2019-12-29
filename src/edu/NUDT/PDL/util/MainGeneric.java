package edu.NUDT.PDL.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import edu.NUDT.PDL.RatingMessages.NodesPair;
import edu.NUDT.PDL.main.RankingLoader;
import edu.NUDT.PDL.measurement.TraceRouteRunner;
import edu.NUDT.PDL.measurement.TraceEntry;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;

public class MainGeneric {

	static Log log = new Log( MainGeneric.class);
	/**
	 * ping type
	 */
	public static int pingType=Integer.parseInt(Config.getConfigProps().getProperty("pingType", "0"));; //0 rtt, 1 loss, 2 bandwidth
	
	//public static ExecutorService execMain = Executors.newFixedThreadPool(15);
	
	public static ExecutorService execMain = Executors.newFixedThreadPool(10);
	
	public static void createThread(String string, Runnable oem) {
		Thread t = new Thread(oem);
		t.setName(string);
		//t.setDaemon(true);
		t.start();

	}

	public static void printAddress(Set<AddressIF> nodes){
		Iterator<AddressIF> ier = nodes.iterator();
		while(ier.hasNext()){
			NetAddress tmp=(NetAddress)ier.next();
			log.info("$: host: "+tmp.getHostname()+", ip: "+NetUtil.byteIPAddrToString(tmp.getByteIPAddr()));
		}
		
	}
	
	
	
	
	/**
	 * parse all nodes
	 */
	public static List<String> parseAllNodes(String AllNodes, int port){
		
		List<String> AllAliveNodes = new ArrayList<String>(100);
	try {
		BufferedReader rf= new BufferedReader(new FileReader(new File(AllNodes)));
		if (rf == null) {
			//System.err.println("ERROR, empty file  !!");
			return AllAliveNodes ;
		} else {
			String cuLine;

			while (true) {

				cuLine = rf.readLine();
				
				//System.out.println("@: CurLine "+cuLine);
				if (cuLine == null) {
					//System.out.println("@: Panic ");
					break;
				}
				if (cuLine.startsWith("#")) {
					continue;
				}
				//trim the empty header
				cuLine=trimEmpty(cuLine);
				if(cuLine.isEmpty()||cuLine==null){
					continue;
				}else{
				log.debug("$: current: "+cuLine);
				//AddressIF addr=AddressFactory.createUnresolved(cuLine,port);
				//log.info("$: real: "+addr.toString());
				AllAliveNodes.add(cuLine);
				}
				}
		}
		
		rf.close();
		PUtil.gc();
		
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
	//int len1=Math.min(Math.round(AllAliveNodes.size()*0.1f),10);
	//Collections.shuffle(AllAliveNodes);
	return AllAliveNodes;
	}
	
	public static String trimEmpty(String s){
		String s1=s.trim();
		while(s1.startsWith("[ (\\s\t]")){
			s1=s1.substring(1);
		}
		while(s1.endsWith("[ )\\s\t]")){
			s1=s1.substring(0,s1.length());
		}
		return s1;
	}
	
	/**
	 * translate the DNS name 
	 * @param AllNodes
	 * @param outFile
	 */
	public static void DNS2IPAddress(String AllNodes,final String outFile){
		int port =80;
		List<String> allnodes=parseAllNodes(AllNodes,port);
		AddressFactory.createResolved(allnodes,
				port, new CB1<Map<String, AddressIF>>() {
			
			protected void cb(CBResult result,
					Map<String, AddressIF> addrMap) {
			switch(result.state){
			case OK:{
				
				
				
				if(addrMap!=null&&!addrMap.isEmpty()){
					
					try {
						PrintWriter TestCaseStream = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(
								outFile),true)));
						
						Iterator<String> ier = addrMap.keySet().iterator();	
						while(ier.hasNext()){
							String dnsName=ier.next();
							String IP=NetUtil.byteIPAddrToString(((NetAddress)addrMap.get(dnsName)).getByteIPAddr());
							TestCaseStream.append(dnsName+"\t"+IP+"\n");
							TestCaseStream.flush();
						}
						TestCaseStream.close();
						
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					

				}				
				break;
			}
			case TIMEOUT:
			case ERROR:{
				log.warn("can not parse the address");
				break;
			}
			
			}
			}		
		}
	);
	}
	
	
	
	
	public static boolean isWindows() {
		String osName = System.getProperty("os.name");
		if (osName.indexOf("Windows") >= 0 || osName.indexOf("windows") >= 0) {
			return true;
		} else {
			return false;
		}

	}

	
	
	public static boolean isLinux() {
		return !isWindows();
	}

	
	//Ericfu
	public static boolean
    validateAnIpAddressWithRegularExpression(String iPaddress){
      Pattern IP_PATTERN =
              Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    return IP_PATTERN.matcher(iPaddress).matches();
	}
	
	public static  void removeNonIP(List<String> nsList){
		if(nsList!=null&&!nsList.isEmpty()){
			Iterator<String> ier = nsList.iterator();
			while(ier.hasNext()){
				String tmp = ier.next();
				if(!validateAnIpAddressWithRegularExpression(tmp)){
					ier.remove();
				}
			}
		}
	}
	
	/**
	 * 
	 * ping timeout, 8 seconds in default
	 */
	
	
	
	/**
	 * ping a list of nodes
	 * @param list
	 */
	public static List<NodesPair>  pingTargetedNodes(final AddressIF me, final List list){
		
				// TODO Auto-generated method stub
				List<NodesPair> cachedMeasurements=new ArrayList<NodesPair>(5);
				
				Iterator<AddressIF> ier = list.iterator();
				try {
					
					while(ier.hasNext()){
						AddressIF target=ier.next();
						String ip=target.getHostname();
						if(ip==null||ip.isEmpty()){
							ip=NetUtil.byteIPAddrToString(((NetAddress)target).getByteIPAddr());
						}
						
						double rtt=MainGeneric.doPingDirect(ip);
						if(rtt>=0){
							cachedMeasurements.add(new NodesPair(me,target,rtt));	
						}else{
							log.debug("error incurs");
						}
						}
										
				} catch (Exception e) {
					
					e.printStackTrace();
				}
		return cachedMeasurements;
}
	
	
	public static double doPingDirect(final String ipToPing) {

		// not suitable for the KNN search process!,
		// [java] ER 1255106574621 EL :
		// java.util.ConcurrentModificationException
		// [java] java.lang.Exception: Stack trace
		// [java] at java.lang.Thread.dumpStack(Thread.java:1206)
		// [java] at edu.harvard.syrah.prp.Log.error(Log.java:181)
		// [java] at
		// edu.harvard.syrah.sbon.async.EL.handleSelectCallbacks(EL.java:836)
		// [java] at edu.harvard.syrah.sbon.async.EL.main(EL.java:434)
		// [java] at edu.NUDT.pdl.Nina.Ninaloader.main(Ninaloader.java:408)

				double rtt = -1;

				 //System.out.println("ipToPing: "+ipToPing);
				try {
					/*if (isWindows()) {

						Process p;

						p = Runtime.getRuntime().exec("ping -w 8000 -n 3 "+ipToPing);
						//System.out.println("@@");

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));

						String line;

						int notReadyCount = 0;
						boolean notDone = true;

						while (notDone) {
							while (!in.ready()) {
								try {
									Thread.sleep(1 * 1000);
									notReadyCount++;
									if (notReadyCount > 4) {
										notDone = false;
										//System.out.println("@@");
										break;
									}
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
							notReadyCount = 0;
							if ((line = in.readLine()) == null) {
								notDone = false;
								break;
							}
							//System.out.println(line);
							// find RTT
							if (line.contains("Average =")
									|| line.contains("平均 =")) {
								int beginIndex = line.lastIndexOf("= ") + 2;
								int endIndex = line.lastIndexOf("ms");
								String val = line.substring(beginIndex,
										endIndex);
								rtt = Double.parseDouble(val);
								notDone = false;
								break;
							}

						}
						// System.out.println("@@: "+rtt);
						// then destroy
						//close the opened file
						in.close();
						p.destroy();

					} else if (isLinux()) {*/
						//System.out.println("Is NonWindows!");
						//timout in 8 seconds, as default
						
						Process p;
						p = Runtime.getRuntime().exec(
								"ping -w 8 -i 0.21 -q -c 3 " + ipToPing);

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));
						String line;

						while ((line = in.readLine()) != null) {
							// find RTT
							if (line.contains("rtt")
									|| line.contains("round-trip")) {
								String[] vals = line.split("/");
								rtt = Double.parseDouble(vals[4]);

								break;
							}
						}

						// then destroy
						in.close();
						p.destroy();
					//}
				} catch (IOException e) {
					System.err.println(e.toString());

				}
				return rtt;
	}
	
	
	/**
	 * traceroute result
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doTraceroute(final String ipToPing, final CB1<double[]> cbDone){
		//start the traceroute
		
		/*
				execMain.execute( new Runnable() {

					public void run() {*/
						double hop = -1;

						// System.out.println("ipToPing: "+ipToPing);
						try {
							/*if (isWindows()) {

								Process p;

								p = Runtime.getRuntime().exec("ping -w 8000 -n 5 "+ipToPing);
								// System.out.println("@@");

								BufferedReader in = new BufferedReader(
										new InputStreamReader(p.getInputStream()));

								String line;

								int notReadyCount = 0;
								boolean notDone = true;

								while (notDone) {
									while (!in.ready()) {
										try {
											Thread.sleep(1 * 1000);
											notReadyCount++;
											if (notReadyCount > 4) {
												notDone = false;
												//System.out.println("@@");
												break;
											}
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}

									}
									notReadyCount = 0;
									if ((line = in.readLine()) == null) {
										notDone = false;
										break;
									}
									// find RTT
									if (line.contains("Average =")
											|| line.contains("平锟斤拷 =")) {
										int beginIndex = line.lastIndexOf("= ") + 2;
										int endIndex = line.lastIndexOf("ms");
										String val = line.substring(beginIndex,
												endIndex);
										rtt = Double.parseDouble(val);
										notDone = false;
										break;
									}

								}
								// System.out.println("@@: "+rtt);
								// then destroy
								p.destroy();
								in.close();

							} else if (isLinux()) {*/
								//System.out.println("Is NonWindows!");
							
							
							
							boolean isV6 = Util.isV6Address(ipToPing);
			            	String v6Text = "";
			            	String commandText = "";
							if (isV6){
								//v6Text = "-A inet6 ";
								commandText = "traceroute6";
							}else{
								commandText = "traceroute";
							}
							
			            	Process p = Runtime.getRuntime().exec(commandText+" -n -w 3 " + v6Text + ipToPing);

			            	BufferedReader in = new BufferedReader(new InputStreamReader(
			                            p.getInputStream()));
			                String line;
			                ArrayList<TraceEntry> data = new ArrayList<TraceEntry>();
			                
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
//			                		System.out.println("i:"+i);
			                		if (entries[i].contains("*")){
			                			te.router[te.numRouters++] = TraceRouteRunner.getRouterIp(entries[i]);
			                			te.rtt[rttCount++] = -1;
			                			//System.out.println("Found asterisk!");
			                		}
			                		else {
			                			if(TraceRouteRunner.isValidIP(entries[i])){
			                				// if code reaches this point, this is an ip
			                				te.router[te.numRouters++] = TraceRouteRunner.getRouterIp(entries[i]);
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
							
							hop=data.size();
							//release
							data.clear();
							data=null;
							
							
							
							
							

								// then destroy
								p.destroy();
								in.close();
							//}
						} catch (IOException e) {
							log.warn(e.toString());

						}
						double[] results={hop};
						cbDone.call(CBResult.OK(), results);
		
		
	}
	
	
	/**
	 * 
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doPing(final String ipToPing, final CB1<double[]> cbDone) {
		/**
		 * rtt
		 */
		if(pingType==0){
			doPing_RTT(ipToPing, cbDone);
		}
		/**
		 * loss	
		 */
		else if(pingType==1){
			doPing_loss(ipToPing, cbDone);
		}/**
		  bandwidth
		**/
		else{
			//doPing_bandwidth(ipToPing, cbDone);
			doPing_bandwidth_UDP(ipToPing, cbDone);
		}
	}
	
	//iterate and measure
	public static void collectMeasurements2Nodes(List<String> ips,BufferedWriter bw){
		Iterator<String> ier = ips.iterator();
		while(ier.hasNext()){			
			try {
				collectMeasurements2OneNode(ier.next(), bw);
				//Thread.sleep(1 * 50);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * write the results
	 * @param ipToPing
	 * @param bw
	 */
	public static void collectMeasurements2OneNode(final String ipToPing,final BufferedWriter bw){
		//rtt
		doPing_RTT(ipToPing, 
				new CB1<double[]>(){
					
					protected void cb(CBResult result, double[] arg1) {
						switch(result.state){
						case OK:{
							try {
								bw.append(Util.currentGMTTime()+" "+ipToPing+",rtt: "+arg1[0]);
								bw.newLine();
								bw.flush();
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							break;
						}
						case ERROR:
						case TIMEOUT:{
							break;
						}
						
						}//
					
					
					}
					}		
		);
		
		doPing_loss(ipToPing, 
				new CB1<double[]>(){

					
					protected void cb(CBResult result, double[] arg1) {
						switch(result.state){
						case OK:{
							try {
								bw.append(Util.currentGMTTime()+" "+ipToPing+",loss: "+arg1[0]);
								bw.newLine();
								bw.flush();
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						}
						case ERROR:
						case TIMEOUT:{
							break;
						}
						
						}//
					
					}
					}		
		);
		
		
		doTraceroute(ipToPing, 
				new CB1<double[]>(){
					
					protected void cb(CBResult result, double[] arg1) {
						switch(result.state){
						case OK:{
							try {
								bw.append(Util.currentGMTTime()+" "+ipToPing+",hop: "+arg1[0]);
								bw.newLine();
								bw.flush();
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							break;
						}
						case ERROR:
						case TIMEOUT:{
							break;
						}
						
						}//
					
					
					}
					}		
		);
		
		if(false){
		doPing_bandwidth_UDP(ipToPing, 
				new CB1<double[]>(){

					
					protected void cb(CBResult result, double[] arg1) {
						switch(result.state){
						case OK:{
							try {
								bw.append(Util.currentGMTTime()+" "+ipToPing+",bw: "+arg1[0]);
								bw.newLine();
								bw.append(Util.currentGMTTime()+" "+ipToPing+",reorder: "+arg1[1]);
								bw.newLine();
								bw.flush();
								
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
													
							break;
						}
						case ERROR:
						case TIMEOUT:{
							break;
						}
						
						}//
					
					
					}
					}		
		);
		}
		
	}
	
	
	/**
	 * synchronous ping based on windows or linux primitives
	 */
	public static void doPing1(final String ipToPing, final CB1<Double> cbDone) {

		// not suitable for the KNN search process!,
		// [java] ER 1255106574621 EL :
		// java.util.ConcurrentModificationException
		// [java] java.lang.Exception: Stack trace
		// [java] at java.lang.Thread.dumpStack(Thread.java:1206)
		// [java] at edu.harvard.syrah.prp.Log.error(Log.java:181)
		// [java] at
		// edu.harvard.syrah.sbon.async.EL.handleSelectCallbacks(EL.java:836)
		// [java] at edu.harvard.syrah.sbon.async.EL.main(EL.java:434)
		// [java] at edu.NUDT.pdl.Nina.Ninaloader.main(Ninaloader.java:408)
/*
		execMain.execute( new Runnable() {

			public void run() {*/
				double rtt = -1;

				// System.out.println("ipToPing: "+ipToPing);
				try {
					/*if (isWindows()) {

						Process p;

						p = Runtime.getRuntime().exec("ping -w 8000 -n 5 "+ipToPing);
						// System.out.println("@@");

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));

						String line;

						int notReadyCount = 0;
						boolean notDone = true;

						while (notDone) {
							while (!in.ready()) {
								try {
									Thread.sleep(1 * 1000);
									notReadyCount++;
									if (notReadyCount > 4) {
										notDone = false;
										//System.out.println("@@");
										break;
									}
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
							notReadyCount = 0;
							if ((line = in.readLine()) == null) {
								notDone = false;
								break;
							}
							// find RTT
							if (line.contains("Average =")
									|| line.contains("平锟斤拷 =")) {
								int beginIndex = line.lastIndexOf("= ") + 2;
								int endIndex = line.lastIndexOf("ms");
								String val = line.substring(beginIndex,
										endIndex);
								rtt = Double.parseDouble(val);
								notDone = false;
								break;
							}

						}
						// System.out.println("@@: "+rtt);
						// then destroy
						p.destroy();
						in.close();

					} else if (isLinux()) {*/
						//System.out.println("Is NonWindows!");

						Process p;
						p = Runtime.getRuntime().exec(
								"ping -w 8 -i 0.21 -q -c 3 " + ipToPing);

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));
						String line;

						while ((line = in.readLine()) != null) {
							// find RTT
							if (line.contains("rtt")
									|| line.contains("round-trip")) {
								String[] vals = line.split("/");
								rtt = Double.parseDouble(vals[4]);

								break;
							}
						}

						// then destroy
						p.destroy();
						in.close();
					//}
				} catch (IOException e) {
					log.warn(e.toString());

				}

				cbDone.call(CBResult.OK(), rtt);

			/*}

		});
*/
	}

	/**
	 * RTT measurement
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doPing_RTT(final String ipToPing, final CB1<double[]> cbDone) {

	
/*
		execMain.execute( new Runnable() {

			public void run() {*/
				double rtt = -1;

				// System.out.println("ipToPing: "+ipToPing);
				try {
					/*if (isWindows()) {

						Process p;

						p = Runtime.getRuntime().exec("ping -w 8000 -n 5 "+ipToPing);
						// System.out.println("@@");

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));

						String line;

						int notReadyCount = 0;
						boolean notDone = true;

						while (notDone) {
							while (!in.ready()) {
								try {
									Thread.sleep(1 * 1000);
									notReadyCount++;
									if (notReadyCount > 4) {
										notDone = false;
										//System.out.println("@@");
										break;
									}
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
							notReadyCount = 0;
							if ((line = in.readLine()) == null) {
								notDone = false;
								break;
							}
							// find RTT
							if (line.contains("Average =")
									|| line.contains("平锟斤拷 =")) {
								int beginIndex = line.lastIndexOf("= ") + 2;
								int endIndex = line.lastIndexOf("ms");
								String val = line.substring(beginIndex,
										endIndex);
								rtt = Double.parseDouble(val);
								notDone = false;
								break;
							}

						}
						// System.out.println("@@: "+rtt);
						// then destroy
						p.destroy();
						in.close();

					} else if (isLinux()) {*/
						//System.out.println("Is NonWindows!");

						Process p;
						p = Runtime.getRuntime().exec(
								"ping -w 8 -i 0.21 -q -c 3 " + ipToPing);

						BufferedReader in = new BufferedReader(
								new InputStreamReader(p.getInputStream()));
						String line;

						while ((line = in.readLine()) != null) {
							// find RTT
							if (line.contains("rtt")
									|| line.contains("round-trip")) {
								String[] vals = line.split("/");
								rtt = Double.parseDouble(vals[4]);

								break;
							}
						}

						// then destroy
						p.destroy();
						in.close();
					//}
				} catch (IOException e) {
					log.warn(e.toString());

				}
				double[] results={rtt};
				cbDone.call(CBResult.OK(), results);

			/*}

		});
*/
	}
	
	/**
	 * loss ping
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doPing_loss(final String ipToPing, final CB1<double[]> cbDone) {
		double loss = -1;

		int total=20;
		double loss1;
		double shift=2;
		
		int from,to;
		// System.out.println("ipToPing: "+ipToPing);
		try {
			if (MainGeneric.isWindows()) {

				Process p;

				p = Runtime.getRuntime().exec("ping -n 20 -l 1024 "+ipToPing);
				 //System.out.println("@@");

				BufferedReader in = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				String line;

				int notReadyCount = 0;
				boolean notDone = true;

				while (notDone) {
					while (!in.ready()) {
						try {
							Thread.sleep(1 * 1000);
							notReadyCount++;
							if (notReadyCount > 4) {
								notDone = false;
								//System.out.println("@@");
								break;
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					notReadyCount = 0;
					if ((line = in.readLine()) == null) {
						notDone = false;
						break;
					}
					// find RTT
					if (line.contains("%")) {
						//System.out.println(line);
						
						int beginIndex = line.lastIndexOf("=") + 5;
						int endIndex = line.lastIndexOf("%");
						
						String val = line.substring(beginIndex,
								endIndex);
						loss1 = Double.parseDouble(val);
						
						
						
						loss=loss1+shift;
						
						notDone = false;
						break;
					}

				}
				// System.out.println("@@: "+rtt);
				// then destroy
				p.destroy();
				in.close();

			} else if (MainGeneric.isLinux()) {
				//System.out.println("Is NonWindows!");

				Process p;
				p = Runtime.getRuntime().exec(
						"ping -s 1024 -i 0.3 -q -c 20 " + ipToPing);

				BufferedReader in = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				String line;

				while ((line = in.readLine()) != null) {
					// find RTT
					if (line.contains("loss")) {
						
						if(line.contains("%")){
						//String[] vals = line.split("[ ]");
						//for linux
						//String percentage=vals[5].substring(0,vals[5].lastIndexOf("%"));	
						
						to=line.lastIndexOf("%");
						String subLine = line.substring(0, to+1);
						
						from=subLine.lastIndexOf(" ")+1;
						to=subLine.lastIndexOf("%");
						
						String percentage=subLine.substring(from, to);
							
						loss = Double.parseDouble(percentage);
						loss+=shift;
						}else{
							loss=-1;
						}
						break;
					}
				}

				// then destroy
				p.destroy();
				in.close();
			}
			
		} catch (IOException e) {
			log.warn(e.toString());

		}
		double[] results={(loss+0.0)/total};
		cbDone.call(CBResult.OK(), results);
	}
	
	/**
	 * capacity estimation by packet dispersion,
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doPing_bandwidth_UDP(final String ipToPing, final CB1<double[]> cbDone){
		
		final int packetsize=1024;
		final int packetNum=20;
		
		if(RankingLoader.getMeasurementComm()!=null){
			AddressFactory.createResolved(ipToPing,RankingLoader.MeasureCOMM_PORT , new CB1<AddressIF>(){
				protected void cb(CBResult result, AddressIF remoteNode) {
					// TODO Auto-generated method stub
				switch(result.state){
				case OK:{
					RankingLoader.getMeasurementComm().sendProbeTrains(remoteNode,
							packetsize, packetNum, cbDone);
					break;
				}
				case TIMEOUT:
				case ERROR:{
					log.warn("measurement failed due to unresovled address: "+ipToPing);
					break;
				}	
				}
				}		
			});
			
		}
		
	}
	/**
	 * TODO: bandwidth measurement
	 * @param ipToPing
	 * @param cbDone
	 */
	public static void doPing_bandwidth(final String ipToPing, final CB1<Double> cbDone){
	
		long timeStart=System.currentTimeMillis();
		long timeEnd=0;
		double interval=1;
		double totalPacket=5;
		double rtt=1;
		double packetSize=1024;
		try {
			if (isWindows()) {

				Process p;
				interval=1;
				p = Runtime.getRuntime().exec("ping -n 3 -l 1024 "+ipToPing); //-w ms
				// System.out.println("@@");
				//timeStart=System.currentTimeMillis();
				BufferedReader in = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				String line;

				int notReadyCount = 0;
				boolean notDone = true;

				while (notDone) {
					while (!in.ready()) {
						try {
							Thread.sleep(1 * 1000);
							notReadyCount++;
							if (notReadyCount > 4) {
								notDone = false;
								//System.out.println("@@");
								break;
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					notReadyCount = 0;
					if ((line = in.readLine()) == null) {
						notDone = false;
						break;
					}
					// find RTT
					if (line.contains("Average =")
							|| line.contains("平均")) {
						
						
						
						int beginIndex = line.lastIndexOf("= ") + 2;
						int endIndex = line.lastIndexOf("ms");
						String val = line.substring(beginIndex,
								endIndex);
						rtt = Double.parseDouble(val);
						
						timeEnd=System.currentTimeMillis();
						notDone = false;
						break;
					}

				}
				// System.out.println("@@: "+rtt);
				// then destroy
				p.destroy();
				in.close();

			} else if (isLinux()) {
				//System.out.println("Is NonWindows!");
				interval=0.21;
				Process p;
				p = Runtime.getRuntime().exec(
						"ping -s 1024 -i 0.21 -q -c 3 " + ipToPing);
				//timeStart=System.currentTimeMillis();
				BufferedReader in = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				String line;

				while ((line = in.readLine()) != null) {
										
					// find RTT
					if (line.contains("rtt")
							|| line.contains("round-trip")) {
						timeEnd=System.currentTimeMillis();
						String[] vals = line.split("/");
						rtt = Double.parseDouble(vals[4]);

						break;
					}
				}

				// then destroy
				p.destroy();
				in.close();
			}
		} catch (IOException e) {
			log.warn(e.toString());

		}
		log.debug("d0: "+(timeEnd-timeStart)+" d1: "+rtt+" d2: "+(totalPacket-1)*interval);
		
		double estimatedElapsedTime=(timeEnd-timeStart-rtt-1000*(totalPacket-1)*interval);
		
		//TODO: the value estimatedElapsedTime may be negative!, maybe the rtt is too large
		if( estimatedElapsedTime==0){
			cbDone.call(CBResult.OK(), -1.0);
			return;
		}else{
		double capacityEstimator=
			(1000.0*8*packetSize)/Math.abs(estimatedElapsedTime); //bps
		
			cbDone.call(CBResult.OK(), capacityEstimator);
		}
	}
	
	/**
	 * convert the System.currentTimeMillis() to date
	 * @param curSerialNumber
	 * @return
	 */
	public static String currentMilliSeconds2Date(long curSerialNumber){
		DateFormat formatter=new 
		SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
	
		Calendar calendat=Calendar.getInstance();
		calendat.setTimeInMillis(curSerialNumber);
		return formatter.format(calendat.getTime());
	
	}
	
	
	
	public static double ifSameSubsetThenDo(final AddressIF nodeA, final AddressIF nodeB){
		double latency=-1;
		Random r=new Random(System.currentTimeMillis());
		double eRTT=r.nextFloat()*3;
		String ANode=NetUtil.byteIPAddrToString(((NetAddress)nodeA).getByteIPAddr());
		String BNode=NetUtil.byteIPAddrToString(((NetAddress)nodeB).getByteIPAddr());
		
		String[] A=ANode.split("[., ]");
		String[] B=BNode.split("[., ]");
		
		//System.out.println(ANode+"\n"+BNode+"\n "+A.length+", "+B.length);
		if(A.length==B.length){
			//IPv4
			boolean same=true;
			//e.g. 192.168.1.X
			for(int i=0;i<3;i++){
				//System.out.println(A[i]+" <> "+B[i]);
				if(!A[i].equalsIgnoreCase(B[i])){
					same=false;
				}
			}
			
			if(same){
				return eRTT;
			}else{
				return latency;
			}
		}
		return latency;
		
	}
	
	public static void main(String[] args) {

		/*String ipToPing = args[0];*/

		// MainGeneric test=new MainGeneric ();
/*		MainGeneric.doPing(ipToPing, new CB1<Double>() {

			@Override
			protected void cb(CBResult result, Double lat) {
				// TODO Auto-generated method stub
				System.out.println("RTT: " + lat);
			}

		});*/
	/*	int port=55509;
		AddressIF A=AddressFactory.createUnresolved("192.168.1.7", port);
		AddressIF B=AddressFactory.createUnresolved("192.168.1.8", port);
		AddressIF C=AddressFactory.createUnresolved("192.168.2.14", port);
		
		List targets=new ArrayList(2);
		targets.add(A);
		targets.add(B);
		targets.add(C);
		
		AddressIF me=AddressFactory.createUnresolved("202.197.22.56", port);
		List<NodesPair>  result=pingTargetedNodes(me,targets);
		Iterator<NodesPair> ier = result.iterator();
		while(ier.hasNext()){
			System.out.println(ier.next().toString());
		}
		
		*/
		/*
		System.out.println(ifSameSubsetThenDo(A,B));
		System.out.println(ifSameSubsetThenDo(C,B));
		
		//double rtt=doPingDirect("192.168.1.85");
		//System.out.println(rtt);
		Set<AddressIF> nodes= parseAllNodes(args[0],80);
		 printAddress(nodes);
/*		String cuLine="192.168.1.85";
		int port=80;
		AddressIF addr=AddressFactory.createUnresolved(cuLine,port);
		log.info(addr.toString());*/
		
		
		//pase DNS
		//input name, output name
	/*	if(args.length<2){
			usage();
		}
		DNS2IPAddress(args[0],args[1]);*/
		
		
		doPing_loss("66.249.89.104", new CB1<double[]>(){

			@Override
			protected void cb(CBResult result, double[] arg1) {
				// TODO Auto-generated method stub
				System.out.println(arg1[0]);
			}
			
		});
/*		boolean useBW=true;
		if(useBW){
		doPing_bandwidth("66.249.89.104", new CB1<Double>(){

			@Override
			protected void cb(CBResult result, Double arg1) {
				// TODO Auto-generated method stub
				switch(result.state){
				case OK:{
					System.out.println("bandwidth: "+arg1.doubleValue());
					break;
				}
				case TIMEOUT:
				case ERROR:{
					System.err.println("timeout!");
					break;
				}
				}
			}
			
		});
		}else{
			doPing_loss("66.249.89.104", new CB1<Double>(){

				@Override
				protected void cb(CBResult result, Double arg1) {
					// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
						System.out.println("loss: "+arg1.doubleValue());
						break;
					}
					case TIMEOUT:
					case ERROR:{
						System.err.println("timeout!");
						break;
					}
					}
				}
				
			});
		}*/
		
		
		
	}
	
	
	public static void usage(){
		log.info("MainGeneric: DNSAddrFile, outPutFile");
		
	}
	
}
