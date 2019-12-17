package net.vwzq.polca;

class MRU extends CachePolicy {

	public final int INSERT_VAL = 1;
	public final int REPL_VAL = 0;

	public MRU (Config config) {
		super (config);
		this.reset();
	}

	public void reset() {
		// all values to 0 except for last one
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], REPL_VAL);
		}
		// set mru
		this.cache[this.WAYS-1].state = INSERT_VAL;
	}

	protected int hit(String block) {
		int ret = -1;
		for (int i=0; i <this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				ret = i;
				this.cache[i].state = INSERT_VAL;
			}
		}
		this.down(ret); // check for reset
		return -1;
	}

	protected void down(int mru) {
		boolean reset = true;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].state != INSERT_VAL) {
				reset = false;
				break;
			}
		}
		if (reset) {
			// if all equal 1, reset to 0
			for (int i=0; i < this.WAYS; i++) {
				this.cache[i].state = REPL_VAL;
			}
			// except for mru
			this.cache[mru].state = INSERT_VAL;
		}
	}

	protected int miss(String block) {
		int ret = -1;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].state == REPL_VAL) { // replacement
				this.cache[i].block = block; // insertion
				this.cache[i].state = INSERT_VAL;
				ret = i;
				break;
			}
		}
		this.down(ret); // check for reset
		return ret;
	}

}
