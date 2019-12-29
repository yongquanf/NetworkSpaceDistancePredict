package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class CluDatResponseMsg extends ObjMessage {
	static final long serialVersionUID = 61L;
	public AddressIF from;

	CluDatResponseMsg(AddressIF _from) {
		from = AddressFactory.create(_from);
	}
}
