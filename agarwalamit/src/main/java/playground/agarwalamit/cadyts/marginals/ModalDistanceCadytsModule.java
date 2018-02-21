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

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.matsim.core.controler.AbstractModule;

/**
 * Created by amit on 21.02.18.
 */

public class ModalDistanceCadytsModule extends AbstractModule{

    private final DistanceDistribution inputDistanceDistrbution;

    ModalDistanceCadytsModule(DistanceDistribution inputDistanceDistrbution){
        this.inputDistanceDistrbution = inputDistanceDistrbution;
    }

    @Override
    public void install() {
        bind(DistanceDistribution.class).toInstance(inputDistanceDistrbution);
        bind(Key.get(new TypeLiteral<DistanceDistribution>(){}, Names.named("calibration"))).toInstance(inputDistanceDistrbution);

        bind(ModalDistanceCadytsContext.class).asEagerSingleton();
        addControlerListenerBinding().to(ModalDistanceCadytsContext.class);

        bind(BeelineDistanceCollector.class);
        bind(BeelineDistancePlansTranslatorBasedOnEvents.class).asEagerSingleton();
    }
}
