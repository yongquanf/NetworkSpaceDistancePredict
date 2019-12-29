/**
 * News Project
 *
 * File:         Util.java
 * RCS:          $Id: Util.java,v 1.7 2009/10/24 01:26:35 drc915 Exp $
 * Description:  Util class (see below)
 * Author:       David Choffnes
 *               Northwestern Systems Research Group
 *               Department of Computer Science
 *               Northwestern University
 * Created:      Aug 28, 2006 at 11:21:35 AM
 * Language:     Java
 * Package:      edu.northwestern.news.util
 * Status:       Experimental (Do Not Distribute)
 *
 * (C) Copyright 2006, Northwestern University, all rights reserved.
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
package edu.NUDT.PDL.util;



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Map.Entry;

import edu.NUDT.PDL.cluster.Statistics;
import edu.NUDT.PDL.time.SntpClient;



/**
 * @author David Choffnes &lt;drchoffnes@cs.northwestern.edu&gt;
 *
 * The Util class provides utility functions for the package.
 */
public class Util {
    
	
	 public static final String  OSName = System.getProperty("os.name");
	  
	  public static final boolean isOSX				= OSName.toLowerCase().startsWith("mac os");
	  public static final boolean isLinux			= OSName.equalsIgnoreCase("Linux");
	  public static final boolean isSolaris			= OSName.equalsIgnoreCase("SunOS");
	  public static final boolean isFreeBSD			= OSName.equalsIgnoreCase("FreeBSD");
	  public static final boolean isWindowsXP		= OSName.equalsIgnoreCase("Windows XP");
	  public static final boolean isWindows95		= OSName.equalsIgnoreCase("Windows 95");
	  public static final boolean isWindows98		= OSName.equalsIgnoreCase("Windows 98");
	  public static final boolean isWindows2000		= OSName.equalsIgnoreCase("Windows 2000");
	  public static final boolean isWindowsME		= OSName.equalsIgnoreCase("Windows ME");
	  public static final boolean isWindows9598ME	= isWindows95 || isWindows98 || isWindowsME;
	  public static final boolean isWindows	= OSName.toLowerCase().startsWith("windows");
	  // If it isn't windows or osx, it's most likely an unix flavor
	  public static final boolean isUnix = !isWindows && !isOSX;
	
	
	static BufferedReader in;

	private static Util self;
    
	private static long newsTimeOffset = 0;
    
    public static interface PingResponse {
    	public void response(double rtt);
    }

    
    public static void createThread(String name,final Runnable target) {
		
    	AEThread2 t = 
			new AEThread2(name, true )
			{
				public void
				run()
				{
					//callWithPluginThreadContext( pi, target );
					target.run();
				}
			};
			
		t.start();
		
	}
    
	public static URL getURLInput(String url, int timeout){
		URL u;
	      InputStream is = null;
	      try {
		u = new URL(url);
	       
        URLConnection c = u.openConnection();
        c.setConnectTimeout(timeout);
        c.connect();
        return u;
        
	 } catch (MalformedURLException mue) {

         System.out.println("Ouch - a MalformedURLException happened.");
         mue.printStackTrace();

      } catch (IOException ioe) {

         System.out.println("Oops- an IOException happened.");
         ioe.printStackTrace();

      } finally {


	         try {
	        	 if (is!=null)
	            is.close();
	         } catch (IOException ioe) {
	            // just going to ignore this one
	         }

	      } // end of 'finally' clause
      return null;
	}

