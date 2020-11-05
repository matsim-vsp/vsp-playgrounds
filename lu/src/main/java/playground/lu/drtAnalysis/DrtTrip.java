package playground.lu.drtAnalysis;

public class DrtTrip {
	private final String requestId;
	private double submissionTime;
	private Double acceptedTime = null;
	private Double rejectedTime = null;
	private Double scheduledPickUpTime = null;
	private Double actualPickUpTime = null;
	private Double dropOffTime = null;

	private String fromLinkId;
	private String toLinkId;

	private boolean requestAccepted = false;
	private boolean requestRejected = false;

	public DrtTrip(String requestId) {
		this.requestId = requestId;
	}

	public void setSubmissionTime(double submissionTime) {
		this.submissionTime = submissionTime;
	}

	public double getSubmissionTime() {
		return submissionTime;
	}

	public void acceptRequest(double acceptedTime) {
		this.acceptedTime = acceptedTime;
		requestAccepted = true;
	}

	public Double getAcceptedTime() {
		return acceptedTime;
	}

	public void setScheduledPickUpTime(Double scheduledPickUpTime) {
		this.scheduledPickUpTime = scheduledPickUpTime;
	}

	public Double getScheduledPickUpTime() {
		return scheduledPickUpTime;
	}

	public void rejectRequest(double rejectedTime) {
		this.rejectedTime = rejectedTime;
		requestRejected = true;
	}

	public Double getRejectedTime() {
		return rejectedTime;
	}

	public void setPickUpTime(double pickUpTime) {
		this.actualPickUpTime = pickUpTime;
	}

	public Double getPickUpTime() {
		return actualPickUpTime;
	}

	public void setDropOffTime(double dropOffTime) {
		this.dropOffTime = dropOffTime;
	}

	public Double getDropOffTime() {
		return dropOffTime;
	}

	public void setFromLinkId(String fromLinkId) {
		this.fromLinkId = fromLinkId;
	}

	public String getFromLinkId() {
		return fromLinkId;
	}

	public void setToLinkId(String toLinkId) {
		this.toLinkId = toLinkId;
	}

	public String getToLinkId() {
		return toLinkId;
	}

	public String getRequestId() {
		return requestId;
	}

	public boolean checkIfRequestRejected() {
		return requestRejected;
	}

	public boolean checkIfReque3stAccepted() {
		return requestAccepted;
	}

	public Double getWaitTime() {
		if (requestAccepted && actualPickUpTime != null) {
			return actualPickUpTime - submissionTime;
		}
		return null;
	}

}
