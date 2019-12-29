package edu.NUDT.PDL.util;

public class 
PausableAverage 
	extends Average
{
	public static PausableAverage 
	getPausableInstance(
		int 	refreshRate,
		int 	period ) 
	{
		if ( refreshRate < 100 ){
			
			return null;
		}
		
		if (( period * 1000 ) < refreshRate ){
			
			return null;
		}
	
		return new PausableAverage(refreshRate, period);
	}
	
	private long	offset;
	private long	pause_time;
	
	private 
	PausableAverage(
		int _refreshRate, 
		int _period )
	{
		super( _refreshRate, _period );
	}
	
	public void
	addValue(
		long	value )
	{		
		super.addValue( value );
	}
	
	public long
	getAverage()
	{
		long	average = super.getAverage();
		
		return( average );
	}
	
	protected long
	getEffectiveTime()
	{
		return( SystemTime.getCurrentTime() - offset );
	}
	
	public void
	pause()
	{
		if ( pause_time == 0 ){
			
			pause_time = SystemTime.getCurrentTime();
		}
	}
	
	public void
	resume()
	{
		if ( pause_time != 0 ){
			
			long	now = SystemTime.getCurrentTime();
			
			if ( now > pause_time ){
				
				offset += now - pause_time;
			}
			
			pause_time	= 0;
		}
	}
}