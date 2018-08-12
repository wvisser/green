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
}
