package net.vwzq.polca;

class PLIP extends CachePolicy {

	// tree-based PLIP
	enum Arrow { LEFT, RIGHT};
	public final Arrow arrows[];

	private boolean is_power_of_two(int x)
	{
		return x != 0 && ((x & (x - 1)) == 0);
	}

	public PLIP(Config config) throws Exception {
		super (config);
        if (is_power_of_two(config.ways)) {
			int p = (1 << (int) (Math.log(config.ways)/Math.log(2))) - 1;
			this.arrows = new Arrow[p];
			this.reset();
		} else {
			throw new Exception("assoc should be power of 2");
		}
	}

	// points all arrows in tree towards left
	public void reset() {
		for (int i =0; i < this.arrows.length; i++) {
			this.arrows[i] = Arrow.LEFT;
		}
		for (int i=0; i < this.WAYS; i++) {
			this.cache[i] = new Line(this.alpha[i], 0);
		}
	}

	protected int hit(String block) {
		int update = -1;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				update = i;
			}
		}
		int lvl = this.WAYS >> 1, i = 0, acc = 0;
		while (lvl > 0) {
			if (update < acc + lvl) {
				this.arrows[i] = Arrow.RIGHT; // point arrow to opposite branch
				i += 1; // update arrow index
			} else {
				this.arrows[i] = Arrow.LEFT; // point arrow to opposite branch
				i += lvl; // update arrow index
				acc += lvl;
			}
			lvl >>= 1; // next lvl
		}
		return -1;
	}

	// insert in LRU w/o updating tree
	protected int miss(String block) {
		int lvl = this.WAYS >> 1, i = 0, j = 0, rep = 0;
		while (lvl > 0) {
			j = i;
			if (this.arrows[i] == Arrow.LEFT) { // go to left
				i += 1;
			} else { // go to right
				i += lvl;
				rep += lvl;
			}
			lvl >>= 1;
		}
		// insert new block
		this.cache[rep].block = block;
		return j;
	}

}
