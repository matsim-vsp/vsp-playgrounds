package ch.ethz.matsim.ier;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansReplanning;

import ch.ethz.matsim.ier.emulator.AgentEmulator;
import ch.ethz.matsim.ier.emulator.FirstSimpleSimulationEmulator;
import ch.ethz.matsim.ier.emulator.SimulationEmulator;

/**
 * This module overrides the default replanning.
 * 
 * @author shoerl
 */
public final class IERModule extends AbstractModule {
	@Override
	public void install() {
		bind(PlansReplanning.class).to(IERReplanning.class);
		bind(AgentEmulator.class);

		// We choose the simple emulator for now.
		bind(SimulationEmulator.class).to(FirstSimpleSimulationEmulator.class);
	}
}
