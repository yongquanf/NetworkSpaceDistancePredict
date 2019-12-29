package edu.NUDT.PDL.measurement;

import java.util.ArrayList;

import edu.NUDT.PDL.util.Util;

public class TraceResult {
	public String source;
	public String dest;
	public ArrayList<TraceEntry> entries;
	public long timestamp = Util.currentGMTTime();
}