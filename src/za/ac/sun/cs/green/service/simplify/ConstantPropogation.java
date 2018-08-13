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

import com.sun.javafx.binding.IntegerConstant;

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
import za.ac.sun.cs.green.expr.Operation.Operator;

public class ConstantPropogation extends BasicService {

	/*
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

	public Expression simplify(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			Propagator constPropagator = new Propagator();
			expression.accept(constPropagator);
			expression = constPropagator.getExpression();
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class Propagator extends Visitor {

		private Stack<Expression> stack;

		/*
		* Created new Hashmap to keep track of variable-constant assignment
		*/
		private HashMap<String, Integer> hmap;

		public constPropagator() {
			stack = new Stack<Expression>();
			hmap = new HashMap<String, Integer>();		
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

			if (op != null) {

				Expression r = stack.pop();
				Expression l = stack.pop();
				Operation e; 

				/*
				* Checks for '==' operator, thus checking for variable to constant assignment
				*/
				if (operation.getOperator() == Operation.Operator.EQ) {
	
					/*
					* This provides a check for whether the variable is stated first and then the constant, vice versa
					*/
					if (((l instanceof IntVariable) && (r instanceof IntConstant)) || ((r instanceof IntVariable) && (l instanceof IntConstant))) {

						if ((l instanceof IntVariable) && (r instanceof IntConstant)) {

							hmap.put(l.toString(), (Integer.parseInt(r.toString())));

						} else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {

							hmap.put(r.toString(), (Integer.parseInt(l.toString())));
						}
					} 
					/*
					* Once varibles and their assignments have been added to HashMap, create the operation
					*/
					e = new Operation(op, l, r);
					
				} else {

					if (r instanceof IntVariable) {
						/*
						* Scans through HashMap to check whether variable already exists within the hash map
						* If is does exist, replace the variable with the value associated with it
						* Eventually pushing this change to the stack
						*/
						if (hmap.containsKey(r.toString())) {

							r = new IntConstant(hmap.get(r.toString()));

						}
					}

					if (l instanceof IntVariable) {

						if (hmap.containsKey(l.toString())) {

							l = new IntConstant(hmap.get(l.toString()));

						}
					}
					e = new Operation(op, l, r);
				}
				stack.push(e);
			}
		 }
	}
}
