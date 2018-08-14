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
import java.util.Iterator;
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

	public Expression simplify(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
			expression = propagationVisitor.getExpression();
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	/*
	 * Propagation of one constant
	 */
	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Expression> variableMap = new HashMap<Expression, Expression>();

		public PropagationVisitor() {
			stack = new Stack<Expression>();
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
			Expression key = variable;
			//Checks if the variable has been saved in the map before
			if (variableMap.containsKey(key)){
				//If yes, push the variable's value instead of the variable again
				stack.push(variableMap.get(key));
			} else {
					stack.push(variable);
			}
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();
			//If the operation is assigning a constant to a variable, add the expressions to the hash map
			if (op == Operation.Operator.EQ && l instanceof IntVariable && r instanceof IntConstant) {
				variableMap.put(l, r);
				stack.push(new Operation(op, l , r));
			} else {
					stack.push(new Operation(op, l, r));
			}
		}
	}
}
