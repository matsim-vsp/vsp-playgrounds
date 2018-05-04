/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;
import org.matsim.api.core.v01.Customizable;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 04.05.18.
 */

public class AttributableQVehicle extends QVehicle implements Attributable, Customizable {

    private final Attributes attributes = new Attributes();
    private Customizable customizableDelegate;

    public AttributableQVehicle(Vehicle basicVehicle) {
        super(basicVehicle);
    }


    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        if (this.customizableDelegate == null) {
            this.customizableDelegate = CustomizableUtils.createCustomizable();
        }
        return this.customizableDelegate.getCustomAttributes();
    }
}
