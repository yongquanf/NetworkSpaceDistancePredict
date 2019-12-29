package edu.NUDT.PDL.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.POut;

public class TimeSerialParser {

	
	static Log logger=new Log(TimeSerialParser.class);
	
	static TimeSerialParser self=null;
	
	public TimeSerialParser getInstance(){
		if(self==null){
			self=new TimeSerialParser();
		}
		return self;
	}
	
	Hashtable<pair,ArrayList<Double>> cachedData=new Hashtable<pair,ArrayList<Double>>(100);
	class pair{
		String from;
		String to;
		
		public pair(String _A,String _B){
			from=_A;
			to=_B;
		}

		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			pair tmp=(pair)obj;
			if(tmp.from.equalsIgnoreCase(from)&&
					tmp.to.equalsIgnoreCase(to)){
				return true;
			}else{
				return true;
			}
		}
		
	}
	
	
	/**
	 * record the error
	 * @param coord
	 * @param selection, 0, coordinate.log, 1, controlLog.log, 2, rawMeasurement
	 */
	public void parseLog(String logName,int selection,String outname){
		//Open the file for reading
	     try {
	       BufferedReader br = new BufferedReader(new FileReader(logName));
	     //Construct the BufferedWriter object
           BufferedWriter writer= new BufferedWriter(new FileWriter(outname));

	       String thisLine;
		while ((thisLine = br.readLine()) != null) { // while loop begins here
			switch(selection){
			case 0:{
				parseCoordinateLog(thisLine,writer);
				break;
			}
			case 1:{
				parseControlLog(thisLine,writer);
				break;
			}
			case 2:{
				parserawMeasurementLog(thisLine,writer);
				break;
			}
			
			}
			
	       } // end while 
			br.close();
			writer.close();
	     } // end try
	     catch (IOException e) {
	       System.err.println("Error: " + e);
	     }

	}

	private void parserawMeasurementLog(String thisLine, BufferedWriter writer) {
		// TODO Auto-generated method stub
		String[] tmp = thisLine.split("[ ]");
		if(tmp!=null&&tmp.length>2){
			String from=tmp[1];
			String to=tmp[2];
			double dat=Double.parseDouble(tmp[3]);
			if(dat<0){
				logger.warn("negative!");
				return;
			}
			pair p=new pair(from,to);
			if(!cachedData.containsKey(p)){
				cachedData.put(p, new ArrayList<Double>());
			}else{
				cachedData.get(p).add(dat);
			}
		}
		
	}

	private void parseControlLog(String thisLine, BufferedWriter writer) {
		// TODO Auto-generated method stub
		
	}
	/**
	 * parse the coordinate Log
	 * @param thisLine
	 * @param writer 
	 * @throws IOException 
	 */
	private void parseCoordinateLog(String thisLine, BufferedWriter writer) throws IOException {
		// TODO Auto-generated method stub
		if(thisLine.startsWith("Timer:")){
			writer.newLine();
			String[]info=thisLine.split("[ \t]");
			String date="";
			if(info.length>=2){
				date=MainGeneric.currentMilliSeconds2Date(Long.parseLong(info[1]));
			}
			writer.append(date+" ");
		}else{
			//not the coordinate
			if(!thisLine.contains("(")){
				writer.append(thisLine+" ");
			}
		}
		writer.flush();
	}
	
	/**
	 * parse a data file
	 * @param args
	 */
	public static void main(String[] args){
		TimeSerialParser test=new TimeSerialParser();
		
		//logName, outName
		test.parseLog(args[0],Integer.parseInt(args[4]), args[1]);
		//ping type
		int pingType=Integer.parseInt(args[2]);
		//outStatistics
		try {
			test.saveStatistics(pingType,args[3]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * save the statistics
	 * @param pingType,0 rtt, 1 loss, 2 bandwidth
	 * @throws IOException 
	 */
	private void saveStatistics(int pingType,String outStatistics) throws IOException {
		// TODO Auto-generated method stub
		
		//Construct the BufferedWriter object
        BufferedWriter writer= new BufferedWriter(new FileWriter(outStatistics));
        //write the median value of each measurement pair
		//write the std value of one pair
        Vector median=new Vector();
        Vector std=new Vector();
        
        Iterator<Entry<pair, ArrayList<Double>>> ier = cachedData.entrySet().iterator();
        ArrayList<Double> val;
		//rtt
		if(pingType==0){
			
			while(ier.hasNext()){
				Entry<pair, ArrayList<Double>> rec = ier.next();
				val = rec.getValue();
				if(val.size()>1){
					 median.add(Util.median(val));
					 std.add(Util.std(val));
				}else if(val.size()==1){
					 median.add(val.get(0));
				}
				
			}
			
			
			
		}else if(pingType==1){//loss
			
			double threshold=2;
			while(ier.hasNext()){
				Entry<pair, ArrayList<Double>> rec = ier.next();
				val = rec.getValue();
				if(val.size()>1){
					 median.add(Util.median(val)-threshold);
					 std.add(Util.std(val));
				}else if(val.size()==1){
					 median.add(val.get(0)-threshold);
				}
				
			}
			
			
		}else if(pingType==2){//bandwidth
			
			while(ier.hasNext()){
				Entry<pair, ArrayList<Double>> rec = ier.next();
				val = rec.getValue();
				if(val.size()>1){
					 median.add(Util.median(val));
					 std.add(Util.std(val));
				}else if(val.size()==1){
					 median.add(val.get(0));
				}
				
			}
		}
		
		cachedData.clear();
		
		writer.append(POut.toString(median));
		writer.flush();
		writer.append("\n######################\n");
		writer.append(POut.toString(std));
		
		writer.flush();
		writer.close();
	}
}
