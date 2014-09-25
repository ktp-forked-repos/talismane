///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
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
package com.joliciel.talismane.fr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.frenchTreebank.TreebankReader;
import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.frenchTreebank.export.FtbPosTagMapper;
import com.joliciel.frenchTreebank.export.TreebankExportService;
import com.joliciel.frenchTreebank.upload.TreebankUploadService;
import com.joliciel.lefff.LefffService;
import com.joliciel.lefff.LefffServiceLocator;
import com.joliciel.talismane.LanguageSpecificImplementation;
import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.NeedsTalismaneSession;
import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.TalismaneConfig;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneService;
import com.joliciel.talismane.TalismaneServiceLocator;
import com.joliciel.talismane.Talismane.Command;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.extensions.Extensions;
import com.joliciel.talismane.fr.tokeniser.filters.AllUppercaseFrenchFilter;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenAfterDuFilter;
import com.joliciel.talismane.fr.tokeniser.filters.EmptyTokenBeforeDuquelFilter;
import com.joliciel.talismane.fr.tokeniser.filters.LowercaseFirstWordFrenchFilter;
import com.joliciel.talismane.fr.tokeniser.filters.UpperCaseSeriesFrenchFilter;
import com.joliciel.talismane.lexicon.LexiconChain;
import com.joliciel.talismane.lexicon.LexiconDeserializer;
import com.joliciel.talismane.lexicon.PosTagMapper;
import com.joliciel.talismane.lexicon.PosTaggerLexicon;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.parser.ParserRegexBasedCorpusReader;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.TransitionSystem;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.posTagger.filters.PosTagSequenceFilter;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorAnnotatedCorpusReader;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.tokeniser.TokeniserAnnotatedCorpusReader;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.filters.TokenSequenceFilter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.StringUtils;

/**
 * The default French implementation of Talismane.
 * @author Assaf Urieli
 *
 */
public class TalismaneFrench implements LanguageSpecificImplementation {
	private static final Log LOG = LogFactory.getLog(TalismaneFrench.class);
	private TalismaneServiceLocator talismaneServiceLocator = null;
	private TalismaneService talismaneService;
	private TalismaneSession talismaneSession;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private MachineLearningService machineLearningService;
	private LefffServiceLocator lefffServiceLocator;
	private LefffService lefffService;
	private List<Class<? extends TokenSequenceFilter>> availableTokenSequenceFilters;
	
	private static ClassificationModel<SentenceDetectorOutcome> sentenceModel;
	private static ClassificationModel<TokeniserOutcome> tokeniserModel;
	private static ClassificationModel<PosTag> posTaggerModel;
	private static MachineLearningModel parserModel;
	private static PosTaggerLexicon lexicon;

	private enum CorpusFormat {
		/** CoNLL-X format */
		conll,
		/** French Treebank XML reader */
		ftb,
		/** French Treebank converted to dependencies */
		ftbDep,
		/** SPMRL format */
		spmrl
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	Map<String,String> argsMap = StringUtils.convertArgs(args);
    	CorpusFormat corpusReaderType = null;
    	String treebankDirPath = null;
		boolean keepCompoundPosTags = true;

    	if (argsMap.containsKey("corpusReader")) {
    		corpusReaderType = CorpusFormat.valueOf(argsMap.get("corpusReader"));
    		argsMap.remove("corpusReader");
    	}
    	if (argsMap.containsKey("treebankDir")) {
    		treebankDirPath = argsMap.get("treebankDir");
    		argsMap.remove("treebankDir");
    	}
    	if (argsMap.containsKey("keepCompoundPosTags")) {
    		keepCompoundPosTags = argsMap.get("keepCompoundPosTags").equalsIgnoreCase("true");
    		argsMap.remove("keepCompoundPosTags");
    	}
    	
    	Extensions extensions = new Extensions();
    	extensions.pluckParameters(argsMap);
    	
    	String sessionId = "";
       	TalismaneServiceLocator locator = TalismaneServiceLocator.getInstance(sessionId);
       	TalismaneService talismaneService = locator.getTalismaneService();
    	TalismaneFrench talismaneFrench = new TalismaneFrench(sessionId);
    	
    	TalismaneConfig config = talismaneService.getTalismaneConfig(argsMap, talismaneFrench);
    	if (config.getCommand()==null)
    		return;
    	
