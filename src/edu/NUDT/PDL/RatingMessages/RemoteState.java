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

package edu.NUDT.PDL.RatingMessages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import edu.NUDT.PDL.coordinate.Coordinate;
import edu.NUDT.PDL.coordinate.ratingCoord;

/**
 * The state kept of a remote node between samples.
 * 
 * @author Michael Parker, Jonathan Ledlie
 * 
 * @param <T>
 *            the type of the unique identifier of a host
 */
public class RemoteState<T> {
	

	// made not final so they can be changed by simulator
	protected static double SAMPLE_PERCENTILE = 0.5;

	public static double ALPHA = 0.5;
	// Don't keep more than this many samples
	public static int MAX_SAMPLE_SIZE = 16;
	// Don't use a guy unless we have this many samples
	public static int MIN_SAMPLE_SIZE = 1;

	protected final T addr;
	protected final WindowStatistic ping_samples;


	protected double last_error;
	// when we last update our coord vs this node
	protected long last_update_time;
	// when we last pinged this node
	protected long last_ping_time;

	public double oldRating=0;
	
	// the accumulated error
	protected List<Double> _measured_err;

	protected double _measured_AbsErr;

	Coordinate last_coords;

	public RemoteState(T _addr) {
		addr = _addr;
		ping_samples = new WindowStatistic(MAX_SAMPLE_SIZE);

		last_error = 0.0;
		last_update_time = -1L;
		last_ping_time = 0L;
		_measured_AbsErr = 0;
		_measured_err = new ArrayList<Double>(10);

	}

	@Override
	public String toString() {
		return "[" + addr + " s " + ping_samples.getSize() + " update "
				+ last_update_time + " ping " + last_ping_time + " error "
				+ last_error +"]";
	}

	public T getAddress() {
		return addr;
	}



	public void addSample(double sample_rtt) {
		ping_samples.add(sample_rtt);
	}

	public void addSample(double sample_rtt, long curr_time) {
		ping_samples.add(sample_rtt);
		last_update_time = curr_time;
	}

	public void addSample(double sample_rtt,Coordinate r_coord, long curr_time) {
		ping_samples.add(sample_rtt);
		last_coords = r_coord;
		last_update_time = curr_time;
	}
	
	public void addSample(double sample_rtt, long sample_age,
			Coordinate r_coord, double r_error, long curr_time) {
		ping_samples.add(sample_rtt);
		last_coords = r_coord;
		last_error = r_error;
		if (sample_age > 0)
			last_update_time = curr_time - sample_age;
		else
			last_update_time = curr_time;
	}

	public void addMeasurementErrors(double sample_rtt, Coordinate r_coord,
			Coordinate my_coord) {
		// TODO: revise
		// evaluate the abnormal case
		double dist = r_coord.distanceTo(my_coord);

		if (dist == 0.) {
			System.err.println("bad distance " + dist);
			return;
		}
		assert (sample_rtt > 0.);

		double absErr = Math.abs(sample_rtt - dist);
		double error = absErr / sample_rtt;
		// System.out.println("##########################");
		// System.out.println("$: absErr: "+absErr+", dist: "+dist+", sampleRTT"+sample_rtt);
		if (_measured_err != null) {
			_measured_err.add(error);
			if (_measured_AbsErr == 0.) {
				_measured_AbsErr = absErr;
			} else {
				_measured_AbsErr = _measured_AbsErr * ALPHA + (1 - ALPHA)
						* absErr;
			}
		}
		// System.out.println("$:  _measured_AbsErr: "+ _measured_AbsErr);
		// System.out.println("##########################");
	}

	public boolean isValid(long curr_time) {
		/*
		 * if (getLastError() <= 0. || last_update_time <= 0 ||
		 * last_coords.atOrigin()) { return false; }
		 */
		// System.out.println("getSampleSize(): "+getSampleSize()+", GetSample: "+getSample());
		if (getSampleSize() >= MIN_SAMPLE_SIZE && getSample() > 0) {
			return true;
		}

		if (getSampleSize() >= 2 && ping_samples.withinVariance(.1)) {
			return true;
		}

		return false;
	}

	public double getSample() {
		// Percentile filter
		return ping_samples.getPercentile(SAMPLE_PERCENTILE);
	}

	public double err_eliminate() {

		// double
		// origin=ping_samples.samples.get(ping_samples.samples.size()-1).sample;
		double origin = ping_samples.getPercentile(SAMPLE_PERCENTILE);
		// System.out.println("$: origin: "+origin+", measured_absErr"+_measured_AbsErr);
		return origin + _measured_AbsErr;
	}

	public int getSampleSize() {
		return ping_samples.getSize();
	}

	/*
	 * public boolean isLowVariance () { return ping_samples.isLowVariance(); }
	 */

	public Coordinate getLastCoordinate() {
		return last_coords;
	}

	public double getLastError() {
		return last_error;
	}

	/*
	 * public boolean beenSampled() { return (last_update_time >= 0L); }
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

	public static void main(String args[]) {
		System.out.println("Testing Remote State Object");
		String sampleFile = args[0];
		RemoteState<String> rs = new RemoteState<String>(sampleFile);
		BufferedReader sampleReader = null;
		try {
			sampleReader = new BufferedReader(new FileReader(new File(
					sampleFile)));
		} catch (FileNotFoundException ex) {
			System.err.println("Cannot open file " + sampleFile + ": " + ex);
			System.exit(-1);
		}

		long sample_age = 0;
		Coordinate r_coord = null;
		double r_error = 0;

		try {
			String sampleLine = sampleReader.readLine();
			while (sampleLine != null) {
				// reads in timestamp in ms and raw rtt
				StringTokenizer sampleTokenizer = new StringTokenizer(
						sampleLine);
				long curr_time = Long.parseLong((String) (sampleTokenizer
						.nextElement()));
				int rawRTT = Integer.parseInt((String) (sampleTokenizer
						.nextElement()));
				sampleLine = sampleReader.readLine();
				rs.addSample(rawRTT, sample_age, r_coord, r_error, curr_time);
				double smoothedRTT = rs.getSample();
				System.out.println(curr_time + " raw " + rawRTT + " smooth "
						+ smoothedRTT);
			}
		} catch (Exception ex) {
			System.err.println("Problem parsing " + sampleFile + ": " + ex);
			System.exit(-1);
		}
	}

}
