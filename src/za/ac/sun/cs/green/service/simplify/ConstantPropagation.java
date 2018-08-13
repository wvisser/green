package za.ac.sun.cs.green.service.canonizer;

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

public class ConstantPropagation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;

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

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			invocations++;
			PVisitor pVisitor = new PVisitor();
			expression.accept(pVisitor);
			expression = simplify.getExpression();
			log.log(Level.FINEST, "After Simplification: " + expression);
			return simplify;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class PVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> variables;

		public PVisitor() {
			stack = new Stack<Expression>();
			hash = new HashMap<>();
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
		public void preVisit(Operation operation) throws VisitorException {
			Operation.Operator operator = operation.getOperator();
				if (operator.equals(Operation.Operator.EQ)) {
						Expression left = operation.getOperand(0);
						Expression Right = operation.getOperand(1);
						if ((left instanceof IntConstant) && (right instanceof IntVariable)) {
							hash.put((IntVariable) right, (IntConstant) left);
						} else if ((left instanceof IntVariable) && (right instanceof IntConstant){
							hash.put((IntVariable left, (IntConstant) right);
						}
				}
		}
		
		@Override
		public void postVisit(Operation operation) {
			Operation.Operator operator = operation.getOperator();

			if (stack.size() >= 2) {
				Expression right = stack.pop();
				Expression left = stack.pop();
				if (!operator.equals(Operation.Operator.EQ)) {
					if ((left instanceof IntVariable) && (hash.containsKey(left)) {
						left = map.get(left);						
					}
					if ((right instanceof IntVariable) && (hash.containsKey(right)){
						right = map.get(right);
					}
				}
				Operation operation = new Operation(operation.getOperator(), left, right);
				stack.push(operation);
			}
		}
	}
}
