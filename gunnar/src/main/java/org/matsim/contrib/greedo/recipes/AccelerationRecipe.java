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
package org.matsim.contrib.greedo.recipes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.LogDataWrapper;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationRecipe implements ReplannerIdentifierRecipe {

	private final ReplannerIdentifierRecipe backupRecipe;

	private boolean useBackupRecipe = false;

	public AccelerationRecipe(final ReplannerIdentifierRecipe backupRecipe) {
		this.backupRecipe = backupRecipe;
	}

	@Override
	public void update(final LogDataWrapper logDataWrapper) {
		this.backupRecipe.update(logDataWrapper);
	}

	public void setUseBackupRecipe(final boolean useBackupRecipe) {
		this.useBackupRecipe = useBackupRecipe;
	}

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo) {
		if (this.useBackupRecipe) {
			return this.backupRecipe.isReplanner(personId, deltaScoreIfYes, deltaScoreIfNo);
		} else {
			return (deltaScoreIfYes < deltaScoreIfNo);
		}
	}

	@Override
	public String getDeployedRecipeName() {
		if (this.useBackupRecipe) {
			return this.backupRecipe.getClass().getSimpleName();
		} else {
			return this.getClass().getSimpleName();
		}
	}
}
