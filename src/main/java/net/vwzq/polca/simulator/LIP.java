package net.vwzq.polca;

class LIP extends CachePolicy {

	public LIP (Config config) {
		super (config);
		this.reset();
	}

	// fill cache with current timestamps
	public void reset() {
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], this.clock++);
		}
	}

	protected int hit(String block) {
		// update timestamp of hit
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				this.cache[i].state = this.clock;
				break;
			}
		}
		return -1;
	}

	protected int miss(String block) {
		int victim = 0;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].state < this.cache[victim].state) {
				victim = i;
			}
		}
		this.cache[victim].block = block;
		this.cache[victim].state = 0; // insert with MRU position (timestamp=0)
		return victim;
	}

}
