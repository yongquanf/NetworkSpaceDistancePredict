package edu.NUDT.PDL.time;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.TimeZone;

import edu.NUDT.PDL.util.Util;


public class getTime {

	
	
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
	
}
