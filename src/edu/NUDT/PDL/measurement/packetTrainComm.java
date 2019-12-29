package edu.NUDT.PDL.measurement;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.NetUtil;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB2;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.EL.Priority;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.NetAddress;
import edu.harvard.syrah.sbon.async.comm.UDPComm;
import edu.harvard.syrah.sbon.async.comm.UDPCommCB;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessageIF;

/**
 * measure one way loss rates, 
 * based on the cooperative sequence no caching scheme.
 * 
 * @author ericfu
 *
 */
public class packetTrainComm extends UDPComm implements packetTrainIF {

	static Log log=new Log(packetTrainComm.class);
	static int sendQueryId=0;
	static int inQueryId=0;
	
	Random random=new Random(System.currentTimeMillis());
	//public static ExecutorService execpacketTrain = Executors.newFixedThreadPool(15);
	//packet type
	/**
	 * probe packet
	 */
	public static int probepacketTrainNo=10;
	/**
	 * response packet, default, is not sent
	 */
	public static int responseTrainNo=101;
	/**
	 * statistics packet, bw
	 */
	public static int statisticsTrainNo=102;
	//reorder
	public static int statisticsTrainReorder=106;
	
	/**
	 * the size of response packet, 
	 */
	public static int responsePacketSize=100;
	
	/**
	 * statistics interval
	 */
	public static int StatisticsInterval=Integer.parseInt(Config
			.getConfigProps().getProperty("StatisticsInterval", "30000"));
	
	
	public static int maximumItems= Integer.parseInt(Config
			.getConfigProps().getProperty("maximumItems", "1000"));
	
	
	public static int LContinuousPkts=Integer.parseInt(Config
			.getConfigProps().getProperty("LContinuousPkts", "7"));
	
	public static BufferedWriter logOneWayLoss;
	
	public String logOneWayLossName="logOneWayLossName";
	/**
	 * for receiver, save received sequence no for each sender
	 */
	Map<AddressIF,ArrayList> cachedReceivedSequenceNumber;
	/**
	 * for receiver, save the latest timestamp for each sender
	 */
	Map<AddressIF,Vector<Double>> cachedReceivedStamp;
	
	
	
	/**
	 * for sender, save sent sequences for each target
	 */
	Map<AddressIF,ArrayList> cachedSendingSequenceNumber;
	
	
	Map<AddressIF,ArrayList> cachedLossTowardsTarget;
	/**
	 * callback for udp channel
	 */
	packetTrainProbeUDPCommCB udpCB;
	
	
	public AddressIF myContactAddress=null;
	private long START_TIME=-1;
	/**
	 *  call the cached callback iteration process
	 */
	public static int IterateTimeout=600000;
	
	/**
	 * udp wrapper header
	 */
	public static int packetIDX=20;
	
	/**
	 * cache the callbacks
	 */
	Hashtable<AddressIF,ArrayList<pair>> cachedCallbacks = new Hashtable<AddressIF,ArrayList<pair>>(2);
	
	Hashtable<AddressIF,ArrayList<Integer>> cachedSeqNo = new Hashtable<AddressIF,ArrayList<Integer>>(2);
	
	/**
	 * time out one callback job
	 */
	private long timeoutCallback=Integer.parseInt(Config
			.getConfigProps().getProperty("timeoutCallback", "600000"));
	
