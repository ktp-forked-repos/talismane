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
package com.joliciel.talismane.tokeniser;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.tokeniser.features.TokenFeature;
import com.joliciel.talismane.tokeniser.patterns.TokenMatch;


/**
 * A token is more-or-less equivalent to a word, although punctuation (and in some case white space) count as tokens as well.
 * Some implementations may consider compound words (e.g. "of course") as a single token, and some may
 * insert empty tokens into sentences (e.g. when a single token represents two POS tags downstream, such as "auquel" in French).
 * Some languages may break a single word up into multiple tokens
 * (e.g. biblical or modern Hebrew for the coordinating conjunction "ve").
 * @author Assaf Urieli
 *
 */
public interface Token extends Comparable<Token>, TokenWrapper {
    /**
     * An imaginary word that can be placed at the start of the sentence to simplify algorithms.
     */
    public static final String START_TOKEN = "[[START]]";
    
	/**
	 * The text represented by this token.
	 * @return
	 */
	public String getText();
	public void setText(String text);
	
	/**
	 * The original text represented by this token, before filters updated it
	 * for analysis.
	 * @return
	 */
	public String getOriginalText();
	
	/**
	 * The token sequence containing this token.
	 * @return
	 */
	public TokenSequence getTokenSequence();
	
	/**
	 * The index of this token in the containing sequence.
	 * @return
	 */
	public int getIndex();
	public void setIndex(int index);
	
	/**
	 * The index of this token in the containing sequence when whitespace is taken into account.
	 * @return
	 */
	public int getIndexWithWhiteSpace();
	public void setIndexWithWhiteSpace(int indexInclWhiteSpace);

	/**
	 * A set of possible postags (assigned externally by a lexicon).
	 * @param possiblePosTags
	 */
	public abstract Set<PosTag> getPossiblePosTags();

	/**
	 * A list of postags and counts for this token in a training corpus (assigned externally by a statistics service).
	 * @return
	 */
	public abstract Map<PosTag, Integer> getFrequencies();
	public abstract void setFrequencies(Map<PosTag, Integer> frequencies);
	
	/**
	 * Get a feature result from the cache.
	 * @return
	 */
	public <T> FeatureResult<T> getResultFromCache(TokenFeature<T> tokenFeature);

	/**
	 * Get a feature result from the cache.
	 * @return
	 */
	public <T> void putResultInCache(TokenFeature<T> tokenFeature, FeatureResult<T> featureResult);

	/**
	 * The start index of this token within the enclosing sentence.
	 * @return
	 */
	public abstract int getStartIndex();

	/**
	 * The end index of this token within the enclosing sentence.
	 * @return
	 */
	public abstract int getEndIndex();
	
	/**
	 * Is this token a separator or not?
	 * @return
	 */
	public boolean isSeparator();
	public void setSeparator(boolean separator);
	
	/**
	 * Is this token white-space or not?
	 * @return
	 */
	public boolean isWhiteSpace();
	
	/**
	 * A list of TokeniserPatterns for which this token was matched, and the pattern index at which the match occurred.
	 * @return
	 */
	public List<TokenMatch> getMatches();
	
	/**
	 * A list of atomic decisions which make up this token.
	 * @return
	 */
	public List<TaggedToken<TokeniserOutcome>> getAtomicParts();
	public void setAtomicParts(
			List<TaggedToken<TokeniserOutcome>> atomicParts);
	
	/**
	 * Have this token's features already been logged once?
	 * Used to avoid logging a token multiple times during a beam search.
	 * @return
	 */
	public boolean isLogged();
	public void setLogged(boolean logged);
	
	/**
	 * Is this an empty token (without any textual content?)
	 * @return
	 */
	public boolean isEmpty();
}