package optimize.cten.convert.cten2matsim.restrictions;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.utils.collections.Tuple;

import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgStreet;
import optimize.cten.data.TtRestriction;
import optimize.cten.ids.DgIdConverter;
import optimize.cten.ids.DgIdPool;

public class Restriction2ConflictData {
	
	private DgIdConverter idConverter;

	private Network fullNetwork;
	
	private Map<Id<Node>, Id<SignalSystem>> nodeId2signalSystemId = new HashMap<>();
	
	public Restriction2ConflictData(DgIdPool idPool, Network fullNetwork, SignalSystemsData signalSystemsFullNetwork){
		this.idConverter = new DgIdConverter(idPool);
		this.fullNetwork = fullNetwork;
		
		// initialize nodeId2signalSystemId map:
		for (SignalSystemData system : signalSystemsFullNetwork.getSignalSystemData().values()) {
			for (SignalData signal : system.getSignalData().values()) {
				nodeId2signalSystemId.put(fullNetwork.getLinks().get(signal.getLinkId()).getToNode().getId(), system.getId());
				break; // all signalized links of a signal system have the same to-node id
			}
		}
	}

	public void convertConflicts(ConflictData conflictData, Map<Id<DgCrossing>, DgCrossing> crossings) {
		for (DgCrossing crossing : crossings.values()) {
			// only consider crossings with restrictions
			if (!crossing.getRestrictions().isEmpty()) {
				// identify signal system id
				Id<Node> nodeIdFullNetwork = idConverter.convertCrossingId2NodeId(crossing.getId());
				Id<SignalSystem> signalSystemId = nodeId2signalSystemId.get(nodeIdFullNetwork);

				// create ConflictingDirections for system and add to conflictData
				IntersectionDirections conflictingDirections = conflictData.getFactory()
						.createConflictingDirectionsContainerForIntersection(signalSystemId, nodeIdFullNetwork);
				conflictData.addConflictingDirectionsForIntersection(signalSystemId, nodeIdFullNetwork,
						conflictingDirections);

				// fill the restrictions
				for (TtRestriction restriction : crossing.getRestrictions().values()) {
					DgStreet light = crossing.getLights().get(restriction.getLightId());
					// identify from and to link for this direction/light
					Tuple<Id<Link>, Id<Link>> fromToLinkIdTupleFullNetwork = identifyFromAndToLinkForThisDirection(
							light, nodeIdFullNetwork);
					// create direction object for this light
					Direction direction = conflictData.getFactory().createDirection(signalSystemId, nodeIdFullNetwork,
							fromToLinkIdTupleFullNetwork.getFirst(), fromToLinkIdTupleFullNetwork.getSecond(),
							Id.create(restriction.getLightId(), Direction.class));
					conflictingDirections.addDirection(direction);

					// add all directions as conflicting or non-conflicting
					for (Id<DgStreet> rlightId : restriction.getRlightsAllowed().keySet()) {
						if (restriction.isAllowed()) {
							addAsNonConflicting(direction, rlightId);
						} else {
							addAsConflict(direction, rlightId);
						}
					}
					for (Id<DgStreet> lightId : restriction.getRlightsOff()) {
						addAsConflict(direction, lightId);
					}
					// add all remaining lights as conflicting (if 'allowed') or as non-conflicting (if '!allowed')
					for (Id<DgStreet> lightId : crossing.getLights().keySet()) {
						if (!direction.getConflictingDirections().contains(lightId)
								&& !direction.getNonConflictingDirections().contains(lightId)) {
							if (restriction.isAllowed()) {
								addAsConflict(direction, lightId);
							} else {
								addAsNonConflicting(direction, lightId);
							}
						}
					}
					if (!restriction.getRlightsOn().isEmpty()) {
						throw new RuntimeException("not yet implemented. would need to be in the same signal group.");
					}
				}
			}
		}
	}

	private void addAsNonConflicting(Direction direction, Id<DgStreet> lightId) {
		// identify direction from light id
		Id<Direction> nonConflictingDirectionId = Id.create(lightId, Direction.class);
		direction.addNonConflictingDirection(nonConflictingDirectionId);
	}

	private void addAsConflict(Direction direction, Id<DgStreet> lightId) {
		// identify direction from light id
		Id<Direction> conflictingDirectionId = Id.create(lightId, Direction.class);
		direction.addConflictingDirection(conflictingDirectionId);
	}

	private Tuple<Id<Link>, Id<Link>> identifyFromAndToLinkForThisDirection(DgStreet light, Id<Node> matsimNodeId) {
		// get from and to links of this light from the ids of the crossing nodes
		Id<Link> fromLinkIdExpandedNetwork = idConverter.convertToCrossingNodeId2LinkId(light.getFromNode().getId());
		Id<Link> toLinkIdExpandedNetwork = idConverter.convertFromCrossingNodeId2LinkId(light.getToNode().getId());
		
		// find corresponding link in the full network 
		Id<Link> fromLinkIdFullNetwork = null;
		Id<Link> toLinkIdFullNetwork = null;
		if (fullNetwork.getLinks().containsKey(fromLinkIdExpandedNetwork)) {
			fromLinkIdFullNetwork = fromLinkIdExpandedNetwork;
		} else {
			for (Id<Link> inLinkIdFullNetwork : fullNetwork.getNodes().get(matsimNodeId).getInLinks().keySet()) {
				// look for the id corresponding to the end of the expanded network id (link concatenation merges ids via '-')
				if (fromLinkIdExpandedNetwork.toString().endsWith(inLinkIdFullNetwork.toString())) {
					fromLinkIdFullNetwork = inLinkIdFullNetwork;
				}
			}
		}
		if (fullNetwork.getLinks().containsKey(toLinkIdExpandedNetwork)) {
			toLinkIdFullNetwork = toLinkIdExpandedNetwork;
		} else {
			for (Id<Link> outLinkIdFullNetwork : fullNetwork.getNodes().get(matsimNodeId).getOutLinks().keySet()) {
				// look for the id corresponding to the beginning of the expanded network id (link concatenation merges ids via '-')
				if (toLinkIdExpandedNetwork.toString().startsWith(outLinkIdFullNetwork.toString())) {
					toLinkIdFullNetwork = outLinkIdFullNetwork;
				}
			}
		}
		return new Tuple<Id<Link>, Id<Link>>(fromLinkIdFullNetwork, toLinkIdFullNetwork);
	}

}
