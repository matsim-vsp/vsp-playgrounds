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
package besttimeresponseintegration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            location type
 * @param M
 *            mode type
 *
 */
public class TravelTimeCache<L, M> {

	// -------------------- INTERNAL DATA STRUCTURE --------------------

	private class Key {
		private final List<Object> keyData;

		private Key(final Integer timeStep, final L origin, final L destination, final M mode) {
			this.keyData = new ArrayList<>(4);
			this.keyData.add(timeStep);
			this.keyData.add(origin);
			this.keyData.add(destination);
			this.keyData.add(mode);
		}

		@Override
		public int hashCode() {
			return this.keyData.hashCode();
		}

		@Override
		public boolean equals(final Object other) {
			if (other instanceof TravelTimeCache<?, ?>.Key) {
				return this.keyData.equals(((TravelTimeCache<?, ?>.Key) other).keyData);
			} else {
				return false;
			}
		}
	}

	private final Map<Key, Double> key2tt_s = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TravelTimeCache() {
	}

	// -------------------- CONTENT ACCESS --------------------

	public int size() {
		return this.key2tt_s.size();
	}
	
	public void clear() {
		this.key2tt_s.clear();
	}

	public void putTT_s(final Integer timeStep, final L origin, final L destination, final M mode, final Double tt_s) {
		this.key2tt_s.put(new Key(timeStep, origin, destination, mode), tt_s);
	}

	public Double getTT_s(final Integer timeStep, final L origin, final L destination, final M mode) {
		return this.key2tt_s.get(new Key(timeStep, origin, destination, mode));
	}

}
