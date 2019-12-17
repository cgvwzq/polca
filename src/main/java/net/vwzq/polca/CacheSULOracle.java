package net.vwzq.polca;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import de.learnlib.api.SUL;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import net.automatalib.incremental.mealy.dag.IncrementalMealyDAGBuilder;

public class CacheSULOracle implements MealyMembershipOracle<String, String> {

    private final CacheSUL sul;
	private final String label;
	private Config config;
	static private IncrementalMealyDAGBuilder<String, ArrayList<String>> cache;

    public CacheSULOracle(CacheSUL sul, Config conf, String label) {
        this.sul = sul;
		this.config = conf;
		this.label = label;
		if (!this.config.no_cache && this.config.is_hw && this.cache == null) {
			this.cache = new IncrementalMealyDAGBuilder<>(sul.getAlphabet());
		}
    }

    @Override
    public void processQueries(Collection<? extends Query<String, Word<String>>> queries) {
		synchronized (sul) {
                processQueries(sul, queries, this.config, label, cache);
        }
    }

    private static void processQueries(CacheSUL sul, Collection<? extends Query<String, Word<String>>> queries, Config config, String label, IncrementalMealyDAGBuilder cache) {
        for (Query<String, Word<String>> q : queries) {
			if (config.verbose) System.out.println("(" + label + ") " + "processQuery: " + q);
			Word<String> output;
			if (cache == null) {
				output = answerQuery(sul, q.getPrefix(), q.getSuffix());
			} else {
				output = answerQueryWithCache(sul, q.getPrefix(), q.getSuffix(), cache);
			}
            q.answer(output);
        }
    }

    @NonNull
    public static Word<String> answerQuery(CacheSUL sul, Word<String> prefix, Word<String> suffix) {
        sul.pre();
        try {
            // Prefix: Execute symbols, don't record output
            for (String sym : prefix) {
                sul.step(sym);
            }

            // Suffix: Execute symbols, outputs constitute output word
            WordBuilder<String> wb = new WordBuilder<>(suffix.length());
            for (String sym : suffix) {
                wb.add(sul.step(sym));
            }

            return wb.toWord();
        } finally {
            sul.post();
        }
    }

    @NonNull
    public static Word<String> answerQueryWithCache(CacheSUL sul, Word<String> prefix, Word<String> suffix, IncrementalMealyDAGBuilder cache) {
		// prefix is always empty if using cache
		// we split suffix with the prefix that is already in cache, and step on it without executing
		// step can reuse the translation from abstract to concrete stored in cache
        sul.pre();
        try {

			Word<String> newPrefix = Word.epsilon(), newSuffix = Word.epsilon();
			List<ArrayList<String>> cachedInfo = new ArrayList<>(); // pairs concreteInput-abstractOutput
			// Find prefix in cache
			int i;
			for (i=suffix.length(); i>0; i--) {
				cachedInfo = new ArrayList<>();
				Word<String> preAI = suffix.subWord(0, i);
				if (cache.lookup(preAI, cachedInfo)) {
					newPrefix = preAI;
					break;
				}
			}
			if (i > 0) {
				newSuffix = suffix.subWord(i, suffix.length());
			} else {
				newSuffix = suffix;
			}

			// Use concrete input and outputs from cache, update internal states, and return abstract output
			Word<String> output = sul.cachedSteps(newPrefix, cachedInfo);

			// Iterate over new suffix to setup cache state
            WordBuilder<String> wb = new WordBuilder<>();
			List<ArrayList<String>> toCache = new ArrayList<>();
			i = 0;
            for (String sym : newSuffix) {
				// need to record abstract input - abstract output pairs as ArrayList<String> and insert it with suffix in cache
				String r = sul.step(sym, toCache);
                wb.add(r);
				i++;
            }
			// Concat results
			output = output.concat(wb.toWord());
			// Insert to cache
			cache.insert(suffix, Word.fromList(cachedInfo).concat(Word.fromList(toCache)));
			// Return
			return output;
        } finally {
            sul.post();
        }
    }

}
