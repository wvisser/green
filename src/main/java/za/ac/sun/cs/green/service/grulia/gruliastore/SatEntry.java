package za.ac.sun.cs.green.service.grulia.gruliastore;

import za.ac.sun.cs.green.expr.Variable;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * SatEntry stored in the GruliaStore.
 */
public class SatEntry extends Entry {

	/**
	 * The model stored in this entry.
	 */
	private final Map<Variable, Object> solution;

	/**
	 * Construct a new model entry.
	 *
	 * @param satDelta SatDelta value for the new entry
	 * @param solution model for the new entry
	 */
	public SatEntry(double satDelta, Map<Variable, Object> solution) {
		super(satDelta, solution.size());
		this.solution = solution;
	}

	/**
	 * Construct a new model entry. This version is used mostly for {@code null}
	 * models that are used as anchor points when searching for nearby models.
	 *
	 * @param satDelta SatDelta value for the new entry
	 * @param solution model for the new entry
	 * @param size     size of the model
	 */
	public SatEntry(double satDelta, Map<Variable, Object> solution, int size) {
		super(satDelta, size);
		this.solution = solution;
	}

	/**
	 * Getter method for the mapping of the variables.
	 *
	 * @return a map of the variables.
	 */
	public Map<Variable, Object> getSolution() {
		return solution;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString0()
	 */
	@Override
	public String toString0() {
		StringBuilder s = new StringBuilder();
		s.append("model=");
		Map<Variable, Object> model = getSolution();
		if (model == null) {
			s.append("null");
		} else {
			s.append(new TreeMap<>(model));
		}
		return s.toString();
	}

	@Override
	public boolean isValidFor(Entry entry) {
		return getSize() >= ((SatEntry) entry).getSize();
	}

	@Override
	public double distanceTo(Entry entry) {
		return Math.abs(this.getSatDelta() - entry.getSatDelta());
	}
}
