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
package cba.toynet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class DemandAnalyzer {

	private final Map<TourSequence.Type, Double> tourSeqType2cnt = new LinkedHashMap<>();

	private final Map<TourSequence.Type, Double> tourSeqType2sampersUtl = new LinkedHashMap<>();

	private final Map<TourSequence.Type, Double> tourSeqType2matsimUtl = new LinkedHashMap<>();

	private int personCnt = 0;

	DemandAnalyzer() {
	}

	private <K> void add(final K key, final double addend, final Map<K, Double> key2cnt) {
		final Double oldCnt = key2cnt.get(key);
		if (oldCnt == null) {
			key2cnt.put(key, addend);
		} else {
			key2cnt.put(key, oldCnt + addend);
		}

	}

	void registerChoice(final PlanForResampling planForResampling) {
		final TourSequence tourSequence = planForResampling.getTourSequence();
		assert tourSequence != null;
		this.personCnt++;
		this.add(tourSequence.type, 1.0, this.tourSeqType2cnt);
		this.add(tourSequence.type, planForResampling.getSampersOnlyScore() + planForResampling.getSampersEpsilonRealization()
				+ planForResampling.getSampersTimeScore(), this.tourSeqType2sampersUtl);
		this.add(tourSequence.type, planForResampling.getSampersOnlyScore() + planForResampling.getSampersEpsilonRealization()
				+ planForResampling.getMATSimTimeScore(), this.tourSeqType2matsimUtl);
	}

	private double null2zero(Double val) {
		return ((val == null) ? 0.0 : val);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("PATTERN\tCOUNT\tFREQ.\tSAMPERS(total)\tMATSim(total)\n");
		for (TourSequence.Type type : TourSequence.Type.values()) {
			final double cnt = this.null2zero(this.tourSeqType2cnt.get(type));
			result.append(type);
			result.append("\t");
			result.append(cnt);
			result.append("\t");
			result.append(100.0 * cnt / this.personCnt);
			result.append("\t");
			result.append(this.null2zero(this.tourSeqType2sampersUtl.get(type)) / cnt);
			result.append("\t");
			result.append(this.null2zero(this.tourSeqType2matsimUtl.get(type)) / cnt);
			result.append("\n");
		}
		return result.toString();
	}
}
