package za.ac.sun.cs.green.service.grulia.gruliastore;

import za.ac.sun.cs.green.expr.Expression;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * UnsatEntry stored in the GruliaStore.
 */
public class UnsatEntry extends Entry {

	/**
	 * The list of unsat cores.
	 */
	private Set<Expression> solution;

	public UnsatEntry(double satDelta, Set<Expression> solution) {
		super(satDelta, solution.size());
		this.solution = solution;
	}

	public UnsatEntry(double satDelta, Set<Expression> solution, int size) {
		super(satDelta, size);
		this.solution = solution;
	}

	/**
	 * Get the unsat core
	 *
	 * @return expression of unsat-core
	 */
	public Set<Expression> getSolution() {
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
		s.append("core=");
		Set<Expression> core = getSolution();
		if (core == null) {
			s.append("null");
		} else {
			s.append(new TreeSet<>(core));
		}
		return s.toString();
	}

	@Override
	public boolean isValidFor(Entry entry) {
		return true;
	}

	@Override
	public double distanceTo(Entry entry) {
		return Math.abs(this.getSatDelta() - entry.getSatDelta());
	}
}
