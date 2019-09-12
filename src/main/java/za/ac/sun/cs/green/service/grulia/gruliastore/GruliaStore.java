package za.ac.sun.cs.green.service.grulia.gruliastore;

import java.util.List;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * Storage interface for Grulia store.
 */
public interface GruliaStore<E extends Entry> {

	void add(E entry);

	List<E> getEntries();

	int size();

	List<E> extract(E anchor, int k);

	void flushAll();

	void clear();

	String getKey();

}
