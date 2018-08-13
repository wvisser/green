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
import za.ac.sun.cs.green.expr.Operation.Operator;
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

	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, IntConstant> map = new HashMap<Variable, IntConstant>();
			final Expression e = Propogate(instance.getFullExpression(), map);
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

	/**
	 * Handles exceptions, calls propagation function and logs the before and after expression results.
	 * @param   expression   The expression to be propagated and simplified
	 * @param   map   The map used to IntVariables to their corresponding IntConstants (eg, x:1)
	 */
	public Expression Propogate(Expression expression,
			Map<Variable, IntConstant> map) {
		try {
			log.log(Level.FINEST, "Before Propogation: " + expression);
			invocations++;
			Expression propogated = expression;
			propogated = GetSimplifiedExpression(map, propogated);
			log.log(Level.FINEST, "After Propogation: " + propogated);
			return propogated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}
	
	/**
	 * Calls the Renamer Visitor which handles the propagation of the Expression given as parameter.
	 * It then returns this 'simplified' or 'renamed' expression. 
	 * @param   expression   The expression to be propagated and simplified
	 * @param   map   The map used to IntVariables to their corresponding IntConstants (eg, x:1)
	 */
	private static Expression GetSimplifiedExpression(Map<Variable, IntConstant> map, Expression ex) throws VisitorException {
		boolean changeMade = false;
		boolean endExpressionFalse = false;
		if (ex != null) {
			Renamer re = new Renamer(map, changeMade);
			ex = re.rename(ex);
			changeMade = re.getChangeMade();
			endExpressionFalse = re.getEndExpressionFalse();
			while (changeMade == true) {
				changeMade = false;
				Renamer re2 = new Renamer(map, changeMade);
				ex = re2.rename(ex);
				changeMade = re2.getChangeMade();
				endExpressionFalse = re.getEndExpressionFalse();
			}
		}
		if (endExpressionFalse == true) {
			return Operation.FALSE;
		} else {
			return ex;
		}
	}
	
	/**
	 * This Visitor steps through the Expression and simplifies the expression so far as it can in one round of the 
	 * function call. 
	 * @param   expression   The expression to be propagated and simplified
	 * @param   map   The map used to IntVariables to their corresponding IntConstants (eg, x:1)
	 * @param   changeMade   flag indicating if any changes were made to the expression during execution of the function
	 * 						 or if none were and the simplification in its final form. 
	 */
	private static class Renamer extends Visitor {
		
		private Map<Variable, IntConstant> map;
		private boolean changeMade;
		private boolean endExpressionFalse = false;

		private Stack<Expression> stack;

		public Renamer(Map<Variable, IntConstant> map, boolean changeMade) {
			this.changeMade = changeMade;
			this.map = map;
			stack = new Stack<Expression>();
		}

		public Expression rename(Expression expression) throws VisitorException {
			expression.accept(this);
			return stack.pop();
		}
		
		public boolean getChangeMade() {
			return changeMade;
		}
		
		public boolean getEndExpressionFalse() {
			return endExpressionFalse;
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			boolean replacementMade = false;
			int arity = operation.getOperator().getArity();
			Expression operands[] = new Expression[arity];
			for (int i = arity; i > 0; i--) {
				operands[i - 1] = stack.pop();
			}
			
			/**
			 * Handles the situation where an operation contains an IntVariable on the left and an InstConstant on the right
			 */
			if (operands[0].getClass().equals(IntVariable.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					IntConstant v = map.get(operands[0]);
					if (v == null) {
						IntConstant con = new IntConstant(Integer.parseInt(operands[1].toString()));
						Variable var = new IntVariable(operands[0].toString(), 0, 99999);
						map.put(var, con);
						changeMade = true;
					} else {
						if (!v.equals(operands[1])) {
							endExpressionFalse = true;
						}
					}
					break;
				case NE:
					IntConstant vNE = map.get(operands[0]);
					if (vNE != null) {
						if (vNE.getValue() != (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case LT:
					IntConstant vLT = map.get(operands[0]);
					if (vLT != null) {
						if (vLT.getValue() < (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case LE:
					IntConstant vLe = map.get(operands[0]);
					if (vLe != null) {
						if (vLe.getValue() <= (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					}
					break;
				case GT:
					IntConstant vGT = map.get(operands[0]);
					if (vGT != null) {
						if (vGT.getValue() > (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case GE:
					IntConstant vGE = map.get(operands[0]);
					if (vGE != null) {
						if (vGE.getValue() >= (new IntConstant(Integer.parseInt(operands[1].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case ADD:
					IntConstant vADD = map.get(operands[0]);
					if (vADD != null) {
						operands[0] = vADD;
						changeMade = true;
					} 
					break;
				case SUB:
					IntConstant vSUB = map.get(operands[0]);
					if (vSUB != null) {
						operands[0] = vSUB;
						changeMade = true;
					}
					break;
				case MUL:
					IntConstant vMUL = map.get(operands[0]);
					if (vMUL != null) {
						operands[0] = vMUL;
						changeMade = true;
					}
					break;
				case DIV:
					IntConstant vDIV = map.get(operands[0]);
					if (vDIV != null) {
						operands[0] = vDIV;
						changeMade = true;
					}
					break;
				default:
					break;
				}
				
			}
			
			/**
			 * Handles the situation where an operation contains an IntVariable on the right and an InstConstant on the let
			 */
			if (operands[1].getClass().equals(IntVariable.class) && operands[0].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					IntConstant v = map.get(operands[1]);
					if (v == null) {
						IntConstant con = new IntConstant(Integer.parseInt(operands[0].toString()));
						Variable var = new IntVariable(operands[1].toString(), 0, 99999);
						map.put(var, con);
						changeMade = true;
					} 
					break;
				case NE:
					IntConstant vNE = map.get(operands[1]);
					if (vNE != null) {
						if (vNE.getValue() != (new IntConstant(Integer.parseInt(operands[0].toString()))).getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case LT:
					IntConstant vLT = map.get(operands[1]);
					if (vLT != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() < vLT.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case LE:
					IntConstant vLe = map.get(operands[1]);
					if (vLe != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() <= vLe.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					}
					break;
				case GT:
					IntConstant vGT = map.get(operands[1]);
					if (vGT != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() > vGT.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case GE:
					IntConstant vGE = map.get(operands[1]);
					if (vGE != null) {
						if ((new IntConstant(Integer.parseInt(operands[0].toString()))).getValue() >= vGE.getValue()) {
							stack.push(Operation.TRUE);
							replacementMade = true;
							changeMade = true;
						} else {
							stack.push(Operation.FALSE);
							replacementMade = true;
							changeMade = true;
						}
					} 
					break;
				case ADD:
					IntConstant vADD = map.get(operands[1]);
					if (vADD != null) {
						operands[1] = vADD;
						changeMade = true;
					} 
					break;
				case SUB:
					IntConstant vSUB = map.get(operands[1]);
					if (vSUB != null) {
						operands[1] = vSUB;
						changeMade = true;
					}
					break;
				case MUL:
					IntConstant vMUL = map.get(operands[1]);
					if (vMUL != null) {
						operands[1] = vMUL;
						changeMade = true;
					}
					break;
				case DIV:
					IntConstant vDIV = map.get(operands[1]);
					if (vDIV != null) {
						operands[1] = vDIV;
						changeMade = true;
					}
					break;
				default:
					break;
				}
			}
			
			/**
			 * Handles the situation where an operation contains an IntVariable on the left and the right
			 */
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
	
			/**
			 * Handles the situation where an operation contains an Operation on the left and an InstConstant on the right
			 */
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					Operation opp = (Operation) operands[0];
					switch (opp.getOperator()) {
					case ADD:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(1);
							IntVariable var = new IntVariable(opp.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(0);
							IntVariable var2 = new IntVariable(opp.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[0] = opp.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case NE:
					Operation oppNE = (Operation) operands[0];
					switch (oppNE.getOperator()) {
					case ADD:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(1);
							IntVariable var = new IntVariable(oppNE.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(0);
							IntVariable var2 = new IntVariable(oppNE.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[0] = oppNE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case LT:
					Operation oppLT = (Operation) operands[0];
					switch (oppLT.getOperator()) {
					case ADD:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case MUL:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[0] = oppLT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case LE:
					Operation oppLE = (Operation) operands[0];
					switch (oppLE.getOperator()) {
					case ADD:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							IntVariable var = new IntVariable(oppLE.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							IntVariable var2 = new IntVariable(oppLE.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[0] = oppLE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case GT:
					Operation oppGT = (Operation) operands[0];
					switch (oppGT.getOperator()) {
					case ADD:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							IntVariable var = new IntVariable(oppGT.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							IntVariable var2 = new IntVariable(oppGT.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[0] = oppGT.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case GE:
					Operation oppGE = (Operation) operands[0];
					switch (oppGE.getOperator()) {
					case ADD:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							IntVariable var = new IntVariable(oppGE.getOperand(1).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							IntVariable var2 = new IntVariable(oppGE.getOperand(0).toString(), 0, 99999);
							operands[1] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(1);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[0] = oppGE.getOperand(0);
							operands[1] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
				default:
					break;
				}
			}
			
			/**
			 * Handles the situation where an operation contains an Operation on the right and an InstConstant on the left
			 */
			if (operands[1].getClass().equals(Operation.class) && operands[0].getClass().equals(IntConstant.class)) {
				Operator operator = operation.getOperator();
				switch (operator) {
				case EQ:
					Operation opp = (Operation) operands[1];
					switch (opp.getOperator()) {
					case ADD:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(1);
							IntVariable var = new IntVariable(opp.getOperand(1).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(0);
							IntVariable var2 = new IntVariable(opp.getOperand(0).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (opp.getOperand(0).getClass().equals(IntConstant.class)
								&& opp.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) opp.getOperand(0);
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (opp.getOperand(1).getClass().equals(IntConstant.class)
								&& opp.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) opp.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, opp.getOperand(1), new IntConstant(ans));
							operands[1] = opp.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case NE:
					Operation oppNE = (Operation) operands[1];
					switch (oppNE.getOperator()) {
					case ADD:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(1);
							IntVariable var = new IntVariable(oppNE.getOperand(1).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(0);
							IntVariable var2 = new IntVariable(oppNE.getOperand(0).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppNE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppNE.getOperand(0);
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppNE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppNE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[1];
							IntConstant const2 = (IntConstant) oppNE.getOperand(1);
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppNE.getOperand(1), new IntConstant(ans));
							operands[1] = oppNE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
				case LT:
					Operation oppLT = (Operation) operands[0];
					switch (oppLT.getOperator()) {
					case ADD:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.LT, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case MUL:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppLT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLT.getOperand(1), new IntConstant(ans));
							operands[1] = oppLT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case LE:
					Operation oppLE = (Operation) operands[0];
					switch (oppLE.getOperator()) {
					case ADD:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(1);
							IntVariable var = new IntVariable(oppLE.getOperand(1).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(0);
							IntVariable var2 = new IntVariable(oppLE.getOperand(0).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppLE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppLE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppLE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppLE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.GE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppLE.getOperand(1), new IntConstant(ans));
							operands[1] = oppLE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case GT:
					Operation oppGT = (Operation) operands[0];
					switch (oppGT.getOperator()) {
					case ADD:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(1);
							IntVariable var = new IntVariable(oppGT.getOperand(1).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(0);
							IntVariable var2 = new IntVariable(oppGT.getOperand(0).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppGT.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGT.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGT.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGT.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LT, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGT.getOperand(1), new IntConstant(ans));
							operands[1] = oppGT.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
					break;
				case GE:
					Operation oppGE = (Operation) operands[0];
					switch (oppGE.getOperator()) {
					case ADD:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() - const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case SUB:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							int ans = const2.getValue() - const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(1);
							IntVariable var = new IntVariable(oppGE.getOperand(1).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var, new IntConstant(ans));
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							int ans = const1.getValue() + const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(0);
							IntVariable var2 = new IntVariable(oppGE.getOperand(0).toString(), 0, 99999);
							operands[0] = new IntConstant(ans);
							map.put(var2, new IntConstant(ans));
							changeMade = true;
						}
						break;
					case MUL:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					case DIV:
						if (oppGE.getOperand(0).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(1).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(0);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const2.getValue() / const1.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(1);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						} else if (oppGE.getOperand(1).getClass().equals(IntConstant.class)
								&& oppGE.getOperand(0).getClass().equals(IntVariable.class)) {
							IntConstant const1 = (IntConstant) operands[0];
							IntConstant const2 = (IntConstant) oppGE.getOperand(1);
							if (const2.getValue() < 0) {
								operation = new Operation(Operator.LE, operation.getOperand(0), operation.getOperand(1));
							}
							int ans = const1.getValue() / const2.getValue();
							operation = new Operation(Operation.Operator.EQ, oppGE.getOperand(1), new IntConstant(ans));
							operands[1] = oppGE.getOperand(0);
							operands[0] = new IntConstant(ans);
							changeMade = true;
						}
						break;
					default:
						break;
					}
				default:
					break;
				}
			}			
			
			/**
			 * Handles the situation where an operation contains an IntConstant on the left and the right
			 */
			if (operands[0].getClass().equals(IntConstant.class) && operands[1].getClass().equals(IntConstant.class)) {
				Operator opp = operation.getOperator();
				IntConstant constLeft = (IntConstant) operands[0];
				IntConstant constRight = (IntConstant) operands[1];
				switch (opp) {
				case EQ:
					if ((constLeft.getValue() == constRight.getValue()) && (constLeft.getValue() != 0)) {
						stack.push(Operation.TRUE);
						replacementMade = true;
					} else if (!operands[0].equals(operands[1])) {
						stack.push(Operation.FALSE);
						replacementMade = true;
					}
					break;
				case LT:
					if(constLeft.getValue() < constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case LE:
					if(constLeft.getValue() <= constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case GT:
					if(constLeft.getValue() > constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case GE:
					if(constLeft.getValue() >= constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case NE:
					if(constLeft.getValue() != constRight.getValue()) {
						stack.push(Operation.TRUE);
					} else {
						stack.push(Operation.FALSE);
					}
					replacementMade = true;
					break;
				case ADD:
					IntConstant operationToConstantADD = new IntConstant(constLeft.getValue() + constRight.getValue());
					stack.push(operationToConstantADD);
					replacementMade = true;
					break;
				case SUB:
					IntConstant operationToConstantSUB = new IntConstant(constLeft.getValue() - constRight.getValue());
					stack.push(operationToConstantSUB);
					replacementMade = true;
					break;
				case MUL:
					IntConstant operationToConstantMUL = new IntConstant(constLeft.getValue() * constRight.getValue());
					stack.push(operationToConstantMUL);
					replacementMade = true;
					break;
				case DIV:
					IntConstant operationToConstantDIV = new IntConstant(constLeft.getValue() / constRight.getValue());
					stack.push(operationToConstantDIV);
					replacementMade = true;
					break;	
				
				default:
					break;
				}
			}
			
			/**
			 * Handles the situation where an operation contains an Operation on the left and an InstVariable on the right
			 */
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(IntVariable.class)) {
				Expression left = GetSimplifiedExpression(map, operands[0]);
				Expression right = GetSimplifiedExpression(map, operands[1]);	
				operands[0] = left;
				operands[1] = right;
			}
			
			/**
			 * Handles the situation where an operation contains an Operation on the right and an InstVariable on the left
			 */
			if (operands[1].getClass().equals(Operation.class) && operands[0].getClass().equals(IntVariable.class)) {
				Expression left = GetSimplifiedExpression(map, operands[0]);
				Expression right = GetSimplifiedExpression(map, operands[1]);	
				operands[0] = left;
				operands[1] = right;
			}
			
			/**
			 * Handles the situation where an operation contains an Operation on the right and on the left
			 */
			if (operands[0].getClass().equals(Operation.class) && operands[1].getClass().equals(Operation.class)) {
				if (operands[0].equals(Operation.TRUE) && !operands[1].equals(Operation.TRUE) && operation.getOperator().equals(Operator.AND)) {
					Operation newopp = (Operation) operands[1];
					operands[0] = newopp.getOperand(0);
					operands[1] = newopp.getOperand(1);
					operation = new Operation(newopp.getOperator(), operation.getOperand(0), operation.getOperand(1));
					changeMade = true;
				} else if (operands[1].equals(Operation.TRUE) && !operands[0].equals(Operation.TRUE) && operation.getOperator().equals(Operator.AND)) {
					Operation newopp = (Operation) operands[0];
					operands[0] = newopp.getOperand(0);
					operands[1] = newopp.getOperand(1);
					operation = new Operation(newopp.getOperator(), operation.getOperand(0), operation.getOperand(1));
					changeMade = true;
				}
			}
		
			if (replacementMade == false) {
				stack.push(new Operation(operation.getOperator(), operands));
			}
		}
	}
}
