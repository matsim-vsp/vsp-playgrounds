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

package playground.agarwalamit.cadyts.marginals;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;

/**
 * Created by amit on 20.02.18.
 */

public class ModalBinLoopUp implements LookUpItemFromId<ModalBin> {

    private final Map<Id<ModalBin>, ModalBin> mapping ;

    ModalBinLoopUp(Map<Id<ModalBin>, ModalBin> mapping){
        this.mapping = mapping;
    }

    @Override
    public ModalBin getItem(Id<ModalBin> id) {
        return this.mapping.get(id);
    }
}
