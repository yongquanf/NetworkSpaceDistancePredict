package edu.NUDT.PDL.measurement;

import java.util.Hashtable;

import edu.harvard.syrah.sbon.async.CallbacksIF.CB1;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public interface packetTrainIF {

	/**
	 * send the packet trains
	 * @param cachedProbeTrains
	 * @param remoteNode
	 * @param packetsize
	 * @param PINGCounter
	 * @param PING_DELAY
	 * @param cbPing
	 */
	public void sendProbeTrains(final AddressIF remoteNode,final int packetsize,
			final int[] PINGCounter,final int PING_DELAY);
}