    	if (corpusReaderType!=null) {
    		if (corpusReaderType==CorpusFormat.ftbDep) {
    			File inputFile = new File(config.getInFilePath());
    			FtbDepReader ftbDepReader = new FtbDepReader(inputFile, config.getInputCharset());
    			ftbDepReader.setParserService(config.getParserService());
    			ftbDepReader.setPosTaggerService(config.getPosTaggerService());
    			ftbDepReader.setTokeniserService(config.getTokeniserService());
    			ftbDepReader.setTokenFilterService(config.getTokenFilterService());
    			ftbDepReader.setTalismaneService(config.getTalismaneService());
    			
    			ftbDepReader.setKeepCompoundPosTags(keepCompoundPosTags);
    			ftbDepReader.setPredictTransitions(config.isPredictTransitions());
    			
	  			config.setParserCorpusReader(ftbDepReader);
				config.setPosTagCorpusReader(ftbDepReader);
				config.setTokenCorpusReader(ftbDepReader);
				config.setSentenceCorpusReader(ftbDepReader);
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
					FtbDepReader ftbDepEvaluationReader = new FtbDepReader(evaluationFile, config.getInputCharset());
					ftbDepEvaluationReader.setKeepCompoundPosTags(keepCompoundPosTags);
		  			config.setParserEvaluationCorpusReader(ftbDepEvaluationReader);
					config.setPosTagEvaluationCorpusReader(ftbDepEvaluationReader);
				}
	  		} else if (corpusReaderType==CorpusFormat.ftb) {
	  			TreebankServiceLocator treebankServiceLocator = TreebankServiceLocator.getInstance(locator);
	  			TreebankUploadService treebankUploadService = treebankServiceLocator.getTreebankUploadServiceLocator().getTreebankUploadService();
				TreebankExportService treebankExportService = treebankServiceLocator.getTreebankExportServiceLocator().getTreebankExportService();
	  			File treebankFile = new File(treebankDirPath);
				TreebankReader treebankReader = treebankUploadService.getXmlReader(treebankFile);
	  			
				// we prepare both the tokeniser and pos-tag readers, just in case they are needed
	  			InputStream posTagMapStream = talismaneFrench.getDefaultPosTagMapFromStream();
	  			Scanner scanner = new Scanner(posTagMapStream,"UTF-8");
	  			List<String> descriptors = new ArrayList<String>();
	  			while (scanner.hasNextLine())
	  				descriptors.add(scanner.nextLine());
				FtbPosTagMapper ftbPosTagMapper = treebankExportService.getFtbPosTagMapper(descriptors, talismaneFrench.getDefaultPosTagSet());
				PosTagAnnotatedCorpusReader posTagAnnotatedCorpusReader = treebankExportService.getPosTagAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, keepCompoundPosTags);
				config.setPosTagCorpusReader(posTagAnnotatedCorpusReader);

				TokeniserAnnotatedCorpusReader tokenCorpusReader = treebankExportService.getTokeniserAnnotatedCorpusReader(treebankReader, ftbPosTagMapper, keepCompoundPosTags);
  				config.setTokenCorpusReader(tokenCorpusReader);
  				
