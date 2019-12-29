package edu.NUDT.PDL.RatingMessages;

import java.util.Hashtable;

import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustIssueRequestMsg extends ObjMessage {
	static final long serialVersionUID = 63L;
	public AddressIF from;
	public int IndexOfFrom;
	public  Hashtable<AddressIF,ratingCoord<AddressIF>> ratingCache;
	final long timeStamp;
	final long version;

	ClustIssueRequestMsg(AddressIF _from,Hashtable<AddressIF,ratingCoord<AddressIF>> _ratingCache,
			int _IndexOfFrom, long _timeStamp, long _version) {
		from = _from;
		ratingCache=new  Hashtable<AddressIF,ratingCoord<AddressIF>>(2);
		ratingCache.putAll(_ratingCache);
		
		IndexOfFrom = _IndexOfFrom;
		timeStamp = _timeStamp;
		version = _version;
	}
	ClustIssueRequestMsg(AddressIF _from,Hashtable<AddressIF,ratingCoord<AddressIF>> _ratingCache,
			long _timeStamp, long _version) {
		from = _from;
		ratingCache=new  Hashtable<AddressIF,ratingCoord<AddressIF>>(2);
		ratingCache.putAll(_ratingCache);
		
		IndexOfFrom = -1;
		timeStamp = _timeStamp;
		version = _version;
	}
}
