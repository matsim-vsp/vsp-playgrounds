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
package cba.toynet2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class UtilityFunction {

	// -------------------- MEMBERS --------------------

	private final double sampersDefaultTimeUtl;
	
	private final Map<TourSequence.Type, Double> tourSeq2modeDestUtil = new LinkedHashMap<>();

	// private final Map<TourSequence.Type, Double> tourSeq2sampersTimeUtil;

	// -------------------- CONSTRUCTION --------------------

	UtilityFunction(final double sampersDefaultDestModeUtil, final double sampersDefaultTimeUtl) {

		// mode and destination choice (only ASCs)

		final double universalASC = sampersDefaultDestModeUtil;
		final double singleTourASC = 0.0; // 103.5;
		final double carASC = 0.0; // -1.30;
		final double pt1ASC = 0.0;

		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_car, universalASC + singleTourASC + carASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_car_other1_car, universalASC + 0.0 + carASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_car_other1_pt, universalASC + 0.0 + carASC + pt1ASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_car_other2_car, universalASC + 0.0 + carASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_pt, universalASC + singleTourASC + pt1ASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_pt_other1_car, universalASC + 0.0 + pt1ASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_pt_other1_pt, universalASC + 0.0 + pt1ASC);
		this.tourSeq2modeDestUtil.put(TourSequence.Type.work_pt_other2_car, universalASC + 0.0 + pt1ASC);

		// sampers time choice (based on uncongested conditions)

		this.sampersDefaultTimeUtl = sampersDefaultTimeUtl;
		
//		final Network network = scenario.getNetwork();
//		final FreeSpeedTravelTime freeTT = new FreeSpeedTravelTime();
//		final double ttFree12 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("1_2")), 0, null, null);
//		final double ttFree21 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("2_1")), 0, null, null);
//		final double ttFree14 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("1_4")), 0, null, null);
//		final double ttFree41 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("4_1")), 0, null, null);
//		final double ttFree23 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("2_3")), 0, null, null);
//		final double ttFree32 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("3_2")), 0, null, null);
//		final double ttFree45 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("4_5")), 0, null, null);
//		final double ttFree54 = freeTT.getLinkTravelTime(network.getLinks().get(Id.createLinkId("5_4")), 0, null, null);
//
//		final double workTourTTutil = betaTravelSampers_1_h * H_PER_S * 2.0
//				* (ttFree12 + ttFree23 + ttFree32 + ttFree21);
//		final double other1TourTTutil = workTourTTutil;
//		final double other2TourTTutil = betaTravelSampers_1_h * H_PER_S * 2.0
//				* (ttFree14 + ttFree45 + ttFree54 + ttFree41);

//		this.tourSeq2sampersTimeUtil = new LinkedHashMap<>();
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_car, workTourTTutil);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_car_other1_car, workTourTTutil + other1TourTTutil);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_car_other1_pt, workTourTTutil);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_car_other2_car, workTourTTutil + other2TourTTutil);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_pt, 0.0);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_pt_other1_car, other1TourTTutil);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_pt_other1_pt, 0.0);
//		this.tourSeq2sampersTimeUtil.put(TourSequence.Type.work_pt_other2_car, other2TourTTutil);
	}

	// -------------------- IMPLEMENTATION --------------------

	Double getSampersTimeUtility(final TourSequence.Type tourSeqType) {
		return this.sampersDefaultTimeUtl;
		// return this.tourSeq2sampersTimeUtil.get(tourSeqType);
	}

	Double getActivityModeUtility(final TourSequence.Type tourSeqType) {
		return this.tourSeq2modeDestUtil.get(tourSeqType);
	}
}
