package edu.NUDT.PDL.util;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;


public class 
Java15Utils 
{
	private static ThreadMXBean	thread_bean;
	
	static{
		try{
			thread_bean = ManagementFactory.getThreadMXBean();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}

	public static void
	setConnectTimeout(
		URLConnection		con,
		int					timeout )
	{
		con.setConnectTimeout( timeout );
	}
	
	public static void
	setReadTimeout(
		URLConnection		con,
		int					timeout )
	{
		con.setReadTimeout( timeout );
	}
	
	public static long
	getThreadCPUTime()
	{
		if ( thread_bean == null ){
			
			return( 0 );
		}
		
		return( thread_bean.getCurrentThreadCpuTime());
	}
	
	public static void
	dumpThreads()
	{
		//AEThreadMonitor.dumpThreads();
	}
	
	public static URLConnection 
	openConnectionForceNoProxy(
		URL url) 
	
		throws IOException 
	{
		return url.openConnection(Proxy.NO_PROXY);
	}
}