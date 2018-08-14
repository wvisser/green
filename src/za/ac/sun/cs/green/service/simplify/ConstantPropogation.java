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
	 * Number of times the propagator has been invoked.
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
			final Expression e = propogate(instance.getFullExpression(), map);
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

	/**
	* This method takes in an expression, propogates
	* and  logs the results before and after propogation.
	*/
	public Expression propogate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propogation: " + expression);
			invocations++;

			PropogateVisitor propVisitor = new PropogateVisitor();
			expression.accept(propVisitor);
			expression = propVisitor.getExpression();
			
			log.log(Level.FINEST, "After Propogation: " + expression);
			return expression;

		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class PropogateVisitor extends Visitor {
		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> map;

		public PropogateVisitor() {
			stack = new Stack<Expression>();
			map = new HashMap<IntVariable, IntConstant>();
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
			Operation.Operator nop = null;
						
			//Checks whether there is an operator
			if (op != null) {

				//Gets the expression on the left and right side of the operator
				Expression r = stack.pop();
				Expression l = stack.pop();

				//Checks whether a variable has been assigned a value, if so it is added to the HashMap
				if ((r instanceof IntVariable) && (l instanceof IntConstant) && op == Operation.Operator.EQ) {
					map.put((IntVariable)r, (IntConstant)l);
					stack.push(new Operation(op, l, r));
				} 
				else if ((l instanceof IntVariable) && (r instanceof IntConstant) && op == Operation.Operator.EQ) {
					map.put((IntVariable)l, (IntConstant)r);
					stack.push(new Operation(op, l, r));
				} 
				//If the operand is anything but an equals, try to substitute constants in
				else {
					if(map.containsKey(r)) r = map.get(r);
					if(map.containsKey(l)) l = map.get(l);	
					stack.push(new Operation(op, l, r));
				}
			//If there is no operand
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();

				if(map.containsKey(r)) r = map.get(r);
				if(map.containsKey(l)) l = map.get(l);	
				stack.push(new Operation(op, l, r));
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}

	}

}
