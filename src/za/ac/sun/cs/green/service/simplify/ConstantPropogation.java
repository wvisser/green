package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

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

	public Expression canonize(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Renaming: " + expression);
			invocations++;
			Expression canonized = expression;
			if (canonized != null) {
				canonized = new Renamer(map).rename(canonized);
			}
			log.log(Level.FINEST, "After Renaming: " + canonized);
			return canonized;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}
	
	private static class Renamer extends Visitor {

		private Map<Variable, Variable> map;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, Variable> map) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			expression.accept(this);
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
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
			
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class)) {
				Variable v = map.get(operands[0]);
				if (v == null) {
					Variable value = new IntVariable(operands[1].toString(), 0, 99999);
					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
					map.put(var, value);
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class)) {
				Variable v = map.get(operands[1]);
				if (v == null) {
					Variable value = new IntVariable(operands[0].toString(), 0, 99999);
					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
					map.put(var, value);
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
				Variable v = map.get(operands[0]);
				if (v != null) {
					operands[0] = v;
				}
				v = map.get(operands[1]);
				if (v != null) {
					operands[1] = v;
				}
			}
			
			stack.push(new Operation(operation.getOperator(), operands));
		}

	}
	
}
