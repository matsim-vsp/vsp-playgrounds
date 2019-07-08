/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.greedo;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestOneOverAgeApprox {

	public static void main(String[] args) {
		
		final Random rnd = new Random();
		final int[] _Deltas = new int[] {-1, 1};
		final double[] _DeltaX2 = new double[11];
		
		
		int maxOuter = 10;
		int maxInner = 10;
		
		for (int outer = 0; outer < maxOuter; outer++) {
			for (int inner = 0; inner < maxInner; inner++) {
				
				
				
			}
		}
		
		final double _DeltaXni = rnd.nextInt(3) - 1;
		final double age = rnd.nextDouble() * 10;
		
		
		
	}
	
}

