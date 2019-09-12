package za.ac.sun.cs.green.service.grulia.gruliastore;

import java.io.Serializable;

/**
 * @author JH Taljaard (USnr 18509193)
 * @author Willem Visser (Supervisor)
 * @author Jaco Geldenhuys (Supervisor)
 * <p>
 * Description:
 * Parent class for Entry stored in the Repository.
 */
public abstract class Entry implements Comparable<Entry>, Serializable {

	/**
	 * The SatDelta value of this entry.
	 */
	private final double satDelta;

	/**
	 * String representation for this entry.
	 */
	private String stringRepresentation = null;

	/**
	 * The size of the solution stored in this entry.
	 */
	private final int size;
	/**
	 * Construct an entry.
	 *
	 * @param satDelta SatDelta value for the new entry
	 */
	public Entry(final double satDelta, final int size) {
		this.satDelta = satDelta;
		this.size = size;
	}

	/**
	 * Return the SatDelta value for this entry.
	 *
	 * @return the entry's SatDelta value
	 */
	public double getSatDelta() {
		return satDelta;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Entry entry) {
		// first sort according to satDelta
		int satDeltaCompare = Double.compare(getSatDelta(), entry.getSatDelta());
		if (satDeltaCompare == 0) {
			// if same satDelta: compare solution size
			int sizeCompare = Integer.compare(this.getSize(), entry.getSize());
			// if same size: compare the string
			return (sizeCompare == 0) ? toString().compareTo(entry.toString()) : sizeCompare;
		} else {
			// sort satDelta in ascending order
			return satDeltaCompare;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Entry)) {
			return false;
		}

		if (this.getSatDelta() == ((Entry) o).getSatDelta()) {
			return this.toString().equals(((Entry) o).toString());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public final String toString() {
		if (stringRepresentation == null) {
			StringBuilder s = new StringBuilder();
			s.append("(satDelta=").append(getSatDelta());
			s.append(", size=").append(getSize());
			s.append(", ").append(toString0());
			s.append(')');
			stringRepresentation = s.toString();
		}
		return stringRepresentation;
	}

	/**
	 * Return a string representation for this entry.
	 *
	 * @return a string representation for this entrty
	 */
	public abstract String toString0();

	public abstract boolean isValidFor(Entry entry);

	public abstract double distanceTo(Entry entry);

	public int getSize() {
		return size;
	}

}
