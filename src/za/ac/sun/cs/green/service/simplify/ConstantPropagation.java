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

public class ConstantPropagation extends BasicService{
	
	public ConstantPropagation(Green solver) {
		super(solver);
	}

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;
	
	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagateConstants(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression propagateConstants(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			//OrderingVisitor orderingVisitor = new OrderingVisitor();
			PropagationVisitor propVisitor = new PropagationVisitor();
			expression.accept(orderingVisitor);
			//expression = orderingVisitor.getExpression();
			//CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			//expression.accept(canonizationVisitor);
			Expression propagated = propVisitor.getExpression();
			log.log(Level.FINEST, "After constant propagation: " + propagated);
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class PropagationVisitor extends Visitor {

		private Map<Variable, Variable> map;

		private Stack<Expression> stack;

		public PropagationVisitor(Map<Variable, Variable> map, SortedSet<IntVariable> variableSet) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			Variable v = map.get(variable);
			if (v == null) {
				v = new IntVariable("v" + map.size(), variable.getLowerBound(),
						variable.getUpperBound());
				map.put(variable, v);
			}
			stack.push(v);
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			stack.push(new Operation(operation.getOperator(), operands));
		}
		
		@Override
		public void preVisit(Operation operation) {
			// TODO
		}

	}

}
