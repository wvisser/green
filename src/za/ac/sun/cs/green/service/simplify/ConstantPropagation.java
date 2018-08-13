package za.ac.sun.cs.green.service.simplify;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.service.canonizer.SATCanonizerService;

import java.util.*;

public class ConstantPropagation extends BasicService {

    public ConstantPropagation(Green solver) {
        super(solver);
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

    /*
     * This function creates the PropagationVisitor object and
     * has the expression accept it. The purpose of the do-while is to continuously
     * re-propagate the expression until it can no longer be further propagated, at which point
     * it is returned to processRequest
     *
     * @param expression: The expresssion to be propagated
     */
    public Expression propagate(Expression expression) {
        try {
            PropagationVisitor propagationVisitor= new PropagationVisitor();
            expression.accept(propagationVisitor);
            expression = propagationVisitor.getExpression();
            Expression old_expression = null;
            do {
                old_expression = expression;
                expression.accept(propagationVisitor);
                expression = propagationVisitor.getExpression();
            } while(!old_expression.equals(expression));
            return expression;
        } catch (VisitorException x) {
            System.out.println(x);
        }
        return null;
    }

    /*
     * PropagationVisitor pushes constants and variables to the stack
     * When it encounters an operation, it pops the two expressions from the top of the stack,
     * checks the operator and operands and propagates the expression accordingly
     */
    private static class PropagationVisitor extends Visitor {

        // HashMap that stores the variable names and their corresponding values
        private HashMap<String, IntConstant> constantValueMap;

        // Stack for storing and retrieving expressions
        private Stack<Expression> stack;

        public PropagationVisitor() {
            constantValueMap = new HashMap<>();
            stack = new Stack<>();
        }

        /*
         * Returns the expression at the top of the stack
         */
        public Expression getExpression() {
            return stack.pop();
        }

        @Override
        /*
         * Pushes variable to the top of the stack
         */
        public void postVisit(Variable variable) {
            stack.push(variable);
        }

        @Override
        /*
         * Pushes constant to the top of the stack
         */
        public void postVisit(Constant constant) {
            stack.push(constant);
        }

        @Override
        /*
         * Propagates operations by examining the operator and both operands
         * If the operator is "==" and one of the operands is a variable
         * and the other a constant, the variable and value are put into the
         * HashMap. If that variable is accessed in future operations/propagations, the variable name
         * will be replaced by the value associated with it in the HashMap, and the new value will be pushed
         * to the stack instead of the variable name
         */
        public void postVisit(Operation operation) {
            Operation.Operator op = operation.getOperator();

            if (op.getArity() == 2) {
                // Left and right expressions are popped from the top of the stack
                Expression l = stack.pop();
                Expression r = stack.pop();

                if(op.name().equals("EQ")) {
                    // Checks if one of the operands is a variable and the other a constant
                    if (l instanceof IntVariable && r instanceof IntConstant) {
                        constantValueMap.put(((IntVariable) l).getName(), (IntConstant) r);
                    } else if (r instanceof  IntVariable && l instanceof IntConstant) {
                        constantValueMap.put(((IntVariable) r).getName(), (IntConstant) l);
                    }
                } else {
                    // Checks if the variable is present in the HashMap. If it is, replace the variable with its
                    // associated value
                    if (l instanceof IntVariable) {
                        if (constantValueMap.containsKey(((IntVariable) l).getName())) {
                            l = constantValueMap.get(((IntVariable) l).getName());
                        }
                    } if (r instanceof IntVariable) {
                        if (constantValueMap.containsKey(((IntVariable) r).getName())) {
                            r = constantValueMap.get(((IntVariable) r).getName());
                        }
                    }
                }
                // Push the new left and right operands as a new operation to the stack
                stack.push(new Operation(op, r, l));
            }

        }

    }
}