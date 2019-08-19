package za.ac.sun.cs.green.store.memstore;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.service.grulia.repository.Entry;
import za.ac.sun.cs.green.store.BasicStore;
import za.ac.sun.cs.green.store.redis.RedisStore;
import za.ac.sun.cs.green.util.Reporter;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of a {@link za.ac.sun.cs.green.store.Store} based on redis (<code>http://www.redis.io</code>).
 *
 * @author Jaco Geldenhuys <jaco@cs.sun.ac.za>
 */
public class MemStore extends BasicStore {

	/**
	 * Number of times <code>get(...)</code> was called.
	 */
	private int retrievalCount = 0;

	/**
	 * Number of times <code>put(...)</code> was called.
	 */
	private int insertionCount = 0;

	/**
	 * The Memory Store
	 */
	private Map<String, Object> db;
	private RedisStore redisStore;

	private long timePut = 0;
	private long timeGet = 0;
	private long timeConsumption = 0;
	private long getKeySetTime = 0;

	/**
	 * Constructor to create memory store
	 */
	public MemStore(Green solver, Properties properties) {
		super(solver);
		db = new HashMap<String, Object>();
		redisStore = new RedisStore(solver, "localhost", 6379);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "retrievalCount = " + retrievalCount);
		reporter.report(getClass().getSimpleName(), "insertionCount = " + insertionCount);
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
		reporter.report(getClass().getSimpleName(), "getTime = " + timeGet);
		reporter.report(getClass().getSimpleName(), "putTime = " + timePut);
	}

	@Override
	public synchronized Object get(String key) {
		long start = System.currentTimeMillis();
		retrievalCount++;
		Object s = db.get(key);
		if (s == null) {
			// Greedy approach:
			// if the solution is not in the memstore,
			// look for it in redis (persistent store)
			// and add to memstore
			if (redisStore.isSet()) {
				s = redisStore.get(key);
				if (s != null) {
					db.put(key, s);
				}
			}
		}
		timeGet += (System.currentTimeMillis() - start);
		timeConsumption += (System.currentTimeMillis() - start);
		return s;
	}

	@Override
	public synchronized void put(String key, Serializable value) {
		long start = System.currentTimeMillis();
		insertionCount++;
		// Unnecessary to convert to Base64 string
		db.put(key, value);
		timePut += (System.currentTimeMillis() - start);
		timeConsumption += (System.currentTimeMillis() - start);
	}

	@Override
	public Set<String> keySet(String pattern) {
		long startTime = System.currentTimeMillis();
		Set<String> keys;
		if (redisStore.isSet()) {
			// look in redis for keys
			if (pattern == null || pattern.equals("")) {
				// get all keys
				keys = redisStore.keySet("*");
			} else {
				// get keys based on pattern
				keys = redisStore.keySet(pattern);
			}
		} else {
			// look in local database
			if (pattern == null || pattern.equals("")) {
				keys = db.keySet();
			} else {
				if (pattern.endsWith("*")) {
//					pattern = pattern.replaceAll("^.*\\*", "");
					pattern = pattern.replace("*", "");
				}
				final String finalPattern = pattern;
				// TODO: filter is not robust, can fail if not given exact search
				// eg. fail on pattern being ab*.extension
				keys = db.keySet().stream()
						.filter(e -> e.startsWith(finalPattern))
						.collect(Collectors.toSet());
			}
		}
		getKeySetTime += (System.currentTimeMillis() - startTime);
		timeConsumption += (System.currentTimeMillis() - startTime);
		return Collections.unmodifiableSet(keys);
	}

	@Override
	public void flushAll() {
		long start = System.currentTimeMillis();
		flushAllToRedis();
		timeConsumption += (System.currentTimeMillis() - start);
	}

	@Override
	public void clear() {
		long start = System.currentTimeMillis();
		db.clear();
//		if (redisStore.isSet()) {
//			redisStore.clear();
//		}
		timeConsumption += (System.currentTimeMillis() - start);
	}

	@Override
	public boolean isSet() {
		try {
			db.get("foo");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void flushAllToRedis() {
		if (redisStore.isSet()) {
			db.forEach((k, v) -> redisStore.put(k, (Serializable) v));
		}
	}

}
