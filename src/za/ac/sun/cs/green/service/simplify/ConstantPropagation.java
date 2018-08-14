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
  	
	/**
	* Taken from SATCanonizerService.java
	*/
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
	
  	/**
	* Taken from SATCanonizerService.java
	*/
  	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}
  	
	/**
	* Based off of the same method in SATCanonizerService.java
	* @param expression: expression to propagate
	*        map: hashmap of variables
	* @return updated expression
	*/
  	public Expression propagate(Expression expression,
				   Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}
	
	/**
	* Class to handle propagation of constants
	*/
	private static class OrderingVisitor extends Visitor {
		
		private Map<IntVariable, IntConstant> hashmap;
		private Stack<Expression> stack;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			hashmap = new HashMap<IntVariable, IntConstant>(); //Added a hash map to store constants propagated to variables
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
		
		/**
		* This method propagates a constant through the expression, replacing all instances of a certain variable
		* with a constant value.
		* @param operation: the current operator that must be handled
		*/
		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			if (stack.size() >= 2) {
				Expression right = stack.pop(); 
				Expression left = stack.pop();
				if (op == Operation.Operator.EQ) { //checks for  '=='
					//if there is a variable to the right of the operator and an integer integer to the left
					//then assign the integer value to a key corresponding to the variable
					if (right instanceof IntConstant && left instanceof IntVariable) { 
						hashmap.put((IntVariable) left, (IntConstant) right);
					}
					//form the new operation and push it to the stack
					Operation nop = new Operation(operation.getOperator(), left, right);
					stack.push(nop);
				} else {
					// If the the current expression is not of the type above create a new expression and push
					// it to the stack
					if (hashmap.containsKey(left)) {
						 left = hashmap.get(left);
					} else if (hashmap.containsKey(right)) {
						right = hashmap.get(right);	
					}
					Operation nop = new Operation(operation.getOperator(), left, right);
					stack.push(nop);
				} 
			} 
		}
	}
}
