package edu.NUDT.PDL.BiasMatrixApproximation;

import edu.NUDT.PDL.RatingMessages.NodesPair;

public class Pair<T> {

	public T from;
	public T to;
	
	public double sample;
	
	
	public Pair(T _from, T _to,double _sample){
		from=_from;
		to=_to;
		sample=_sample;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}

}
