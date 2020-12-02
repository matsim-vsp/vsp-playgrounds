package playground.lu.modifiedPlusOne;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.LinkBasedRelocationCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

/**
 * This modified Plus One Rebalancing Strategy should be used with a matching
 * algorithm that does not reject any requests
 * 
 */
public class ModifiedPlusOneRebalancingStrategy
		implements RebalancingStrategy, PassengerRequestSubmittedEventHandler, MobsimScopeEventHandler {

	private static final Logger log = Logger.getLogger(ModifiedPlusOneRebalancingStrategy.class);

	private final String mode;
	private final Network network;
	private final LinkBasedRelocationCalculator linkBasedRelocationCalculator;

	private final List<Id<Link>> targetLinkIdList = new ArrayList<>();

	public ModifiedPlusOneRebalancingStrategy(String mode, Network network,
			LinkBasedRelocationCalculator linkBasedRelocationCalculator) {
		this.mode = mode;
		this.network = network;
		this.linkBasedRelocationCalculator = linkBasedRelocationCalculator;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		List<? extends DvrpVehicle> rebalancableVehicleList = rebalancableVehicles.collect(toList());

		final List<Id<Link>> copiedTargetLinkIdList;
		synchronized (this) {
			// may happen in parallel to handling PassengerRequestScheduledEvent emitted by
			// UnplannedRequestInserter
			copiedTargetLinkIdList = new ArrayList<>(targetLinkIdList);
			targetLinkIdList.clear(); // clear the target map for next rebalancing cycle
		}

		final List<Link> targetLinkList = copiedTargetLinkIdList.stream().map(network.getLinks()::get)
				.collect(toList());

		log.debug("There are in total " + targetLinkList.size() + " rebalance targets at this time period");
		log.debug("There are " + rebalancableVehicleList.size() + " vehicles that can be rebalanced");

		// calculate the matching result
		return linkBasedRelocationCalculator.calcRelocations(targetLinkList, rebalancableVehicleList);
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			Id<Link> targetLinkId = event.getFromLinkId();
			synchronized (this) {
				targetLinkIdList.add(targetLinkId);
			}
		}

	}

}
