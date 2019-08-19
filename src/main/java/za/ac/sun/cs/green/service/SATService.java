package za.ac.sun.cs.green.service;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.util.Reporter;

import java.util.Set;

public abstract class SATService extends BasicService {

	private static final String SERVICE_KEY = "SAT:";

	private int invocationCount = 0;

	protected int cacheHitCount = 0;
	protected int satHitCount = 0;
	protected int unsatHitCount = 0;

	protected int cacheMissCount = 0;
	protected int satMissCount = 0;
	protected int unsatMissCount = 0;

	private long timeConsumption = 0;
	protected long storageTimeConsumption = 0;

	protected int satCount = 0;
	protected int unsatCount = 0;

	public SATService(Green solver) {
		super(solver);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocationCount = " + invocationCount);
		reporter.report(getClass().getSimpleName(), "cacheHitCount = " + cacheHitCount);
		reporter.report(getClass().getSimpleName(), "satCacheHitCount = " + satHitCount);
		reporter.report(getClass().getSimpleName(), "unsatCacheHitCount = " + unsatHitCount);
		reporter.report(getClass().getSimpleName(), "cacheMissCount = " + cacheMissCount);
		reporter.report(getClass().getSimpleName(), "satCacheMissCount = " + satMissCount);
		reporter.report(getClass().getSimpleName(), "unsatCacheMissCount = " + unsatMissCount);
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
		reporter.report(getClass().getSimpleName(), "storageTimeConsumption = " + storageTimeConsumption);
		reporter.report(getClass().getSimpleName(), "satQueries = " + satCount);
		reporter.report(getClass().getSimpleName(), "unssatQueries = " + unsatCount);
	}

	@Override
	public Object allChildrenDone(Instance instance, Object result) {
		return instance.getData(getClass());
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		Boolean result = (Boolean) instance.getData(getClass());
		if (result == null) {
			result = solve0(instance);
			if (result != null) {
				instance.setData(getClass(), result);
			}
		}
//        assert result != null;
		if (result)
			satCount++;
		else
			unsatCount++;
		return null;
	}

	private Boolean solve0(Instance instance) {
		invocationCount++;
		String key = SERVICE_KEY + instance.getFullExpression().getCachedString();
		long tmpConsumption = 0L;
		long start = System.currentTimeMillis();
		Boolean result = store.getBoolean(key);

		if (result == null) {
			cacheMissCount++;
			long startTime = System.currentTimeMillis();
			result = solve(instance);
			timeConsumption += (System.currentTimeMillis() - startTime);
			tmpConsumption = System.currentTimeMillis() - startTime;
			if (result != null) {
				if (result) {
					satMissCount++;
				} else {
					unsatMissCount++;
				}
				store.put(key, result);
			}
		} else {
			cacheHitCount++;
			if (result) {
				satHitCount++;
			} else {
				unsatHitCount++;
			}
		}
		storageTimeConsumption += ((System.currentTimeMillis() - start) - tmpConsumption);
		return result;
	}

	private Boolean solve1(Instance instance) {
		invocationCount++;
		cacheMissCount++;
		long startTime = System.currentTimeMillis();
		Boolean result = solve0(instance);
		timeConsumption += (System.currentTimeMillis() - startTime);
		return result;
	}

	protected abstract Boolean solve(Instance instance);

}
