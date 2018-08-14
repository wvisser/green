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

import sun.util.logging.resources.logging;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Operation.Operator;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;

public class ConstantPropagation extends BasicService {

	public static boolean cont = true;

	public ConstantPropagation(Green solver) {
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

	/*
	 * Simplifies the input expression as far as possible
	 * 
	 * @param expression The input expression
	 * 
	 * @param map The variable map used to simplify
	 * 
	 * @return The simplified expression
	 */
	public Expression simplify(Expression expression, Map<Variable, Variable> map) {
		cont = true;
		try {
			log.log(Level.FINEST, "Before variable mapping: " + expression);
			VariableVisitor varaibleVisitor = new VariableVisitor();
			CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			SimplificationVisitor simplificationVisitor = new SimplificationVisitor(map);
			while (cont) {
				while (cont) {
					cont = false;
					expression.accept(varaibleVisitor);
					expression = varaibleVisitor.getExpression();
				}
				log.log(Level.FINEST, "After Variable mapping: " + expression);
				expression.accept(canonizationVisitor);
				expression = canonizationVisitor.getExpression();
				log.log(Level.FINEST, "After Canonization: " + expression);
				expression.accept(simplificationVisitor);
				expression = simplificationVisitor.getExpression();
				log.log(Level.FINEST, "After Simplification: " + expression);

			}
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "encountered an exception -- this should not be happening!", x);
		}
		return null;
	}

	private static class VariableVisitor extends Visitor {
		private Stack<Expression> stack;
		private HashMap<Expression, Expression> constants = new HashMap<Expression, Expression>();

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
				nop = Operation.Operator.LT;
				break;
			case LE:
				nop = Operation.Operator.LE;
				break;
			case GT:
				nop = Operation.Operator.GT;
				break;
			case GE:
				nop = Operation.Operator.GE;
				break;
			default:
				break;
			}

