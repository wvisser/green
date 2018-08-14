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

public class ConstantPropogation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = simplify(instance.getFullExpression(), map);
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

	public Expression simplify(Expression expression,Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Simplify: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			log.log(Level.FINEST, "After Simplify: " + expression);
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return expression;
	}

	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> map;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			map = new TreeMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();

			if (op.equals(Operation.Operator.EQ)) {
				Expression oL = operation.getOperand(0);
				Expression oR = operation.getOperand(1);
				if ((oL instanceof IntConstant) && (oR instanceof IntVariable)) {
					map.put((IntVariable) oR, (IntConstant) oL);
				} else if ((oL instanceof IntVariable) && (oR instanceof IntConstant)) {
					map.put((IntVariable) oL, (IntConstant) oR);
				}
			}

			op = operation.getOperator();

			if (stack.size() >= 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				if (op.equals(Operation.Operator.ADD)) {
					if (l instanceof IntVariable) {
						if (map.containsKey(l)) {
							l = map.get(l);
						}
					}
					if (r instanceof IntVariable) {
						if (map.containsKey(r)) {
							r = map.get(r);
						}
					}
				}
				Operation e = new Operation(operation.getOperator(), l, r);
				stack.push(e);
			}
		}
	}
}
