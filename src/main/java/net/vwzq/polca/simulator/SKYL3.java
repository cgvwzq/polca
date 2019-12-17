package net.vwzq.polca;

class SKYL3 extends CachePolicy {

	public final int INSERT_VAL = 1;
	public final int REPL_VAL = 3;

	public SKYL3 (Config config) {
		super (config);
		this.reset();
	}

	public void reset() {
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], REPL_VAL);
		}
	}

	protected int hit(String block) {
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				if (this.cache[i].state > 1) {
					this.cache[i].state = 1;
				} else {
					this.cache[i].state = 0;
				}
				break;
			}
		}
		this.update();
		return -1;
	}

	protected void update() {
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
				this.cache[i].state++;
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
		this.update();
		return victim;
	}

}
