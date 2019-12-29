package edu.NUDT.PDL.RatingMessages;


import java.io.Serializable;

import edu.harvard.syrah.sbon.async.comm.AddressFactory;
import edu.harvard.syrah.sbon.async.comm.AddressIF;

public class NodesPair<T> implements Serializable{
	
	static final long serialVersionUID = 1000000001L;

	final static protected int CLASS_HASH = NodesPair.class.hashCode();

	
	public T startNode;
	public T endNode;
	public double value;

	public NodesPair(){}
	public NodesPair(T _startNode, T _endNode, double _value){
		startNode = _startNode;
		endNode = _endNode;
		value = _value;
	}
	void setvalue(double _value){
		value = _value;
		return;
	}
	
	/*public void toSerialized(DataOutputStream dos) throws IOException {
		
		dos.writeByte(version);
		for (int i = 0; i < num_dims; ++i) {
			// when writing, cast to float
			dos.writeFloat((float) coords[i]);
		}
		// if (VivaldiClient.USE_HEIGHT) dos.writeFloat((float) coords[num_dims]);
	}
	*/
	public NodesPair makeCopy(){
		NodesPair cpy=new NodesPair(startNode, endNode, value);
		return cpy;
	}
	
	public int hashCode() {
		int hc = CLASS_HASH;
		hc^=startNode.hashCode();
		hc^=endNode.hashCode();
		return hc;
	}
	public String toString(){
		final StringBuffer sbuf = new StringBuffer(1024);
		sbuf.append("(");
		sbuf.append(startNode.toString());
		sbuf.append(",");
		sbuf.append(endNode.toString());
		sbuf.append(",");
		sbuf.append(value);
		sbuf.append(")");
		
	
		return sbuf.toString();
		
	}
}