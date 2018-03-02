package signals.advancedPlanbased;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.poi.ddf.EscherColorRef.SysIndexSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;

import com.google.inject.Provider;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.laemmer.model.LaemmerLane;
import signals.sensor.LinkSensorManager;

/**
 * Created by nkuehnel on 05.04.2017.
 */
public class AdvancedPlanBasedSignalSystemController implements SignalController, Analyzable {

    private final LinkSensorManager sensorManager;
    private final DefaultPlanbasedSignalSystemController delegate = new DefaultPlanbasedSignalSystemController();
    protected SignalSystem system;
    private final TtTotalDelay delayCalculator;
	private double averageWaitingCarCount;
	private boolean isAvgQueueLengthNumWritten;
	private Lanes lanes;
	private double lastAvgCarNumUpdate;
	private Network network;
    public static final String IDENTIFIER = "AdvancedPlanBasedSignalSystemController";


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LinkSensorManager sensorManager;
        private final TtTotalDelay delayCalculator;
		private Scenario scenario;

        public SignalControlProvider(LinkSensorManager sensorManager, TtTotalDelay delayCalculator, Scenario scenario) {
            this.sensorManager = sensorManager;
            this.delayCalculator = delayCalculator;
            this.scenario = scenario;
        }

        @Override
        public SignalController get() {
            return new AdvancedPlanBasedSignalSystemController(sensorManager, delayCalculator, scenario);
        }
    }


//    public AdvancedPlanBasedSignalSystemController(LinkSensorManager sensorManager, TtTotalDelay delayCalculator) {
//        this.sensorManager = sensorManager;
//        this.delayCalculator = delayCalculator;
//    }

    public AdvancedPlanBasedSignalSystemController(LinkSensorManager sensorManager, TtTotalDelay delayCalculator, Scenario scenario) {
        this.sensorManager = sensorManager;
        this.delayCalculator = delayCalculator;
        this.lanes = scenario.getLanes();
        this.network = scenario.getNetwork();
    }
    
    @Override
    public void updateState(double timeSeconds) {
        delegate.updateState(timeSeconds);
        logQueueLengthToFile(timeSeconds);
    }

    @Override
    public void addPlan(SignalPlan plan) {
        delegate.addPlan(plan);
    }

    @Override
    public void reset(Integer iterationNumber) {
        delegate.reset(iterationNumber);
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        delegate.simulationInitialized(simStartTimeSeconds);
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?)
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
            }
        }
    	this.initializeSensoring();
    }

    @Override
    public void setSignalSystem(SignalSystem signalSystem) {
        delegate.setSignalSystem(signalSystem);
        this.system = signalSystem;
    }

    @Override
    public String getStatFields() {
        StringBuilder builder = new StringBuilder();
        builder.append("total_delay;");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            builder.append("state_group_" + group.getId() + ";");
            builder.append("n_group_" + group.getId() + ";");
        }
        return builder.toString();
    }

    private void logQueueLengthToFile(double now) {
		double currentQueueLengthSum = 0.0;
    	if (now > 30.0*60.0 && now <= 90.0*60.0) {
    		for (Signal signal : this.system.getSignals().values()) {
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
					currentQueueLengthSum += this.getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
				} else {
					for (Id<Lane> laneId : signal.getLaneIds()) {
						currentQueueLengthSum += this.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), laneId);	
					}
				}
    		}
    		this.averageWaitingCarCount *= (lastAvgCarNumUpdate-30.0*60.0+1.0); 
    		this.averageWaitingCarCount	+= currentQueueLengthSum;
    		this.averageWaitingCarCount /= (now - 30.0*60.0+1.0);
    		this.lastAvgCarNumUpdate = now;
		} else if (now > 90.0*60.0 && !this.isAvgQueueLengthNumWritten) {
		    try { 
		    	if (Files.notExists(Paths.get(("/tmp/avgQueueLength-signalSystem"+this.system.getId().toString()+".csv")))){
		    		Files.createFile(Paths.get(("/tmp/avgQueueLength-signalSystem"+this.system.getId().toString()+".csv")));
		    	}
				Files.write(Paths.get(("/tmp/avgQueueLength-signalSystem"+this.system.getId().toString()+".csv")), Double.toString(averageWaitingCarCount).concat("\n").getBytes(), StandardOpenOption.APPEND);
				this.isAvgQueueLengthNumWritten  = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
    
    int getNumberOfExpectedVehiclesOnLink(double now, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., now);
    }
    
    int getNumberOfExpectedVehiclesOnLane(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (lanes.getLanesToLinkAssignments().get(linkId).getLanes().size() == 1) {
            return getNumberOfExpectedVehiclesOnLink(now, linkId);
        } else {
            return this.sensorManager.getNumberOfCarsInDistanceOnLane(linkId, laneId, 0., now);
        }
    }
    
    private void initializeSensoring() {
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
                        this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoringOnLane(signal.getLinkId(), laneId, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?), xy
                //moved this to next for-loop, unsure, if this is still needed, pschade Nov'17 
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId(), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        }
        //moved here from above, pschade Nov'17
        for (Link link : this.network.getLinks().values()) {
            this.sensorManager.registerNumberOfCarsInDistanceMonitoring(link.getId(), 0.);
            this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(link.getId(), Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
    }
    
    @Override
    public String getStepStats(double now) {
        StringBuilder builder = new StringBuilder();
        builder.append(delayCalculator.getTotalDelay() + ";");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            if(group.getState() != null) {
                builder.append(group.getState().name() + ";");
            } else {
                builder.append("null;");
            }
            int totalN = 0;
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        totalN += sensorManager.getNumberOfCarsInDistanceOnLane(signal.getLinkId(), laneId, 0.,now);
                    }
                } else {
                    totalN += sensorManager.getNumberOfCarsInDistance(signal.getLinkId(), 0., now);
                }
            }
            builder.append(totalN + ";");
        }
        return builder.toString();
    }

    @Override
    public boolean isAnalysisEnabled() {
        return true;
    }
}
