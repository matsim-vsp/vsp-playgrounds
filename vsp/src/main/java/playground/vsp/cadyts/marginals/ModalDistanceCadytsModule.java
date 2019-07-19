package playground.vsp.cadyts.marginals;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.StageActivityTypes;

public class ModalDistanceCadytsModule extends AbstractModule {

	private final DistanceDistribution expectedDistanceDistribution;

	public ModalDistanceCadytsModule(DistanceDistribution expectedDistanceDistribution) {
		this.expectedDistanceDistribution = expectedDistanceDistribution;
	}

	@Override
	public void install() {

		// make the expected distance distribution injectable
		bind(DistanceDistribution.class).toInstance(this.expectedDistanceDistribution);

		// register all activities of actType: '<mode> interaction' as interaction activity
		bind(StageActivityTypes.class).toInstance(type -> type.endsWith("interaction"));

		bind(TripEventHandler.class).in(Singleton.class);
		bind(ModalDistanceCadytsContext.class).in(Singleton.class);
		bind(ModalDistancePlansTranslator.class).in(Singleton.class);

		addControlerListenerBinding().to(ModalDistanceCadytsContext.class);
		addEventHandlerBinding().to(TripEventHandler.class);


	}
}
