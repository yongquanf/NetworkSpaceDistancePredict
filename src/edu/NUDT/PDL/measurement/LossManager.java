package edu.NUDT.PDL.measurement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.PDL.util.MainGeneric;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;



/**
 * sending out 100 1KB ICMP-ECHO probes along each link with an 
 * inter-probe spacing of 2 seconds
 * iPlane
 * @author Administrator
 *
 */
public class LossManager {

	private static Log log=new Log(LossManager.class);
	
	public static ExecutorService execMain = Executors.newFixedThreadPool(10);
	
	
	
	/**
	 * use the ping method to find the loss percentage of 100 measurements
	 * @param target
	 * @param cbDone
	 */
	public void doPing(final AddressIF target, final CB1<Double> cbDone) {				
		execMain.execute(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				String ipToPing=NetUtil.byteIPAddrToString(((NetAddress)target).getByteIPAddr());
				doPing(ipToPing,cbDone);
			}						
		});	
		
	}

		
	public static void doPing(final String ipToPing, final CB1<Double> cbDone) {
		double loss = -1;

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
						loss = Double.parseDouble(val)*0.01;
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
						"ping -s 1024 -i 2 -q -c 20 " + ipToPing);

				BufferedReader in = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				String line;

				while ((line = in.readLine()) != null) {
					// find RTT
					if (line.contains("loss")
							|| line.contains("packet")) {
						String[] vals = line.split("[ ]");
						//for linux
						String percentage=vals[5].substring(0,vals[5].lastIndexOf("%"));						
						loss = 0.01*Double.parseDouble(percentage);

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
		
		cbDone.call(CBResult.OK(), loss);
	}
	
	/**
	 * main function
	 * @param args
	 */
	public static void main(String[] args){
		
		EL.set(new EL());
		
		String ipToPing="www.nudt.edu.cn";
		doPing(ipToPing, new CB1<Double>(){

			@Override
			protected void cb(CBResult result, Double arg1) {
				// TODO Auto-generated method stub
			
				System.out.println("loss: "+arg1.doubleValue());
			}			
			
		});
		

		//EL.get().exit();
		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}
		log.main("Shutdown");
		System.exit(0);
	}
	

}
