package za.ac.sun.cs.green.service.factorizer;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Service;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ModelFactorizerService extends BasicService {

	private static final String FACTORS = "FACTORS";
	private static final String MODELS = "MODELS";
	private static final String FACTORS_UNSOLVED = "FACTORS_UNSOLVED";

	private FactorExpression factorizer;
	private int invocationCount = 0; // number of times factorizer has been invoked (constraints processed)
	private int factorCount = 0; // number of factors
	private long timeConsumption = 0;

	public ModelFactorizerService(Green solver) {
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
//			log.debug("Factorizer computes instance for :" + factor);
			result.add(new Instance(getSolver(), instance.getSource(), null, factor));
		}
		result = Collections.unmodifiableSet(result);
		instance.setData(FACTORS_UNSOLVED, new HashSet<>(result));
//		return result;
		instance.setData(FACTORS, result);
		instance.setData(MODELS, new HashMap<Variable, Object>());

		log.debug("Factorizer exiting with " + result.size() + " results");

		factorCount += factors.size();
		return result;
	}

	private boolean isUnsat(Object result) {
		if (result == null) {
			return true;
		} else if (result instanceof HashMap) {
//			HashMap<Variable, Object> isSat = (HashMap<Variable, Object>) result;
			return false;
		} else if (result instanceof Boolean) {
			Boolean isSat = (Boolean) result;
			return !isSat;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object childDone(Instance instance, Service subservice, Instance subinstance, Object result) {
		Set<Instance> unsolved = (HashSet<Instance>) instance.getData(FACTORS_UNSOLVED);
		if (isUnsat(result)) {
			return null;
		}

//		if (unsolved.contains(subinstance)) {
			// new child finished
//			HashMap<Variable, Object> parent_solutions = (HashMap<Variable, Object>) instance.getData(MODELS);
//			parent_solutions.putAll((HashMap<Variable, Object>) result);
//			instance.setData(MODELS, parent_solutions);
			// Remove the subinstance now that it is solved
//		unsolved.remove(subinstance);
//		instance.setData(FACTORS_UNSOLVED, unsolved);
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

	protected Object handleNewChild(Instance instance, Service subservice, Instance subinstance, Object result) {
		if (result instanceof HashMap) {
			HashMap<Variable, Object> model = (HashMap<Variable, Object>) result;
			@SuppressWarnings("unchecked")
			HashMap<Variable, Object> wholeModel = (HashMap<Variable, Object>) instance.getData(MODELS);
			wholeModel.putAll(model);
		}
		return result;
	}

	public Object allChildrenDone(Instance instance, Object result) {
		if (result instanceof HashMap) {
			HashMap<Variable, Object>  model = (HashMap<Variable, Object> ) result;
			if (!model.isEmpty()) {
				@SuppressWarnings("unchecked")
				HashMap<Variable, Object> wholeModel = (HashMap<Variable, Object>) instance.getData(MODELS);
				return new HashMap<>(wholeModel);
			}
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocationCount);
		reporter.report(getClass().getSimpleName(), "factoredConstraints = " + factorCount);
		reporter.report(getClass().getSimpleName(), "conjunctCount = " + factorizer.getConjunctCount());
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
	}

}
