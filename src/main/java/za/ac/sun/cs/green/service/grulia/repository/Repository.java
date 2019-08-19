package za.ac.sun.cs.green.service.grulia.repository;

import java.util.List;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * Repository interface for Grulia store.
 */
public interface Repository<E extends Entry> {

	void add(E entry);

	List<E> getEntries();

	int size();

	List<E> extract(E anchor, int k);

	void flushAll();

	void clear();

	String getKey();

}
