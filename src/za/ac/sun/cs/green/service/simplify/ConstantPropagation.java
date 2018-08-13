package za.ac.sun.cs.green.service.simplify;

import java.util.Arrays;
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
import java.util.stream.Stream;

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
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			boolean changed = true;
			Map<IntVariable, IntConstant> constants = new HashMap<IntVariable, IntConstant>();

			int n = 0;
			/* while the expressions is still being changed, simplify it */
			while (changed) {
				ConstantVisitor constantVisitor = new ConstantVisitor(constants);
				OrderingVisitor orderingVisitor = new OrderingVisitor();
				SimplifyVisitor simplifyVisitor = new SimplifyVisitor();

				expression.accept(constantVisitor);
				expression = constantVisitor.getExpression();
				expression.accept(orderingVisitor);
				expression = orderingVisitor.getExpression();
				expression.accept(simplifyVisitor);
				expression = simplifyVisitor.getExpression();
				changed = orderingVisitor.hasChanged() ||
					constantVisitor.hasChanged() ||
					simplifyVisitor.hasChanged();

				/* check if we're still simplifying after 50 iterations */
				if (n++ > 50) {
					log.log(Level.SEVERE, "Failed to simplify further after 50 attempts");
					break;
				}

			}

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
	 * Traverses the expression from the bottom up and orders all subtrees in
	 * the expression according to a specific format.
	 */
	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;
		private boolean changed;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			changed = false;
		}

		/**
		 * Indicates whether any changes have occured to the expression
		 */
		public boolean hasChanged() {
			return changed;
		}

		/**
		 * Returns the expression in it's most recent form
		 */
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
		 * Where an expression of the following formats occur:
		 *      1 + x
		 *      1 * x
		 * the expression is replaced respectively as:
		 *      x + 1
		 *      x * 1
		 * Any other expressions are left in the same order.
		 */
		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();

			switch (op) {
				case ADD:
				case MUL:
					Expression r = stack.pop();
					Expression l = stack.pop();

					if (l instanceof IntConstant && r instanceof IntVariable) {
						stack.push(new Operation(op, r, l));
						changed = true;
					} else {
						stack.push(new Operation(op, l, r));
					}

					break;
				default:
					int arity = operation.getOperator().getArity();
					Expression operands[] = new Expression[arity];
					for (int i = arity; i > 0; i--) {
						operands[i - 1] = stack.pop();
					}
					stack.push(new Operation(operation.getOperator(), operands));
			}
		}

	}


	/**
	 * Traverses the expression from the top down and determines if there are
	 * any constant values assigned to variables. If so, when traversing the
	 * expression from the bottom up, wherever else the variable appears is
	 * replaced with the value associated with that variable.
	 */
	private static class ConstantVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<IntVariable, IntConstant> constants;
		private boolean replace;
		private boolean changed;
		private boolean unsatisfiable;

		public ConstantVisitor(Map<IntVariable, IntConstant> constants) {
			stack = new Stack<Expression>();
			this.constants = constants;
			replace = true;
			changed = false;
			unsatisfiable = false;
		}

		/**
		 * Indicates whether any changes have occured to the expression
		 */
		public boolean hasChanged() {
			return changed;
		}

		/**
		 * Returns the expression in its most recent form
		 */
		public Expression getExpression() {
			if (unsatisfiable) {
				return Operation.FALSE;
			} else {
				return stack.pop();
			}
		}

		/**
		 * Finds all variables that are assigned to constants within the
		 * expression e.g. x == 5 and adds their value to the lookup table
		 */
		@Override
		public void preVisit(Operation operation) throws VisitorException {
			if (unsatisfiable) {
				return;
			}

			Operation.Operator op = operation.getOperator();

			if (op == Operation.Operator.EQ) {
				Expression r = operation.getOperand(0);
				Expression l = operation.getOperand(1);

				/* case x == 5 */
				if (r instanceof IntVariable &&
						l instanceof IntConstant) {
					IntVariable v = (IntVariable) r;
					IntConstant c = (IntConstant) l;

					/* determines if the constant evaluation has already been
					 * added to the mapping of variables to constants */
					if (!constants.containsKey(v)) {
						constants.put(v, c);
						changed = true;
					} else if (!constants.get(v).equals(c)) {
						/* case x == 1 && x == 2 */
						unsatisfiable = true;
					}

					replace = false;
				/* case 5 == x */
				} else if (r instanceof IntConstant &&
						l instanceof IntVariable) {
					IntVariable v = (IntVariable) l;
					IntConstant c = (IntConstant) r;

					/* determines if the constant evaluation has already been
					 * added to the mapping of variables to constants */
					if (!constants.containsKey(v)) {
						constants.put(v, c);
						changed = true;
					} else if (!constants.get(v).equals(c)) {
						/* case 1 == x && 2 == x */
						unsatisfiable = true;
					}

					replace = false;
				}
			}
		}

		@Override
		public void postVisit(IntConstant constant) {
			if (unsatisfiable) {
				return;
			}

			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			if (unsatisfiable) {
				return;
			}

			if (replace && constants.containsKey(variable)) {
				stack.push(constants.get(variable));
				changed = true;
			} else {
				stack.push(variable);
			}
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			if (unsatisfiable) {
				return;
			}

			replace = true;
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			stack.push(new Operation(operation.getOperator(), operands));
		}
	}

	/**
	 * Traverses the expression from the bottom up and simplifies it where
	 * possible. For example, the subtree 3 == 5 is evaluated to 0 == 1
	 * indicating that the subtree is false.
	 */
	private static class SimplifyVisitor extends Visitor {

		private Stack<Expression> stack;
		private boolean changed;

		public SimplifyVisitor() {
			stack = new Stack<Expression>();
			changed = false;
		}

		/**
		 * Returns the expression in its most recent form
		 */
		public Expression getExpression() {
			return stack.pop();
		}

		/**
		 * Indicates whether any changes have occured to the expression
		 */
		public boolean hasChanged() {
			return changed;
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
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}

			Stream<Expression> str = Arrays.stream(operands);

			/* determine if we're dealing with an entirely constant expression
			 * or not */
			if (str.allMatch(e ->
						e instanceof IntConstant
						|| e.equals(Operation.TRUE)
						|| e.equals(Operation.FALSE)
					)) {
				simplifyConstants(operation, operands);
			} else {
				simplifyRelations(operation, operands);
			}

		}

		/**
		 * Simplifies expressions of similar forms to x + 4 = 10 as well as
		 * and/or expressions which have true/false operands.
		 * For example x + 4 = 10 is converted to x = 10 - 4 for further
		 * simplification in the next iteration. Similarly 1 = x/3 becomes
		 * 1*3 = x. Boolean operations with one operand as true or false simplify
		 * to their other operand or true/false depending on the semantics, and
		 * statements such as x = 1 && x = 1 are simplified to x = 1.
		 */
		private void simplifyRelations(Operation operation, Expression[] operands) {
			Operation.Operator op = operation.getOperator();

			switch (op) {
				case EQ:
				case NE:
				case LT:
				case GT:
				case LE:
				case GE:
					Expression l = operands[0];
					Expression r = operands[1];
					Operation.Operator op2 = null;
					Operation operator = null;
					IntConstant constant = null;
					boolean eq = false;
					boolean lefty = false;

					/* Determine if we have an expression of the form x + y = 10 */
					if (l instanceof Operation && r instanceof IntConstant) {
						op2 = ((Operation) l).getOperator();
						operator = (Operation) l;
						constant = (IntConstant) r;
						eq = true;
						lefty = true;
					/* Determine if we have an expression of the form 10 = x + y */
					} else if (r instanceof Operation && l instanceof IntConstant) {
						op2 = ((Operation) r).getOperator();
						operator = (Operation) r;
						constant = (IntConstant) l;
						eq = true;
					}

					if (eq) {
						Operation.Operator nop = null;

						/* Get the negative operation of +, - and / */
						switch (op2) {
							case ADD:
								nop = Operation.Operator.SUB;
								break;
							case SUB:
								nop = Operation.Operator.ADD;
								break;
							case DIV:
								nop = Operation.Operator.MUL;
								break;
						}

						/* we have an operation which we can simplify */
						if (nop != null) {
							changed = true;
							Expression left, right;

							if (lefty) { // move left term to the right
								left = operator.getOperand(0);
								right = new Operation(nop, constant, operator.getOperand(1));
							} else { // move right term to the left
								left = new Operation(nop, constant, operator.getOperand(1));
								right = operator.getOperand(0);
							}

							operands[0] = left;
							operands[1] = right;
						}
					}

					break;

				/* simplify AND expressions where possible */
				case AND:
					l = operands[0];
					r = operands[1];

					if (l.equals(Operation.TRUE)) {
						stack.push(r);
					} else if (r.equals(Operation.TRUE)) {
						stack.push(l);
					} else if (l.equals(Operation.FALSE)) {
						stack.push(l);
					} else if (r.equals(Operation.FALSE)) {
						stack.push(r);
					} else if (r.equals(l)) {
						stack.push(r);
					} else {
						break;
					}

					return;

				/* simplify OR expressions where possible */
				case OR:
					l = operands[0];
					r = operands[1];

					if (l.equals(Operation.TRUE)) {
						stack.push(l);
					} else if (r.equals(Operation.TRUE)) {
						stack.push(r);
					} else if (l.equals(Operation.FALSE)) {
						stack.push(r);
					} else if (r.equals(Operation.FALSE)) {
						stack.push(l);
					} else if (r.equals(l)) {
						stack.push(r);
					} else {
						break;
					}

					return;
			}

			Expression ex = new Operation(operation.getOperator(), operands);
			stack.push(ex);
		}

		/**
		 * Simplifies expressions where only constants are present.
		 */
		private void simplifyConstants(Operation operation, Expression[] operands) {
			Stream<Expression> str = Arrays.stream(operands);
			Operation.Operator op = operation.getOperator();

			/* determines the value of an only constant expression and adds it
			 * to the stack as an IntConstant or TRUE/FALSE instead of
			 * an Operation subtree */
			switch (op) {
				case EQ:
					if (operation.equals(Operation.TRUE) ||
						operation.equals(Operation.FALSE)) {
						/* Dont keep checking if 0 == 1 or if 0 == 0 since
						 * that is how we represent true and false */
						stack.push(operation);
						return;
					}
					stack.push(operands[0].equals(operands[1]) ?
							Operation.TRUE : Operation.FALSE);
					break;
				case NE:
					stack.push(!operands[0].equals(operands[1]) ?
							Operation.TRUE : Operation.FALSE);
					break;
				case LT:
					stack.push(((IntConstant) operands[0]).getValue() <
							((IntConstant) operands[1]).getValue() ?
							Operation.TRUE : Operation.FALSE);
					break;
				case GT:
					stack.push(((IntConstant) operands[0]).getValue() >
							((IntConstant) operands[1]).getValue() ?
							Operation.TRUE : Operation.FALSE);
					break;
				case LE:
					stack.push(((IntConstant) operands[0]).getValue() <=
							((IntConstant) operands[1]).getValue() ?
							Operation.TRUE : Operation.FALSE);
					break;
				case GE:
					stack.push(((IntConstant) operands[0]).getValue() >=
							((IntConstant) operands[1]).getValue() ?
							Operation.TRUE : Operation.FALSE);
					break;

				case AND: /* if all subtrees are true */
					stack.push(str.allMatch(e -> e.equals(Operation.TRUE)) ?
								Operation.TRUE : Operation.FALSE);
					break;
				case OR: /* if any subtrees are true */
					stack.push(str.anyMatch(e -> e.equals(Operation.TRUE)) ?
								Operation.TRUE : Operation.FALSE);
					break;

				case ADD:
					stack.push(new IntConstant(str
								.mapToInt(e -> ((IntConstant) e).getValue())
								.sum()));
					break;
				case SUB:
					stack.push(new IntConstant(((IntConstant) operands[0]).getValue() -
							((IntConstant) operands[1]).getValue()));
					break;
				case MUL:
					stack.push(new IntConstant(((IntConstant) operands[0]).getValue() *
							((IntConstant) operands[1]).getValue()));
					break;
				case DIV:
					stack.push(new IntConstant(((IntConstant) operands[0]).getValue() /
							((IntConstant) operands[1]).getValue()));
					break;
				case MOD:
					stack.push(new IntConstant(((IntConstant) operands[0]).getValue() %
							((IntConstant) operands[1]).getValue()));
					break;
				case NEG:
					stack.push(new IntConstant(-((IntConstant) operands[0]).getValue()));
					break;

				default:
					stack.push(operation);
					return;
			}

			changed = true;
		}

	}

}
