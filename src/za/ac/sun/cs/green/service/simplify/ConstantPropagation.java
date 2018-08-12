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
import java.util.logging.Logger;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Operation.Operator;
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

	//public Expression propagateConstants(Expression expression,
	public Expression simplify(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Constant propagation: " + expression);
			invocations++;
			PropagationVisitor propVisitor = new PropagationVisitor();
			expression.accept(propVisitor);
			Expression propagated = propVisitor.getExpression();
			log.log(Level.FINEST, "After Constant propagation: " + propagated);
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class PropagationVisitor extends Visitor {
		
		private Map<Variable, Constant> map;

		private Stack<Expression> stack;

		public PropagationVisitor() {
			map = new HashMap<Variable, Constant>();
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
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
			Operation.Operator op = operation.getOperator();
			Operation.Operator nop = null;
			switch (op) {
			case EQ:
				nop = Operation.Operator.EQ;
				break;
			case NE:
				nop = Operation.Operator.NE;
				break;
			case LT:
				nop = Operation.Operator.GT;
				break;
			case LE:
				nop = Operation.Operator.GE;
				break;
			case GT:
				nop = Operation.Operator.LT;
				break;
			case GE:
				nop = Operation.Operator.LE;
				break;
			default:
				break;
			}
			/*if (false) {
				//System.out.println("POPPING THIS FROM STACK: " + stack.peek());
				Expression r = stack.pop();
				//System.out.println("POPPING THIS FROM STACK: " + stack.peek());
				Expression l = stack.pop();
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, r, l));
				} else {
					stack.push(operation);
				}
			} else */
			if (op.equals(Operation.Operator.EQ)) {
				// Extract constant
				Expression r = stack.pop();
				Expression l = stack.pop();
				
				// save the assignment
				if (l instanceof Variable && r instanceof Constant) {
					map.put((Variable)l, (Constant)r);
					System.out.println("Stuck <" + l + "," + r + "> into map");
				}
				if (r instanceof Variable && l instanceof Constant) {
					map.put((Variable)r, (Constant)l);
					System.out.println("Stuck <" + r + "," + l + "> into map");
				}
				stack.push(new Operation(op, l, r));
				//stack.push(new Operation(Operation.Operator.NE, r, r));
				//stack.push(new Operation(Operation.Operator.NE, l, r)); // wrong push for testing
			} else {
				// TODO sniff out variables and replace with saved constants
				/*
				System.out.println("Non assignment operator: " + op);
				for (int i = op.getArity(); i > 0; i--) {
					System.out.println("I want to die " + stack.peek());
					stack.pop();
				}
				stack.push(operation);*/
				Expression r = stack.pop();
				Expression l = stack.pop();
				if (map.containsKey(r)) {
					r = map.get(r);
				}
				if (map.containsKey(l)) {
					l = map.get(l);
				}
				
				stack.push(new Operation(op, l, r));
			}
		}

	}
}
