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

	public ConstantPropogation(Green solver){
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		Expression expression = instance.getFullExpression();
		log.log(Level.FINEST, "Before propogation: " + expression);
		final Expression e = propegate(expression);
		final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
		log.log(Level.FINEST, "After propogation: " + i.getExpression());
		return Collections.singleton(i);
	}

	private Expression propegate(Expression expression){
		try {
			PropogationVisitor propogationVisitor = new PropogationVisitor();
			expression.accept(propogationVisitor);
			return propogationVisitor.getExpression();
		} catch (VisitorException ex){
			log.log(Level.SEVERE, "This should not be happening - probably will.",ex);
		}
		return null;
	}

	private static class PropogationVisitor extends Visitor {

		private Map<Expression, Expression> constants;
		private Stack<Expression> stack;

		public Expression getExpression() {
			return stack.pop();
		}

		public PropogationVisitor() {
			stack = new Stack<Expression>();
			this.constants = new HashMap<Expression, Expression>();
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

			r = propegateForward(r, l);
			l = propegateForward(l, r);

			if (op.equals(Operation.Operator.EQ)) {
				extractEquality(r, l);
				extractEquality(l, r);
			}

			/* Handles the case if an equality was discovered after a potential variable to change
			has already been visited. e.i. (x + y) is visited before ('x = 1') */
			if (l instanceof Operation) {
				// visit left branch of parse tree
				l.accept(this);
				stack.push(new Operation(op, stack.pop(), r));
			} else {
				stack.push(new Operation(op, l, r));
			}
		}

	/**
		* Determines if an 'x = 1' case exists between r and l.
		*
		* @param r The potential 'x'
		* @param l The corresponding '1'
		*/
		private void extractEquality(Expression r, Expression l) {
			if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
				constants.putIfAbsent(r, l);
			}
		}

	/**
		* Replaces an IntVariable with IntConstant if IntVariable is in 'constants'.
		* Excludes the case where 'x = 1' --> '1 = 1'
		*
		* @param r IntVariable to be replaced
		* @param l Expression to test for '1 == 1' case. (if l is an IntConstant).
		* @return r if r is not in 'constants' else the new IntVariable
		*/
		private Expression propegateForward(Expression r, Expression l) {
			IntConstant var;
			return (var = (IntConstant) constants.get(r)) != null
							&& r instanceof IntVariable && !(l instanceof IntConstant) ? var : r;
		}
	}

}