			if (nop != null) {

				Expression r = stack.pop();
				Expression l = stack.pop();
				if (nop.equals(Operation.Operator.EQ) && r instanceof IntVariable && l instanceof IntConstant) {
					if (!constants.containsKey(r)) {
						cont = true;
					} else if (!constants.get(r).equals(l)) {
						stack.push(new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1)));
						return;
					}
					constants.put(r, l);

				} else if (nop.equals(Operation.Operator.EQ) && l instanceof IntVariable && r instanceof IntConstant) {
					if (!constants.containsKey(l)) {
						cont = true;
					} else if (!constants.get(l).equals(r)) {
						stack.push(new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1)));
						return;
					}
					constants.put(l, r);
				}

				if ((r instanceof IntVariable) && (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
				} else if ((r instanceof IntVariable) && (l instanceof IntConstant)
						&& !nop.equals(Operation.Operator.EQ)) {
					if (constants.containsKey(r)) {
						stack.push(new Operation(nop, constants.get(r), l));
					} else {
						stack.push(new Operation(nop, r, l));
					}
				} else if ((r instanceof IntConstant) && (l instanceof IntVariable)
						&& !nop.equals(Operation.Operator.EQ)) {
					if (constants.containsKey(l)) {
						stack.push(new Operation(nop, r, constants.get(l)));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else if ((r instanceof IntVariable) && (l instanceof IntVariable)
						&& nop.equals(Operation.Operator.EQ)) {
					if (constants.containsKey(r)) {
						cont = true;
						stack.push(new Operation(nop, l, constants.get(r)));
					} else if (constants.containsKey(l)) {
						cont = true;
						stack.push(new Operation(nop, r, constants.get(l)));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else {
					stack.push(new Operation(nop, l, r));
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				if (constants.containsKey(l)) {
					stack.push(new Operation(op, constants.get(l), r));
				} else if (constants.containsKey(r)) {
					stack.push(new Operation(op, l, constants.get(r)));
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

		/*
		 * gets the expression in a more canonized form.
		 * 
		 * @return The simplified expression
		 */
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

		/*
		 * Tells if if the expression is bound or not
		 * 
		 * @return A true or false if the expression is bound
		 */
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
					Expression op1 = stack.pop();
					Expression op2 = stack.pop();
					Expression e = merge(scale(-1, op1), op2);
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
						}
					} else {
						stack.push(swop(op, e));
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

		/*
		 * Swops the = 0 on the RHS to a = <variable or constant> instead.
		 * 
		 * @param op the operator to swop on
		 * 
		 * @param e the expression to swop on
		 * 
		 * @return The swapped expression
		 */
		private Expression swop(Operator op, Expression e) {
			if (!(e instanceof Operation)) {
				new Operation(op, e, Operation.ZERO);
			}

			Operation operation = (Operation) e;
			Expression left = operation.getOperand(0);
			Expression right = operation.getOperand(1);
			if (left instanceof Operation) {
				if (right instanceof IntConstant) {
					return new Operation(op, left, new IntConstant(-1 * ((IntConstant) right).getValue()));

				}
			} else if (right instanceof Operation) {
				if (left instanceof IntConstant) {
					return new Operation(op, right, new IntConstant(-1 * ((IntConstant) left).getValue()));
				}
			}

			return new Operation(op, e, Operation.ZERO);
		}

		/*
		 * Merges the left and right expressions
		 * 
		 * @param left the left expression
		 * 
		 * @param right the right expression
		 * 
		 * @return The merged expression
		 */
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

		private Map<Variable, Variable> map;

		private Stack<Expression> stack;

		public SimplificationVisitor(Map<Variable, Variable> map) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		/*
		 * Returns the expression
		 * 
		 * @return The expression
		 */
		public Expression getExpression() throws VisitorException {
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			Variable v = map.get(variable);
			if (v == null) {
				v = new IntVariable(variable.toString(), variable.getLowerBound(), variable.getUpperBound());
				map.put(variable, v);
			}
			stack.push(v);
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				Expression e = stack.pop();
				operands[i - 1] = e;
			}

			if (operation.getOperand(0).equals(Operation.ONE)
					&& operation.getOperator().equals(Operation.Operator.MUL)) {
				stack.push(operation.getOperand(1));
			} else if (operation.getOperand(0).equals(new IntConstant(-1))
					&& operation.getOperator().equals(Operation.Operator.MUL)) {
				stack.push(new Operation(Operator.NEG, operation.getOperand(1)));
			} else if (isNegative(operation.getOperand(0))) {
				if (operation.getOperand(0) instanceof Operation) {
					Operation op1 = (Operation) operation.getOperand(0);
					if (isNegative(operation.getOperand(1))) {
						if (operation.getOperand(1) instanceof IntConstant) {
							IntConstant op2 = (IntConstant) operation.getOperand(1);
							stack.push(new Operation(operation.getOperator(), op1.getOperand(1),
									new IntConstant(op2.getValue() * -1)));

						} else {
							Operation op2 = (Operation) operation.getOperand(1);
							stack.push(new Operation(operation.getOperator(), op1.getOperand(1), op2.getOperand(1)));
						}
					} else {
						stack.push(new Operation(operation.getOperator(), op1.getOperand(1), operation.getOperand(1)));
					}
				} else {
					stack.push(new Operation(operation.getOperator(), operands));
				}
			} else {
				stack.push(new Operation(operation.getOperator(), operands));
			}

		}
		/*
		 * checks if the expression is negative
		 * 
		 * @param op the expression to check on
		 * 
		 * @return If the expression is negative
		 */
		public boolean isNegative(Expression op) {
			if (op instanceof IntConstant) {
				if (((IntConstant) op).getValue() < 0) {
					return true;
				}
			}
			if (!(op instanceof Operation)) {
				return false;
			}

			if (((Operation) op).getOperand(0).toString().equals("-1")
					&& (((Operation) op).getOperand(1) instanceof IntVariable)) {
				return true;
			} else if (((Operation) op).getOperand(0).toString().equals("-1")
					&& (((Operation) op).getOperand(1) instanceof IntConstant)) {
				return true;
			}
			return false;
		}

	}

}
