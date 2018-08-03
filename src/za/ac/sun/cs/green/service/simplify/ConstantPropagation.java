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

	public Expression propagate(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Constant Propagation: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			log.log(Level.FINEST, "After Constant Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {
        private Stack<Expression> stack;
        private HashMap<IntVariable, IntConstant> variables;

		public OrderingVisitor() {
            stack = new Stack<Expression>();
            variables = new HashMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
            Expression finalExp = stack.pop();
            System.out.println("Final expression is" + finalExp);
			return finalExp;
		}

		@Override
		public void postVisit(IntConstant constant) {
            System.out.println("Pushing constant");
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
            if(variables.containsKey(variable)) {
                System.out.println("Propagating a constant: " + variable + " = " + variables.get(variable));
                stack.push(new IntConstant(3));
            } else {
                System.out.println("Tried propagating constant but didn't find it in map. Pushing variable name");
                stack.push(variable);
            }
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
            Operation.Operator op = operation.getOperator();
            
			if (op == Operation.Operator.EQ) {
				Expression r = stack.pop();
                Expression l = stack.pop();
                
				if (r instanceof IntConstant && l instanceof IntVariable) {
                    System.out.println("Found a constant assignment. Assigning " + l + " with value " + r);
                    variables.put((IntVariable) l, (IntConstant) r);
                }
                System.out.println("Pushing something == something");
                stack.push(new Operation(op, l, r));
			} else {
                System.out.println("Pushing operator sign");
				stack.push(operation);
			}
		}

	}

}
