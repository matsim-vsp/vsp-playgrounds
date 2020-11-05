package playground.lu.drtAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

public class DrtTripsEventHandler implements DrtRequestSubmittedEventHandler, PassengerRequestScheduledEventHandler,
		PassengerRequestRejectedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {
	private final List<DrtTrip> drtTrips = new ArrayList<>();
	private final Map<String, DrtTrip> temporaryStorageMap = new HashMap<>();

	public DrtTripsEventHandler() {
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		String requestId = event.getRequestId().toString();
		DrtTrip drtTrip = temporaryStorageMap.get(requestId);
		drtTrip.setDropOffTime(event.getTime());
		drtTrips.add(drtTrip);
		temporaryStorageMap.remove(requestId);
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		String requestId = event.getRequestId().toString();
		DrtTrip drtTrip = temporaryStorageMap.get(requestId);
		drtTrip.setPickUpTime(event.getTime());
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		String requestId = event.getRequestId().toString();
		DrtTrip drtTrip = temporaryStorageMap.get(requestId);
		drtTrip.rejectRequest(event.getTime());
		drtTrips.add(drtTrip);
		temporaryStorageMap.remove(requestId);
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		String requestId = event.getRequestId().toString();
		DrtTrip drtTrip = temporaryStorageMap.get(requestId);
		drtTrip.acceptRequest(event.getTime());
		drtTrip.setScheduledPickUpTime(event.getPickupTime());
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		String requestId = event.getRequestId().toString();
		DrtTrip drtTrip = new DrtTrip(requestId);
		drtTrip.setSubmissionTime(event.getTime());
		drtTrip.setFromLinkId(event.getFromLinkId().toString());
		drtTrip.setToLinkId(event.getToLinkId().toString());
		temporaryStorageMap.put(requestId, drtTrip);
	}

	@Override
	public void reset(int iteration) {
		drtTrips.clear();
		temporaryStorageMap.clear();
	}

	public List<DrtTrip> getDrtTrips() {
		return drtTrips;
	}
}
