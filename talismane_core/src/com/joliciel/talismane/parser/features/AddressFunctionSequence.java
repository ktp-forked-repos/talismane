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
 * Retrieves the nth pos-tagged token in the sequence of tokens.
 * @author Assaf Urieli
 *
 */
public class AddressFunctionSequence extends AbstractAddressFunction {
	private IntegerFeature<ParseConfiguration> indexFeature;
	
	public AddressFunctionSequence(IntegerFeature<ParseConfiguration> indexFeature) {
		super();
		this.indexFeature = indexFeature;
		this.setName("Seq(" + indexFeature.getName() + ")");
	}

	@Override
	public FeatureResult<PosTaggedToken> checkInternal(ParseConfiguration configuration) {
		PosTaggedToken resultToken = null;
		FeatureResult<Integer> indexResult = indexFeature.check(configuration);
		if (indexResult!=null) {
			int index = indexResult.getOutcome();
			if (index>=0 && index<configuration.getPosTagSequence().size())
				resultToken = configuration.getPosTagSequence().get(index);
		}
		FeatureResult<PosTaggedToken> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}

}