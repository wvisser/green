package za.ac.sun.cs.green.service.simplify;

import java.util.Arrays;
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

	private int invocations = 0;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = propagateConstants(instance.getFullExpression());
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

	public Expression propagateConstants(Expression expression) {
		try {
			log.log(Level.FINEST, "Before constants are propagated: " + expression);
			invocations++;

			// Init
			HashMap<String, Integer> map = new HashMap<String, Integer>();

			// Search
			ConstantPropPopulate constantPropPopulator = new ConstantPropPopulate(map);
			expression.accept(constantPropPopulator);
			expression = constantPropPopulator.getExpression();

			// Replacer
			ConstantPropReplacer constantPropReplacer = new ConstantPropReplacer(map);
			expression.accept(constantPropReplacer);
			expression = constantPropReplacer.getExpression();

			log.log(Level.FINEST, "After constants are propagated: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class ConstantPropPopulate extends Visitor {

		private Stack<Expression> stack;
		private HashMap<String, Integer> map;

		public ConstantPropPopulate(HashMap<String, Integer> map_in) {
			stack = new Stack<Expression>();
			map = map_in;
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
			boolean detected = false;
			Expression r = stack.pop();
			Expression l = stack.pop();

			if (operation.getOperator() == Operation.Operator.EQ) {
				
				// If we have an equality expression involving a variable and constant,
				// assign variable to constant
				if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
					map.put(l.toString(), Integer.parseInt(r.toString()));
					detected = true;
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
					map.put(r.toString(), Integer.parseInt(l.toString()));
					detected = true;
				}

			}

			stack.push(operation);
		}
	}

	private static class ConstantPropReplacer extends Visitor {

		private Stack<Expression> stack;
		private HashMap<String, Integer> map;

		public ConstantPropReplacer(HashMap<String, Integer> map_in) {
			stack = new Stack<Expression>();
			map = map_in;
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
			Expression r = stack.pop();
			Expression l = stack.pop();

			if ((l instanceof IntVariable) && (r instanceof IntConstant) || (r instanceof IntVariable) && (l instanceof IntConstant)) {
				stack.push(new Operation(operation.getOperator(), l, r));
			} else {
				// Check if variable in hashmap, and replace.
				if ((r instanceof IntVariable && map.get(r.toString()) != null)) {
					r = new IntConstant(map.get(r.toString()));
				}

				// Check if variable in hashmap, and replace.
				if ((l instanceof IntVariable && map.get(l.toString()) != null)) {
					l = new IntConstant(map.get(l.toString()));
				}

				stack.push(new Operation(operation.getOperator(), l, r));
			}
		}
	}
}
