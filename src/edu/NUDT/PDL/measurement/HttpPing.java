package edu.NUDT.PDL.measurement;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.http.HTTPCB;
import edu.harvard.syrah.sbon.async.comm.http.HTTPComm;
import edu.harvard.syrah.sbon.async.comm.http.HTTPCommIF;
/**
 * TODO, construct the http ping process
 * @author Administrator
 *
 */
public class HttpPing {

	private static Log log=new Log(HttpPing.class);
	
	//http communicator
	HTTPCommIF httpComm = new HTTPComm();
	
	public void doPing(final AddressIF target, final CB2<Double, Long> cbDone) {
	

	final int timeout = 10000;

	final long starter=System.nanoTime();
	httpComm.sendHTTPRequest(urlAddr(target), null, false,
			new HTTPCB(timeout) {
				protected void cb(CBResult result, Integer resultCode, String httpResponse,
						String httpData) {
					switch (result.state) {
						case OK: {
							final long ender=System.nanoTime();
							cbDone.call(result, (ender-starter)/1000000d, Long.valueOf(starter));
							break;
						}
						case TIMEOUT:
						case ERROR: {
							log.warn(result.toString());
							cbDone.call(result, -1d, Long.valueOf(starter));
							break;
						}
					}
				}
			});
	
	}
	
	
	String urlAddr(AddressIF  addr){
		String header = "http://";
		return header+addr.getHostname();
	}
}
