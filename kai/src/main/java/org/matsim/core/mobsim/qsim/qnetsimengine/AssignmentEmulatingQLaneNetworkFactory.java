package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.LaneFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.lanes.Lane;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import javax.inject.Inject;

public final class AssignmentEmulatingQLaneNetworkFactory implements QNetworkFactory {
	
	private QNodeImpl.Builder nodeBuilder;
	private QLinkImpl.Builder linkBuilder;
	private Scenario scenario;
	private EventsManager events;
	
	@Inject
	public AssignmentEmulatingQLaneNetworkFactory( Scenario scenario, EventsManager events ) {
		this.scenario = scenario ;
		this.events = events ;
		throw new RuntimeException( AssignmentEmulatingQLane.DO_NOT_USE_ASSIGNMENT_EMULATING_QLANE ) ;
	}

	@Override
	public void initializeFactory( AgentCounter agentCounter1, MobsimTimer mobsimTimer1, NetsimInternalInterface netsimEngine ) {
		QSimConfigGroup qsimConfig = scenario.getConfig().qsim() ;
		Network network = scenario.getNetwork() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );

		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = AbstractQNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

		final NetsimEngineContext context = new NetsimEngineContext(events, network.getEffectiveCellSize(), agentCounter1, snapshotInfoBuilder, qsimConfig, mobsimTimer1, linkWidthCalculator ) ;

		nodeBuilder = new QNodeImpl.Builder( netsimEngine, context, scenario.getConfig().qsim() );

		linkBuilder = new QLinkImpl.Builder(context, netsimEngine) ;
		linkBuilder.setLaneFactory(new LaneFactory() {
			@Override public QLaneI createLane(AbstractQLink qLinkImpl) {
				VehicleQ<QVehicle> vehicleQueue = new FIFOVehicleQ() ; 
				LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator() ;
				final Id<Lane> laneId = Id.create( qLinkImpl.getLink().getId(), Lane.class ) ;
				return new AssignmentEmulatingQLane(qLinkImpl, vehicleQueue, laneId, context, linkSpeedCalculator ) ;
			}
		});
	}

	@Override
	public QNodeI createNetsimNode(Node node) {
		return nodeBuilder.build( node ) ;
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNodeI queueNode) {
		return linkBuilder.build(link, queueNode) ; 
	}

}
