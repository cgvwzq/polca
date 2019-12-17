package net.vwzq.polca;

import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.automatalib.words.WordBuilder;
import de.learnlib.api.SUL;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.SimpleAlphabet;
import net.automatalib.words.Word;
import de.learnlib.api.exception.SULException;

public class CacheSUL implements SUL<String, String> {

	private Alphabet<String> concreteAlphabet;
	private Alphabet<String> abstractAlphabet;
	private Cache cache;
	private Config config;
	private int numQueries;
	private Map<Integer, String> intern;
	private ArrayList<String> path;

	public void reset() {
		if (this.config.verbose) System.out.println("sul reset()");
		// Reset path
		this.path = new ArrayList<String>();
		// Reset intern dictionary used by abstract <-> concrete translation
		this.intern = new HashMap<Integer, String>();
		for (int i=0; i<this.config.ways+1; i++) {
			this.intern.put(i, this.concreteAlphabet.getSymbol(i));
		}
	}

	public Cache instantiateCache(Config config) throws Exception {
		// Instantiate cache policy
		switch (this.config.policy) {
			case HW:
				return new HW(this.config);
			case SRRIPHP:
				return new SRRIPHP(this.config);
			case SRRIPFP:
				return new SRRIPFP(this.config);
			case MRU:
				return new MRU(this.config);
			case PLRU:
				return new PLRU(this.config);
			case LRU:
				return new LRU(this.config);
			case FIFO:
				return new FIFO(this.config);
			case LIP:
				return new LIP(this.config);
			case PLIP:
				return new PLIP(this.config);
			case SKYL3:
				return new SKYL3(this.config);
			case SKYL2:
				return new SKYL2(this.config);
			default:
				return null;
		}
	}

	public CacheSUL(Config config, Alphabet<String> abstractAlphabet) throws Exception {
		this.config = config;
		this.concreteAlphabet = new SimpleAlphabet<String>(Arrays.asList("abcdefghijklmnopqrstuvwxyz".substring(0,config.ways+1).split("")));
		this.abstractAlphabet = abstractAlphabet;
		this.cache = instantiateCache(this.config);
		this.numQueries = 0;
		this.reset();
	}

	public int getNumSULQueries() {
		return this.numQueries;
	}

	public Alphabet<String> getAlphabet() {
		return this.abstractAlphabet;
	}

	public boolean canFork() {
		return false;
	}

	public String mapInput(String abstractInput) {
		// from input hit_offset or miss, choose block
		String ret;
		if (abstractInput.startsWith("h")) {
			int pos = Integer.parseInt(abstractInput.replaceAll("[^\\d]", "")); // get number
			ret = intern.get(pos);
		} else {
			ret = intern.get(this.config.ways); // last represents element outside cache
		}
		if (this.config.verbose) System.out.print(ret + " ");
		return ret;
	}

	// TODO: fix cache bypass here
	public String mapOutput(int concreteOutput) throws Exception {
		if (concreteOutput != 1) return "_";
		// assume concreteOutput == 1, we only care about MISS case
		String ret = "";
		int index = -1;
		ArrayList<String> candidates = new ArrayList<String>(this.intern.values());
		String miss = this.cache.locateMiss(this.path, candidates); // return replaced element
		this.numQueries++;
		if (miss != "") {
			// Update intern mapping
			index = candidates.indexOf(miss);
			return updateInternalOnMiss(index);
		} else {
			throw new Exception("ERROR: NO MISS.");
		}
	}

	private String updateInternalOnMiss(int idx) {
		String tmp = intern.get(idx);
		intern.put(idx, intern.get(this.config.ways));
		intern.put(this.config.ways, tmp);
		return Integer.toString(idx);
	}

	@Override
	public String step(String symbol) {
		return this.step(symbol, null);
	}


	// process abstract input, returns abstract output
	public String step(String symbol, List<ArrayList<String>> translationCache) {
		if (this.config.verbose) System.out.print("step: " + symbol + " path: " + this.path + " -> ");
		String result = "";
		String input = mapInput(symbol);
		this.path.add(input);
		// no need to access hits now, wait til a miss
		if (symbol.startsWith("h(")) {
			result = "_";
		} else {
			try {
				result = mapOutput(1); // 1=miss
			} catch (Exception e) {
				e.printStackTrace();
				result = "ERR";
				throw new SULException(new Throwable("Invalid response"));
			}
		}
		if (this.config.verbose) System.out.println(" / " + result);
		// Add concrete input and abstract output into cache
		if (translationCache != null) {
			translationCache.add(new ArrayList(Arrays.asList(input, result)));
		}
		return result;
	}

	public Word<String> cachedSteps(Word<String> abstractInput, List<ArrayList<String>> translationCache) {
		if (this.config.verbose) System.out.println("cache hit: " + abstractInput);
		Iterator<String> aIt = abstractInput.iterator();
		Iterator<ArrayList<String>> rIt = translationCache.iterator();
		// Iterate over new suffix to setup cache state
		WordBuilder<String> result = new WordBuilder<>();
		while (aIt.hasNext() && rIt.hasNext()) {
			String sym = aIt.next();
			ArrayList<String> ret = rIt.next();

			String concreteInput = ret.get(0);
			String abstractOutput = ret.get(1);

			this.path.add(concreteInput); // update path with cached concrete input
			if (sym.startsWith("m()")) {
				int idx = Integer.parseInt(abstractOutput); // replaced element id
				updateInternalOnMiss(idx); // update internal state upon miss
			}
			result.add(abstractOutput);
		}
		return result.toWord();
	}

	@Override
	public void pre() {
		try {
			this.reset();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void post() {}

}
