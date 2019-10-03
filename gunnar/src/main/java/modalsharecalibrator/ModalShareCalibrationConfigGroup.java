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
package modalsharecalibrator;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibrationConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "modalShareCalibration";

	public ModalShareCalibrationConfigGroup() {
		super(GROUP_NAME);
	}

	private Double initialTrustRegion = null;

	@StringGetter("initialTrustRegion")
	public Double getInitialTrustRegion() {
		return this.initialTrustRegion;
	}

	@StringSetter("initialTrustRegion")
	public void setInitialTrustRegion(final double initialTrustRegion) {
		this.initialTrustRegion = initialTrustRegion;
	}

	private Double iterationExponent = null;

	@StringGetter("iterationExponent")
	public Double getIterationExponent() {
		return this.iterationExponent;
	}

	@StringSetter("iterationExponent")
	public void setIterationExponent(final double iterationExponent) {
		this.iterationExponent = iterationExponent;
	}

	/* package for testing */ void addObservation(final String mode, final double share) {
		this.addParameterSet(new TransportModeDataSet(mode, share));
	}
	
	@Override
	public ConfigGroup createParameterSet(final String type) {
		if (TransportModeDataSet.TYPE.equals(type)) {
			return new TransportModeDataSet();
		} else {
			return super.createParameterSet(type);
		}
	}

	public static class TransportModeDataSet extends ReflectiveConfigGroup {

		public static final String TYPE = "modalShareData";

		public TransportModeDataSet() {
			super(TYPE);
		}

		public TransportModeDataSet(final String mode, final double share) {
			this();
			this.mode = mode;
			this.share = share;
		}

		private String mode = null;

		@StringGetter("mode")
		public String getMode() {
			return this.mode;
		}

		@StringSetter("mode")
		public void setMode(final String mode) {
			this.mode = mode;
		}

		private Double share = null;

		@StringGetter("share")
		public Double getShare() {
			return this.share;
		}

		@StringSetter("share")
		public void setShare(final double share) {
			this.share = share;
		}
	}

	public static void main(String[] args) {

		ModalShareCalibrationConfigGroup config = new ModalShareCalibrationConfigGroup();
		config.addObservation("car", 0.4);
		config.addObservation("pt", 0.6);

		System.out.println(config.toString());
		for (ConfigGroup modeconf : config.getParameterSets(TransportModeDataSet.TYPE)) {
			System.out.println(modeconf);
		}

	}

}
