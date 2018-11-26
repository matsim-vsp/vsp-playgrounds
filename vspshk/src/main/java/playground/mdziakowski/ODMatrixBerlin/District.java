package playground.mdziakowski.ODMatrixBerlin;

public class District {
	
	private String name;
	private int id = nextId++;
	private static int nextId = 0;
	
	public District(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

}
