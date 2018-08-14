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
import java.util.ArrayList;

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

	public ConstantPropagation(Green solver) {
		super(solver);
	}

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}

	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
	
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			/*CanonizationVisitor canonizationVisitor = new CanonizationVisitor();
			expression.accept(canonizationVisitor);
			Expression canonized = canonizationVisitor.getExpression();*/
			
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {

        	private Stack<Expression> stack;
        	private HashMap<Variable, IntConstant> h;


	        public OrderingVisitor() {
	            stack = new Stack<Expression>();
	            h = new HashMap< Variable, IntConstant > ();
	        }

	
	        public Expression getExpression() {
	            return stack.pop();
	        } 
	        

	        @Override
	        public void postVisit(Operation o) throws VisitorException {
	            Operation.Operator op = o.getOperator();

        	    if (op.getArity() == 2) {
        	        Expression r = stack.pop();
                	Expression l = stack.pop();
                	if ((r instanceof IntVariable) && (l instanceof IntConstant) && op == Operation.Operator.EQ) {
                	    	h.put((Variable) r, (IntConstant) l);
                	} else if ((r instanceof IntConstant) && (l instanceof IntVariable) && op == Operation.Operator.EQ) {
                	    	h.put((Variable) l, (IntConstant) r);
                	} else if (l instanceof IntVariable && h.containsKey(l)) {
                	  	l = h.get(l);
                	} else if (l instanceof IntVariable&& h.containsKey(r)) {
                		r = h.get(l);
                	}
                		stack.push(new Operation(op, l, r));
        	    	}
        	}

		@Override
	        public void postVisit(IntConstant cons) {
	            stack.push(cons);
	        }

		@Override
		public void postVisit(IntVariable var) {
			stack.push(var);
		}
    	}

	


}
