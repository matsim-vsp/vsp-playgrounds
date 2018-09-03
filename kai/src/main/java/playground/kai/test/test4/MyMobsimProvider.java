package playground.kai.test.test4;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimBuilder;

public class MyMobsimProvider implements Provider<Mobsim> {
	@Inject Scenario scenario ;
	@Inject EventsManager events ;
	
	@Override
	public Mobsim get() {
		return new QSimBuilder(scenario.getConfig()).useDefaults().build(scenario, events);
	}

}
