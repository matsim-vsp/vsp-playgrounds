package playground.vsp.cadyts.marginals;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution2;

public class ModalDistanceCadytsModule2 extends AbstractModule {

	private final DistanceDistribution2 expectedDistanceDistribution;

	public ModalDistanceCadytsModule2(DistanceDistribution2 expectedDistanceDistribution) {
		this.expectedDistanceDistribution = expectedDistanceDistribution;
	}

	@Override
	public void install() {

		// make the expected distance distribution injectable
		bind(DistanceDistribution2.class).toInstance(this.expectedDistanceDistribution);

		// register all activities of actType: '<mode> interaction' as interaction activity
		bind(StageActivityTypes.class).toInstance(type -> type.endsWith("interaction"));

		bind(TripEventHandler.class).in(Singleton.class);
		bind(ModalDistanceCadytsContext2.class).in(Singleton.class);
		bind(ModalDistancePlansTranslator.class).in(Singleton.class);

		addControlerListenerBinding().to(ModalDistanceCadytsContext2.class);
		addEventHandlerBinding().to(TripEventHandler.class);


	}
}
