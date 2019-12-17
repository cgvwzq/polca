package net.vwzq.polca;

import java.util.ArrayList;

interface Cache {
	public String locateMiss(ArrayList<String> word, ArrayList<String> candidates);
	public void reset();
}

abstract class CachePolicy implements Cache {

	protected int WAYS;
	protected Line[] cache;
	protected String[] alpha;
	protected int clock;

	class Line {
		public String block;
		public int state;
		public Line(String block, int state) {
			this.block = block;
			this.state = state;
		}
	}

	abstract public void reset();
	abstract protected int hit(String block);
	abstract protected int miss(String block);

	public CachePolicy(Config config) {
		this.WAYS = config.ways;
		this.cache = new Line[config.ways];
		this.alpha = "abcdefghijklmnopqrstuvwxyz".substring(0, config.ways).split("");
		this.clock = 1;
	}

	public int access(String block) {
		int ret;
		this.clock++; // increase tick
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				ret = this.hit(block);
				return -1;
			}
		}
		ret = this.miss(block); // need to query all blocks to find replaced block
		return 1;
	}

	// Access complete word ended by a miss
	public int access(ArrayList<String> word) {
		int ret = 1;
		for (String block : word) {
			ret = this.access(block);
		}
		return ret; // return last, is a MISS/1
	}

	// Access word and check which element from candidates is not in cache
	public String locateMiss(ArrayList<String> word, ArrayList<String> candidates) {
		for (String c : candidates) {
			this.reset();
			this.access(word);
			// if miss, we found evicted block
			if (this.access(c) == 1) {
				return c;
			}
		}
		return "";
	}

}
