package edu.NUDT.PDL.RatingMessages;

import java.util.Set;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

class ClustUpdateRequestMsg extends ObjMessage {
	static final long serialVersionUID = 65L;
	public AddressIF from;
	Set<AddressIF> landmarks;
	final long timeStamp;

	ClustUpdateRequestMsg(AddressIF _from, Set<AddressIF> _landmarks,
			long _timeStamp) {
		from = _from;
		landmarks = _landmarks;
		timeStamp = _timeStamp;

	}
}
