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
import za.ac.sun.cs.green.util.Reporter;

public class ConstantPropogation extends BasicService {

	/**
	 * Number of times the slicer has been invoked.
	 */
	private int invocations = 0;
	private static boolean changeMade = false;

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, IntConstant> map = new HashMap<Variable, IntConstant>();
			final Map<Expression, Expression> operationsMap = new HashMap<Expression, Expression>();
			final Expression e = canonize(instance.getFullExpression(), map);
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

	public Expression canonize(Expression expression,
			Map<Variable, IntConstant> map) {
		try {
			log.log(Level.FINEST, "Before Renaming: " + expression);
			invocations++;
			Expression canonized = expression;
			if (canonized != null) {
				System.out.println("--run renamer''\n");
				canonized = new Renamer(map).rename(canonized);
//				OrderingVisitor orderingVisitor = new OrderingVisitor();
//				expression.accept(orderingVisitor);
//				expression = orderingVisitor.getExpression();
				while (changeMade == true) {
					changeMade = false;
					System.out.println("--run renamer''\n");
					canonized= new Renamer(map).rename(canonized);
//					orderingVisitor = new OrderingVisitor();
//					expression.accept(orderingVisitor);
//					expression = orderingVisitor.getExpression();
				}
				//canonized = new Renamer(map).rename(canonized);
			}
			log.log(Level.FINEST, "After Renaming: " + canonized);
			return canonized;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}
	
	private static class Renamer extends Visitor {

		private Map<Variable, IntConstant> map;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, IntConstant> map) {
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			expression.accept(this);
			return stack.pop();
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
			System.out.println("1: " + variable + "\n");
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
			System.out.println("2: " + constant + "\n");
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			
			for (int i = arity; i > 0; i--) {
				System.out.println("**" + i + ": " + operands[i - 1]);
			}
//			System.out.println("sup: " + operands[0].getClass());
//			System.out.println("sup: " + operands[1].getClass());
			
			
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class) && operation.getOperator().equals(Operation.Operator.EQ)) {
				IntConstant v = map.get(operands[0]);
				if (v == null) {
					IntConstant con = new IntConstant(Integer.parseInt(operands[1].toString()));
					Variable var = new IntVariable(operands[0].toString(), 0, 99999);
					map.put(var, con);
					System.out.println("3: " + var + " : " + con +  "\n");
					changeMade = true;
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class) && operation.getOperator().equals(Operation.Operator.EQ)) {
				IntConstant v = map.get(operands[1]);
				if (v == null) {
					IntConstant con = new IntConstant(Integer.parseInt(operands[0].toString()));
					Variable var = new IntVariable(operands[1].toString(), 0, 99999);
					map.put(var, con);
					System.out.println("4: " + var + " : " + con +  "\n");
					changeMade = true;
				}
			}
			
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntVariable.class)) {
				IntConstant v = map.get(operands[0]);
				if (v != null) {
					operands[0] = v;
					changeMade = true;
				}
				v = map.get(operands[1]);
				if (v != null) {
					operands[1] = v;
					changeMade = true;
				}
			}

			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntConstant.class)
					&& operation.getOperator().equals(Operation.Operator.EQ)) {

				Operation opp = (Operation) operands[0];
				switch (opp.getOperator()) {
				case EQ:
					// TODO
//						if (opp.getOperand(1).equals(operands[1])) {
//							operation = new Operation(Operation.Operator.EQ, opp.getOperand(0), opp.getOperand(1));
//							operands[0] = opp.getOperand(0);
//							operands[1] = opp.getOperand(1);
//						} else if (!opp.getOperand(1).equals(operands[1])) {
//							operation = new Operation(Operation.Operator.EQ, new IntConstant(0), new IntConstant(1));
//							operands[0] = new IntConstant(0);
//							operands[1] = new IntConstant(1);
//						}
					break;
				case NE:
					// TODO
					break;
				case LT:
					// TODO
					break;
				case LE:
					// TODO
					break;
				case GT:
					// TODO
					break;
				case GE:
					// TODO
					break;
				case ADD:
					if (opp.getOperand(0).getClass().equals(IntConstant.class)
							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
						// System.out.println(operands[1].getClass().toString() +
						// opp.getOperand(1).getClass());
						IntConstant const1 = (IntConstant) operands[1];
						IntConstant const2 = (IntConstant) opp.getOperand(0);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(1);
						operands[1] = new IntConstant(ans);
					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
						IntConstant const1 = (IntConstant) operands[1];
						IntConstant const2 = (IntConstant) opp.getOperand(1);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(0);
						operands[1] = new IntConstant(ans);
					}
					break;
				case SUB:
					// TODO
					break;
				case MUL:
					// TODO
					break;
				case NOT:
					// TODO
					break;
				default:
					break;
				}
				System.out.println("11: " + operands[0] + " : " + operands[1] + "\n");
				changeMade = true;
			} else if (operands[1].getClass().equals(Operation.class) && operands[0].getClass().equals(IntConstant.class)
					&& operation.getOperator().equals(Operation.Operator.EQ)) {
				Operation opp = (Operation) operands[1];
				switch (opp.getOperator()) {
				case EQ:
					// TODO
					break;
				case NE:
					// TODO
					break;
				case LT:
					// TODO
					break;
				case LE:
					// TODO
					break;
				case GT:
					// TODO
					break;
				case GE:
					// TODO
					break;
				case ADD:
					if (opp.getOperand(0).getClass().equals(IntConstant.class)
							&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
						// System.out.println(operands[1].getClass().toString() +
						// opp.getOperand(1).getClass());
						IntConstant const1 = (IntConstant) operands[0];
						IntConstant const2 = (IntConstant) opp.getOperand(0);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(1);
						operands[1] = new IntConstant(ans);
					} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
							&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
						IntConstant const1 = (IntConstant) operands[0];
						IntConstant const2 = (IntConstant) opp.getOperand(1);
						int ans = const1.getValue() - const2.getValue();
						operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
						operands[0] = opp.getOperand(0);
						operands[1] = new IntConstant(ans);
					}
					break;
				case SUB:
					// TODO
					break;
				case MUL:
					// TODO
					break;
				case NOT:
					// TODO
					break;
				default:
					break;
				}
			}
			System.out.println("5: " + operation.getOperator() + "\n");
			System.out.println("6: " + operands[0] + "\n");
			System.out.println("7: " + operands[1] + "\n");
			stack.push(new Operation(operation.getOperator(), operands));
		}
		
		

	}
	
	private static class OrderingVisitor extends Visitor {

		private Stack<Expression> stack;

		public OrderingVisitor() {
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
				if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					stack.push(new Operation(nop, r, l));
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					stack.push(new Operation(nop, r, l));
				} else {
					stack.push(operation);
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				stack.push(new Operation(op, l, r));
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
//				new TreeSet<Expression>();
				Expression c = null;
				for (Expression e : newConjuncts) {
					if (e.equals(Operation.FALSE)) {
						return Operation.FALSE;
					} else if (e instanceof Operation) {
						Operation o = (Operation) e;
						if (o.getOperator() == Operation.Operator.GT) {
							e = new Operation(Operation.Operator.LT, scale(-1,
									o.getOperand(0)), o.getOperand(1));
						} else if (o.getOperator() == Operation.Operator.GE) {
							e = new Operation(Operation.Operator.LE, scale(-1,
									o.getOperand(0)), o.getOperand(1));
						}
						o = (Operation) e;
						if (o.getOperator() == Operation.Operator.GT) {
							e = new Operation(Operation.Operator.GE, merge(
									o.getOperand(0), new IntConstant(-1)),
									o.getOperand(1));
						} else if (o.getOperator() == Operation.Operator.LT) {
							e = new Operation(Operation.Operator.LE, merge(
									o.getOperand(0), new IntConstant(1)),
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
		public void postVisit(Variable variable) {
			if (linearInteger && !unsatisfiable) {
				if (variable instanceof IntVariable) {
					variableSet.add((IntVariable) variable);
					stack.push(new Operation(Operation.Operator.MUL, Operation.ONE,
							variable));
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
					Operation term = new Operation(Operation.Operator.MUL,
							new IntConstant(coef), e.getKey());
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
				return new Operation(Operation.Operator.ADD, lr,
						new IntConstant(s));
			}
		}

		private boolean hasRightConstant(Expression expression) {
			return isAddition(expression)
					&& (getRightExpression(expression) instanceof IntConstant);
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
				return new IntConstant(factor
						* ((IntConstant) expression).getValue());
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
	
}
