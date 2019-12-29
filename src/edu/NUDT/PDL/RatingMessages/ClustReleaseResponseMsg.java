package edu.NUDT.PDL.RatingMessages;

import java.util.Hashtable;
import java.util.List;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

public class ClustReleaseResponseMsg extends ObjMessage {
	static final long serialVersionUID = 64L;
	public AddressIF from;
	public  Hashtable<AddressIF,ratingCoord<AddressIF>> ratingCache;
	final List<AddressIF> landmarks;
	public final Vec  outSeparators;
	final long version;

	ClustReleaseResponseMsg(AddressIF _from, Hashtable<AddressIF,ratingCoord<AddressIF>> _ratingCache,
			List<AddressIF> _landmarks, long _version,Vec _outSeparators) {
		if(_from!=null){
		from = AddressFactory.create(_from);
		}else{
			from=null;
		}
		ratingCache=null;
		if(_ratingCache!=null){
			ratingCache=new  Hashtable<AddressIF,ratingCoord<AddressIF>>(2);
			ratingCache.putAll(_ratingCache);
		}
		landmarks = _landmarks;
		version = _version;
		if( _outSeparators!=null){
			outSeparators=new Vec(_outSeparators);
		}else{
			outSeparators=null;
		}
	}
}