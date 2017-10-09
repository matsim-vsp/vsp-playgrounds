/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.templates.callBuilder;

import java.util.ArrayList;
import javax.annotation.Nullable;
import com.google.callbuilder.BuilderField;
import com.google.callbuilder.CallBuilder;
import com.google.callbuilder.style.ArrayListAdding;

/**
 * Created by amit on 09.10.17.
 */

public class CallBuilderPerson {

    @CallBuilder
    CallBuilderPerson(
            String familyName,
            String givenName,
            @BuilderField(style = ArrayListAdding.class) ArrayList<String> addressLines,
            @Nullable Integer age) {
        // ...
    }

}
