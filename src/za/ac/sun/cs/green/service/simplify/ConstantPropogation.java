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
import za.ac.sun.cs.green.expr.Operation

public class ConstansPropogation extends BasicService {

	private static final String RENAME = "RENAME";
	
	public ModelCanonizerService(Green solver) {
		super(solver);
	}

	private Map<Variable, Variable> reverseMap(Map<Variable, Variable> map) {
		Map<Variable, Variable> revMap = new HashMap<Variable, Variable>();
		for (Map.Entry<Variable,Variable> m : map.entrySet()) {
			revMap.put(m.getValue(), m.getKey());
		}
		return revMap;
	}
	
	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = canonize(instance.getFullExpression(), map);
			Map<Variable, Variable> reverseMap = reverseMap(map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
			instance.setData(RENAME, reverseMap);
		}
		return result;
	}

	@Override
	public Object childDone(Instance instance, Service subService,
			Instance subInstance, Object result) {
		@SuppressWarnings("unchecked")
		HashMap<Variable,Object> r = (HashMap<Variable,Object>)result;
		if (r == null) {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		HashMap<Variable, Variable> reverseMap = (HashMap<Variable, Variable>)instance.getData(RENAME);

		HashMap<Variable,Object> newResult = new HashMap<Variable,Object>();
		for (Map.Entry<Variable,Object> m : r.entrySet()) {
			newResult.put(reverseMap.get(m.getKey()), m.getValue());
		}
		return newResult;
	}
	
}
