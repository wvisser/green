package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropogation extends BasicService {

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = simplify(instance.getFullExpression());
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	public Expression simplify(Expression expression) {
		try {
			log.log(Level.FINEST, "Before simplification: " + expression);
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
			expression.accept(simplificationVisitor);
			Expression simplified = simplificationVisitor.getExpression();
			log.log(Level.FINEST, "After simplification: " + simplified);
			return simplified;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "Houston, we have a problem!", x);
		}
		return null;
	}

	private static class SimplificationVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Constant> knownValues;

		public SimplificationVisitor() {
			stack = new Stack<Expression>();
			knownValues = new HashMap<Expression, Constant>();
		}

		public Expression getExpression() {
			Expression top = stack.pop();
			top = propogateConstants(top);
			return top;
		}

		@Override
		public void postVisit(Variable variable) {
			pushAndPrint(variable);
		}

		@Override
		public void postVisit(Constant constant) {
			pushAndPrint(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				Expression l = operation.getOperand(0);
				Expression r = operation.getOperand(1);
				if (r instanceof Constant) {
					final Constant constVal = (Constant) r;
					if (l instanceof Variable) {
						final Variable constVar = (Variable) l;
						knownValues.put(constVar, constVal);
					}
				}
			}
			pushAndPrint(operation);
		}

		private Expression propogateConstants(Expression expression) {

			if (expression instanceof Constant) {
				return expression;
			}

			if (expression instanceof Variable) {
				if (knownValues.get(expression) != null) {
					return knownValues.get(expression);
				} else {
					return expression;
				}
			}

			if (expression instanceof Operation) {
				Operation op = (Operation) expression;
				Expression l = op.getOperand(0);
				Expression r = op.getOperand(1);

				if (onlyContainsConstants(op)) {
					// If the operation only contains constants we should evaluate it and return the constant value
					return evaluate(op);
				}

				// Handle variable assignment as a special case so we don't end up with val == val
				if (op.getOperator().equals(Operation.Operator.EQ)) {
					if (l instanceof Variable && r instanceof Constant) {
						return op;
					}
				}

				l = propogateConstants(l);
				r = propogateConstants(r);
				Operation newop = new Operation(op.getOperator(), l, r);
				return newop;
			}

			System.out.println("We should never get to this point!");
			return expression;
		}

		private Expression evaluate(Expression expression) {
			if (expression instanceof Constant) return expression;

			assert expression instanceof Operation;
			Operation operation = (Operation) expression;
			Expression l = operation.getOperand(0);
			Expression r = operation.getOperand(1);

			if (l instanceof Operation) l = evaluate(l);
			if (r instanceof Operation) r = evaluate(r);

			Operation.Operator op = operation.getOperator();
			int rint;
			int lint;
			switch (op) {
				case EQ:
					lint = ((IntConstant) l).getValue();
					rint = ((IntConstant) r).getValue();
					if (lint == rint) {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
					} else {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
					}
				case GT:
					lint = ((IntConstant) l).getValue();
					rint = ((IntConstant) r).getValue();
					if (lint > rint) {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
					} else {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
					}
				case LT:
					lint = ((IntConstant) l).getValue();
					rint = ((IntConstant) r).getValue();
					if (lint < rint) {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
					} else {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
					}
				case AND:
					Operation lop = (Operation) l;
					Operation rop = (Operation) r;
					assert lop.getOperator().equals(Operation.Operator.EQ);
					assert rop.getOperator().equals(Operation.Operator.EQ);
					if (lop.equals(rop)) {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(0));
					} else {
						return new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
					}
				default:
					System.out.println("Should not get here!");
					return expression;
			}
		}

		private boolean onlyContainsConstants(Operation operation) {
			Expression l = operation.getOperand(0);
			Expression r = operation.getOperand(1);

			if (!(l instanceof Operation || r instanceof Operation)) {
				return (l instanceof Constant && r instanceof Constant);
			}

			boolean ret = true;
			if (l instanceof Operation) {
				ret = ret && onlyContainsConstants((Operation) l);
			}
			if (r instanceof Operation) {
				ret = ret && onlyContainsConstants((Operation) r);
			}
			return ret;
		}

		// For debugging purposes
		private void pushAndPrint(Expression e) {
			// System.out.println("Pushing expression to stack: " + e);
			stack.push(e);
		}
	}
}
