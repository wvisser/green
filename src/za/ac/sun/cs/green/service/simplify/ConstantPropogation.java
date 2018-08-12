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
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = canonize(instance.getFullExpression(), map);
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

	public Expression canonize(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			ConstantPropVisitor constantPropVisitor = new ConstantPropVisitor();
			expression.accept(constantPropVisitor);
			expression = constantPropVisitor.getExpression();
			log.log(Level.FINEST, "After Canonization: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class ConstantPropVisitor extends Visitor {

		private Stack<Expression> stack;
		private HashMap<String, Integer> map;

		public ConstantPropVisitor() {
			stack = new Stack<Expression>();
			map = new HashMap<String, Integer>();
		}

		public Expression getExpression() {
			// Print HashMap
			System.out.println(Arrays.asList(map));
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

			if (operation.getOperator() == Operation.Operator.EQ
				&& ((l instanceof IntVariable) && (r instanceof IntConstant)
				|| (r instanceof IntVariable) && (l instanceof IntConstant))) {

				// If we have an equality expression involving a variable and constant,
				// assign variable to constant
				if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
					map.put(l.toString(), Integer.parseInt(r.toString()));
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
					map.put(r.toString(), Integer.parseInt(l.toString()));
				}

				// Push operation back to stack
				stack.push(operation);
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
