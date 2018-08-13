package za.ac.sun.cs.green.service.simplify;



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;

public class ConstantPropagation extends BasicService {
	private int invocations = 0;
	private static Map<Expression, Expression> specialMap = new HashMap<>();
	static boolean changed = false;
	private static int ONE = 1;
	
	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Expression e = propagate(instance.getFullExpression());
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}
	
	/**
	 * Propagates variables from the given paramater expression and simplifies the expressions
	 * in order to try achieve the most simplified version. It continues to visit the expression until
	 * no changes have been detected.
	 * 
	 * @param expression
	 * 			The expression to propagate and simplify
	 * 
	 * @return exp
	 * 			The simplified expression
	 */
	public Expression propagate(Expression expression) {
		try {
			specialMap = new HashMap<>();
			ONE = 1;
			log.log(Level.FINEST, "Before Canonization: " + expression);
			invocations++;
			constantVisitor constantVisitor = new constantVisitor();
			expression.accept(constantVisitor);
			while (changed) {
				changed = false;
				constantVisitor.getExpression().accept(constantVisitor);
				//One extra iteration to account for boolean expressions like 3 < 5 to be simplified after variable propagation has occured.
				if (changed == false && ONE == 1) {
					ONE--;
					changed = true;
				}
			}
			Expression exp = constantVisitor.getExpression();
			log.log(Level.FINEST, "After Constant Propagation and Simplification: " + exp);
			return exp;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	/*
	 * Visits the expression, propagates variables and simplifies expressions until 
	 * there are no more changes
	 */
	private static class constantVisitor extends Visitor {

		private Stack<Expression> stack;

		public constantVisitor() {
			stack = new Stack<Expression>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void preVisit(Operation operation) throws VisitorException {
			//Checks if current expression is equality check
			if (operation.getOperator().equals(Operation.Operator.EQ)) {
				//Variable to constant
				if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntConstant) {
					//Replaces value if key already in map
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						//This catches x==2 && x==4 from infinitely looping. Does not rewrite already defined variables.
						if (!(specialMap.get(operation.getOperand(0)) instanceof IntConstant)) {
							specialMap.replace(operation.getOperand(0), operation.getOperand(1));
							changed = true;
						} 
					//Adds new key and value
					} else if (!specialMap.containsKey(operation.getOperand(0))) {
						changed = true;
						specialMap.put(operation.getOperand(0), operation.getOperand(1));
					}
				//Constant to variable
				} else if (operation.getOperand(0) instanceof IntConstant && operation.getOperand(1) instanceof IntVariable) {
					if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))){
						if (!(specialMap.get(operation.getOperand(1)) instanceof IntConstant)) {
							specialMap.replace(operation.getOperand(1), operation.getOperand(0));
							changed = true;
						} 
					} else if (!specialMap.containsKey(operation.getOperand(1))) {
						changed = true;
						specialMap.put(operation.getOperand(1), operation.getOperand(0));
					}
				//Variable to variable
				} else if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
					//Left variable contained in map
					if (specialMap.containsKey(operation.getOperand(0)) && !(specialMap.get(operation.getOperand(0)).equals(operation.getOperand(1)))) {
						specialMap.replace(operation.getOperand(0), operation.getOperand(1));
						changed = true;
					//Right variable contained in map
					} else if (specialMap.containsKey(operation.getOperand(1)) && !(specialMap.get(operation.getOperand(1)).equals(operation.getOperand(0)))) {
						specialMap.replace(operation.getOperand(1), operation.getOperand(0));
						changed = true;
					} else {
						//Left variable new entry in map
						if (!specialMap.containsKey(operation.getOperand(0))) {
							changed = true;
							specialMap.put(operation.getOperand(0), operation.getOperand(1));
						//Right variable new entry in map
						} else if (!specialMap.containsKey(operation.getOperand(1))) {
							changed = true;
							specialMap.put(operation.getOperand(1), operation.getOperand(0));
						} 
					} 
				}
			}
			//Propagates variables down the map, i.e. x = y, y = 2 would result in x = 2, y = 2. Reduces iterations required for visitor.
			if (operation.getOperand(0) instanceof IntVariable && operation.getOperand(1) instanceof IntVariable) {
				Object t[] = specialMap.keySet().toArray();
				for (int i = t.length-1; i >= 0; i--) {
					if (specialMap.containsKey(specialMap.get(t[i]))) {
						specialMap.replace((IntVariable) t[i], specialMap.get(specialMap.get(t[i])));
					}
				}	
			}	
		}

		@Override
		public void postVisit(IntConstant constant) throws VisitorException {
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
				IntConstant constant = null;
				IntVariable variable = null;
				Operation oper = null;
				//Equate function
				if (nop.equals(Operation.Operator.EQ)) {
					//left side is an operation, e.g. 2 + 2, 2 + x, x + y 
					if (l instanceof Operation) {
						//right side contains a variable
						if (r instanceof IntVariable) {
							//Replace variable with value if it exists in the map
							if (specialMap.containsKey(r)) {
								variable = (IntVariable) specialMap.get(r);
							} else {
								variable = (IntVariable) r;
							}
							//If both values on the left side are constants
							if (((Operation) l).getOperand(0) instanceof IntConstant && ((Operation) l).getOperand(1) instanceof IntConstant) {
								//Simplify the constant values
								if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() + ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() - ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() * ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant) ((Operation) l).getOperand(0)).getValue() / ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								}
							} 
							//Simplified peration to push to the stack
							oper = new Operation(Operation.Operator.EQ, constant, variable);
						//Right side is a constant
						} else if (r instanceof IntConstant) {
							int coeff = 1;
							if (((Operation) l).getOperand(0) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(1);
								//Coeff when moving variable
								if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
									constant = new IntConstant(coeff*(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
								} else if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(coeff*(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
								} else if (((Operation) l).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(coeff*(((IntConstant) r).getValue() / ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
								} else if (((Operation) l).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(coeff*(((IntConstant) r).getValue() * ((IntConstant) ((Operation) l).getOperand(0)).getValue()));
								}
							} else if (((Operation) l).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) l).getOperand(0);
								if (((Operation) l).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) r).getValue() - ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) r).getValue() + ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant)r).getValue() / ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								} else if (((Operation) l).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant)r).getValue() * ((IntConstant) ((Operation) l).getOperand(1)).getValue());
								}
							}
							oper = new Operation(Operation.Operator.EQ, variable, constant);
						}
						stack.push(oper);
					} else if (r instanceof Operation) {
						if (l instanceof IntVariable) {
							variable = (IntVariable) l;
							//Simplifies two constants to one
							if (((Operation) r).getOperand(0) instanceof IntConstant && ((Operation) r).getOperand(1) instanceof IntConstant) {
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() + ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() - ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() * ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant) ((Operation) r).getOperand(0)).getValue() / ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								}
								oper = new Operation(Operation.Operator.EQ, l, constant);
							} else {
								oper = new Operation(Operation.Operator.EQ, l, r);
							}
						} else if (l instanceof IntConstant) {
							if (((Operation) r).getOperand(0) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) r).getOperand(1);
								int coeff = 1;
								//Multiplies through by coefficient of the variable in order for the variable to remain positive
								if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									coeff = -1;
								}
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(coeff * ((IntConstant)l).getValue() - ((IntConstant) ((Operation) r).getOperand(0)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(coeff * ((IntConstant)l).getValue() + ((IntConstant) ((Operation) r).getOperand(0)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(coeff * ((IntConstant) l).getValue() / ((IntConstant) ((Operation) r).getOperand(0)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(coeff * ((IntConstant)l).getValue() * ((IntConstant) ((Operation) r).getOperand(0)).getValue());
								}
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else if (((Operation) r).getOperand(1) instanceof IntConstant) {
								variable = (IntVariable) ((Operation) r).getOperand(0);
								if (((Operation) r).getOperator().equals(Operation.Operator.ADD)) {
									constant = new IntConstant(((IntConstant)l).getValue() - ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
									constant = new IntConstant(((IntConstant)l).getValue() + ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.MUL)) {
									constant = new IntConstant(((IntConstant) l).getValue() / ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								} else if (((Operation) r).getOperator().equals(Operation.Operator.DIV)) {
									constant = new IntConstant(((IntConstant)l).getValue() * ((IntConstant) ((Operation) r).getOperand(1)).getValue());
								}
								oper = new Operation(Operation.Operator.EQ, constant, variable);
							} else {
								oper = operation;
							}
						}
						stack.push(oper);
					} else if (operation.equals(Operation.TRUE) || operation.equals(Operation.FALSE)) {
						stack.push(operation);
					} else {
						if (specialMap.containsKey(l) && l instanceof IntVariable) {
							//Example x==2 && x==4 pushes a false onto the stack
							if (!specialMap.get(l).equals(r) && r instanceof IntConstant) {
								stack.push(Operation.FALSE);
							} else if (l instanceof IntVariable) {
								stack.push(new Operation(nop, l, specialMap.get(l)));	
							} 
						} else if (specialMap.containsKey(l) && l instanceof IntVariable) {
							if (!specialMap.get(r).equals(l) && l instanceof IntConstant) {
								stack.push(Operation.FALSE);
							} else if (r instanceof IntVariable) {
								stack.push(new Operation(nop, r, specialMap.get(r)));	
							} 
						} else {
							stack.push(new Operation(nop, l, r));
						}
					}
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntVariable)
						&& (((IntVariable) r).getName().compareTo(
								((IntVariable) l).getName()) < 0)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(nop, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(nop, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(nop, specialMap.get(l), r));
					} else {
						stack.push(new Operation(nop, l, r));
					}
				} else if ((r instanceof IntVariable)
						&& (l instanceof IntConstant)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), r));
					} else {
						stack.push(new Operation(op, l, r));
					}
				} else if ((l instanceof IntVariable)
						&& (r instanceof IntConstant)) {
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), r));
					} else {
						stack.push(new Operation(op, l, r));
					}
				} else {
					if (l instanceof IntConstant && r instanceof IntConstant) {
						//Checks if expression is boolean
						switch (op) {
						case GE:
							if (((IntConstant)l).getValue() >= ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case GT:
							if (((IntConstant)l).getValue() > ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case LE:
							if (((IntConstant)l).getValue() <= ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case LT:
							if (((IntConstant)l).getValue() < ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						case NE:
							if (((IntConstant)l).getValue() != ((IntConstant)r).getValue()) {
								oper = (Operation) Operation.TRUE;
							} else {
								oper = (Operation) Operation.FALSE;
							}
							break;
						default:
							break;
						}
					} else if (l instanceof Operation) {
						if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntConstant) {
							switch (((Operation) l).getOperator()) {
							case ADD:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() + ((IntConstant)((Operation)l).getOperand(1)).getValue());
								if (specialMap.containsKey(r)) {
									oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
								} else {
									oper = new Operation(operation.getOperator(), constant, r);
								}
								break;
							case SUB:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() - ((IntConstant)((Operation)l).getOperand(1)).getValue());
								if (specialMap.containsKey(r)) {
									oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
								} else {
									oper = new Operation(operation.getOperator(), constant, r);
								}
								break;
							case MUL:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() * ((IntConstant)((Operation)l).getOperand(1)).getValue());
								if (specialMap.containsKey(r)) {
									oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
								} else {
									oper = new Operation(operation.getOperator(), constant, r);
								}
								break;
							case DIV:
								constant = new IntConstant(((IntConstant)((Operation)l).getOperand(0)).getValue() / ((IntConstant)((Operation)l).getOperand(1)).getValue());
								if (specialMap.containsKey(r)) {
									oper = new Operation(operation.getOperator(), constant, specialMap.get(r));
								} else {
									oper = new Operation(operation.getOperator(), constant, r);
								}
								break;
							default:
								oper = operation;
								break;
							}
							if (oper.getOperand(0) instanceof IntConstant && oper.getOperand(1) instanceof IntConstant) {
								switch (oper.getOperator()) {
								case GE:
									if (((IntConstant)oper.getOperand(0)).getValue() >= ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case GT:
									if (((IntConstant)oper.getOperand(0)).getValue() > ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case LE:
									if (((IntConstant)oper.getOperand(0)).getValue() <= ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case LT:
									if (((IntConstant)oper.getOperand(0)).getValue() < ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								case NE:
									if (((IntConstant)oper.getOperand(0)).getValue() != ((IntConstant)oper.getOperand(1)).getValue()) {
										oper = (Operation) Operation.TRUE;
									} else {
										oper = (Operation) Operation.FALSE;
									}
									break;
								default:
									break;
								}
							}
						} else if (((Operation)l).getOperand(0) instanceof IntConstant && ((Operation)l).getOperand(1) instanceof IntVariable) {
							int coeff = 1;
							if (((Operation) l).getOperator().equals(Operation.Operator.SUB)) {
								coeff = -1;
							} 
							if (l instanceof IntConstant) {
								constant = new IntConstant(coeff*(((IntConstant)r).getValue() - ((IntConstant)((Operation)l).getOperand(0)).getValue()));	
								oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
							} else {
								oper = operation;
							}
						} else {
							oper = operation;
						}
					} else if (r instanceof Operation) {
						if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntConstant) {
							switch (((Operation) r).getOperator()) {
							case ADD:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() + ((IntConstant)((Operation)r).getOperand(1)).getValue());
								if (specialMap.containsKey(l)) {
									oper = new Operation(operation.getOperator(), specialMap.get(l), constant);
								} else {
									oper = new Operation(operation.getOperator(), l, constant);
								}
								break;
							case SUB:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() - ((IntConstant)((Operation)r).getOperand(1)).getValue());
								if (specialMap.containsKey(l)) {
									oper = new Operation(operation.getOperator(), specialMap.get(l), constant);
								} else {
									oper = new Operation(operation.getOperator(), l, constant);
								}
								break;
							case MUL:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() * ((IntConstant)((Operation)r).getOperand(1)).getValue());
								if (specialMap.containsKey(l)) {
									oper = new Operation(operation.getOperator(), specialMap.get(l), constant);
								} else {
									oper = new Operation(operation.getOperator(), l, constant);
								}
								break;
							case DIV:
								constant = new IntConstant(((IntConstant)((Operation)r).getOperand(0)).getValue() / ((IntConstant)((Operation)r).getOperand(1)).getValue());
								if (specialMap.containsKey(l)) {
									oper = new Operation(operation.getOperator(), specialMap.get(l), constant);
								} else {
									oper = new Operation(operation.getOperator(), l, constant);
								}
								break;
							default:
								oper = operation;
								break;
							}
						} else if (((Operation)r).getOperand(0) instanceof IntConstant && ((Operation)r).getOperand(1) instanceof IntVariable) {
							int coeff = 1;
							if (((Operation) r).getOperator().equals(Operation.Operator.SUB)) {
								coeff = -1;
							} 
							if (l instanceof IntConstant) {
								constant = new IntConstant(coeff*(((IntConstant)l).getValue() - ((IntConstant)((Operation)r).getOperand(0)).getValue()));					
								oper = new Operation(operation.getOperator(), constant, ((Operation) r).getOperand(1));
							} else {
								oper = operation;
							}	
						} else {
							oper = (Operation) Operation.TRUE;
						}
					} 
					stack.push(oper);
				}
			} else if (op.getArity() == 2) {
				Expression r = stack.pop();
				Expression l = stack.pop();
				Expression shrink = null;
				if (r.equals(Operation.TRUE) || r.equals(Operation.FALSE) || l.equals(Operation.TRUE) || l.equals(Operation.FALSE)) {
					//If true statement, shrink to the left statement as the true does not affect it.
					if (r.equals(Operation.TRUE)) {
						shrink = l;
					//If false statement, shrink to false statement.
					} else if (r.equals(Operation.FALSE)) {
						shrink = r;
					} 
					if (l.equals(Operation.TRUE)) {
						shrink = r;
					} else if (l.equals(Operation.FALSE)) {
						shrink = l;
					}
					stack.push(shrink);
				} else {
					//Push new operations whilst checking for corresponding values in the map
					if (specialMap.containsKey(r) && specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l), specialMap.get(r)));
					} else if (specialMap.containsKey(r)) {
						stack.push(new Operation(op, l, specialMap.get(r)));
					} else if (specialMap.containsKey(l)) {
						stack.push(new Operation(op, specialMap.get(l) , r));
					} else {
						stack.push(new Operation(op, l, r));
					} 
				}
			} else {
				for (int i = op.getArity(); i > 0; i--) {
					stack.pop();
				}
				stack.push(operation);
			}
		}
	}
}
