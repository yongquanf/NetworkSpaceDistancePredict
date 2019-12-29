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
/*
 * @author Last modified by $Author: ledlie $
 * @version $Revision: 1.3 $ on $Date: 2008/11/05 01:22:49 $
 * @since Mar 7, 2006
 */

package edu.NUDT.PDL.RatingMessages;

public class EWMAStatistic {

	public static final double GAIN = 0.01;

	protected final double gain;

	protected double value;

	/*public EWMAStatistic(double g) {

		gain = g;

		value = 0;

	}*/
	public EWMAStatistic(double initiaValue) {

		gain = GAIN;

		value = initiaValue;

	}
	public EWMAStatistic() {

		gain = GAIN;

		value = 0;

	}

	synchronized public void add(double item) {
		//first element, directly assign
		//otherwise, use the EW 
		if(value==0){
			value=item;
		}else{
		value = (GAIN * item) + ((1. - GAIN) * value);
		}
	}

	synchronized public double get() {

		return value;

	}

	public static void main(String[]args){
		EWMAStatistic test=new EWMAStatistic();
		System.out.println("V1: "+test.get());
		test.add(56.9);
		System.out.println("V2: "+test.get());
	}
}
