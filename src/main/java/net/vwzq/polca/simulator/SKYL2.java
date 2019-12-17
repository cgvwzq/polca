package net.vwzq.polca;

class SKYL2 extends CachePolicy {

	public final int INSERT_VAL = 1;
	public final int REPL_VAL = 3;

	public SKYL2 (Config config) {
		super (config);
		this.reset();
	}

	public void reset() {
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], REPL_VAL);
		}
		this.cache[this.WAYS-1].state = 0;
	}

	protected int hit(String block) {
		int h = -1;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				this.cache[i].state = 0;
				h = i;
				break;
			}
		}
		this.update(h);
		return -1;
	}

	protected void update(int last) {
		boolean found = false;
		while (!found) {
			for (int i=0; i < this.WAYS; i++) {
				if (this.cache[i].state == REPL_VAL) {
					found = true;
					break;
				}
			}
			if (found) {
				break;
			}
			// if no element with age 3, increase all
			for (int i=0; i < this.WAYS; i++) {
				if (i != last) {
					this.cache[i].state++;
				}
			}
		}
	}

	protected int miss(String block) {
		int victim = -1;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].state == REPL_VAL) { // replacement
				this.cache[i].block = block; // insertion
				this.cache[i].state = INSERT_VAL; // insertion
				victim = i;
				break;
			}
		}
		this.update(victim);
		return victim;
	}

}
