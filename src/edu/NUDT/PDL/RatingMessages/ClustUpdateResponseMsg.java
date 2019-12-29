package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

class ClustUpdateResponseMsg extends ObjMessage {
	static final long serialVersionUID = 65L;
	public AddressIF from;

	ClustUpdateResponseMsg(AddressIF _from) {
		from = _from;
	}
}
