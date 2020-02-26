package playground.kai.softwaredesign.generics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class ProblemWithFacilityIds{

	void run(){
		Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		MyActivity myActivity = new MyActivity();
		
		Id<ActivityFacility> actFacId = Id.create("actFac", ActivityFacility.class ) ;
		myActivity.setFacilityId( actFacId );

		// the best we can retrieve is this:
		Id<? extends Facility> retrievedId = myActivity.getFacilityId();

		// with this, we now have to go through all facility containers and see if we find it:
		scenario.getActivityFacilities().getFacilities().get( retrievedId );
		scenario.getTransitSchedule().getFacilities().get( retrievedId );
		// (However, if we stored Id<Facility>, the problem would remain the same.)

		// The following is not possible
//		Id<ActivityFacility> retrievedId2 = myActivity.getFacilityId();

		// One can cast it:
		Id<ActivityFacility> retrievedId2 = (Id<ActivityFacility>) myActivity.getFacilityId();
		// However, there is no way to make the compiler happy with it.

	}

	class MyActivity {

		// the following is not possible since one cannot put "Id<ActivityFacility>" into it.
//		private Id<Facility> id;
//		void setFacilityId( Id<Facility> id ) {
//			this.id = id;
//		}
//		public Id<Facility> getId(){
//			return id;
//		}
		// So the thing is:
		// (1) A container of type Container<A> will accept elements that extend A.
		// (2) A meta-container of type MetaContainer<Container<A>> will NOT accept elements of type Container<? extends A>

		// so we will have the following:
		private Id<? extends Facility> facilityId ;
		Id<? extends Facility> getFacilityId(){
			return facilityId;
		}
		void setFacilityId( Id<? extends Facility> facilityId ){
			this.facilityId = facilityId;
		}
		// since we are putting in Id<? extends Facility>, this is also what we get out.  Which means that the facility type by force has gotten lost.

	}


}
