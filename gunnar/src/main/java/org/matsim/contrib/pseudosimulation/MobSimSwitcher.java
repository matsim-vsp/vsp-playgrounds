package org.matsim.contrib.pseudosimulation;

import org.apache.log4j.Logger;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MobSimSwitcher
		implements IterationEndsListener, IterationStartsListener, BeforeMobsimListener /* , Provider<Mobsim> */ {

	@Inject
	private PSimProvider pSimProvider;
	@Inject
	private Config config;

	private boolean isQSimIteration = true;

	public PSimProvider getpSimProvider() {
		return pSimProvider;
	}

	public boolean isQSimIteration() {
		return isQSimIteration;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.isQSimIteration = (event.getIteration()
				% ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class).getIterationsPerCycle() == 0);
		if (this.isQSimIteration) {
			Logger.getLogger(this.getClass()).warn("Running full queue simulation");
		} else {
			Logger.getLogger(this.getClass()).info("Running PSim");
			this.pSimProvider.registerAllSelectedPlansForPSim();
		}
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		if (!this.isQSimIteration()) {
			this.pSimProvider.keepOnlyChangedPlansForPSim();
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (!this.isQSimIteration()) {
			this.pSimProvider.reconstructScoreOfPlansNotInPSim();
		}
	}

	// Taking over SwitchingMobsimProvider

	// @Inject
	// private QSimProvider qsimProvider;
	//
	// @Override
	// public Mobsim get() {
	// if (this.isQSimIteration()) {
	// return qsimProvider.get();
	// } else {
	// return pSimProvider.get();
	// }
	// }

}
