package net.vwzq.polca;

class FIFO extends CachePolicy {

	public FIFO (Config config) {
		super (config);
		this.reset();
	}

	public void reset() {
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], this.clock++);
		}
	}

	protected int hit(String block) {
		return -1;
	}

	protected int miss(String block) {
		// replace least recent block
		int victim = 0;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].state < this.cache[victim].state) {
				victim = i;
			}
		}
		this.cache[victim].block = block;
		this.cache[victim].state = this.clock; // insert new with current timestamp
		return victim;
	}

}

