package net.vwzq.polca;

class SRRIPHP extends CachePolicy {

	public final int INSERT_VAL = 2;
	public final int REPL_VAL = 3;

	public SRRIPHP (Config config) {
		super (config);
		this.reset();
	}

	// We assume that upon initial insertion all values are normalized to age=3
	public void reset() {
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], REPL_VAL);
		}
	}

	protected int hit(String block) {
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				this.cache[i].state = 0;
				break;
			}
		}
		return -1;
	}

	protected int miss(String block) {
		while (true) {
			for (int i=0; i < this.WAYS; i++) {
				if (this.cache[i].state == REPL_VAL) { // replacement
					this.cache[i].block = block; // insertion
					this.cache[i].state = INSERT_VAL; // insertion
					return i;
				}
			}
			for (int i=0; i < this.WAYS; i++) {
				if (this.cache[i].state < REPL_VAL) {
					this.cache[i].state++;
				}
			}
		}
	}

}
