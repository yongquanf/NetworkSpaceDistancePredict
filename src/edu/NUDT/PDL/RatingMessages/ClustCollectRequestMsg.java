package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

class ClustCollectRequestMsg extends ObjMessage {

	static final long serialVersionUID = 62L;
	public AddressIF from;

	ClustCollectRequestMsg(AddressIF _from) {

		from = AddressFactory.create(_from);
	};
}
