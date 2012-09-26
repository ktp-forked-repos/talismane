package com.joliciel.talismane.parser;

import java.io.Reader;
import java.util.Set;

import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.machineLearning.CorpusEventStream;
import com.joliciel.talismane.machineLearning.DecisionMaker;
import com.joliciel.talismane.machineLearning.maxent.JolicielMaxentModel;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.parser.features.ParserFeatureService;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.tokeniser.TokeniserService;

public class ParserServiceImpl implements ParserServiceInternal {
	ParserFeatureService parseFeatureService;
	PosTaggerService posTaggerService;
	TokeniserService tokeniserService;
	
	@Override
	public DependencyArc getDependencyArc(PosTaggedToken head,
			PosTaggedToken dependent, String label) {
		DependencyArcImpl arc = new DependencyArcImpl(head, dependent, label);
		return arc;
	}

	@Override
	public ParseConfiguration getInitialConfiguration(
			PosTagSequence posTagSequence) {
		ParseConfigurationImpl configuration = new ParseConfigurationImpl(posTagSequence);
		configuration.setParserServiceInternal(this);
		return configuration;
	}

	@Override
	public ParseConfiguration getConfiguration(ParseConfiguration history) {
		ParseConfigurationImpl configuration = new ParseConfigurationImpl(history);
		configuration.setParserServiceInternal(this);
		return configuration;
	}

	@Override
	public NonDeterministicParser getTransitionBasedParser(DecisionMaker<Transition> decisionMaker, TransitionSystem transitionSystem, Set<ParseConfigurationFeature<?>> parseFeatures, int beamWidth) {
		TransitionBasedParser parser = new TransitionBasedParser(decisionMaker, transitionSystem, parseFeatures, beamWidth);
		parser.setParserServiceInternal(this);
		return parser;
	}

	@Override
	public CorpusEventStream getParseEventStream(
			ParserAnnotatedCorpusReader corpusReader,
			Set<ParseConfigurationFeature<?>> parseFeatures) {
		ParseEventStream eventStream = new ParseEventStream(corpusReader, parseFeatures);
		eventStream.setParseService(this);
		return eventStream;
	}

	@Override
	public ParserEvaluator getParserEvaluator() {
		ParserEvaluatorImpl evaluator = new ParserEvaluatorImpl();
		evaluator.setParserServiceInternal(this);
		return evaluator;
	}

	@Override
	public TransitionSystem getShiftReduceTransitionSystem() {
		ShiftReduceTransitionSystem transitionSystem = new ShiftReduceTransitionSystem();
		return transitionSystem;
	}

	@Override
	public TransitionSystem getArcEagerTransitionSystem() {
		ArcEagerTransitionSystem transitionSystem = new ArcEagerTransitionSystem();
		return transitionSystem;
	}

	@Override
	public NonDeterministicParser getTransitionBasedParser(
			JolicielMaxentModel<Transition> jolicielMaxentModel, int beamWidth) {
		DecisionMaker<Transition> decisionMaker = jolicielMaxentModel.getDecisionMaker();
		TransitionSystem transitionSystem = null;
		String transitionSystemClassName = (String) jolicielMaxentModel.getModelAttributes().get("transitionSystem");
		if (transitionSystemClassName.equalsIgnoreCase("ShiftReduceTransitionSystem")) {
			transitionSystem = this.getShiftReduceTransitionSystem();
		} else if (transitionSystemClassName.equalsIgnoreCase("ArcEagerTransitionSystem")) {
			transitionSystem = this.getArcEagerTransitionSystem();
		} else {
			throw new TalismaneException("Unknown transition system: " + transitionSystemClassName);
		}
		
		Set<ParseConfigurationFeature<?>> parseFeatures = this.getParseFeatureService().getFeatures(jolicielMaxentModel.getFeatureDescriptors());

		return this.getTransitionBasedParser(decisionMaker, transitionSystem, parseFeatures, beamWidth);
	}

	public ParserFeatureService getParseFeatureService() {
		return parseFeatureService;
	}

	public void setParseFeatureService(ParserFeatureService parseFeatureService) {
		this.parseFeatureService = parseFeatureService;
	}

	@Override
	public DependencyNode getDependencyNode(PosTaggedToken token, String label,
			DependencyNode parent, ParseConfiguration parseConfiguration) {
		DependencyNodeImpl dependencyNode = new DependencyNodeImpl(token, label, parent, parseConfiguration);
		dependencyNode.setParserServiceInternal(this);
		dependencyNode.setLexiconService(TalismaneSession.getLexiconService());
		return dependencyNode;
	}

	@Override
	public ParserRegexBasedCorpusReader getRegexBasedCorpusReader(Reader reader) {
		ParserRegexBasedCorpusReaderImpl corpusReader = new ParserRegexBasedCorpusReaderImpl(reader);
		corpusReader.setParserService(this);
		corpusReader.setPosTaggerService(this.getPosTaggerService());
		corpusReader.setTokeniserService(this.getTokeniserService());
		return corpusReader;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}
	
	
}