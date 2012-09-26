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
package com.joliciel.talismane.sentenceDetector.features;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.sentenceDetector.PossibleSentenceBoundary;

/**
 * Returns the <i>n</i> exact characters immediately preceding the current boundary.
 * @author Assaf Urieli
 *
 */
public class PreviousLettersFeature extends AbstractSentenceDetectorFeature<String> implements StringFeature<PossibleSentenceBoundary> {
	IntegerFeature<PossibleSentenceBoundary> nFeature;
	
	public PreviousLettersFeature(IntegerFeature<PossibleSentenceBoundary> nFeature) {
		this.nFeature = nFeature;
		this.setName(super.getName() + "(" + nFeature.getName() + ")");
	}
	
	@Override
	public FeatureResult<String> checkInternal(PossibleSentenceBoundary context) {
		FeatureResult<String> result = null;
		FeatureResult<Integer> nResult = nFeature.check(context);
		if (nResult!=null) {
			int n = nResult.getOutcome();
			int startIndex = context.getIndex() - n;
			if (startIndex>=0) {
				String prevLetters = context.getText().substring(startIndex, context.getIndex());
				result = this.generateResult(prevLetters);
			}
		} // have n
		
		return result;
	}

}