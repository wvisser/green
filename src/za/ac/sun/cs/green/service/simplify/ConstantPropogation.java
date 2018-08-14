package za.sun.ac.cs.green.src.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
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
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropogation extends BasicService {

	private int invocations = 0;
 	public ConstantPropogation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
	@SuppressWarnings("unchecked")
		Set<Instance> setResults = (Set<Instance>) instance.getData(getClass());
		if (setResults==null) {
		    final Map<Variable, Variable> varMap = new HashMap<Variable, Variable>();
           	    final Expression expr = simplify(instance.getFullExpression(), varMap);
           	    final Instance ins = new Instance(getSolver(), instance.getSource(), null, expr);
          	    setResults = Collections.singleton(ins);
          	    instance.setData(getClass(), setResults);
		}
		return setResults;
	}


	@Override
        public void report(Reporter reporter) {
        reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
        }
	public Expression simplify(Expression expression, Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Pre-simplification operation: " + expression);
			invocations++;
			SimplifyVisitor simplifyVisitor = new SimplifyVisitor();
			expression.accept(simplifyVisitor);
			expression = simplifyVisitor.getExpression();
			log.log(Level.FINEST, "Post-simplification operation: " + expression);
		} catch (VisitorException x) {
			log.log(Level.SEVERE, "Error visitor exception encountered", x);
		} finally {
			return expression;
		}
	}

	public class SimplifyVisitor extends Visitor {
		private Map<IntVariable, IntConstant> map;
		private Stack<Expression> Exprstack;

		public SimplifyVisitor() {
			Exprstack = new Stack<Expression>();
			map = new TreeMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			return Exprstack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			Exprstack.push(constant);
		}

		@Override
		public void preVisit(Operation operation) {
			Operation.Operator opr = operation.getOperator();
			if (opr.equals(Operation.Operator.EQ)) {
				Expression opExpr = operation.getOperand(0);
				Expression opR = operation.getOperand(1);
				if ((opExpr instanceof IntVariable) && (opR instanceof IntConstant)) {
					map.put((IntVariable) opExpr, (IntConstant) opR);
				} else if ((opExpr instanceof IntConstant) && (opR instanceof IntVariable)) {
					map.put((IntVariable) opR, (IntConstant) opExpr);
				}
			}
		}

		@Override
		public void postVisit(IntVariable variable) {
			Exprstack.push(variable);
		}

		@Override
		public void postVisit(Operation operation) {
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
							Expression r = Exprstack.pop();
							Expression l = Exprstack.pop();
							if ((r instanceof IntVariable)
									&& (l instanceof IntVariable)
									&& (((IntVariable) r).getName().compareTo(
											((IntVariable) l).getName()) < 0)) {
								Exprstack.push(new Operation(nop, r, l));
							} else if ((r instanceof IntVariable)
									&& (l instanceof IntConstant)) {
								Exprstack.push(new Operation(nop, r, l));
							} else {
								Exprstack.push(operation);
							}
						} else if (op.getArity() == 2) {
							Expression r = Exprstack.pop();
							Expression l = Exprstack.pop();
							Exprstack.push(new Operation(op, l, r));
						} else {
							for (int i = op.getArity(); i > 0; i--) {
								Exprstack.pop();
							}
							Exprstack.push(operation);
			}

		}

	}

}
}
