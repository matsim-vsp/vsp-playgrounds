/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package gunnar.tryout;

import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

public class TryOutMatrices {

	public static void main(String[] args) {

		Matrices matrices = new Matrices();

		{
			final Matrix m1 = matrices.createMatrix("WORK",
					"work tour costs, averaged over entire population");
			m1.createAndAddEntry("fromId1", "toId2", 12.34);
			m1.createAndAddEntry("fromId1", "toId3", 56.78);
			m1.createAndAddEntry("fromId4", "toId4", 90.12);
		}

		{
			final Matrix m2 = matrices.createMatrix("OTHER",
					"other tour costs, averaged over entire population");
			m2.createAndAddEntry("fromId10", "toId20", 11);
			m2.createAndAddEntry("fromId10", "toId30", 22);
			m2.createAndAddEntry("fromId40", "toId40", 33);
		}

		final MatricesWriter writer = new MatricesWriter(matrices);
		writer.setIndentationString("    ");
		writer.setPrettyPrint(true);
		writer.write("test.matrix");
	}

}
