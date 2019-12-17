/* Copyright (C) 2013-2019 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vwzq.polca;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Arrays;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.cli.*;
import javax.annotation.Nullable;
import java.io.PrintStream;

import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealy;
import de.learnlib.counterexamples.LocalSuffixFinders;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealy;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealy;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.counterexamples.AcexLocalSuffixFinder;
import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.api.SUL;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.algorithm.LearningAlgorithm.MealyLearner;
import de.learnlib.api.oracle.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.oracle.equivalence.RandomWpMethodEQOracle;
import de.learnlib.api.statistic.StatisticSUL;
import de.learnlib.drivers.reflect.MethodInput;
import de.learnlib.drivers.reflect.MethodOutput;
import de.learnlib.drivers.reflect.SimplePOJOTestDriver;
import de.learnlib.filter.cache.sul.SULCaches;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.util.Experiment.MealyExperiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.Visualization;
import de.learnlib.mapper.api.SULMapper;
import de.learnlib.mapper.SULMappers;
import net.automatalib.words.Word;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import de.learnlib.drivers.reflect.ReturnValue;
import de.learnlib.drivers.reflect.Error;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import de.learnlib.api.exception.SULException;
import de.learnlib.filter.statistic.oracle.CounterOracle.MealyCounterOracle;
import de.learnlib.filter.cache.mealy.MealyCacheOracle;
import net.automatalib.incremental.ConflictException;
import de.learnlib.filter.cache.mealy.MealyCaches;
import de.learnlib.oracle.equivalence.EQOracleChain.MealyEQOracleChain;

enum PolicyType {
	LRU,
	LIP,
	PLIP,
	FIFO,
	PLRU,
	MRU,
	SRRIPHP,
	SRRIPFP,
	HW,
	SKYL3,
	SKYL2,
}

enum LearnAlgorithmType {
	LSTAR,
	TTT,
	KV,
	MP,
	DT,
	DHC,
	RS,
}

class Config {

	public int max_depth;
	public int ways;
	public PolicyType policy;
	public boolean is_random;
	public boolean no_cache;
	public boolean verbose;
	public boolean temp_model;
	public boolean silent;
	public boolean is_hw;
	public String proxy_path;
	public String output_path;
	public int r_min, r_len, r_bound;
	public int repetitions;
	public Float hit_ratio, miss_ratio;
	public String prefix;
	public int max_size;
	public LearnAlgorithmType learner;
	public int votes;

	public Config (CommandLine cmd) throws Exception {
		this.max_depth = Integer.parseInt(cmd.getOptionValue("depth", "1"));
		this.ways = Integer.parseInt(cmd.getOptionValue("ways", "4"));
		this.r_min = Integer.parseInt(cmd.getOptionValue("r_min", "10"));
		this.r_len = Integer.parseInt(cmd.getOptionValue("r_len", "30"));
		this.r_bound = Integer.parseInt(cmd.getOptionValue("r_len", "1000"));
		this.repetitions = Integer.parseInt(cmd.getOptionValue("repetitions", "100"));
		this.max_size = Integer.parseInt(cmd.getOptionValue("max_size", "2147483647"));
		this.hit_ratio = Float.parseFloat(cmd.getOptionValue("hit_ratio", "0.8"));
		this.miss_ratio = Float.parseFloat(cmd.getOptionValue("miss_ratio", "0.2"));
		this.votes = Integer.parseInt(cmd.getOptionValue("votes", "1"));
		this.prefix = cmd.getOptionValue("prefix", "@");
		this.is_hw = false;
		switch (cmd.getOptionValue("policy", "fifo").toLowerCase()) {
			case "hw":
				this.policy = PolicyType.HW;
				this.is_hw = true;
				break;
			case "lru":
				this.policy = PolicyType.LRU;
				break;
			case "plru":
				this.policy = PolicyType.PLRU;
				break;
			case "mru":
				this.policy = PolicyType.MRU;
				break;
			case "srriphp":
				this.policy = PolicyType.SRRIPHP;
				break;
			case "srripfp":
				this.policy = PolicyType.SRRIPFP;
				break;
			case "fifo":
				this.policy = PolicyType.FIFO;
				break;
			case "lip":
				this.policy = PolicyType.LIP;
				break;
			case "plip":
				this.policy = PolicyType.PLIP;
				break;
			case "new2":
				this.policy = PolicyType.SKYL3;
				break;
			case "new1":
				this.policy = PolicyType.SKYL2;
				break;
			default:
				throw new Exception("unsupported policy");
		}
		switch (cmd.getOptionValue("learner", "kv").toLowerCase()) {
			case "ttt":
				this.learner = LearnAlgorithmType.TTT;
				break;
			case "kv":
				this.learner = LearnAlgorithmType.KV;
				break;
			case "dhc":
				this.learner = LearnAlgorithmType.DHC;
				break;
			case "mp":
				this.learner = LearnAlgorithmType.MP;
				break;
			case "rs":
				this.learner = LearnAlgorithmType.RS;
				break;
			case "dt":
				this.learner = LearnAlgorithmType.DT;
				break;
			case "lstar":
				this.learner = LearnAlgorithmType.LSTAR;
				break;
			default:
				throw new Exception("unsupported learning algorithm");
		}

		if (this.policy == PolicyType.HW && !cmd.hasOption("binary")) {
			throw new Exception("no path to proxy for 'hw' policy");
		}
		this.proxy_path = cmd.getOptionValue("binary", "");
		this.output_path = cmd.getOptionValue("output", "");
		this.is_random = cmd.hasOption("random");
		this.no_cache = cmd.hasOption("no_cache");
		this.verbose = cmd.hasOption("verbose") && !cmd.hasOption("silent");
		this.temp_model = cmd.hasOption("temp");
		this.silent = cmd.hasOption("silent");
	}

}

public final class Polca {

	private Config config;

    public Polca (CommandLine cmd) throws Exception {
		this.config = new Config(cmd);
    }

    public static void main(String[] args) throws Exception {

		// Options
        Options options = new Options();

		// cache settings
		options.addOption(new Option("d", "depth", true, "max_depth for membership queries (default: 1)"));
		options.addOption(new Option("w", "ways", true, "cache associativity (default: 4)"));
		options.addOption(new Option("p", "policy", true, "simulator cache policy: fifo|lru|plru|lip|plip|mru|srriphp|srripfp|new1|new2|hw (default: 'fifo')"));
		options.addOption(new Option("b", "binary", true, "path to proxy for 'hw' policy"));
		// general
		options.addOption(new Option("o", "output", true, "write learnt .dot model into output file"));
		// other
		options.addOption(new Option("prefix",  true, "prefix before every query, used to fill cache (default: '@')"));
		options.addOption(new Option("r", "repetitions", true, "number of measurements by cachequery (default: 100)"));
		options.addOption(new Option("votes", true, "number of votes for deciding result (default: 1)"));
		options.addOption(new Option("hit_ratio", true, "ratio of hits to consider a HIT (default: 0.8)"));
		options.addOption(new Option("miss_ratio", true, "ratio of misses to consider a MISS (default: 0.2)"));
		// learning settings
		options.addOption(new Option("l", "learner", true, "learning algorithm lstar|kv|mp|rs|dhc|dt|ttt (default: 'kv')"));
		options.addOption(new Option("m", "max_size", true, "maximum number of states of SUL"));
		options.addOption(new Option("r_min", true, "minimal length of random word (default: 10)"));
		options.addOption(new Option("r_len", true, "expected length of random word (r_min + r_len) (default: 30)"));
		options.addOption(new Option("r_bound", true, "bound on queries for equivalence, set to 0 for unbounded (default: 1000)"));
		options.addOption(new Option("r_rand", true, "TODO: select custom random generator"));
		// flags
		options.addOption(new Option("random", false, "use random wp-method as equivalence query"));
		options.addOption(new Option("temp", false, "write partial model into '.model.tmp' file"));
		options.addOption(new Option("verbose", false, "output verbose information"));
		options.addOption(new Option("no_cache", false, "don't use cache for membership queries"));
		options.addOption(new Option("h", "help", false, "show this help message"));
		options.addOption(new Option("s", "silent", false, "remove stdout info"));

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Polca", options);
            System.exit(1);
        }

		// show help menu
		if (cmd.hasOption("help")) {
            formatter.printHelp("Polca", options);
			System.exit(1);
		}

		// check config
		Polca learn = null;
		try {
			learn = new Polca(cmd);
		} catch (Exception e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Polca", options);
            System.exit(1);
		}

		// execute
		try{
			learn.run();
		} catch (Exception e) {
			throw e;
		}

	}

	public void run() throws Exception {

		// abstract alphabet
		String[] alphabet = {
			"h(0)", "h(1)", "h(2)", "h(3)", "h(4)", "h(5)", "h(6)", "h(7)", "h(8)",
			"h(9)", "h(10)", "h(11)", "h(12)", "h(13)", "h(14)", "h(15)", "h(16)", "h(17)",
			"h(18)", "h(19)", "h(20)", "h(21)", "h(22)", "h(23)", "h(24)", "h(25)", "h(26)",
		};
		alphabet = Arrays.copyOfRange(alphabet, 0, this.config.ways+1);
		alphabet[alphabet.length-1] = "m()";
		Alphabet<String> abstractInputAlphabet = Alphabets.fromArray(alphabet);

        // instantiate test driver
		CacheSUL memSul = new CacheSUL(this.config, abstractInputAlphabet);
		CacheSUL eqSul = new CacheSUL(this.config, abstractInputAlphabet);
        System.out.println("-------------------------------------------------------");


		// Membership Queries
		CacheSULOracle sulMemOracle = new CacheSULOracle(memSul, this.config, "mq");
		MealyCounterOracle<String, String> statsMemOracle = new MealyCounterOracle<String, String>(sulMemOracle, "membership queries");
		MealyCacheOracle<String, String> cachedMemOracle = MealyCaches.createDAGCache(abstractInputAlphabet, statsMemOracle);
		MealyCounterOracle<String, String> statsCachedMemOracle = new MealyCounterOracle<String, String>(cachedMemOracle, "membership queries hit cache");
		MembershipOracle.MealyMembershipOracle<String, String> effMemOracle = this.config.no_cache ? statsMemOracle : statsCachedMemOracle;

		// Equivalence Queries
		CacheSULOracle sulEqOracle = new CacheSULOracle(eqSul, this.config, "eq");
		MealyCounterOracle<String, String> statsEqOracle = new MealyCounterOracle<String, String>(sulEqOracle, "equivalence queries");
		MealyCacheOracle<String, String> cachedEqOracle = MealyCaches.createDAGCache(abstractInputAlphabet, statsEqOracle);
		MealyCounterOracle<String, String> statsCachedEqOracle = new MealyCounterOracle<String, String>(cachedEqOracle, "equivalence queries hit cache");
		MealyEquivalenceOracle<String, String> consistencyEqOracle = cachedMemOracle.createCacheConsistencyTest();

		// Learner from membership oracle
		LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> learn;
		switch (this.config.learner) {
			case TTT:
				learn = new TTTLearnerMealyBuilder<String, String>().withAlphabet(abstractInputAlphabet).withOracle(effMemOracle).withAnalyzer(AcexAnalyzers.LINEAR_FWD).create();
				break;
			case DHC:
				learn = new MealyDHC<String, String>(abstractInputAlphabet, effMemOracle);
				break;
			case KV:
				learn = new KearnsVaziraniMealy<String, String>(abstractInputAlphabet, effMemOracle, true, AcexAnalyzers.LINEAR_FWD);
				break;
			case MP:
				learn = new MalerPnueliMealy<String, String>(abstractInputAlphabet, effMemOracle);
				break;
			case RS:
				learn = new RivestSchapireMealy<String, String>(abstractInputAlphabet, effMemOracle);
				break;
			case DT:
				learn = new DTLearnerMealy<String, String>(abstractInputAlphabet, effMemOracle, LocalSuffixFinders.RIVEST_SCHAPIRE, true);
				break;
			case LSTAR:
			default:
				learn = new ExtensibleLStarMealyBuilder<String,String>().withAlphabet(abstractInputAlphabet).withOracle(effMemOracle).create();
			break;
		}

		boolean finished = false, random_ce = true;
		MealyMachine<?, String, ?, String> hyp = null;
		DefaultQuery<String, Word<String>> ce = null;
		int count = 0;

		// Main learning loop
		SimpleProfiler.start("learn");
		do {
			if (ce == null) {
				learn.startLearning();
			} else {
				boolean refined = learn.refineHypothesis(ce);
				if (!refined) {
					System.err.println("No refinement effected by counterexample!");
				}
			}

			hyp = learn.getHypothesisModel();
			if (!this.config.silent) System.out.println("--> Hypothesis: " + hyp.getStates() + " - " + hyp.size());
			// update depth with patch from: https://github.com/LearnLib/automatalib/issues/32

			// if reach max size, we can skip comformance testing (learning is sound refinement)
			if (hyp.size() >= this.config.max_size) {
				break;
			}

			if (this.config.temp_model) {
				try {
					PrintStream fileOut = new PrintStream(".model.tmp");
					GraphDOT.write(hyp, abstractInputAlphabet, fileOut); // may throw IOException!
					fileOut.close();
				} catch (IOException e) {}
			}

			if (this.config.is_random) {
				if (this.config.no_cache) {
					MealyEquivalenceOracle<String, String> randomWpMethod = new RandomWpMethodEQOracle.MealyRandomWpMethodEQOracle<>(statsEqOracle, this.config.r_min, this.config.r_len, this.config.r_bound);
					ce = randomWpMethod.findCounterExample(hyp, abstractInputAlphabet);
				} else {
					MealyEquivalenceOracle<String, String> randomWpMethod = new RandomWpMethodEQOracle.MealyRandomWpMethodEQOracle<>(statsCachedEqOracle, this.config.r_min, this.config.r_len, this.config.r_bound);
					MealyEQOracleChain<String, String> eqOracle = new MealyEQOracleChain<>(consistencyEqOracle, randomWpMethod);
					ce = eqOracle.findCounterExample(hyp, abstractInputAlphabet);
				}
				random_ce = (ce != null);
			}

			if (!this.config.is_random || !random_ce) {
				if (this.config.no_cache) {
					MealyEquivalenceOracle<String, String> wpMethod = new WpMethodEQOracle.MealyWpMethodEQOracle<>(statsEqOracle, this.config.max_depth);
					ce = wpMethod.findCounterExample(hyp, abstractInputAlphabet);
				} else {
					MealyEquivalenceOracle<String, String> wpMethod = new WpMethodEQOracle.MealyWpMethodEQOracle<>(statsCachedEqOracle, this.config.max_depth);
					MealyEQOracleChain<String, String> eqOracle = new MealyEQOracleChain<>(consistencyEqOracle, wpMethod);
					ce = eqOracle.findCounterExample(hyp, abstractInputAlphabet);
				}
				random_ce = true;
			}

			if (!this.config.silent) System.out.println("ce : " + ce);

			count++;

		} while (ce != null);

        if (!this.config.silent) System.out.println("-------------------------------------------------------");
		SimpleProfiler.stop("learn");
		System.out.println("\t" + SimpleProfiler.getResults());
        System.out.println("-------------------------------------------------------");
		System.out.println("--> Hypothesis: " + hyp.getStates() + " - " + hyp.size());
        System.out.println("-------------------------------------------------------");
//		System.out.println("Total HW queries: " + totalHwQueries);
		System.out.println("Summary Statistics: ");
		System.out.println("\t" + statsCachedMemOracle.getStatisticalData().getSummary());
		System.out.println("\t" + statsMemOracle.getStatisticalData().getSummary());
		System.out.println("\treal mem queries to SUL: " + memSul.getNumSULQueries());
		System.out.println("\t" + statsCachedEqOracle.getStatisticalData().getSummary());
		System.out.println("\t" + statsEqOracle.getStatisticalData().getSummary());
		System.out.println("\treal eq queries to SUL: " + eqSul.getNumSULQueries());

		if (hyp != null) {
			// model statistics
			System.out.println("\ttotal states: " + hyp.size());
			System.out.println("\talphabet size: " + abstractInputAlphabet.size());
			System.out.println("-------------------------------------------------------");

			// show model
			System.out.println();
			System.out.println("Model: ");
			if (this.config.output_path.isEmpty()) {
				Visualization.visualize(hyp, abstractInputAlphabet);
			} else {
				PrintStream fileOut = new PrintStream(this.config.output_path);
				GraphDOT.write(hyp, abstractInputAlphabet, System.out); // may throw IOException!
				GraphDOT.write(hyp, abstractInputAlphabet, fileOut); // may throw IOException!
			}
		}

        System.out.println("-------------------------------------------------------");

    }

}

