package playground.vsptelematics.ha2;

import com.google.inject.Provides;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

import javax.inject.Singleton;

public class GuidanceQSimModule extends AbstractQSimModule {
	static public String GUIDANCE_AGENT_SOURCE = "GuidanceAgentSource";
	static public String GUIDANCE_LISTENER = "GuidanceListener";

	private final Guidance guidance;
	private final double equipmentFraction;
	private final GuidanceRouteTTObserver ttObserver;

	public GuidanceQSimModule(Guidance guidance, double equipmentFraction, GuidanceRouteTTObserver ttObserver) {
		this.guidance = guidance;
		this.equipmentFraction = equipmentFraction;
		this.ttObserver = ttObserver;
	}

	@Override
	public void configureQSim() {
		bind(PopulationAgentSource.class).asEagerSingleton();
		bind(AgentFactory.class).to(GuidanceAgentFactory.class);
		bind(Guidance.class).toInstance(guidance);

		this.addQSimComponentBinding( GUIDANCE_LISTENER ).to( Guidance.class ) ;
	}
	
	@Provides
	@Singleton
	GuidanceAgentFactory provideGuidanceAgentFactory(QSim qSim) {
		return new GuidanceAgentFactory(qSim, equipmentFraction, guidance, ttObserver);
	}
	
	static public void configureComponents(QSimComponentsConfig components) {
		components.addNamedComponent(GUIDANCE_LISTENER);
	}
}
