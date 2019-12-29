package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class LandmarkRequestMsg extends ObjMessage {

	static final long serialVersionUID = 66L;
	public AddressIF from;
	final long timeStamp;

	public LandmarkRequestMsg(AddressIF _from, long _timeStamp) {

		from = AddressFactory.create(_from);
		timeStamp = _timeStamp;
	};
}
