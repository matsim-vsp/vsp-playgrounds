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

import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class TryOutObjectAttributes {

	static void test1() {
		final ObjectAttributes attrs = new ObjectAttributes();
		attrs.putAttribute("person 1", "homezone", "123");
		attrs.putAttribute("person 1", "workzone", "456");
		attrs.putAttribute("person 1", "otherzone", "789");
		attrs.putAttribute("person 1", "worktourmode", "car");
		attrs.putAttribute("person 1", "othertourmode", "pt");

		attrs.putAttribute("person 2", "homezone", "111");
		attrs.putAttribute("person 2", "workzone", "null");
		attrs.putAttribute("person 2", "otherzone", "333");
		attrs.putAttribute("person 2", "worktourmode", "null");
		attrs.putAttribute("person 2", "othertourmode", "pt");

		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(
				attrs);
		writer.setIndentationString("    ");
		writer.setPrettyPrint(true);
		writer.writeFile("test.attributes");
	}

	static void test2() {
		final ObjectAttributes attrs = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attrs);
		reader.readFile("./150410_worktrips_small.xml");
		System.out.println(attrs);
		
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		// test1();
		test2();
		System.out.println("... DONE");
	}
}
