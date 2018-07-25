package playground.vsptelematics.ha2;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Singleton;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;

public class GuidanceQSimPlugin extends PopulationPlugin {
	static public String GUIDANCE_AGENT_SOURCE = "GuidanceAgentSource";

	private final Guidance guidance;
	private final double equipmentFraction;
	private final GuidanceRouteTTObserver ttObserver;

	public GuidanceQSimPlugin(Config config, Guidance guidance, double equipmentFraction,
			GuidanceRouteTTObserver ttObserver) {
		super(config);
		this.guidance = guidance;
		this.equipmentFraction = equipmentFraction;
		this.ttObserver = ttObserver;
	}

	@Override
	public Collection<Class<? extends MobsimListener>> listeners() {
		return Collections.singleton(Guidance.class);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singleton(new AbstractModule() {
			@Override
			protected void configure() {
				bind(PopulationAgentSource.class).asEagerSingleton();
				bind(AgentFactory.class).to(GuidanceAgentFactory.class);
				bind(Guidance.class).toInstance(guidance);
			}

			@Provides
			@Singleton
			GuidanceAgentFactory provideGuidanceAgentFactory(QSim qSim) {
				return new GuidanceAgentFactory(qSim, equipmentFraction, guidance, ttObserver);
			}
		});
	}
}
