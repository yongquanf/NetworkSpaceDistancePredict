package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustReleaseRequestMsg extends ObjMessage {

	static final long serialVersionUID = 64L;

	public AddressIF from;

	ClustReleaseRequestMsg(AddressIF _from) {
		from = AddressFactory.create(_from);
	};
}