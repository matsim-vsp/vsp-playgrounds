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
package gunnar.ihop2.scaper;

public class TryPopulationSampler {
	public static void main(String[] args){
		String srcpop = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_100%_regent.xml";
		String destpop = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_ppp1%_regent.xml";
		String network = "";
		double sample = 0.00001;
		PopulationSampler popsampler = new PopulationSampler();
		//Call this method with null if network file is not required, otherwise pass network file path
		popsampler.createSample(srcpop, network, sample, destpop);
		}

}
