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

	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {

		try {
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			PropagationVisitor propagationVisitor = new PropagationVisitor();
			expression.accept(propagationVisitor);
			Expression propagated = propagationVisitor.getExpression();
			return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,"encountered an exception -- this should not be happening!",x);
		}
		return null;
	}

	private static class PropagationVisitor extends Visitor {

		private Stack<Expression> stack;

		private IntVariable var;

		private IntConstant const1;

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
			stack.push(variable);
		}

@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			Expression r = stack.pop();
			Expression l = stack.pop();
			if (op == Operation.Operator.EQ) {
				if (r instanceof IntVariable && l instanceof IntConstant) {
					var = (IntVariable) r;
					const1 = (IntConstant) l;
				} else if (l instanceof IntVariable && r instanceof IntConstant) {
					var = (IntVariable) l;
					const1 = (IntConstant) r;
				}
				stack.push(new Operation(op, l, r));
			} else {
				if (r instanceof IntVariable) {
					if (((IntVariable) r).getName().equals(var.getName())) {
						r = const1;
					}
				}
				if (l instanceof IntVariable) {
					if (((IntVariable) l).getName().equals(var.getName())) {
						l = const1;
					}
				}
				stack.push(new Operation(op, l, r));
			}
		}
	}
}
