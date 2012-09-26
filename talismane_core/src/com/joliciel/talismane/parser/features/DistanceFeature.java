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

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * Returns the distance between the token referred to by addressFunction1 and the token
 * referred to by addressFunction2, as an absolute value from 0 to n.
 * @author Assaf Urieli
 *
 */
public class DistanceFeature extends AbstractParseConfigurationFeature<Integer>
		implements IntegerFeature<ParseConfiguration> {
	private AddressFunction addressFunction1;
	private AddressFunction addressFunction2;
	
	public DistanceFeature(AddressFunction addressFunction1,
			AddressFunction addressFunction2) {
		super();
		this.addressFunction1 = addressFunction1;
		this.addressFunction2 = addressFunction2;
		
		this.setName(super.getName() + "(" + addressFunction1.getName() + "," + addressFunction2.getName() + ")");
	}


	@Override
	protected FeatureResult<Integer> checkInternal(ParseConfiguration configuration) {
		FeatureResult<PosTaggedToken> tokenResult1 = addressFunction1.check(configuration);
		FeatureResult<PosTaggedToken> tokenResult2 = addressFunction2.check(configuration);
		FeatureResult<Integer> featureResult = null;
		if (tokenResult1!=null && tokenResult2!=null) {
			PosTaggedToken posTaggedToken1 = tokenResult1.getOutcome();
			PosTaggedToken posTaggedToken2 = tokenResult2.getOutcome();
			int distance = posTaggedToken2.getToken().getIndex() - posTaggedToken1.getToken().getIndex();
			if (distance<0)
				distance = 0 - distance;
			featureResult = this.generateResult(distance);
		}
		return featureResult;
	}

}