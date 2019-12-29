package edu.NUDT.PDL.BasicDistributedMMMF;

import java.util.Hashtable;
import java.util.List;

import edu.NUDT.PDL.coordinate.ratingCoord;
import edu.NUDT.PDL.util.matrix.Matrix;

public interface AbstractDistributedMMMF<T> {

	/**
	 * compute landmarks coordinates
	 * @param timer
	 * @param landmarks
	 * @param centralizedRatingMat
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> processSamplesLandmarks_(long timer, List<T> landmarks,
			Matrix centralizedRatingMat);
	
	/**
	 * compute the hosts' coordinates
	 * @param timer
	 * @param hosts
	 * @param landmarks
	 * @param host2Landmarks
	 * @param landmark_x
	 * @return
	 */
	public Hashtable<T,ratingCoord<T>> processRatingSamplesHost(long timer,List<T> hosts, 
			List<T> landmarks, Matrix host2Landmarks, Hashtable<T,ratingCoord<T>> landmark_x);
	
	
	/**
	 * online update a host's coordinate, each time, move a small step
	 * @param timer
	 * @param host
	 * @param x
	 * @param landmarks
	 * @param landmark_x
	 * @param host2Landmarks
	 * @return
	 */
	public ratingCoord<T> processRatingSamplesHost(long timer,T host,ratingCoord<T> x, 
			List<T> landmarks, Hashtable<T,ratingCoord<T>> landmark_x,Matrix host2Landmarks);
}
