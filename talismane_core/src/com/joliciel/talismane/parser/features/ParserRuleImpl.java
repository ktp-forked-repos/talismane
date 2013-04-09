///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.parser.features;

import com.joliciel.talismane.machineLearning.features.BooleanFeature;
import com.joliciel.talismane.parser.Transition;

class ParserRuleImpl implements ParserRule {
	private BooleanFeature<ParseConfigurationWrapper> condition;
	private Transition transition;
	private boolean negative;
	
	public ParserRuleImpl(BooleanFeature<ParseConfigurationWrapper> condition,
			Transition transition) {
		super();
		this.condition = condition;
		this.transition = transition;
	}
	public BooleanFeature<ParseConfigurationWrapper> getCondition() {
		return condition;
	}

	public Transition getTransition() {
		return transition;
	}
	public boolean isNegative() {
		return negative;
	}
	public void setNegative(boolean negative) {
		this.negative = negative;
	}

}