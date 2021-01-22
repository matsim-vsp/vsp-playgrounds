package playground.lu.congestionAwareDrt;

import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.RequestQueue;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

public class CongestionAwareDrtOptimizer implements DrtOptimizer {
	private static final Logger log = Logger.getLogger(CongestionAwareDrtOptimizer.class);

	private final DrtConfigGroup drtCfg;
	private final Integer rebalancingInterval;
	private final Fleet fleet;
	private final DrtScheduleInquiry scheduleInquiry;
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final RebalancingStrategy rebalancingStrategy;
	private final MobsimTimer mobsimTimer;
	private final EmptyVehicleRelocator relocator;
	private final UnplannedRequestInserter requestInserter;
	private final RequestQueue<DrtRequest> unplannedRequests;

	private final ReroutingStrategy reroutingStrategy;
	private final int rerouteInterval = 30; // TODO temporary

	public CongestionAwareDrtOptimizer(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
			RebalancingStrategy rebalancingStrategy, DrtScheduleInquiry scheduleInquiry,
			ScheduleTimingUpdater scheduleTimingUpdater, EmptyVehicleRelocator relocator,
			UnplannedRequestInserter requestInserter, ReroutingStrategy reroutingStrategy) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.rebalancingStrategy = rebalancingStrategy;
		this.scheduleInquiry = scheduleInquiry;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.relocator = relocator;
		this.requestInserter = requestInserter;
		this.reroutingStrategy = reroutingStrategy;
		rebalancingInterval = drtCfg.getRebalancingParams().map(RebalancingParams::getInterval).orElse(null);
		unplannedRequests = RequestQueue
				.withLimitedAdvanceRequestPlanningHorizon(drtCfg.getAdvanceRequestPlanningHorizon());
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		unplannedRequests.updateQueuesOnNextTimeSteps(e.getSimulationTime());
		if (!unplannedRequests.getSchedulableRequests().isEmpty()) {
			for (DvrpVehicle v : fleet.getVehicles().values()) {
				scheduleTimingUpdater.updateTimings(v);
			}
			requestInserter.scheduleUnplannedRequests(unplannedRequests.getSchedulableRequests());
		}

		if (rebalancingInterval != null && e.getSimulationTime() % rebalancingInterval == 0) {
			rebalanceFleet();
		}

		if (e.getSimulationTime() % rerouteInterval == 0) {
			rerouteRunningVehicles(e.getSimulationTime());
		}

	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated
		// cannot be relocated)
		Stream<? extends DvrpVehicle> rebalancableVehicles = fleet.getVehicles().values().stream()
				.filter(scheduleInquiry::isIdle);
		List<Relocation> relocations = rebalancingStrategy.calcRelocations(rebalancableVehicles,
				mobsimTimer.getTimeOfDay());

		if (!relocations.isEmpty()) {
			log.debug("Fleet rebalancing: #relocations=" + relocations.size());
			for (Relocation r : relocations) {
				Link currentLink = ((DrtStayTask) r.vehicle.getSchedule().getCurrentTask()).getLink();
				if (currentLink != r.link) {
					relocator.relocateVehicle(r.vehicle, r.link);
				}
			}
		}
	}

	private void rerouteRunningVehicles(double now) {
		reroutingStrategy.rerouteVehicles(now, fleet);
	}

	@Override
	public void requestSubmitted(Request request) {
		unplannedRequests.addRequest((DrtRequest) request);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		vehicle.getSchedule().nextTask();
	}

}