  				SentenceDetectorAnnotatedCorpusReader sentenceCorpusReader = treebankExportService.getSentenceDetectorAnnotatedCorpusReader(treebankReader);
  				config.setSentenceCorpusReader(sentenceCorpusReader);
	  		} else if (corpusReaderType==CorpusFormat.conll || corpusReaderType==CorpusFormat.spmrl) {
    			File inputFile = new File(config.getInFilePath());
    			
    			ParserRegexBasedCorpusReader corpusReader = config.getParserService().getRegexBasedCorpusReader(inputFile, config.getInputCharset());
    			
    			corpusReader.setPredictTransitions(config.isPredictTransitions());
    			
	  			config.setParserCorpusReader(corpusReader);
				config.setPosTagCorpusReader(corpusReader);
				config.setTokenCorpusReader(corpusReader);
				config.setSentenceCorpusReader(corpusReader);
				
				if (corpusReaderType==CorpusFormat.spmrl) {
					corpusReader.setRegex("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%");
				}
 				
				if (config.getInputRegex()!=null) {
					corpusReader.setRegex(config.getInputRegex());
				}
				
				if (config.getCommand().equals(Command.compare)) {
					File evaluationFile = new File(config.getEvaluationFilePath());
	    			ParserRegexBasedCorpusReader evaluationReader = config.getParserService().getRegexBasedCorpusReader(evaluationFile, config.getInputCharset());
		  			config.setParserEvaluationCorpusReader(evaluationReader);
					config.setPosTagEvaluationCorpusReader(evaluationReader);
					
					if (corpusReaderType==CorpusFormat.spmrl) {
						evaluationReader.setRegex("%INDEX%\\t%TOKEN%\\t.*\\t.*\\t%POSTAG%\\t.*\\t.*\\t.*\\t%GOVERNOR%\\t%LABEL%");
					}
	 				
					if (config.getInputRegex()!=null) {
						evaluationReader.setRegex(config.getInputRegex());
					}
				}
	  		} else {
	  			throw new TalismaneException("Unknown corpusReader: " + corpusReaderType);
	  		}
    	}
    	Talismane talismane = config.getTalismane();
    	
    	extensions.prepareCommand(config, talismane);
    	
    	talismane.process();
	}

	public TalismaneFrench(String sessionId) {
		talismaneServiceLocator = TalismaneServiceLocator.getInstance(sessionId);
		lefffServiceLocator = LefffServiceLocator.getInstance();
	}
	
	private static ZipInputStream getZipInputStreamFromResource(String resource) {
		InputStream inputStream = getInputStreamFromResource(resource);
		ZipInputStream zis = new ZipInputStream(inputStream);
		
		return zis;
	}

	private static InputStream getInputStreamFromResource(String resource) {
		String path = "/com/joliciel/talismane/fr/resources/" + resource;
		LOG.debug("Getting " + path);
		InputStream inputStream = TalismaneFrench.class.getResourceAsStream(path); 
		
		return inputStream;
	}

	@Override
	public Scanner getDefaultPosTagSetScanner() {
		InputStream posTagInputStream = getInputStreamFromResource("talismaneTagset.txt");
		return new Scanner(posTagInputStream, "UTF-8");
	}
	

	@Override
	public Scanner getDefaultPosTaggerRulesScanner() {
		InputStream inputStream = getInputStreamFromResource("posTaggerConstraints_fr.txt");
		return new Scanner(inputStream, "UTF-8");
	}
	

	@Override
	public Scanner getDefaultParserRulesScanner() {
//		InputStream inputStream = getInputStreamFromResource("parserRules_fr.txt");
//		InputStream inputStream = null;
		return null;
	}


	@Override
	public PosTaggerLexicon getDefaultLexicon() {
		try {
			if (lexicon==null) {
				LexiconChain lexiconChain = new LexiconChain();
				
				LexiconDeserializer deserializer = new LexiconDeserializer(this.getTalismaneSession());
				
				String lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.additions.obj";
				ObjectInputStream ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				PosTaggerLexicon oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
		
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.agglutinates.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
				
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.conj.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
				
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.det.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
				
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.ponct.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
				
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.prep.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
				
				lexiconPath = "/com/joliciel/talismane/fr/resources/talismane_lex.pro.obj";
				ois = new ObjectInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(ois);
				lexiconChain.addLexicon(oneLex);
		
				lexiconPath = "/com/joliciel/talismane/fr/resources/lefff3OpenTalismane.zip";
				ZipInputStream zis = new ZipInputStream(TalismaneFrench.class.getResourceAsStream(lexiconPath)); 
				oneLex = deserializer.deserializeLexiconFile(zis);
				
				String posTagMapperName = "lefff3TalismanePosTagMap.txt";
				InputStream posTagMapperStream = getInputStreamFromResource(posTagMapperName);
				Scanner scanner = new Scanner(posTagMapperStream, "UTF-8");
				PosTagMapper posTagMapper = this.getLefffService().getPosTagMapper(scanner, this.getDefaultPosTagSet());
				oneLex.setPosTagMapper(posTagMapper);
				lexiconChain.addLexicon(oneLex);
		
				lexiconChain.setPosTagSet(this.getDefaultPosTagSet());
				lexicon = lexiconChain;
			}
			return lexicon;
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}


	@Override
	public ClassificationModel<SentenceDetectorOutcome> getDefaultSentenceModel() {
		if (sentenceModel==null) {
			String sentenceModelName = "ftbAll_SentenceDetector_baseline1.zip";
			ZipInputStream zis = TalismaneFrench.getZipInputStreamFromResource(sentenceModelName);
			sentenceModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return sentenceModel;
	}

	@Override
	public ClassificationModel<TokeniserOutcome> getDefaultTokeniserModel() {
		if (tokeniserModel==null) {
			String tokeniserModelName = "ftbAll_tokeniser_baseline2_cutoff3.zip";
			ZipInputStream zis = TalismaneFrench.getZipInputStreamFromResource(tokeniserModelName);
			tokeniserModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return tokeniserModel;
	}

	@Override
	public ClassificationModel<PosTag> getDefaultPosTaggerModel() {
		if (posTaggerModel==null) {
			String posTaggerModelName = "postag_spmrlAll_maxent_i200_cut10_v2.zip";
			ZipInputStream zis = TalismaneFrench.getZipInputStreamFromResource(posTaggerModelName);
			posTaggerModel = this.getMachineLearningService().getClassificationModel(zis);
		}
		return posTaggerModel;
	}

	@Override
	public MachineLearningModel getDefaultParserModel() {
		if (parserModel==null) {
			String parserModelName = "parser_spmrl_all_maxent_i200_cutoff7_v2.zip";
			ZipInputStream zis = TalismaneFrench.getZipInputStreamFromResource(parserModelName);
			parserModel = this.getMachineLearningService().getMachineLearningModel(zis);
		}
		return parserModel;
	}

	@Override
	public List<TokenSequenceFilter> getDefaultTokenSequenceFilters() {
		List<TokenSequenceFilter> tokenFilters = new ArrayList<TokenSequenceFilter>();
		tokenFilters.add(new UpperCaseSeriesFrenchFilter());
		tokenFilters.add(new LowercaseFirstWordFrenchFilter());
		
		for (TokenSequenceFilter filter : tokenFilters) {
			if (filter instanceof NeedsTalismaneSession) {
				((NeedsTalismaneSession) filter).setTalismaneSession(this.getTalismaneSession());
			}
		}
		return tokenFilters;
	}

	@Override
	public Scanner getDefaultTextMarkerFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("text_marker_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}

	@Override
	public Scanner getDefaultTokenFiltersScanner() {
		InputStream inputStream = getInputStreamFromResource("token_filters.txt");
		return new Scanner(inputStream, "UTF-8");
	}
	
	public InputStream getDefaultPosTagMapFromStream() {
		InputStream inputStream = getInputStreamFromResource("ftbCrabbeCanditoTagsetMap.txt");
		return inputStream;
	}

	@Override
	public TransitionSystem getDefaultTransitionSystem() {
		TransitionSystem transitionSystem = this.getParserService().getArcEagerTransitionSystem();
		InputStream inputStream = getInputStreamFromResource("talismaneDependencyLabels.txt");
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		List<String> dependencyLabels = new ArrayList<String>();
		while (scanner.hasNextLine()) {
			String dependencyLabel = scanner.nextLine();
			if (!dependencyLabel.startsWith("#")) {
				if (dependencyLabel.indexOf('\t')>0)
					dependencyLabel = dependencyLabel.substring(0, dependencyLabel.indexOf('\t'));
				dependencyLabels.add(dependencyLabel);
			}
		}
		transitionSystem.setDependencyLabels(dependencyLabels);
		return transitionSystem;
	}

	@Override
	public List<PosTagSequenceFilter> getDefaultPosTagSequenceFilters() {
		List<PosTagSequenceFilter> filters = new ArrayList<PosTagSequenceFilter>();
		return filters;
	}


	@Override
	public PosTagSet getDefaultPosTagSet() {
		Scanner posTagSetScanner = this.getDefaultPosTagSetScanner();
		PosTagSet posTagSet = this.getPosTaggerService().getPosTagSet(posTagSetScanner);
		return posTagSet;
	}
	

	public PosTaggerService getPosTaggerService() {
		if (posTaggerService==null) {
			posTaggerService = talismaneServiceLocator.getPosTaggerServiceLocator().getPosTaggerService();
		}
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		if (parserService==null) {
			parserService = talismaneServiceLocator.getParserServiceLocator().getParserService();
		}
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public LefffService getLefffService() {
		if (lefffService==null) {
			lefffService = lefffServiceLocator.getLefffService();
		}
		return lefffService;
	}

	public void setLefffService(LefffService lefffService) {
		this.lefffService = lefffService;
	}

	@Override
	public List<Class<? extends TokenSequenceFilter>> getAvailableTokenSequenceFilters() {
		if (availableTokenSequenceFilters==null) {
			availableTokenSequenceFilters = new ArrayList<Class<? extends TokenSequenceFilter>>();
			availableTokenSequenceFilters.add(EmptyTokenAfterDuFilter.class);
			availableTokenSequenceFilters.add(EmptyTokenBeforeDuquelFilter.class);
			availableTokenSequenceFilters.add(LowercaseFirstWordFrenchFilter.class);
			availableTokenSequenceFilters.add(UpperCaseSeriesFrenchFilter.class);
			availableTokenSequenceFilters.add(AllUppercaseFrenchFilter.class);
		}
		return availableTokenSequenceFilters;
	}
	

	public TalismaneService getTalismaneService() {
		if (talismaneService==null) {
			this.setTalismaneService(talismaneServiceLocator.getTalismaneService());
		}
		return talismaneService;
	}

	public void setTalismaneService(TalismaneService talismaneService) {
		this.talismaneService = talismaneService;
		this.talismaneSession = talismaneService.getTalismaneSession();
	}

	@Override
	public LinguisticRules getDefaultLinguisticRules() {
		return new FrenchRules();
	}

	public TalismaneSession getTalismaneSession() {
		if (talismaneSession==null) {
			talismaneSession = this.getTalismaneService().getTalismaneSession();
		}
		return talismaneSession;
	}

	public MachineLearningService getMachineLearningService() {
		if (machineLearningService==null) {
			machineLearningService = talismaneServiceLocator.getMachineLearningServiceLocator().getMachineLearningService();
		}
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	
}
