package net.vwzq.polca;

class PLRU extends CachePolicy {

	// tree-based PLRU
	enum Arrow { LEFT, RIGHT};
	public final Arrow arrows[];
	public final int height;

	private boolean is_power_of_two(int x)
	{
		return x != 0 && ((x & (x - 1)) == 0);
	}

	public PLRU (Config config) throws Exception {
		super (config);
        if (is_power_of_two(config.ways)) {
			this.height = (int) (Math.log(config.ways)/Math.log(2));
			int p = (1 << this.height) - 1;
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
		// find idx of hit
		int update = -1;
		for (int i=0; i < this.WAYS; i++) {
			if (this.cache[i].block.equals(block)) {
				update = i;
			}
		}
		int i = 0;
		for (int lvl = this.height; lvl > 0; lvl--) {
			int step = 1 << (lvl - 1);
			if (update < step) { // go left
				this.arrows[i] = Arrow.RIGHT; // update to right
				i += 1; // go left
			} else {
				this.arrows[i] = Arrow.LEFT; // update to left
				i += step; // go right
				update -= step; // normalize
			}
		}
		return -1;
	}

	protected int miss(String block) {
		int i = 0, idx = 0;
		for (int lvl = this.height; lvl > 0; lvl--) {
			int step = 1 << (lvl - 1);
			if (this.arrows[i] == Arrow.LEFT) { // go left
				this.arrows[i] = Arrow.RIGHT; // update to right
				i += 1; // go left
			} else {
				this.arrows[i] = Arrow.LEFT; // update to left
				i += step; // go right
				idx += step; // find idx of replaced block
			}
		}
		// insert new block
		this.cache[idx].block = block;
		return idx;
	}

}
