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

public class ConstantPropogation extends BasicService {


    private int invocations = 0;

    public ConstantPropogation(Green solver) {
        super(solver);
    }

	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagateConstants(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			final Instance f = new Instance(getSolver(), instance.getSource(), null , e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
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
					 cPvisitor cPv = new cPvisitor();
				 	 cPvisitor cpVVtest = new cPvisitor();
					 expr.accept(cPv);
				 	 Expression propTest;
					 Expression prop = cPv.getExpression();
					 log.log(Level.FINEST, "After Constant Propagation: " + prop);
					 return prop;
			 } catch (VisitorException ex) {
					 log.log(Level.SEVERE, "Something is not right.", ex);
			 }

			 return null;
	 }



	 private static class cPvisitor extends Visitor {
		 		private Stack<Expression> stack;
		 		private Stack<Expression> stackyMcStackStack;
		 		private Map<IntVariable, IntConstant> Vmap;
				private Map<IntVariable , IntConstant> mappy;
		 
 				public cPvisitor() {
 					 stack = new Stack<Expression>();
					 stackyMcStackStack = new Stack<Expression>();
					 Vmap = new TreeMap<IntVariable, IntConstant>();
					 mappy = new TreeMap<IntVariable , IntConstant>();
 				}

 		public Expression getExpression() {
			Expression ex = null;
			if(stack.isEmpty()){
						return null;

					}else{
						ex = stack.pop();
						
					}
		 				return ex;
 		}

 		@Override
 		public void postVisit(Constant cons) {
			if(cons instanceof IntConstant){
				stack.push(cons);
				stackyMcStackStack.push(cons);
			} else {
				
					stack.clear();
					stackyMcStackStack.clear();
				}

			
 		}

 		@Override
 		public void postVisit(Variable var) {
			if(var instanceof IntVariable){
				stack.push(var);
				stackyMcStackStack.push(var);
			} else {
					stack.clear();
					stackyMcStackStack.clear();
				}

			
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
				if(stackyMcStackStack.size()>=2){
					Expression rR = stackyMcStackStack.pop();
					Expression lL = stackyMcStackStack.pop();	
					   if((lL instanceof IntVariable) && (rR instanceof IntConstant)){
					  mappy.put((IntVariable)lL ,(IntConstant) rR);
				      }else if((rR instanceof IntVariable) && (lL instanceof IntConstant)){
					  mappy.put((IntVariable)rR,(IntConstant)lL);
				      }
	 			 }
			}
	

      if(stack.size()>=2){
          Expression r = stack.pop();
	  Expression l = stack.pop();
	  Operation.Operator oper = op;
          if(oper.equals(Operation.Operator.EQ)){
              if((l instanceof IntVariable) && (r instanceof IntConstant)){
                  Vmap.put((IntVariable)l ,(IntConstant) r);
              }else if((r instanceof IntVariable) && (l instanceof IntConstant)){
                  Vmap.put((IntVariable)r,(IntConstant)l);
              }
              stack.push(new Operation(oper,l,r));
            } else if(!oper.equals(Operation.Operator.EQ)){
		  
                  if (Vmap.containsKey(l)) {
                      l = Vmap.get(l);
                  } else if(Vmap.containsKey(r)){
                      r = Vmap.get(r);
                  }
                  stack.push(new Operation(oper , l , r));
            }
          }
      }
      //return null;
 		}

 	}

 
