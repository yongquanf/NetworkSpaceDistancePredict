/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.NUDT.PDL.RatingMessages;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import edu.NUDT.PDL.coordinate.Vec;
import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.harvard.syrah.sbon.async.comm.AddressIF;
import edu.harvard.syrah.sbon.async.comm.obj.ObjMessage;

/**
 * Response to GossipRequestMsg. Responds with the receiving node's current
 * coordinate, confidence and last update time.
 */
public class CoordGossipResponseMsg extends ObjMessage {

	static final long serialVersionUID = 18L;

	public final ratingCoord<AddressIF> remoteCoord;
	public final double ratingError;
	
	public AddressIF from;

	public final Set<AddressIF> nodes;
	public Vec separators;

	public CoordGossipResponseMsg(ratingCoord<AddressIF> _remoteCoord,
			Set<AddressIF> _nodes, AddressIF _from, Vec  _separators, double _ratingError) {
		if(_remoteCoord!=null){
			remoteCoord = _remoteCoord.copy();
		}else{
			remoteCoord =null;
		}
		nodes = new HashSet<AddressIF>(2);
		if(_nodes!=null){
			nodes.addAll(_nodes);
		}
		from = _from;
		if(_separators!=null){
			separators=new Vec(_separators);
		}else{
			separators=null;
		}
		ratingError=_ratingError;
	}
	
	public CoordGossipResponseMsg(ratingCoord<AddressIF> _remoteCoord,
			Set<AddressIF> _nodes, AddressIF _from,int ratinglevel, Vec  _separators, double _ratingError) {
		if(_remoteCoord!=null){
			remoteCoord = _remoteCoord.copy();
		}else{
			remoteCoord =null;
		}
	
		nodes = new HashSet<AddressIF>(2);
		if(_nodes!=null){
			nodes.addAll(_nodes);
		}
		from = _from;
		if(_separators!=null){
			separators=new Vec(_separators);
		}else{
			separators=null;
		}
		ratingError=_ratingError;
	}
	
	public CoordGossipResponseMsg(ratingCoord<AddressIF> _remoteCoord,int ratinglevel,Vec  _separators,  double _ratingError) {
		remoteCoord = _remoteCoord;
		if(_separators!=null){
			separators=new Vec(_separators);
		}else{
			separators=null;
		}
		nodes = null;
		from = null;
		ratingError=_ratingError;

	}

	public ratingCoord<AddressIF> getRemoteCoord() {
		return remoteCoord;
	}

	/**
	 * set separators
	 * @param t
	 */
	public void setRatingSeparators( Vec  t){
		if(t!=null){
			separators=t.copy();
		}
	}
	
	
}
