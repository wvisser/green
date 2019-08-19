package za.ac.sun.cs.green.store;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.util.Reporter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class NullStore extends BasicStore {

	private int getCount = 0;

	private int putCount = 0;

	public NullStore(Green solver) {
		super(solver);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "getCount = " + getCount);
		reporter.report(getClass().getSimpleName(), "putCount = " + putCount);
	}

	@Override
	public Object get(String key) {
		getCount++;
		return null;
	}

	@Override
	public void put(String key, Serializable value) {
		putCount++;
	}

	@Override
	public Set<String> keySet(String pattern) {
		return Collections.emptySet();
	}

	@Override
	public void flushAll() {
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean isSet() {
		return true;
	}
}
