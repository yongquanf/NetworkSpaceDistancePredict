package edu.NUDT.PDL.RatingMessages;

import java.util.HashSet;
import java.util.Set;

import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class LandmarkResponseMsg extends ObjMessage {
	static final long serialVersionUID = 66L;

	public final Set<AddressIF> nodes;
	public AddressIF from;
	LandmarkResponseMsg(Set<AddressIF> _nodes, AddressIF _from) {
		nodes = new HashSet<AddressIF>(2);
		nodes.addAll(_nodes);
		from=_from;
	};
}
