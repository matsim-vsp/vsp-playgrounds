package playground.mdziakowski.ODMatrixBerlin;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class MyTrip {
	
	private Id<Person> person;
	private String startdistrict ;
	private String enddistrict ;
	private String mode;
	private String activity;
	private double clockTime ;
	private double movingclockTime;
	private double travelTime;
	
	private boolean check = true;
	
	MyTrip(Id<Person> person, String startdistrict, String enddistrict, String mode, String activity, double clockTime, double movingclockTime, double travelTime) {
		this.person = person;
		this.startdistrict = startdistrict;
		this.enddistrict = enddistrict;
		this.mode = mode;
		this.activity = activity;
		this.clockTime = clockTime;
		this.movingclockTime = movingclockTime;
		this.travelTime = travelTime;
	}

	public Id<Person> getPerson() {
		return person;
	}

	public String getStartdistrict() {
		return startdistrict;
	}

	public String getEnddistrict() {
		return enddistrict;
	}

	public String getMode() {
		return mode;
	}

	public String getActivity() {
		return activity;
	}

	public double getClockTime() {
		return clockTime;
	}

	public double getMovingclockTime() {
		return movingclockTime;
	}

	public double getTravelTime() {
		return travelTime;
	}

	String mytoString() {
		return person + ";" + startdistrict + ";" + enddistrict + ";" + mode + ";" + activity + ";" + clockTime + ";" + movingclockTime+ ";" + travelTime;
	}

	public boolean isCheck() {
		return check;
	}

	public void setCheck(boolean check) {
		this.check = check;
	}
}
