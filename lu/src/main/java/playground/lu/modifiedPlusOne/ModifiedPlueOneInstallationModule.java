package playground.lu.modifiedPlusOne;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.FastHeuristicLinkBasedRelocationCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.LinkBasedRelocationCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

public class ModifiedPlueOneInstallationModule extends AbstractDvrpModeQSimModule {

	private static final Logger log = Logger.getLogger(ModifiedPlusOneRebalancingStrategy.class);

	public ModifiedPlueOneInstallationModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
	}

	@Override
	protected void configureQSim() {
		log.info("Modified Plus one rebalancing strategy is now being installed!");
		bindModal(ModifiedPlusOneRebalancingStrategy.class)
				.toProvider(modalProvider(getter -> new ModifiedPlusOneRebalancingStrategy(getMode(),
						getter.getModal(Network.class), getter.getModal(LinkBasedRelocationCalculator.class))))
				.asEagerSingleton();

		bindModal(LinkBasedRelocationCalculator.class)
				.toProvider(modalProvider(getter -> new FastHeuristicLinkBasedRelocationCalculator()))
				.asEagerSingleton();

		bindModal(RebalancingStrategy.class).to(modalKey(ModifiedPlusOneRebalancingStrategy.class));
		// binding event handler
		addMobsimScopeEventHandlerBinding().to(modalKey(ModifiedPlusOneRebalancingStrategy.class));
	}

}
