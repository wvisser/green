package za.ac.sun.cs.green.service.factorizer;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Service;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SATFactorizerService extends BasicService {

	private FactorExpression factorizer;
	private static final String FACTORS_UNSOLVED = "FACTORS_UNSOLVED";

	private int invocationCount = 0; // number of times factorizer has been invoked
	private int factorCount = 0; // number of factors
	private long timeConsumption = 0;

	public SATFactorizerService(Green solver) {
		super(solver);
		factorizer = new FactorExpression(log);
	}

	/**
	 * Factorize an instance as requested.
	 *
	 * @param instance the instance to factorize
	 * @return set of factors as instances
	 *
	 * @see za.ac.sun.cs.green.service.BasicService#processRequest(za.ac.sun.cs.green.Instance)
	 */
	@Override
	public Set<Instance> processRequest(Instance instance) {
		long startTime = System.currentTimeMillis();
		invocationCount++;
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			result = processRequest0(instance);
			instance.setData(getClass(), result);
		}
		timeConsumption += (System.currentTimeMillis() - startTime);
		return result;
	}

	/**
	 * Internal routine for computing the factors after the required result was not
	 * found in the instance satellite data.
	 *
	 * @param instance the instance to factorize
	 * @return set of factors as instances
	 */
	protected Set<Instance> processRequest0(Instance instance) {
		Set<Expression> factors = factorizer.factorize(instance.getFullExpression());
		Set<Instance> result = new HashSet<>();
		for (Expression factor : factors) {
			result.add(new Instance(getSolver(), instance.getSource(), null, factor));
		}
		result = Collections.unmodifiableSet(result);
		instance.setData(FACTORS_UNSOLVED, new HashSet<>(result));
		factorCount += factors.size();
		return result;
	}

	private boolean checkFalse(Object result) {
		Boolean issat = (Boolean) result;
		return (issat != null) && !issat;
	}

	/**
	 * Process a single result (out of potentially many). At this level, this method
	 * simply records that the result has been solved. Once all outstanding result
	 * has been solved, the final result is returned. Otherwise, {@code null} is
	 * returned to signal to the task manager that the computation should continue.
	 * It is expected that subclasses of this class many implement additional logic.
	 *
	 * @param instance    input instance
	 * @param subservice  subservice (= child service) that computed a result
	 * @param subinstance subinstance which this service passed to the subservice
	 * @param result      result return by the sub-service
	 * @return a new (intermediary) result
	 *
	 * @see za.ac.sun.cs.green.service.BasicService#childDone(za.ac.sun.cs.green.Instance,
	 *      za.ac.sun.cs.green.Service, za.ac.sun.cs.green.Instance,
	 *      java.lang.Object)
	 */
	@Override
	public Object childDone(Instance instance, Service subservice, Instance subinstance, Object result) {

		if (checkFalse(result)) {
			return false;
		}

		@SuppressWarnings("unchecked")
		Set<Instance> unsolved = (Set<Instance>) instance.getData(FACTORS_UNSOLVED);
		if (unsolved.contains(subinstance)) {
			result = handleNewChild(instance, subservice, subinstance, result);
			unsolved.remove(subinstance);
			instance.setData(FACTORS_UNSOLVED, unsolved);
			// Return true if no more unsolved factors; else return null to carry on the
			// computation
			return (unsolved.isEmpty()) ? result : null;
		} else {
			// We have already solved this subinstance; return null to carry on the
			// computation or result if there is no more work outstanding
			return (unsolved.isEmpty()) ? result : null;
		}
	}

	/**
	 * Perform additional processing for intermediate results. This default simply
	 * returns the result returned from the child service.
	 *
	 * @param instance    input instance
	 * @param subservice  subservice (= child service) that computed a result
	 * @param subinstance subinstance which this service passed to the subservice
	 * @param result      result return by the sub-service
	 * @return a new (intermediary) result
	 */
	protected Object handleNewChild(Instance instance, Service subservice, Instance subinstance, Object result) {
		return result;
	}

	/**
	 * Check there are unsolved factors and a {@code null} result is returned and,
	 * if so, issue a warning. This is perhaps an error, but it may also indicate
	 * that the Green pipeline is not able to handle an instance.
	 *
	 * @param instance input instance
	 * @param result   result computed so far by this service
	 * @return final result
	 *
	 * @see za.ac.sun.cs.green.service.BasicService#allChildrenDone(za.ac.sun.cs.green.Instance,
	 *      java.lang.Object)
	 */
	@Override
	public Object allChildrenDone(Instance instance, Object result) {
		@SuppressWarnings("unchecked")
		Set<Instance> unsolved = (Set<Instance>) instance.getData(FACTORS_UNSOLVED);
		if (!unsolved.isEmpty() && (result == null)) {
			log.warn("unsolved factors but result is null");
		}
		return super.allChildrenDone(instance, result);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocationCount);
		reporter.report(getClass().getSimpleName(), "factoredConstraints = " + factorCount);
		reporter.report(getClass().getSimpleName(), "conjunctCount = " + factorizer.getConjunctCount());
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
	}
}

