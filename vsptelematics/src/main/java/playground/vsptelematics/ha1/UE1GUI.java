package playground.vsptelematics.ha1;

import org.matsim.run.gui.Gui;

public class UE1GUI {

	public static void main(String[] args) {
		Gui.show("IVS Basic MATSim GUI", playground.vsptelematics.ha1.ControllerWithOtfvis.class);
	}

	/* To start this class upon double-clicking the jar-file, add the following lines to the pom.xml
	 * and configure the mainClass correctly:
	 * 
	 * 
	 		<build>
				<plugins>
			  	<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-jar-plugin</artifactId>
						<configuration>
							<archive>
								<manifest>
									<mainClass>playground.vsptelematics.ha1.UE1GUI</mainClass>
								</manifest>
							</archive>
						</configuration>
					</plugin>
				</plugins>
			</build>
	 * 
	 * and then, to create the clickable jar-file:
	 * 
	 * - make sure the dependencies (including MATSim-core) is maven-installed, 
	 *   e.g. do "mvn install -DskipTests=true" for all required dependencies
	 * - change to the directory of this project, e.g. cd /path/to/playground/vsptelematics/
	 * - mvn clean
	 * - mvn -Prelease
	 * 
	 * This will result in a zip file in the target-directory which includes the clickable jar-file.
	 */
	
}
