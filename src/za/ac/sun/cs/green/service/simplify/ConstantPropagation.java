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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;

public class ConstantPropagation extends BasicService {

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

	public Expression propagate(Expression expression, Map<Variable, Variable> map) {
		try {
			/* Map variables */
			log.log(Level.FINEST, "Before variable mapping: " + expression);
			VariableVisitor variableVisitor = new VariableVisitor();
			expression.accept(variableVisitor);
			expression = variableVisitor.getExpression();
			log.log(Level.FINEST, "After variable mapping: " + expression);
			/* Canonize */
			CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			expression.accept(canonizationVisitor);
			Expression canonized = canonizationVisitor.getExpression();
			log.log(Level.FINEST, "After canonization: " + canonized);
			/* Simplify */
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor();
			canonized.accept(simplificationVisitor);
			Expression simplified = simplificationVisitor.getExpression();
			log.log(Level.FINEST, "After simplification: " + simplified);

			return simplified;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class VariableVisitor extends Visitor {

		private Stack<Expression> stack;
		private Map<Expression, Expression> map = new HashMap<Expression, Expression>();

		public VariableVisitor() {
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
			if (nop != null) {
				Expression r = stack.pop();
				Expression l = stack.pop();

				/* Map variables to their assigned constant */
				if (nop.equals(Operation.Operator.EQ) && r instanceof IntVariable && l instanceof IntConstant) {
					map.put(r, l);
				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntVariable && r instanceof IntConstant) {
					map.put(l, r);
				}

				if ((r instanceof IntVariable) && (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
					/* If the variable has constant assigned, replace it here */
					if (map.containsKey(r)) {
						stack.push(new Operation(nop, map.get(r), l));
					} else {
						stack.push(new Operation(nop, r, l));
					}
				} else if ((l instanceof IntConstant) && (r instanceof IntVariable)) {
					/* If the variable has constant assigned, replace it here */
					if (map.containsKey(l)) {
						stack.push(new Operation(nop, map.get(l), r));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else {
					stack.push(new Operation(nop, l, r));
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				/* If the variable has constant assigned, replace it here */
				if (map.containsKey(r)) {
					stack.push(new Operation(op, l, map.get(r)));
				} else if (map.containsKey(l)) {
					stack.push(new Operation(op, map.get(l), r));
				} else {
					stack.push(new Operation(op, l, r));
				}

			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}

		/**
		 * @return Returns true if the stack is empty, false otherwise.
		 */
		public boolean isEmpty() {
			return stack.isEmpty();
		}

	}

	private static class CanonizationVisitor extends Visitor {

		private Stack<Expression> stack;

		private SortedSet<Expression> conjuncts;

		private SortedSet<IntVariable> variableSet;

		private Map<IntVariable, Integer> lowerBounds;

		private Map<IntVariable, Integer> upperBounds;

		private IntVariable boundVariable;

		private Integer bound;

		private int boundCoeff;

		private boolean unsatisfiable;

		private boolean linearInteger;

		public CanonizationVisitor() {
			stack = new Stack<Expression>();
			conjuncts = new TreeSet<Expression>();
			variableSet = new TreeSet<IntVariable>();
			unsatisfiable = false;
			linearInteger = true;
		}

		public SortedSet<IntVariable> getVariableSet() {
			return variableSet;
		}

		public Expression getExpression() {
			if (!linearInteger) {
				return null;
			} else if (unsatisfiable) {
				return Operation.FALSE;
			} else {
				if (!stack.isEmpty()) {
					Expression x = stack.pop();
					if (x.equals(Operation.FALSE)) {
						return Operation.FALSE;
					} else if (!x.equals(Operation.TRUE)) {
						conjuncts.add(x);
					}
				}
				SortedSet<Expression> newConjuncts = processBounds();
				// new TreeSet<Expression>();
				Expression c = null;
				for (Expression e : newConjuncts) {
					if (e.equals(Operation.FALSE)) {
						return Operation.FALSE;
					} else if (e instanceof Operation) {
						Operation o = (Operation) e;
						if (o.getOperator() == Operation.Operator.GT) {
							e = new Operation(Operation.Operator.LT, scale(-1, o.getOperand(0)), o.getOperand(1));
						} else if (o.getOperator() == Operation.Operator.GE) {
							e = new Operation(Operation.Operator.LE, scale(-1, o.getOperand(0)), o.getOperand(1));
						}
						o = (Operation) e;
						if (o.getOperator() == Operation.Operator.GT) {
							e = new Operation(Operation.Operator.GE, merge(o.getOperand(0), new IntConstant(-1)),
									o.getOperand(1));
						} else if (o.getOperator() == Operation.Operator.LT) {
							e = new Operation(Operation.Operator.LE, merge(o.getOperand(0), new IntConstant(1)),
									o.getOperand(1));
						}
					}
					if (c == null) {
						c = e;
					} else {
						c = new Operation(Operation.Operator.AND, c, e);
					}
				}
				return (c == null) ? Operation.TRUE : c;
			}
		}

		private SortedSet<Expression> processBounds() {
			return conjuncts;
		}

		@SuppressWarnings("unused")
		private void extractBound(Expression e) throws VisitorException {
			if (e instanceof Operation) {
				Operation o = (Operation) e;
				Expression lhs = o.getOperand(0);
				Operation.Operator op = o.getOperator();
				if (isBound(lhs)) {
					switch (op) {
					case EQ:
						lowerBounds.put(boundVariable, bound * boundCoeff);
						upperBounds.put(boundVariable, bound * boundCoeff);
						break;
					case LT:
						if (boundCoeff == 1) {
							upperBounds.put(boundVariable, bound * -1 - 1);
						} else {
							lowerBounds.put(boundVariable, bound + 1);
						}
						break;
					case LE:
						if (boundCoeff == 1) {
							upperBounds.put(boundVariable, bound * -1);
						} else {
							lowerBounds.put(boundVariable, bound);
						}
						break;
					case GT:
						if (boundCoeff == 1) {
							lowerBounds.put(boundVariable, bound * -1 + 1);
						} else {
							upperBounds.put(boundVariable, bound - 1);
						}
						break;
					case GE:
						if (boundCoeff == 1) {
							lowerBounds.put(boundVariable, bound * -1);
						} else {
							upperBounds.put(boundVariable, bound);
						}
						break;
					default:
						break;
					}
				}
			}
		}

		private boolean isBound(Expression lhs) {
			if (!(lhs instanceof Operation)) {
				return false;
			}
			Operation o = (Operation) lhs;
			if (o.getOperator() == Operation.Operator.MUL) {
				if (!(o.getOperand(0) instanceof IntConstant)) {
					return false;
				}
				if (!(o.getOperand(1) instanceof IntVariable)) {
					return false;
				}
				boundVariable = (IntVariable) o.getOperand(1);
				bound = 0;
				if ((((IntConstant) o.getOperand(0)).getValue() == 1)
						|| (((IntConstant) o.getOperand(0)).getValue() == -1)) {
					boundCoeff = ((IntConstant) o.getOperand(0)).getValue();
					return true;
				} else {
					return false;
				}
			} else if (o.getOperator() == Operation.Operator.ADD) {
				if (!(o.getOperand(1) instanceof IntConstant)) {
					return false;
				}
				bound = ((IntConstant) o.getOperand(1)).getValue();
				if (!(o.getOperand(0) instanceof Operation)) {
					return false;
				}
				Operation p = (Operation) o.getOperand(0);
				if (!(p.getOperand(0) instanceof IntConstant)) {
					return false;
				}
				if (!(p.getOperand(1) instanceof IntVariable)) {
					return false;
				}
				boundVariable = (IntVariable) p.getOperand(1);
				if ((((IntConstant) p.getOperand(0)).getValue() == 1)
						|| (((IntConstant) p.getOperand(0)).getValue() == -1)) {
					boundCoeff = ((IntConstant) p.getOperand(0)).getValue();
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		@Override
		public void postVisit(Constant constant) {
			if (linearInteger && !unsatisfiable) {
				if (constant instanceof IntConstant) {
					stack.push(constant);
				} else {
					stack.clear();
					linearInteger = false;
				}
			}
		}

		@Override
		/**
		 * This adds the 1 multiplied by variable.
		 */
		public void postVisit(Variable variable) {
			if (linearInteger && !unsatisfiable) {
				if (variable instanceof IntVariable) {
					variableSet.add((IntVariable) variable);
					stack.push(new Operation(Operation.Operator.MUL, Operation.ONE, variable));
				} else {
					stack.clear();
					linearInteger = false;
				}
			}
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			if (!linearInteger || unsatisfiable) {
				return;
			}
			Operation.Operator op = operation.getOperator();
			switch (op) {
			case AND:
				if (!stack.isEmpty()) {
					Expression x = stack.pop();
					if (!x.equals(Operation.TRUE)) {
						conjuncts.add(x);
					}
				}
				if (!stack.isEmpty()) {
					Expression x = stack.pop();
					if (!x.equals(Operation.TRUE)) {
						conjuncts.add(x);
					}
				}
				break;
			case EQ:
			case NE:
			case LT:
			case LE:
			case GT:
			case GE:
				if (!stack.isEmpty()) {
					Expression e = merge(scale(-1, stack.pop()), stack.pop());
					if (e instanceof IntConstant) {
						int v = ((IntConstant) e).getValue();
						boolean b = true;
						if (op == Operation.Operator.EQ) {
							b = v == 0;
						} else if (op == Operation.Operator.NE) {
							b = v != 0;
						} else if (op == Operation.Operator.LT) {
							b = v < 0;
						} else if (op == Operation.Operator.LE) {
							b = v <= 0;
						} else if (op == Operation.Operator.GT) {
							b = v > 0;
						} else if (op == Operation.Operator.GE) {
							b = v >= 0;
						}
						if (b) {
							stack.push(Operation.TRUE);
						} else {
							stack.push(Operation.FALSE);
							// unsatisfiable = true;
						}
					} else {
						stack.push(new Operation(op, e, Operation.ZERO));
					}
				}
				break;
			case ADD:
				stack.push(merge(stack.pop(), stack.pop()));
				break;
			case SUB:
				stack.push(merge(scale(-1, stack.pop()), stack.pop()));
				break;
			case MUL:
				if (stack.size() >= 2) {
					Expression r = stack.pop();
					Expression l = stack.pop();
					if ((l instanceof IntConstant) && (r instanceof IntConstant)) {
						int li = ((IntConstant) l).getValue();
						int ri = ((IntConstant) r).getValue();
						stack.push(new IntConstant(li * ri));
					} else if (l instanceof IntConstant) {
						int li = ((IntConstant) l).getValue();
						stack.push(scale(li, r));
					} else if (r instanceof IntConstant) {
						int ri = ((IntConstant) r).getValue();
						stack.push(scale(ri, l));
					} else {
						stack.clear();
						linearInteger = false;
					}
				}
				break;
			case NOT:
				if (!stack.isEmpty()) {
					Expression e = stack.pop();
					if (e.equals(Operation.TRUE)) {
						e = Operation.FALSE;
					} else if (e.equals(Operation.FALSE)) {
						e = Operation.TRUE;
					} else if (e instanceof Operation) {
						Operation o = (Operation) e;
						switch (o.getOperator()) {
						case NOT:
							e = o.getOperand(0);
							break;
						case EQ:
							e = new Operation(Operation.Operator.NE, o.getOperand(0), o.getOperand(1));
							break;
						case NE:
							e = new Operation(Operation.Operator.EQ, o.getOperand(0), o.getOperand(1));
							break;
						case GE:
							e = new Operation(Operation.Operator.LT, o.getOperand(0), o.getOperand(1));
							break;
						case GT:
							e = new Operation(Operation.Operator.LE, o.getOperand(0), o.getOperand(1));
							break;
						case LE:
							e = new Operation(Operation.Operator.GT, o.getOperand(0), o.getOperand(1));
							break;
						case LT:
							e = new Operation(Operation.Operator.GE, o.getOperand(0), o.getOperand(1));
							break;
						default:
							break;
						}
					} else {
						// We just drop the NOT??
					}
					stack.push(e);
				} else {
					// We just drop the NOT??
				}
				break;
			default:
				break;
			}
		}

		private Expression merge(Expression left, Expression right) {
			Operation l = null;
			Operation r = null;
			int s = 0;
			if (left instanceof IntConstant) {
				s = ((IntConstant) left).getValue();
			} else {
				if (hasRightConstant(left)) {
					s = getRightConstant(left);
					l = getLeftOperation(left);
				} else {
					l = (Operation) left;
				}
			}
			if (right instanceof IntConstant) {
				s += ((IntConstant) right).getValue();
			} else {
				if (hasRightConstant(right)) {
					s += getRightConstant(right);
					r = getLeftOperation(right);
				} else {
					r = (Operation) right;
				}
			}
			SortedMap<Variable, Integer> coefficients = new TreeMap<Variable, Integer>();
			IntConstant c;
			Variable v;
			Integer k;

			// Collect the coefficients of l
			if (l != null) {
				while (l.getOperator() == Operation.Operator.ADD) {
					Operation o = (Operation) l.getOperand(1);
					assert (o.getOperator() == Operation.Operator.MUL);
					c = (IntConstant) o.getOperand(0);
					v = (IntVariable) o.getOperand(1);
					coefficients.put(v, c.getValue());
					l = (Operation) l.getOperand(0);
				}
				assert (l.getOperator() == Operation.Operator.MUL);
				c = (IntConstant) l.getOperand(0);
				v = (IntVariable) l.getOperand(1);
				coefficients.put(v, c.getValue());
			}

			// Collect the coefficients of r
			if (r != null) {
				while (r.getOperator() == Operation.Operator.ADD) {
					Operation o = (Operation) r.getOperand(1);
					assert (o.getOperator() == Operation.Operator.MUL);
					c = (IntConstant) o.getOperand(0);
					v = (IntVariable) o.getOperand(1);
					k = coefficients.get(v);
					if (k == null) {
						coefficients.put(v, c.getValue());
					} else {
						coefficients.put(v, c.getValue() + k);
					}
					r = (Operation) r.getOperand(0);
				}
				assert (r.getOperator() == Operation.Operator.MUL);
				c = (IntConstant) r.getOperand(0);
				v = (IntVariable) r.getOperand(1);
				k = coefficients.get(v);
				if (k == null) {
					coefficients.put(v, c.getValue());
				} else {
					coefficients.put(v, c.getValue() + k);
				}
			}

			Expression lr = null;
			for (Map.Entry<Variable, Integer> e : coefficients.entrySet()) {
				int coef = e.getValue();
				if (coef != 0) {
					Operation term = new Operation(Operation.Operator.MUL, new IntConstant(coef), e.getKey());
					if (lr == null) {
						lr = term;
					} else {
						lr = new Operation(Operation.Operator.ADD, lr, term);
					}
				}
			}
			if ((lr == null) || (lr instanceof IntConstant)) {
				return new IntConstant(s);
			} else if (s == 0) {
				return lr;
			} else {
				return new Operation(Operation.Operator.ADD, lr, new IntConstant(s));
			}
		}

		private boolean hasRightConstant(Expression expression) {
			return isAddition(expression) && (getRightExpression(expression) instanceof IntConstant);
		}

		private int getRightConstant(Expression expression) {
			return ((IntConstant) getRightExpression(expression)).getValue();
		}

		private Expression getLeftExpression(Expression expression) {
			return ((Operation) expression).getOperand(0);
		}

		private Expression getRightExpression(Expression expression) {
			return ((Operation) expression).getOperand(1);
		}

		private Operation getLeftOperation(Expression expression) {
			return (Operation) getLeftExpression(expression);
		}

		private boolean isAddition(Expression expression) {
			return ((Operation) expression).getOperator() == Operation.Operator.ADD;
		}

		private Expression scale(int factor, Expression expression) {
			if (factor == 0) {
				return Operation.ZERO;
			}
			if (expression instanceof IntConstant) {
				return new IntConstant(factor * ((IntConstant) expression).getValue());
			} else if (expression instanceof IntVariable) {
				return expression;
			} else {
				assert (expression instanceof Operation);
				Operation o = (Operation) expression;
				Operation.Operator p = o.getOperator();
				Expression l = scale(factor, o.getOperand(0));
				Expression r = scale(factor, o.getOperand(1));
				return new Operation(p, l, r);
			}
		}

	}

	private static class SimplificationVisitor extends Visitor {

		private Stack<Expression> stack;

		public SimplificationVisitor() {
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

			if (nop != null) {
				Expression r = stack.pop();
				Expression l = stack.pop();

				/* Find <something> == 0 */
				if (nop.equals(Operation.Operator.EQ) && (r.equals(Operation.ZERO))) {
					stack.push(swippySwoppy(l, r));
				} /* Find x <op> y */
				else if ((r instanceof IntVariable) && (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, l, r));
				} /* Find x <op> <const> */
				else if ((r instanceof IntVariable) && (l instanceof IntConstant)) {
					stack.push(new Operation(nop, l, r));
				} else {
					stack.push(new Operation(nop, l, r));
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();

				/* Get rid of (1*x) tokens with just the variable */
				if (op.equals(Operation.Operator.MUL)) {
					if ((r instanceof IntVariable) && (l.equals(Operation.ONE))) {
						stack.push(r);
					} else {
						stack.push(new Operation(op, l, r));
					}
				} else {
					stack.push(new Operation(op, l, r));
				}
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}

		/**
		 * Gets called for '<var> + [-]<const> == 0' and places the var on left
		 * and const on right.
		 * 
		 * @param l
		 *            <var> + [-]<const>
		 * @param r
		 *            0
		 * @return <var> == [-]<const>
		 */
		private Operation swippySwoppy(Expression l, Expression r) {
			/* Find variable */
			Pattern ptrn = Pattern.compile("[a-zA-Z]");
			Matcher mtchr = ptrn.matcher(l.toString());
			String variable = "";
			if (mtchr.find()) {
				variable = mtchr.group();
			}
			/* Find constant */
			ptrn = Pattern.compile("-[0-9]");
			mtchr = ptrn.matcher(l.toString());
			String constant = "";
			if (mtchr.find()) {
				constant = mtchr.group();
			} else {
				ptrn = Pattern.compile("[0-9]");
				mtchr = ptrn.matcher(l.toString());
			}

			IntVariable var = new IntVariable(variable, 0, 99);
			IntConstant cons = new IntConstant((-1) * Integer.parseInt(constant));
			return new Operation(Operation.Operator.EQ, var, cons);
		}
	}
}
