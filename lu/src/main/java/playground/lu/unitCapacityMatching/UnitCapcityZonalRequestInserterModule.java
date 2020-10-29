package playground.lu.unitCapacityMatching;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

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

		bindModal(RequestInsertionScheduler.class)
				.toProvider(modalProvider(getter -> new RequestInsertionScheduler(drtCfg, getter.getModal(Fleet.class),
						getter.get(MobsimTimer.class),
						getter.getNamed(TravelTime.class, DvrpTravelTimeModule.DVRP_ESTIMATED),
						getter.getModal(ScheduleTimingUpdater.class), getter.getModal(DrtTaskFactory.class))))
				.asEagerSingleton();

		bindModal(VehicleSelector.class).toProvider(new ModalProviders.AbstractProvider<>(drtCfg.getMode()) {
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Override
			public ShortestPickupTimeVehicleSelector get() {
				Network network = getModalInstance(Network.class);
				TravelDisutility travelDisutility = getModalInstance(TravelDisutilityFactory.class)
						.createTravelDisutility(travelTime);
				DrtTaskFactory taskFactory = getModalInstance(DrtTaskFactory.class);
				return new ShortestPickupTimeVehicleSelector(network, travelTime, travelDisutility, taskFactory,
						drtCfg);
			}
		}).asEagerSingleton();

	}

}
