package playground.lu.zoneBasedMatching;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.QSimScopeForkJoinPoolHolder;
import org.matsim.contrib.drt.optimizer.insertion.DetourPathCalculator;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.ExtensiveInsertionSearch;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.MultiInsertionDetourPathCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

public class ExperimentalModuleInserstionWithZC extends AbstractDvrpModeQSimModule {

	private final DrtConfigGroup drtCfg;

	public ExperimentalModuleInserstionWithZC(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		bindModal(ExtensiveInsertionSearch.class).toProvider(modalProvider(
				getter -> new ExtensiveInsertionSearch(getter.getModal(DetourPathCalculator.class), drtCfg,
						getter.get(MobsimTimer.class), getter.getModal(QSimScopeForkJoinPoolHolder.class).getPool(),
						getter.getModal(InsertionCostCalculator.PenaltyCalculator.class))));

		bindModal(new TypeLiteral<DrtInsertionSearch<OneToManyPathSearch.PathData>>() {
		}).toProvider(modalProvider(getter -> new InsertionSerachWithZonalConstraints(
				getter.getModal(ExtensiveInsertionSearch.class), getter.getModal(DrtZonalSystem.class))));

		addModalComponent(MultiInsertionDetourPathCalculator.class, new ModalProviders.AbstractProvider<>(getMode()) {
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Override
			public MultiInsertionDetourPathCalculator get() {
				Network network = getModalInstance(Network.class);
				TravelDisutility travelDisutility = getModalInstance(TravelDisutilityFactory.class)
						.createTravelDisutility(travelTime);
				return new MultiInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
			}
		});
		bindModal(DetourPathCalculator.class).to(modalKey(MultiInsertionDetourPathCalculator.class));

	}

}