    public static String getPublicIpAddress() {
		
		InputStream is = null;
		String s;
		try {
			URL url =getURLInput("http://checkip.dyndns.org/",
					10 * 1000);
			if (url != null)
				is = url.openStream();
			if (is != null) {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				s = br.readLine();
				if (s.length() == 0)
					return null;
				String ip = s.substring(s.indexOf(": ") + 2, s.indexOf('<', s
						.indexOf(":")));
				
				return ip;
			}
			// otherwise, try to get the local address
			// String s = InetAddress.getLocalHost().toString();
			InetAddress addresses[] = InetAddress.getAllByName(InetAddress
					.getLocalHost().getHostName());
			// TODO verify that this always picks the "right" IP address
			s = addresses[addresses.length - 1].getHostAddress();
			if (s.startsWith("192.168"))
				return null;
			
			return s;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;
	}
    public static long currentGMTTime(){
    	return System.currentTimeMillis()-TimeZone.getDefault().getRawOffset();
    } 
    
    public static long currentNEWSTime(){
    	return System.currentTimeMillis()-newsTimeOffset;
    }
    
    public static long fromNEWSTime(long newsTime){
    	return newsTime+newsTimeOffset;
    }
    
    public static void setNEWSTimeOffset( long offset ){
    	newsTimeOffset = offset;
    }
    
    public static long fromGMTTime(long gmtTime){
    	return gmtTime+TimeZone.getDefault().getRawOffset();
    }
    
    
    
    
    /**
	 * 
	 */
	public static void getNEWSTime() {
		/*URL url = null;
		URLConnection urlConn = null;
		InputStream is = null;
		try {
			url = new URL("http://aqua-lab.org/news/time.php");

			urlConn = url.openConnection();
			// Let the run-time system (RTS) know that we want input.
			urlConn.setDoInput(true);
			// No caching, we want the real thing.
			urlConn.setUseCaches(false);
			is = urlConn.getInputStream();
			StringBuffer sb = new StringBuffer();
			byte b[] = new byte[1];
			boolean eof = false;
			int readCount = 0;
			while (!eof) {
				int c = is.read(b);
				if (c < 0)
					break;
				sb.append((char) b[0]);
				readCount++;
				if (readCount > 10000) {
					System.out.println("Something wrong!?");
					break;
				}
			}
			is.close();
			long newsTime = Long.parseLong(sb.toString());
			Util.setNEWSTimeOffset(System.currentTimeMillis()-newsTime*1000);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		try {
			long offset = -1*SntpClient.getOffset("pool.ntp.org");
			Util.setNEWSTimeOffset(offset+TimeZone.getDefault().getRawOffset());
//			System.out.println("Current GMT Time: "+
//					new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date(Util.currentNEWSTime())));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    
    public synchronized static Util getInstance(){

	    	if (self == null){
	    		self = new Util();
	    	}
    	
    	return self;
    }

   

    /**
     * @param newEdge
     * @return
     */
    public static String getClassCSubnet(String ipaddress) {
        // TODO Auto-generated method stub
        return ipaddress.substring(0, ipaddress.lastIndexOf("."));
    }

	public static byte[] convertLong(long l) {
		byte[] bArray = new byte[8];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        LongBuffer lBuffer = bBuffer.asLongBuffer();
        lBuffer.put(0, l);
        return bArray;
	}
	
	public static  byte[] convertShort(short s) {
		byte[] bArray = new byte[2];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer sBuffer = bBuffer.asShortBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertInt(int s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer sBuffer = bBuffer.asIntBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static  byte[] convertFloat(float s) {
		byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        bBuffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer sBuffer = bBuffer.asFloatBuffer();
        sBuffer.put(0, s);
        return bArray;
	}
	
	public static long byteToLong(byte data[]){
		ByteBuffer bBuffer = ByteBuffer.wrap(data);
		bBuffer.order(ByteOrder.LITTLE_ENDIAN);
		LongBuffer  lBuffer = bBuffer.asLongBuffer();
		return lBuffer.get();
	}

	public static byte[] convertStringToBytes(String key) {
		try {
			return key.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String convertByteToString(byte[] value){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(value);
		
			return baos.toString("ISO-8859-1");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static HashMap<Integer, BigDecimal> findUnionProbability(
			int numComb, HashMap<Integer, Double> perPeerProbs, 
			int maxPeersForProb) {

		HashMap<Integer,BigDecimal> indepProbs = 
			new HashMap<Integer, BigDecimal>();

		double notProbs = -1.0;
		for (Entry<Integer, Double> ent : perPeerProbs.entrySet()){
			if (notProbs<0) notProbs = 1-ent.getValue();
			else notProbs*=1-ent.getValue();
		}

		// store independent probabilities
		indepProbs.put(0, new BigDecimal(1-notProbs));

		if (perPeerProbs.size()>maxPeersForProb){
			double probs[] = new double[perPeerProbs.size()];
			int j = 0;

			for (Entry<Integer, Double> ent2 : perPeerProbs.entrySet()){
				probs[j]+= ent2.getValue();
				j++;
			}

			Arrays.sort(probs);
			double minValue = probs[perPeerProbs.size()-maxPeersForProb];
			HashMap<Integer, Double> newProbs = 
				new HashMap<Integer, Double>();
			double temp = 0;
			for (Entry<Integer,Double> ent : 
				perPeerProbs.entrySet()){


				temp= ent.getValue();

				if (temp>=minValue){
					newProbs.put(ent.getKey(), ent.getValue());
				}
			}
			// switch them over
			perPeerProbs = newProbs;
		}

		// first find all the possible terms, i.e., the unique combinations 
		// of (index+1) nodes seeing problems at the same time

		ArrayList<BigDecimal> terms = new ArrayList<BigDecimal>();
		for (Entry<Integer, Double> ent : 
			perPeerProbs.entrySet()){
			terms.add(new BigDecimal(ent.getValue(), MathContext.UNLIMITED));
		}

//		// now get the union probabilities
		BigDecimal firstParts = new BigDecimal(0);


		int n = terms.size();

		BigDecimal prob[] = new BigDecimal[n];
		for (int i = 0; i < n; i++) prob[i] = 
			terms.get(i);
		BigDecimal psum[] = new BigDecimal[n];
		BigDecimal term[] = new BigDecimal[n];
		BigDecimal prun;
		for (int j=0; j < n; j++){
			psum[j] = new BigDecimal(0, MathContext.UNLIMITED);
			term[j] = new BigDecimal(0, MathContext.UNLIMITED);
		}
		term[n-1] = new BigDecimal(1, MathContext.UNLIMITED);
		prun = new BigDecimal(0, MathContext.UNLIMITED);

		for (int j = 0; j < n; j++){
			term[0] = term[0].add(prob[j], MathContext.UNLIMITED);
			term[n-1] = term[n-1].multiply(prob[j], MathContext.UNLIMITED);
			psum[j] = prob[j];
		}

		if (n-2>0) {

			int i2 = n-1;
			int j2 = n;
			for (int i = 1; i < i2; i++) {
				j2--;
				int k2 = j2 +1;
				for (int j = 0; j < j2; j++){
					psum[j] = new BigDecimal(0, MathContext.UNLIMITED);
					int k1 = j+1;
					for (int k = k1; k < k2; k++){
						psum[j] = psum[j].add(prob[j].multiply(psum[k], MathContext.UNLIMITED)
								, MathContext.UNLIMITED);
					}
					term[i] = term[i].add(psum[j], MathContext.UNLIMITED);
				}
			}

		}


		for (int offset = 1; offset < numComb; offset++){
			int sign = -1;
			prun = new BigDecimal(0, MathContext.UNLIMITED);
			for (int j = offset; j < n; j++){								
				sign*=-1;
//				if (j==index2) continue;
prun=prun.add((new BigDecimal(sign)).multiply(term[j], MathContext.UNLIMITED)
		, MathContext.UNLIMITED);
			}
//			System.out.println(prun);

			indepProbs.put(offset, prun);
		}


		return indepProbs;
	}

	public static String convertIntToIp(int ip) {
		int temp;
		String out = "";
		for (int i = 0; i < 4; i++){
			temp = ip;
			temp >>= (8*(3-i));
			temp &= 0xFF;
			out+=temp+".";
		}
		return out.substring(0, out.length()-1);
	}

	public static int convertIpToInt(String ipv4) {
		String parts[] = ipv4.split("\\.");
		if (parts.length!=4) return -1;
		long ip = 0;
		long temp;
		for (int i = 0; i < parts.length; i++){
			temp = Integer.parseInt(parts[i]);
			temp <<= (8*(3-i));
			ip+=temp;
		}
		return (new Long(ip)).intValue();
	}

	public static String join(String string, Object[] array) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++){
			sb.append(array[i]);
			if (i<array.length-1) sb.append(string);
		}
		return sb.toString();
	}

	public static boolean isV6Address(String dest) {
		try {
			InetAddress addr = InetAddress.getByName(dest);
			if (addr instanceof Inet6Address) return true;
		} catch (UnknownHostException e) {
			// do nothing
		}		
		return false;
	}
	

	/**
	 * compute double value
	 * @param vv
	 * @return
	 */
	public static double mean(Collection vv){
		double[] dat=new double[vv.size()];
		Iterator ier = vv.iterator();
		int index=0;
		while(ier.hasNext()){
			Double tt = (Double)ier.next();
			dat[index]=tt.doubleValue();
			index++;
		}
		double out=Statistics.mean(dat);
		dat=null;
		return out;
	}
	
	/**
	 * standard deviation
	 * @param vv
	 * @return
	 */
	public static double std(Collection vv){
		double[] dat=new double[vv.size()];
		Iterator ier = vv.iterator();
		int index=0;
		while(ier.hasNext()){
			Double tt = (Double)ier.next();
			dat[index]=tt.doubleValue();
			index++;
		}
		double out=Statistics.standardDeviation(dat);
		dat=null;
		return out;
	}
	
	public static double median(Collection vv){
		double[] dat=new double[vv.size()];
		Iterator ier = vv.iterator();
		int index=0;
		while(ier.hasNext()){
			Double tt = (Double)ier.next();
			dat[index]=tt.doubleValue();
			index++;
		}
		Arrays.sort(dat);
		int ind=(int)Math.round(dat.length*0.5);
		
		if(ind>=0&&ind<dat.length){
			return dat[ind];
		}else{
			return Double.MAX_VALUE;
		}
	}
}
