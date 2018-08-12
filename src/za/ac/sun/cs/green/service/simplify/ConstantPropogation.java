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
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;

public class ConstantPropagation extends BasicService {


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
			final Expression e = propogateConstants(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(RENAME, reverseMap);
		}
		return result;
	}

	@Override
	public void report(Reporter reporter) {
			reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}

	public Expression propagateConstants(Expression expr, Map<Variable, Variable> map) {
			 try {
					 log.log(Level.FINEST, "Before Constant Propagation: " + expr);
					 invocations++;

					 constPropV cPv = new constPropV();
					 expr.accept(cPv);
					 Expression prop = cPv.getExpression();

					 log.log(Level.FINEST, "After Constant Propagation: " + prop);
					 return prop;
			 } catch (VisitorException ex) {
					 log.log(Level.SEVERE, "Something is not right.", ex);
			 }

			 return null;
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

 }
