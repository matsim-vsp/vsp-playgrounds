/*
 * Copyright 2018 Mohammad Saleem
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
 * contact: salee@kth.se
 *
 */ 
package saleem.ptoptimisation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.utilities.TimeDiscretization;
import saleem.ptoptimisation.optimisationintegration.PTObjectiveFunction;
import saleem.ptoptimisation.optimisationintegration.PTSchedule;
import saleem.ptoptimisation.optimisationintegration.PTScheduleRandomiser;
import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;

/**
 * An execution class for PT Optimisation.
 * 
 * @author Mohammad Saleem
 *
 */
public class OptimisePT {
	@SuppressWarnings({ "rawtypes", "unused" })
	public static void main(String[] args) {
		System.out.println("STARTED ...");
		
		String path = "./ihop2/matsim-input/configoptimisationcarpt.xml";
        Config config = ConfigUtils.loadConfig(path);
        MatsimServices controler = new Controler(config);
        final Scenario scenario = ScenarioUtils.loadScenario(config);
        double samplesize = 0.05;//One can get this directly from Config file too through config.qsim().getStorageCapFactor()
        						 //Hardcoded since the car network is scaled 1.5 times that of PT for this particulat scenario.
		final String originalOutputDirectory = scenario.getConfig().controler()
				.getOutputDirectory(); // gets otherwise overwritten in config
		final AbstractModule module = new ControlerDefaultsModule();
		
		final int maxMemorizedTrajectoryLength = Integer.MAX_VALUE;  // revisit this based on available RAM
		final boolean interpolate = true; // always
		final int maxRandomSearchIterations = 500; // comp. time per iteration is approx. duration of two full simulations
		final int maxRandomSearchTransitions = Integer.MAX_VALUE; // revisit this later
		final boolean includeCurrentBest = false; // always
		final ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
				500,250);
		final TimeDiscretization timeDiscretization = new TimeDiscretization(0,
				3600, 30); // OK to start with
		final ObjectiveFunction objectiveFunction = new PTObjectiveFunction(scenario); // TODO this is minimized
		
		// Changing vehicle and road capacity according to sample size
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);
		
		Network network = scenario.getNetwork();

		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		final PTSchedule ptschedule = new PTSchedule(scenario, scenario.getTransitSchedule(),scenario.getTransitVehicles());//Decision Variable
			
		final DecisionVariableRandomizer<PTSchedule> decisionVariableRandomizer = 
				new PTScheduleRandomiser(scenario, ptschedule);
		
		final double occupancyScale = 1.0;
		
//		@SuppressWarnings("unchecked")		
//		final MATSimSimulator2<PTSchedule> matsimSimulator = new MATSimSimulator2(
//				new PTMatsimStateFactoryImpl<>(scenario, occupancyScale),
//				scenario);
//		matsimSimulator.addSimulationStateAnalyzer(new PTOccupancyAnalyzer.Provider(timeDiscretization,
//				new LinkedHashSet<>(scenario.getTransitSchedule().getFacilities().keySet())));
//		
//		matsimSimulator.setReplacingModules(module);
//		final RandomSearch<PTSchedule> randomSearch = new RandomSearch<>(
//				matsimSimulator, decisionVariableRandomizer, ptschedule,
//				convergenceCriterion, maxRandomSearchIterations,
//				maxRandomSearchTransitions, 8,
//				objectiveFunction);
//		randomSearch.setIncludeCurrentBest(includeCurrentBest);
//		randomSearch.setInterpolate(interpolate);
//		randomSearch.setRandom(MatsimRandom.getRandom());
//		
//		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		randomSearch.setLogFileName(originalOutputDirectory + "opdyts.log");
//		randomSearch.setConvergenceTrackingFileName(originalOutputDirectory
//				+ "opdyts.con");
//		randomSearch.setOuterIterationLogFileName(originalOutputDirectory
//				+ "opdyts.opt");
//		
//		final SelfTuner tuner = new SelfTuner(0.95);
//		tuner.setNoisySystem(true);
//		randomSearch.setSelfTuner(tuner);
//		
//		randomSearch.run();
		
		System.out.println("... DONE.");
	}
}
