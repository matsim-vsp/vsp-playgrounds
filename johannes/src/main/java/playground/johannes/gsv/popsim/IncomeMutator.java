/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.popsim;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Random;

/**
 * @author johannes
 *
 */
public class IncomeMutator extends AttributeMutator {

	private final Random random;

	public IncomeMutator(Random random, HistogramSync histSync) {
		super(random, CommonKeys.HH_INCOME, DistanceVector.INCOME_KEY, histSync);
		this.random = random;
	}

	@Override
	protected Double newValue(PlainPerson person) {
		return new Double(random.nextInt(8000));
	}

}
