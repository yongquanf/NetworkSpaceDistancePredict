package edu.NUDT.PDL.RatingMessages;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustIssueResponseMsg extends ObjMessage {
	static final long serialVersionUID = 63L;
	public AddressIF from;

	ClustIssueResponseMsg(AddressIF _from) {
		from = _from;
	}
}
