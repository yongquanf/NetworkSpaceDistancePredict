package edu.NUDT.PDL.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.NUDT.PDL.RatingMessages.RankingManager;
import edu.NUDT.PDL.measurement.MeasurementClient;
import edu.NUDT.PDL.measurement.packetTrainComm;
import edu.NUDT.PDL.util.MainGeneric;
import edu.harvard.syrah.prp.ANSI;
import edu.harvard.syrah.prp.Log;
import edu.harvard.syrah.prp.PUtil;
import edu.harvard.syrah.sbon.async.CBResult;
import edu.harvard.syrah.sbon.async.Config;
import edu.harvard.syrah.sbon.async.EL;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB0;
import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjComm;
import edu.harvard.syrah.sbon.async.comm.obj.ObjCommIF;

public class RankingLoader {

	private static final Log log = new Log(RankingLoader.class);

	private static final int MAJOR_VERSION = 0;
	private static final int MINOR_VERSION = 1;
	private static final int MINOR_MINOR_VERSION = 4;
	public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION
			+ "." + MINOR_MINOR_VERSION;

	static {

		// All config properties in the file must start with 'HSHer.'

		Config.read("Ranking", System
				.getProperty("Ranking.config", "config/Ranking.cfg"));
	}

	// Imperial blocks ports outside of 55000-56999
	public static final int COMM_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("port", "55504"));
	public static final int MeasureCOMM_PORT = Integer.parseInt(Config
			.getConfigProps().getProperty("MeasureCOMM_PORT", "55515"));

	public static final boolean USE_ICMP = Boolean.parseBoolean(Config
			.getConfigProps().getProperty("use_icmp", "false"));

	public static final String[] myRegion = Config.getConfigProps()
			.getProperty("myDNSAddress", "").split("[\\s]");
	
	
	//all nodes
	public static final String AllNodes=Config.getProperty("AllNodes",
	"nodeList4Nina.src");
	public 	List<String>  AllAlivecachedNodeStrings=new ArrayList<String>(2);
	
	public static AddressIF me;

	private static final long START_TIME = System.currentTimeMillis();

	public static BufferedWriter logRanking = null;
	public static BufferedWriter logCluster =null;
	public static BufferedWriter logControlTraffic=null;
	
	
	String RankingLogName="recordRawMeasurement";
	String coordName="coordinate";
	String controlLog="controlLog";
	
	public static Random random;
	// private Ninaloader NiNAer = null;

	private ObjCommIF comm;
	
	private static packetTrainComm measureComm;
	
	
	public static long StartTime;
	
	public static ExecutorService execNina = Executors.newFixedThreadPool(15);
	
	/**
	 * ranking manager
	 */
	public RankingManager  rankManager;
	
	/**
	 * perform measurement
	 */
	public MeasurementClient measureClient;
	
	public RankingLoader(){
		
		try {
			logRanking = new BufferedWriter(new FileWriter(new File(
					RankingLogName + ".log")));
			logCluster= new BufferedWriter(new FileWriter(new File(
					coordName + ".log")));
			logControlTraffic= new BufferedWriter(new FileWriter(new File(controlLog
					+ ".log")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StartTime = System.currentTimeMillis();
	}
	
	/**
	 * measurement comm
	 * @return
	 */
	public static packetTrainComm getMeasurementComm(){
		return measureComm;
	}
	
	
	/**
	 * init the measurement collection procedure
	 * @param cbDone
	 */
	private void init(final CB0 cbDone){
		
		random = new Random(System.currentTimeMillis());
		
		final AddressIF MeasureCommAddr = AddressFactory.create(me, MeasureCOMM_PORT);
		
		//parse the input file
		AllAlivecachedNodeStrings = MainGeneric.parseAllNodes(AllNodes, MeasureCOMM_PORT);
		
		//register the trigger to probe
		measureClient = new MeasurementClient();
		//init the measurement scheduler
		measureClient.init(AllAlivecachedNodeStrings, logRanking);
		
		//init the prober
		measureComm=new packetTrainComm();
		measureComm.initServer(MeasureCommAddr, cbDone);

		
	}
	
	
	
	
	private void init0(final CB0 cbDone) {
		random = new Random(System.currentTimeMillis());

		// Initiliase the ObjComm communication module
		comm = new ObjComm();
		//final AddressIF objCommAddr = AddressFactory.createServer(COMM_PORT);
		
		final AddressIF objCommAddr = AddressFactory.create(me, COMM_PORT);
		
		final AddressIF MeasureCommAddr = AddressFactory.create(me, MeasureCOMM_PORT);
		
		
		
		log.debug("Starting objcomm server...");
		comm.initServer(objCommAddr, new CB0() {
			
			protected void cb(CBResult result) {
				switch (result.state) {
				case OK: {
					// Initialise the measurement modules
					rankManager = new RankingManager(comm);
					log.info("Initialising the PingManager...");
					rankManager.init(new CB0() {
												
							protected void cb(
									CBResult result2) {
								// TODO
								// Auto-generated
								// method
								// stub
								switch (result2.state) {
								case OK: {
									//TODO, initialize the ranking process correctly
									//bandwidth measurement
									if(MainGeneric.pingType==2){
									//initialize the measurement comm
										measureComm=new packetTrainComm();
										measureComm.initServer(MeasureCommAddr, cbDone);
									}
									//cbDone.call(result2);	
									break;
								}
								default: {
									cbDone
											.call(result2);
									break;
								}
								}

							}

						}
						);
					}
				default: {
					cbDone.call(result);
				}
				}
			}
		});
	}
	

	public static String getUptimeStr() {
		long now = System.currentTimeMillis();

		return PUtil.getDiffTimeStr(START_TIME, now);
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.main("Ranking Version " + VERSION + " starting...");

		// TODO JTL: do you GetOpt magic here
		// we need a switch to specify the config file and to turn on debugging

		if (args.length > 0 && args[0].equals("-d")) {
			Log.setPackageRoot(RankingLoader.class);
			String[] newArgs = new String[args.length - 1];
			for (int i = 0; i < newArgs.length; i++)
				newArgs[i] = args[i];
			args = newArgs;
		}

		// Turn on assertions
		boolean assertsEnabled = false;
		assert assertsEnabled = true;
		if (!assertsEnabled)
			log
					.error("Please run the code with assertions turned on: java -ea ...");

		// Turn on colour support
		ANSI.use(Boolean.valueOf(Config.getConfigProps().getProperty(
				"sbon.console.ansi", "true")));

		/*
		 * Create the event loop
		 */
		EL.set(new EL(Long.valueOf(Config.getConfigProps().getProperty(
				"sbon.eventloop.statedump", "600000")), Boolean.valueOf(Config
				.getConfigProps().getProperty("sbon.eventloop.showidle",
						"false"))));
		
		

		final RankingLoader NiNAer = new RankingLoader();

							try {
								
						 
						AddressFactory.createResolved(Arrays.asList(myRegion),RankingLoader.COMM_PORT, new CB1<Map<String, AddressIF>>() {
						   					
						   					protected void cb(CBResult result,
						   							Map<String, AddressIF> addrMap) {
						   						switch (result.state) {
						   						case OK: {
						   
						   							for (String node : addrMap.keySet()) {
						   
						   								me = addrMap.get(node);
						   								//me = AddressFactory.create(remoteAddr);
						   								log.debug("MyAddr='" + me + "'");
						   								// System.exit(-1);
						   								break;
						   							}
						   
						   							try {
						   								// init
						   								NiNAer.init(new CB0() {
						   									
						   									protected void cb(CBResult result) {
						   										switch (result.state) {
						   										case OK: {
						   											log.main("NiNAer node initialised successfully");
						   											break;
						   										}
						   										case TIMEOUT:
						   										case ERROR: {
						   											log
						   													.error("Could not initialise NiNAer node: "
						   															+ result.toString());
						   											break;
						   										}
						   										}
						   									}
						   								});
						   
						   							} catch (Exception e) {
						   								// TODO Auto-generated catch block
						   								e.printStackTrace();
						   							}
						   
						   							break;
						   
						   						}
						   						case TIMEOUT:
						   						case ERROR: {
						   							log.error("Could not resolve bootstrap list: "
						   									+ result.what);
						   							break;
						   						}
						   						}
						   					}
						   
						   				});	
							}catch(Exception e){
								e.printStackTrace();
							}

		try {
			EL.get().main();
		} catch (OutOfMemoryError e) {
			EL.get().dumpState(true);
			e.printStackTrace();
			log.error("Error: Out of memory: " + e);
		}
		try {

			logRanking.close();
			logCluster.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		log.main("Shutdown");
		System.exit(0);
	}
	
	
}
