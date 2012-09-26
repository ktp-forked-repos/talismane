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
package com.joliciel.talismane;

import java.io.Reader;
import java.util.List;

import com.joliciel.talismane.filters.TextStreamFilter;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.Parser;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTagger;
import com.joliciel.talismane.sentenceDetector.SentenceDetector;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.Tokeniser;

/**
 * A class for processing an input stream and writing the analysis result to an output stream.<br/>
 * The processing must go from a given start module to a given end module in sequence, where the modules available are:
 * Sentence detector, Tokeniser, Pos tagger, Parser.<br/>
 * A fixed input format is expected depending on the start module:<br/>
 * <li>Sentence detector: newlines indicate sentence breaks, but can have multiple sentences per paragraph.</li>
 * <li>Tokeniser: expect exactly one sentence per newline.</li>
 * <li>Pos tagger: expect one token per line and empty line to indicate sentence breaks. Empty tokens are indicated by an underscore.</li>
 * <li>Parser: each line should start with token-tab-postag and end with a newline.</li>
 * The output format is determined by the processor corresponding to the end-module.<br/>
 * @author Assaf Urieli
 *
 */
public interface Talismane {
	public enum Module {
		SentenceDetector,
		Tokeniser,
		PosTagger,
		Parser
	}
	
	public enum Command {
		analyse,
		evaluate
	}

	/**
	 * Does this instance of Talismane need a sentence detector to perform the requested processing.
	 */
	public boolean needsSentenceDetector();

	/**
	 * Does this instance of Talismane need a tokeniser to perform the requested processing.
	 */
	public boolean needsTokeniser();

	/**
	 * Does this instance of Talismane need a pos tagger to perform the requested processing.
	 */
	public boolean needsPosTagger();

	/**
	 * Does this instance of Talismane need a parser to perform the requested processing.
	 */
	public boolean needsParser();

	/**
	 * Process the reader from the startModule to the endModule,
	 * where the results are processed by the processors provided.
	 */
	public void process(Reader reader);

	public SentenceDetector getSentenceDetector();
	public void setSentenceDetector(SentenceDetector sentenceDetector);

	public Tokeniser getTokeniser();
	public void setTokeniser(Tokeniser tokeniser);

	public PosTagger getPosTagger();
	public void setPosTagger(PosTagger posTagger);

	public Parser getParser();
	public void setParser(Parser parser);
	
	/**
	 * Text stream filters are applied to raw text segments extracted from the stream, 3 segments at a time.
	 * This means that if the text to be updated crosses segment borders, it is handled correctly.
	 * @return
	 */
	public List<TextStreamFilter> getTextStreamFilters();
	public void setTextStreamFilters(List<TextStreamFilter> textStreamFilters);
	public void addTextStreamFilter(TextStreamFilter textStreamFilter);

	/**
	 * Should the beam get propagated between various levels of analysis, e.g. should multiple analyses
	 * be passed on from the tokeniser to the pos-tagger, etc.
	 * @return
	 */
	public boolean isPropagateBeam();
	public void setPropagateBeam(boolean propagateBeam);

	public SentenceProcessor getSentenceProcessor();
	public void setSentenceProcessor(SentenceProcessor sentenceProcessor);

	public TokenSequenceProcessor getTokenSequenceProcessor();
	public void setTokenSequenceProcessor(
			TokenSequenceProcessor tokenSequenceProcessor);

	public PosTagSequenceProcessor getPosTagSequenceProcessor();
	public void setPosTagSequenceProcessor(
			PosTagSequenceProcessor posTagSequenceProcessor);

	public ParseConfigurationProcessor getParseConfigurationProcessor();
	public void setParseConfigurationProcessor(
			ParseConfigurationProcessor parseConfigurationProcessor);

	/**
	 * The requested start module for the processing chain.
	 * @return
	 */
	public Module getStartModule();

	/**
	 * The requested end module for the processing chain.
	 * @return
	 */
	public Module getEndModule();
}