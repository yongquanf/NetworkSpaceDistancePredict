/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.NUDT.PDL.coordinate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.StringTokenizer;


/**
 * The state kept of a remote node between samples.
 * 
 * @author Michael Parker, Jonathan Ledlie
 * 
 * @param <T>
 * the type of the unique identifier of a host
 */
public class RemoteState<T> {
  // made not final so they can be changed by simulator
	protected static double SAMPLE_PERCENTILE = 0.5;
  // Don't keep more than this many samples
  public static int MAX_SAMPLE_SIZE = 16;
  // Don't use a guy unless we have this many samples
  public static int MIN_SAMPLE_SIZE = 4;
  
	protected final T addr;
	protected final WindowStatistic ping_samples;

	protected ratingCoord last_coords;
	protected double last_error;
	// when we last update our coord vs this node
	protected long last_update_time;
	// when we last pinged this node
	protected long last_ping_time;

	public RemoteState(T _addr) {
		addr = _addr;
    ping_samples = new WindowStatistic(MAX_SAMPLE_SIZE);

		last_coords = null;
		last_error = 0.0;
		last_update_time = -1L;
		last_ping_time = 0L;
	}

	public String toString () {
		return "["+addr+" s "+ping_samples.getSize()+
			" update "+last_update_time+
			" ping "+last_ping_time+
			" error "+last_error+
			" coord "+last_coords+"]";
	}

  public T getAddress () {
    return addr;
  }
  
	public void assign(ratingCoord _last_coords, double _last_error,
	    long _curr_time) {
	  last_coords = _last_coords;
		last_error = _last_error;
		last_update_time = _curr_time;
	}

  public void addSample(double sample_rtt, long sample_age, ratingCoord r_coord,
    double r_error, long curr_time) {
		ping_samples.add(sample_rtt);
		last_coords = r_coord;
		last_error = r_error;
    if (sample_age > 0)
      last_update_time = curr_time-sample_age;
    else
      last_update_time = curr_time;
	}
  
  public boolean isValid (long curr_time) {
    if (getLastError() <= 0. || last_update_time <= 0 ) {
      return false;
    }
      
    if (getSampleSize() >= MIN_SAMPLE_SIZE && getSample() > 0) {
      return true;
    }

    if (getSampleSize() >= 2 && ping_samples.withinVariance(.1)) {
      return true;
    }
    
    return false;
  }
  
	public double getSample() {
		return ping_samples.getPercentile(SAMPLE_PERCENTILE);
	}

  public int getSampleSize() {
    return ping_samples.getSize();
  }
  /*
  public boolean isLowVariance () {
    return ping_samples.isLowVariance();
  }
  */
  
	public ratingCoord getLastCoordinate() {
		return last_coords;
	}

	public double getLastError() {
		return last_error;
	}
/*
	public boolean beenSampled() {
		return (last_update_time >= 0L);
	}
*/
	public long getLastUpdateTime() {
		return last_update_time;
	}
	
	public long getLastPingTime() {
		return last_ping_time;
	}
	
	public void setLastPingTime(long time) {
		last_ping_time = time;
	}
  
	public static void main (String args[]) {
	  System.out.println("Testing Remote State Object");
	  String sampleFile = args[0];
	  RemoteState<String> rs = new RemoteState<String>(sampleFile);
	  BufferedReader sampleReader = null;
	  try {
	    sampleReader = new BufferedReader (new FileReader (new File (sampleFile)));
	  }catch (FileNotFoundException ex) {
	    System.err.println("Cannot open file "+sampleFile+": "+ex);
	    System.exit(-1);
	  } 
	  
	  long sample_age = 0;
	  ratingCoord r_coord = null;
	  double r_error = 0;
	  
	  try {
	    String sampleLine = sampleReader.readLine();
	    while (sampleLine != null) {
	      // reads in timestamp in ms and raw rtt
	      StringTokenizer sampleTokenizer = new StringTokenizer (sampleLine);
	      long curr_time = Long.parseLong((String)(sampleTokenizer.nextElement()));
	      int rawRTT = Integer.parseInt((String)(sampleTokenizer.nextElement()));
	      sampleLine = sampleReader.readLine();
	      rs.addSample (rawRTT, sample_age, r_coord, r_error, curr_time);
        double smoothedRTT = rs.getSample();
        System.out.println(curr_time+" raw "+rawRTT+" smooth "+smoothedRTT);
	    }
	  } catch (Exception ex) {
	    System.err.println("Problem parsing "+sampleFile+": "+ex);
	    System.exit(-1);     
	  }
	}
  
}
