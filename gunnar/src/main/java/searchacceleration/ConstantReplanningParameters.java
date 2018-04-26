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
package searchacceleration;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConstantReplanningParameters implements ReplanningParameterProvider {

	private final double meanLambda;

	private final double delta;

	public ConstantReplanningParameters(final double meanLambda, final double delta) {
		this.meanLambda = meanLambda;
		this.delta = delta;
	}

	@Override
	public double getMeanLambda(final int iteration) {
		return this.meanLambda;
	}

	@Override
	public double getDelta(final int iteration) {
		return this.delta;
	}
}
