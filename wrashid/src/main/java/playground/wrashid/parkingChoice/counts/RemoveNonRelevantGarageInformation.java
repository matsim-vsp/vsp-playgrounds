package playground.wrashid.parkingChoice.counts;

import playground.wrashid.lib.obj.StringMatrixFilter;

public class RemoveNonRelevantGarageInformation implements StringMatrixFilter{

	private final String garageName;

	public RemoveNonRelevantGarageInformation(String garageName){
		this.garageName = garageName;
		
	}
	
	@Override
	public boolean removeLine(String line) {
		return !line.contains(garageName);
	}
	
}