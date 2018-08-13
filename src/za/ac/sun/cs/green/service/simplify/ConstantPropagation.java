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
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
			Expression propagated = propagationVisitor.getExpression();
			log.log(Level.FINEST, "After Propagation: " + propagated);
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	/*
	 * The PropagationVisitor steps through the tree looking for candidate
	 * variables for constant propagation, and attempts to take care of some
	 * other low-hanging fruit for expressions which would never be satisfied.
	 */
	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;
		private boolean never_satisfied;

		// name and value of the variable to propogate
		private String propName;
		private int propValue;

		public PropagationVisitor() {
			stack = new Stack<Expression>();
			never_satisfied = false;
		}

		public Expression getExpression() {
			// check if we had constant equalities which would never be
			// satisfied
			if (never_satisfied) {
				stack.clear();
				IntConstant new_l = new IntConstant(0);
				IntConstant new_r = new IntConstant(1);
				stack.push(new Operation(Operation.Operator.EQ, new_l, new_r));
			}

			return stack.pop();
		}

		@Override
		public void postVisit(Constant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Variable variable) {
			// check if variable has been marked for propogation
			if ((propName != null) && (propName.equals(variable.toString()))) {
				// replace variable with the replacement value
				IntConstant replacement = new IntConstant(propValue);
				stack.push(replacement);
			} else {
				stack.push(variable);
			}
		}

		@Override
		public void postVisit(Operation operation) {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();

			if (op == Operation.Operator.EQ) {
				// if we have found an equality statement, check if we have a
				// candidate for propagation
				if ((l instanceof IntVariable) && (r instanceof IntConstant)) {
					propName = new String(((IntVariable) l).getName());
					propValue = ((IntConstant) r).getValue();
				}
				stack.push(new Operation(op, l, r));
			} else if (op == Operation.Operator.GT) {
				// check if we have constant expressions which are never
				// satisfied, or conversely always satisfied
				if ((l instanceof IntConstant) && (r instanceof IntConstant)) {
					int l_value = ((IntConstant) l).getValue();
					int r_value = ((IntConstant) r).getValue();

					if (l_value <= r_value) {
						// never satisfied
						never_satisfied = true;
						IntConstant new_l = new IntConstant(0);
						IntConstant new_r = new IntConstant(1);
						stack.push(new Operation(Operation.Operator.EQ, new_l, new_r));
					} else {
						// always satisfied
						IntConstant new_l = new IntConstant(0);
						IntConstant new_r = new IntConstant(0);
						stack.push(new Operation(Operation.Operator.EQ, new_l, new_r));
					}
				} else {
					stack.push(new Operation(op, l, r));
				}
			} else if (op == Operation.Operator.LT) {
				// check if we have constant expressions which are never
				// satisfied, or conversely always satisfied
				if ((l instanceof IntConstant) && (r instanceof IntConstant)) {
					int l_value = ((IntConstant) l).getValue();
					int r_value = ((IntConstant) r).getValue();

					if (l_value >= r_value) {
						// never satisfied
						never_satisfied = true;
						IntConstant new_l = new IntConstant(0);
						IntConstant new_r = new IntConstant(1);
						stack.push(new Operation(Operation.Operator.EQ, new_l, new_r));
					} else {
						// always satisfied
						IntConstant new_l = new IntConstant(0);
						IntConstant new_r = new IntConstant(0);
						stack.push(new Operation(Operation.Operator.EQ, new_l, new_r));
					}
				} else {
					stack.push(new Operation(op, l, r));
				}
			} else {
				stack.push(new Operation(op, l, r));
			}
		}
	}
}
