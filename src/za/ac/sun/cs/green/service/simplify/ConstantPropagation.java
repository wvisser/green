package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation  extends BasicService {

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
		public Set<Instance> processRequest(Instance instance) {
			@SuppressWarnings("unchecked")
			Set<Instance> result = (Set<Instance>) instance.getData(getClass());
			if (result == null) {
				final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
				final Expression e = propagate(instance.getFullExpression(), map);
				final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
				result = Collections.singleton(i);
				instance.setData(getClass(), result);
			}
			return result;
		}

	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplification:\n" + expression);
			
			PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
			Expression propagated = propagationVisitor.getExpression();

			log.log(Level.FINEST, "After Simplification:\n" + propagated);
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;

		private SortedSet<Expression> conjuncts;

		private SortedSet<IntVariable> variableSet;

		private boolean unsatisfiable;

		private boolean linearInteger;

		public PropagationVisitor() {
			stack = new Stack<Expression>();
			conjuncts = new TreeSet<Expression>();
			variableSet = new TreeSet<IntVariable>();
			unsatisfiable = false;
			linearInteger = true;
		}

		public SortedSet<IntVariable> getVariableSet() {
			return variableSet;
		}

		public Expression getExpression() {

			

		}

	}

}