	class pair{
		long timeStamp;
		CB1<double[]> cb;
		pair(long _timeStamp,CB1<double[]> _cb){
			timeStamp=_timeStamp;
			cb=_cb;
		}
	}
	
	
	public packetTrainComm() {
		super(Priority.NORMAL);
		log.info("Initialising packetTrainComm...");
		udpCB=new packetTrainProbeUDPCommCB();
		
		//register the udp callback		
		this.registerPacketCallback(udpCB);
		
		cachedReceivedStamp=new ConcurrentHashMap<AddressIF,Vector<Double>> ();
		cachedReceivedSequenceNumber=new ConcurrentHashMap<AddressIF,ArrayList> ();
		cachedSendingSequenceNumber=new ConcurrentHashMap<AddressIF,ArrayList>();
		cachedLossTowardsTarget= new ConcurrentHashMap<AddressIF,ArrayList>();
		
		registerIterateCachedTimer(this);
		
		try {
			logOneWayLoss=new BufferedWriter(new FileWriter(new File(logOneWayLossName + ".log")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * init the channel
	*/
	public void initServer(AddressIF bindAddress, CB0 cbInit) {
		//must be such order
		//myContactAddress=AddressFactory.create(bindAddress);
		myContactAddress=bindAddress;
		//myContactAddress=bindAddress;
		super.initServer(bindAddress, cbInit);
		log.info("Binding to addr=" + bindAddress + " (localAddr=" + localNetAddress + ")");
		START_TIME=System.currentTimeMillis();
	
	}

	
	
	public void registerIterateCachedTimer(final packetTrainComm  comm){
				
		EL.get().registerTimerCB(IterateTimeout, new CB0(){
			@Override
			protected void cb(CBResult result) {
				iterateAllCachedPacketTrainsForTimeout(comm);
			}									
		});	
		
	}
	
	/**
	 * print the table
	 * @param table
	 */
	void printCached(Map table){
		
	StringBuffer buf=new StringBuffer();
	Iterator ier = table.keySet().iterator();
		while(ier.hasNext()){
			Object tmp = ier.next();
			
			buf.append(tmp+"\t"+table.get(tmp)+"\n");			
		}
	log.debug(buf.toString());
	
	}
	
	/**
	 * send the probe packets, save the callbacks, 
	 * if multiple callbacks, save old callback
	 * @param remoteNode
	 * @param packetsize
	 * @param packetNum
	 * @param cbCapacity
	 */
	public void sendProbeTrains(final AddressIF remoteNode,final int packetsize,
			int packetNum,CB1<double[]> cbCapacity){
		int PING_DELAY=0;
		int[] PINGCounter={packetNum};
		int rept=5;
		//send 5 packet trains, each 
		
		sendProbeTrains(remoteNode,packetsize,PINGCounter, PING_DELAY);			
		if(cbCapacity!=null){
			synchronized(cachedCallbacks){
			if(!cachedCallbacks.containsKey(remoteNode)){
				cachedCallbacks.put(remoteNode, new ArrayList<pair>(2));
			}
			cachedCallbacks.get(remoteNode).add(new pair(System.currentTimeMillis(), cbCapacity));
			}
		}
		
	}

	/**
	 * ping the target with specified ping settings
	 * @param remoteNode
	 * @param packetsize
	 * @param PINGCounter
	 * @param PING_DELAY
	 * @param cbPing
	 */
	
	public void sendProbeTrains(final AddressIF remoteNode,final int packetsize,
			final int[] PINGCounter,final int PING_DELAY){
		int total=0;
		int seqNo;
		while(total<PINGCounter[0]){
			seqNo=getSendQueryId();
			sendPacket(probepacketTrainNo, myContactAddress, seqNo, packetsize, remoteNode);
						
			total++;
		}
	}
	public void sendProbeTrains1(final AddressIF remoteNode,final int packetsize,
			final int[] PINGCounter,final int PING_DELAY){
		log.debug("packet train remains: "+PINGCounter[0]);
		
		if(PINGCounter[0]==0){
		//all probes are finished	
		log.debug("all probe packets are sent!\t \n@ "+NetUtil.byteIPAddrToString(this.myContactAddress.getByteIPAddrPort()));	
		//TODO: sent all packets	
		
		//printCached(cachedSendingSequenceNumber);
		
		}else {
		//continue next probes	
			PINGCounter[0]--;
			//save the seqence Number
			int seqNo=getSendQueryId();
/*			if(!cachedSendingSequenceNumber.containsKey(remoteNode)){
				cachedSendingSequenceNumber.put(remoteNode, new ArrayList());
			}
			//add records
			synchronized(cachedSendingSequenceNumber.get(remoteNode)){
			cachedSendingSequenceNumber.get(remoteNode).add( seqNo);
			}
			//maintain the records
			maintainCachedRecords(cachedSendingSequenceNumber.get(remoteNode));*/
			
			sendPacket(probepacketTrainNo, myContactAddress, seqNo, packetsize, remoteNode);
			
			//send next packet
			sendProbeTrains(remoteNode,packetsize,PINGCounter, PING_DELAY);	
/*			log.debug("setting timer to " + PING_DELAY);
			double rnd = random.nextGaussian();
			long delay2 =PING_DELAY + (long) (PING_DELAY * rnd);
			
			EL.get().registerTimerCB(delay2, new CB0(){
				@Override
				protected void cb(CBResult result) {
					// TODO Auto-generated method stub
					sendProbeTrains(remoteNode,packetsize,
							PINGCounter, PING_DELAY);	
				}									
			});	*/
		}
		
	}

	/**
	 * maintainRecords
	 * @param list
	 */
	void maintainCachedRecords(Collection list){
		synchronized(list){
		if(list.size()>maximumItems){
			int toBeRemoved=list.size()-maximumItems;
			Iterator ier = list.iterator();
			while((toBeRemoved>0)&&(ier.hasNext())){
				ier.remove();
				toBeRemoved--;
			}
		}
		}
	}
	
	
	/**
	 * with callback
	 * @param sequenceNo
	 * @param packetsize
	 * @param remoteNode
	 * @param cbPing
	 */
	public void sendPacket(int packetType, final AddressIF myaddr, final int sequenceNo,int packetsize, AddressIF remoteNode){
		
		//create the packet
		byte[]content=new byte[packetsize];
		int rand=1;
		for(int i=0;i<packetsize;i++){
			content[i]=(byte)(rand >>8);
		}		
		byte[] b=this.setProbePacket(sequenceNo,packetType,packetsize,content);
		
		ByteBuffer outPacket = ByteBuffer.allocate(b.length);
		outPacket.put(b);
		//timeStamp	
		final long timeStamp=System.currentTimeMillis();
		
		//send the packet
		//sendPacket(outPacket,remoteNode, udpCB);	
		sendPacket(outPacket,remoteNode);	
		outPacket.clear();
		b=null;
	}
	
	
	/**
	 * compute the one way loss
	 * @param target
	 * @param receivedSequenceNo
	 */
	public double computeSingleWayLossTowardsTarget(BufferedWriter writer, AddressIF target, ArrayList receivedSequenceNo){
		
		StringBuffer rec=new StringBuffer();
		double loss=-1;
		ArrayList cachedSequenceNos=cachedSendingSequenceNumber.get(target);
		
		synchronized(cachedSequenceNos){
		assert(cachedSequenceNos.size()>0);
		
		int total=cachedSequenceNos.size();
		int missing=0;
		
		//=========================================
		if(receivedSequenceNo==null||receivedSequenceNo.isEmpty()){
			missing=total;
			
		}else{			
		Iterator ier = cachedSequenceNos.iterator();
		while(ier.hasNext()){
			Object tmp = ier.next();
			if(receivedSequenceNo.contains(tmp)){
			//in the list	
			log.debug("in the list"+": "+tmp);				
			}else{
				missing++;
			}			
		}
				}
		
		
		loss=(missing+0.00)/total;		
		rec.append(getUptimeStr()+", loss rate: "+loss+", total: "+total+", missing: "+missing+", src: "+myContactAddress.toString()+", dest: "+target.toString()+"\n");
	
		}
		//=========================================
	
		//Ninaloader.logOneWayLoss=null;
		try {
			
			writer.append(rec.toString());
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{

		}
		
		
		if(!cachedLossTowardsTarget.containsKey(target)){
			cachedLossTowardsTarget.put(target, new ArrayList());			
		}
		cachedLossTowardsTarget.get(target).add(loss);
		
		return loss;		
	}
	
	
	private String getUptimeStr() {
		// TODO Auto-generated method stub
		
			long now = System.currentTimeMillis();

			return PUtil.getDiffTimeStr(START_TIME, now);
		
	}


	/**
	 * register the packet handler
	 * @author ericfu
	 *
	 */
	public class packetTrainProbeUDPCommCB extends UDPCommCB{

		
		@Override
		protected void cb(CBResult result, ByteBuffer inPacket,final AddressIF remoteAddr22,
				Long arg3, CB1<Boolean> arg4) {
			// TODO Auto-generated method stub
	
			
			try{
			//parse the sequence No, 
			InputStream is = PUtil.createInputStream(inPacket);

			log.debug("Reading response...");

																	

			int len = inPacket.remaining();

			

			//log.debug("len=" + len);

			

			final byte[] data = new byte[len];


			//log.debug("Received DNS response with responseId=" + responseId + " for queryId=" + queryId + " wlen=" + wlen);

			

			int off = 0;

			int rlen = 0;

			do {

				rlen += is.read(data, off, len - off);

			} while(rlen < len);

				

			is.close();

			

			int responseId1 = (data[0] & 0xff) << 8;

			responseId1 |= (data[1] & 0xff);
			
			final int responseId=responseId1;
												
			int packetType1=(data[2] & 0xff) << 8;
			packetType1 |= (data[3] & 0xff);	
			
			final int packetType=packetType1;
			
			int contentSize1=(data[4] & 0xff) << 8;
			contentSize1 |= (data[5] & 0xff);	
			
			final int contentSize=contentSize1;
			
			byte[] byteIPAddr=new byte[4];	
			System.arraycopy(data, 6, byteIPAddr, 0, 4);
			
			int addr= ((byteIPAddr[0] & 0xff) << 24) ;
			 addr |= ((byteIPAddr[1] & 0xff) << 16) ;
			 addr |= ((byteIPAddr[2] & 0xff) << 8) ;
			 addr |= (byteIPAddr[3] & 0xff);	 
		
			int RealRemotePort=(data[10] & 0xff) << 8;
			RealRemotePort |= (data[11] & 0xff);	
			
			//target address
			String str=NetUtil.byteIPAddrToString(NetUtil.intIPToByteIP(addr));
			log.debug("RAW: "+str+", "+RealRemotePort);
			
			//timeStamp	
			
			AddressIF remoteAddr=AddressFactory.createUnresolved(str, RealRemotePort);
			//handler the probe process
			if(packetType==packetTrainComm.probepacketTrainNo){
			//send the correspondence packet
				
				//save it to the corresponding table 
				if(!cachedReceivedSequenceNumber.containsKey(remoteAddr)){
					log.debug("can not found remote address: "+ remoteAddr.toString());
					cachedReceivedSequenceNumber.put(remoteAddr, new ArrayList(22));
					//cachedReceivedSequenceNumber.get(remoteAddr).add(Integer.valueOf(responseId));
				}
				synchronized(cachedReceivedSequenceNumber.get(remoteAddr)){
				cachedReceivedSequenceNumber.get(remoteAddr).add(Integer.valueOf(responseId));									
				maintainCachedRecords(cachedReceivedSequenceNumber.get(remoteAddr));
				}
			
			//int responseType=responseTrainNo;
			//byte[] b=setProbePacket(responseId, responseType, contentSize);
			
			//ByteBuffer outPacket = ByteBuffer.allocate(b.length);
			//outPacket.put(b);
			
			
			//save the timestamp
			if(!cachedReceivedStamp.containsKey(remoteAddr)){
				cachedReceivedStamp.put(remoteAddr, new Vector<Double>(2));
			}
			final double timeStamp=System.nanoTime()/1000000.0;
			
			synchronized(cachedReceivedStamp.get(remoteAddr)){
				cachedReceivedStamp.get(remoteAddr).add(timeStamp);									
				maintainCachedRecords(cachedReceivedStamp.get(remoteAddr));
				}
			//send the response
			//sendPacket(outPacket,remoteAddr,udpCB);
			double[] results=testSendCapacityEstimation(remoteAddr,LContinuousPkts,len);
			//capacity
			double capacity = results[0];
			double reorderRatio=results[1];
			//iterateAllCachedPacketTrainsForTimeout();
			if(capacity>0){
				byte[] capacityInfo=NetUtil.serializeObject(results);						
				sendPacket(statisticsTrainNo, myContactAddress, getInQueryId(), capacityInfo,remoteAddr, null);	
			
				capacityInfo=null;
			}
			
			
			
			
			}else if(packetType==packetTrainComm.responseTrainNo){			
				//nothing
			
			}else if(packetType==packetTrainComm.statisticsTrainNo){
			//statistics
			
				
			int idx=packetIDX;	
			int lengthOfContent=data.length-idx;
			//copy the 	
			byte[] cont=new byte[contentSize];
			System.arraycopy(data, idx, cont, 0, lengthOfContent);
			
			//read the data from the packet
			double[] results=(double[])NetUtil.deserializeObject(cont);
			//double capacityEstimation = ((Double)NetUtil.deserializeObject(cont)).doubleValue();
			double capacityEstimation=results[0];
			
			cont=null;
			//compute the senquence
			//TODO: wake up the callbacks,and remove the callbacks	
			synchronized(cachedCallbacks){
			if(cachedCallbacks.containsKey(remoteAddr)){
				Iterator<pair> ier = cachedCallbacks.get(remoteAddr).iterator();
				while(ier.hasNext()){
					ier.next().cb.call(CBResult.OK(), results);
				}
				cachedCallbacks.remove(remoteAddr);
			}else{
				log.warn("already removed");
			}
			}
			
			
			}			
			else{
				log.warn("error, un-identified packet type!");
			}
					
			}catch(Exception e){
				e.printStackTrace();
				
			}
			
			
		}		
	}
	
	/**
	 * test whether we send the capacity estimation
	 * @param sender
	 */
	public double[] testSendCapacityEstimation(AddressIF sender,int L,int packetSize){
		
		double[] results={-1,-1};
		
		if(!cachedReceivedSequenceNumber.containsKey(sender)){
			return results;
		}
		ArrayList NoList = cachedReceivedSequenceNumber.get(sender);
		if( NoList.size()<L){
			return results;
		}
		//if cached continuous sequence number exceeds or equals with L
		int seqNo;
		int maxSubLisTSize=0;
		
		int startIndex=0;
		Integer previous = ((Integer)NoList.get(0)).intValue()-1;
		boolean  hasEnoughDat=false;
		
		int typePReordered=0;
		int NextExpect=0;
		
		int index=0;
		Iterator ier = NoList.iterator();
		while(ier.hasNext()){
		  seqNo=(Integer)ier.next();
		  if(maxSubLisTSize>=L){
				hasEnoughDat=true;
			    break;
			}
		  
		
			//sublist  
		  if( Math.abs(seqNo-previous)==1){
			  maxSubLisTSize++;
		  }else{
			  maxSubLisTSize=0;
			  startIndex=index;
		  }
		  previous=seqNo;
		  index++;
		}
		
		ier= NoList.iterator();
		while(ier.hasNext()){
			seqNo=(Integer)ier.next();
			  //reordered 
			  if(seqNo>=NextExpect)
			  {
				  NextExpect++;
			  }
			  else{
				  typePReordered++; 
			  }
			
			
		}
		
		//if has enough data
		if( hasEnoughDat){
			double elapsed=(cachedReceivedStamp.get(sender).get(startIndex+L-1)-cachedReceivedStamp.get(sender).get(startIndex));
			if(elapsed==0){
				log.warn("zero second passed!");
				elapsed=10;
			}
			double capacity = packetSize*(L-1)/ elapsed;//KB
			results[0]=capacity;
			results[1]=typePReordered/NoList.size();
			
			clearCachedSenderRecord(sender);
			
					
			return results;
		}else{
			return results;
		}
	}
	
	/**
	 * the sender's infomation is not useful
	 * @param sender
	 */
	private void clearCachedSenderRecord(AddressIF sender) {
		// TODO Auto-generated method stub
		cachedReceivedStamp.remove(sender);
		cachedReceivedSequenceNumber.remove(sender);
	}


	/**
	 * send the content
	 * @param packetType
	 * @param myaddr
	 * @param sequenceNo
	 * @param content
	 * @param remoteNode
	 * @param cbPing
	 */
	public void sendPacket(int packetType, final AddressIF myaddr, final int sequenceNo,byte[] content, AddressIF remoteNode,final CB1<Hashtable<Integer,Double>> cbPing){
		
		//create the packet
	
		int packetsize=content.length;
		byte[] b=setProbePacket(sequenceNo,packetType,packetsize,content);
		
		ByteBuffer outPacket = ByteBuffer.allocate(b.length);
		outPacket.put(b);
		//timeStamp	
		final long timeStamp=System.currentTimeMillis();
		
		//send the packet
		//sendPacket(outPacket,remoteNode, udpCB);	
		sendPacket(outPacket,remoteNode);
		
		
		outPacket.clear();
		b=null;
	}
	
	/**
	 * iterate all possible packet trains
	 */
	public void iterateAllCachedPacketTrainsForTimeout(final packetTrainComm  comm){
		
		registerIterateCachedTimer(comm);
		/**
		 * time out value of callbacks
		 */
		//double timeoutCallback;
		
		synchronized(cachedCallbacks){
			pair job;
		Iterator<Entry<AddressIF, ArrayList<pair>>> ier = cachedCallbacks.entrySet().iterator();
		while(ier.hasNext()){
			Entry<AddressIF, ArrayList<pair>> rec = ier.next();
			Iterator<pair> tmpIer = rec.getValue().iterator();
			
			while(tmpIer.hasNext()){
				job = tmpIer.next();
				long time =job.timeStamp;
				//remove the item from the list
				if((System.currentTimeMillis()-time)>=timeoutCallback){					
					//call now, as failed
					job.cb.call(CBResult.TIMEOUT(), null);
					//
					tmpIer.remove();
				}
			}
			//remove the whole list
			if(rec.getValue()==null||rec.getValue().isEmpty()){
				ier.remove();
			}
		}
		}
		//clear cached receiver info
		synchronized(cachedReceivedStamp){
			Iterator<Entry<AddressIF, Vector<Double>>> ier = cachedReceivedStamp.entrySet().iterator();
			while(ier.hasNext()){
				Entry<AddressIF, Vector<Double>> rec = ier.next();
				AddressIF target = rec.getKey();
				
				Vector<Double> ierVec = rec.getValue();
				if(ierVec==null||ierVec.isEmpty()){
					log.warn("empty list!");
				}else{
					double latestTimer=ierVec.lastElement().doubleValue();
					if(System.currentTimeMillis()-latestTimer>=IterateTimeout){
						ier.remove();
						if(cachedReceivedSequenceNumber.containsKey(target)){
							cachedReceivedSequenceNumber.remove(target);
						}
					}
				}
			}
		
		}
/*		execpacketTrain.execute(new Runnable(){
		@Override
		public void run() {*/
			// TODO Auto-generated method stub
//			log.debug("List the cachedReceivedLatestStamp:");
//			//printCached(cachedReceivedSequenceNumber);
//			
//			Iterator<AddressIF> ier = cachedReceivedStamp.keySet().iterator();	
//			AddressIF remoteAddr;
//			while(ier.hasNext()){
//				remoteAddr=ier.next();
//				
//				//response packet
//				if(!cachedReceivedStamp.containsKey(remoteAddr)){
//					continue;				
//				}else{
//					long elapsed=cachedReceivedStamp.get(remoteAddr).lastElement().longValue()-cachedReceivedStamp.get(remoteAddr).get(0).longValue();
//					//not timeouted
//						
//					log.debug("do statistics!");
//					//printCached(cachedReceivedSequenceNumber);
//					
//					//save all sequence no into the packet
//					synchronized(cachedReceivedSequenceNumber.get(remoteAddr)){
//					byte[] sequenceNos=NetUtil.serializeObject(cachedReceivedSequenceNumber.get(remoteAddr));						
//					comm.sendPacket(statisticsTrainNo, myContactAddress, getInQueryId(), sequenceNos,remoteAddr, null);	
//					}	
//					
//					
//				}
//				
//			}

	/*		
		}
		
	});*/
	}
	
	/**
	 * create a packet
	 * @param len
	 * @return
	 */
	public byte[] setProbePacket(int sequenceNo, int packetType, int packetSize, byte[] content){
		
		assert(packetSize>2);
		//content
		int idx=packetIDX;
		
		int len=idx;
		len+=packetSize;
				
		byte[] data=new byte[len];
		
		//sequence number
		data[0]=(byte)(sequenceNo >> 8);
		data[1]=(byte)(sequenceNo  & 0xff);
		
		//packet train standard field
		
		data[2]=(byte)(packetType >> 8);
		data[3]=(byte)(packetType  & 0xff);
		
		
		//content length
		data[4]=(byte)(packetSize >> 8);
		data[5]=(byte)(packetSize  & 0xff);
		
	
		//ipAddressAndport
		if(this.myContactAddress==null){
			log.warn("my contact address is: NULL");
		}
		Integer  intIPAddr = ((NetAddress)this.myContactAddress).getIntIPAddr();

		data[6]=(byte) ((intIPAddr >> 24) & 0xff);
		data[7]=(byte) ((intIPAddr >> 16) & 0xff);
		data[8]=(byte) ((intIPAddr >> 8) & 0xff);
		data[9]=(byte)  (intIPAddr & 0xff);
		
		
		Integer intPort =((NetAddress)this.myContactAddress).getPort();
		data[10]=(byte) ((intPort  >> 8) & 0xff);
		data[11]=(byte)  (intPort  & 0xff);
		
		

		System.arraycopy(content,0,data,idx,content.length);
		
		//
		return data;		
	}
	
/**
 * 	
 * @param sequenceNo
 * @param packetType
 * @param packetSize
 * @return
 */
public byte[] setProbePacket(int sequenceNo, int packetType, int packetSize){
		
		assert(packetSize>2);
		
		byte[]content=new byte[packetSize];
		int rand=1;
		for(int i=0;i<packetSize;i++){
			content[i]=(byte)(rand >>8);
		}
		
		//content
		int idx=packetIDX;
		
		int len=idx;
		len+=packetSize;
				
		byte[] data=new byte[len];
		
		//sequence number
		data[0]=(byte)(sequenceNo >> 8);
		data[1]=(byte)(sequenceNo  & 0xff);
		
		//packet train standard field
		
		data[2]=(byte)(packetType >> 8);
		data[3]=(byte)(packetType  & 0xff);
		
		
		//content length
		data[4]=(byte)(packetSize >> 8);
		data[5]=(byte)(packetSize  & 0xff);
		

		//ipAddressAndport
		Integer  intIPAddr = ((NetAddress)this.myContactAddress).getIntIPAddr();

		data[6]=(byte) ((intIPAddr >> 24) & 0xff);
		data[7]=(byte) ((intIPAddr >> 16) & 0xff);
		data[8]=(byte) ((intIPAddr >> 8) & 0xff);
		data[9]=(byte)  (intIPAddr & 0xff);
		
		
		Integer intPort =((NetAddress)this.myContactAddress).getPort();
		data[10]=(byte) ((intPort  >> 8) & 0xff);
		data[11]=(byte)  (intPort  & 0xff);
		

		System.arraycopy(content,0,data,idx,content.length);
		
		//
		return data;		
	}
	
	
	/**
	 * in query
	 * @return
	 */
	public int getSendQueryId() {

		return (sendQueryId++)%Integer.MAX_VALUE;

	}
	
	/**
	 * out query
	 * @return
	 */
	public int getInQueryId() {

		return (inQueryId++)%Integer.MAX_VALUE;

	}
	
	
	protected Object unmarshallMsg(byte[] msgArray) {
		Object msg = NetUtil.deserializeObject(msgArray);
		// log.debug("message=" + msg);
		return msg;
	}
	
	/**
	 * arraylist
	 * @param message
	 * @return
	 */
	private byte[] marshallMsg(Object message) {
		// log.debug("message=" + message);
		byte[] byteArray = NetUtil.serializeObject(message);
		// log.debug("message2=" + Util.deserializeObject(byteArray));
		return byteArray;
	}
	
	/**
	 * 
	 * @param packetType
	 * @return
	 */
	private String getPacketType(int packetType){
		if(packetType==this.probepacketTrainNo){
			return "probepacketTrainNo";
		}else if(packetType==this.responseTrainNo){
			return "responseTrainNo";
		}else if(packetType==this.statisticsTrainNo){
			return "statisticsTrainNo";
		}else{
			return "unknown";
		}
		
	}
	
	
	public static void main(String[] args){
		ANSI.use(true);

		EL.set(new EL());

		final int sendPacket=Integer.parseInt(args[0]); //0 server, 1 client
		
		final packetTrainComm comm = new packetTrainComm();
		
		
		final AddressIF addr=AddressFactory.createUnresolved("127.0.0.1", 55532);
		
		final AddressIF newaddr=AddressFactory.createUnresolved("127.0.0.1", 55534);
		
		comm.initServer(newaddr, new CB0(){
			protected void cb(CBResult result) {
				// TODO Auto-generated method stub
			switch(result.state){	
			
			case OK:{
			if(sendPacket==1){	
			comm.sendProbeTrains(addr, 1024, 8, new CB1(){
				protected void cb(CBResult result, Object arg1) {
					// TODO Auto-generated method stub
					switch(result.state){
					case OK:{
						//System.out.println("capacity: "+((Double)arg1).doubleValue());
						double[] results=(double[])arg1;
						System.out.println("capacity: "+results[0]);
						System.out.println("reorder: "+results[1]);
						break;
					}
					case ERROR:
					case TIMEOUT:{
						System.err.println("timeout");
						break;
					}
					}
				}			
			});	
			}else{
				System.out.println("has initialized!");
			}
			
			System.out.println("OK!");
			break;
			}
			case ERROR:
			case TIMEOUT:{
				System.err.println("Initialized timeout");
				break;
			}
			}
			
			
			}		
		});
		
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
