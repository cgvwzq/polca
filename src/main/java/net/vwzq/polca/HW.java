package net.vwzq.polca;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class HW extends CachePolicy {

	static class Proxy {

		static BufferedReader stdout;
		static PrintWriter stdin;
		static String binary;
		static boolean ready = false;
		static Process proc;

		public static void start(Config config) {
			try {
				binary = config.proxy_path;
				proc = Runtime.getRuntime().exec(binary);
				stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				stdin = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())));
				// Read lines til welcome message
				String pattern = "CacheQuery interactive shell\\..*";
				Pattern r = Pattern.compile(pattern);
				Matcher m;
				do {
					String line = stdout.readLine();
					m = r.matcher(line);
				} while (!m.matches());
				stdout.readLine(); // read empty line
				// TODO: Check for error
				ready = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static int write(String line) throws IOException {
			if (!ready) throw new IOException("can't write on stopped proxy");
			stdin.write(line + "\n");
			stdin.flush();
			return 0;
		}

		public static String read() throws IOException {
			if (!ready) throw new IOException("cant't read on stopped proxy");
			String ret = stdout.readLine();
			if (ret == null) {
				throw new IOException("end of stream");
			}
			return ret;
		}

		public static boolean isReady() {
			return ready;
		}

		public static void terminate() {
			if (ready) {
				stdin.write("q\n");
				// Give time to turn on regular settings
				try {
					Thread.currentThread().sleep(1000);
				} catch (Exception e) {}
				proc.destroy();
				ready =  false;
			}
		}

		public static void reset(Config config) {
			// In some systems running for a long time cause saturation.
			// We reset when detecting errors, and sleep the process for a while.
			terminate ();
			System.out.println("reset...");
			try {
				Thread.currentThread().sleep(5000);
			} catch (Exception e) {}
			System.out.println("retry!");
			start(config);
		}

	}

	private Config config;

	private Proxy proxy;

	public HW (Config config) {
		super (config); // no need
		this.config = config;
		if (!this.proxy.isReady()) {
			this.proxy.start(this.config);
			// if error?
		}
	}

	/// Do majority vote to locateMiss
	public String locateMiss(ArrayList<String> word, ArrayList<String> candidates) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (int i=0; i<this.config.votes; i++) {
			String ret = locateMiss(word, candidates, this.config.votes>1);
			if (map.get(ret) != null) {
				map.put(ret, map.get(ret) + 1);
			} else {
				map.put(ret, 1);
			}
		}
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			String key = entry.getKey();
			int val = entry.getValue();
			if (val > this.config.votes/2) {
				return key;
			}
		}
		return "";
	}

	// Find element replaced upon miss, and returns its index (i.e. abstractOutput)
	public String locateMiss(ArrayList<String> word, ArrayList<String> candidates, boolean bypass) {
		String pattern = "^(?:\\((?:L1|L2|L3):\\d+\\) )?(.*) -> (\\d+)$";
		Pattern r = Pattern.compile(pattern);
		String result = "";
		int ret = -1, len = candidates.size(), rep = 0;
		while (rep < 100) {
			boolean foundMiss = false;
			try {
				proxy.write(((rep > 0 || bypass) ? "rr " : "r ") + this.config.prefix + " " + String.join(" ", word) + " [" + String.join(" ", candidates) + "]?");
				ArrayList<String> lines = new ArrayList<String>();
				for (int i = 0; i < len; i++) {
					lines.add(this.proxy.read());
				}
				int i = 0;
				for (String line : lines) {
					Matcher m = r.matcher(line);
					m.find();
					try {
						ret = Integer.parseInt(m.group(2), 10);
					} catch (Exception e) {
						// probably a cachequery syntax error or broken connection
						e.printStackTrace();
						return "";
					}
					// check if it's miss
					if (ret <= (this.config.repetitions * this.config.miss_ratio)) {
						if (!foundMiss) {
							result = candidates.get(i);
							foundMiss = true;
						} else {
							// if more than one miss, try again
							result = "";
							break;
						}
					} else if (ret >= (this.config.repetitions * this.config.hit_ratio)) {
						// hits are fine
					} else {
						result = ""; // noisy measurements, better repeat
						break;
					}
					i++;
				}
				// return if we are confident that we located the miss
				if (result != "") {
					return result;
				}
				// try again bypassing the cache
				this.proxy.reset(this.config);
				rep++;
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}
		return "";
	}

	public void close() {
		this.proxy.terminate();
	}

	public int hit(String block) { return -1; }
	public int miss(String block) { return 1; }
	public void reset() {}

}
