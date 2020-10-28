package unitCapacityMatching;

import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class UnitCapcityZonalRequestInserterModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public UnitCapcityZonalRequestInserterModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {

		bindModal(UnplannedRequestInserter.class)
				.toProvider(modalProvider(getter -> new UnitCapcityZonalRequestInserter(drtCfg,
						getter.getModal(Fleet.class), getter.get(EventsManager.class), getter.get(MobsimTimer.class),
						getter.getModal(DrtZonalSystem.class), getter.getModal(VehicleSelector.class),
						getter.getModal(VehicleData.EntryFactory.class),
						getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool())))
				.asEagerSingleton();

		bindModal(VehicleSelector.class).toProvider(modalProvider(getter -> new ShortestPickupTimeVehicleSelector(
				getter.getModal(LeastCostPathCalculator.class), getter.get(TravelTime.class)))).asEagerSingleton();

	}

}
