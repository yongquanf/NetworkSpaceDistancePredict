/*
 * Copyright (C) 2003-2005 Northwestern University
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package edu.NUDT.PDL.util;


/**
 * Implementation of a pair.
 *
 * @author Stefan Birrer
 *
 * @param <K> the first object type
 * @param <V> the second object type
 *
 */
public class Pair<K extends Comparable<K>, V> {
    /** The first object. */
    private final K key;

    /** The second object. */
    private V value;

    /**
     * Creates a new Pair.
     *
     * @param key the first object
     * @param value the second object
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Get the first object.
     *
     * @return the first object
     */
    public K getKey() {
        return key;
    }

    /**
     * Get the second object.
     *
     * @return the second object
     */
    public V getValue() {
        return value;
    }

    /**
     * Set the second object.
     *
     * @param value the new value
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the Object to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }

        Pair<K, V> p = (Pair<K, V>) o;

        return ((key == null) && (p == null)) ||
        (((key != null) && key.equals(p.getKey())) &&
        ((p.getValue() == null) && (o == null))) ||
        ((p.getValue() != null) && value.equals(p.getValue()));
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object
     */
    public int hashCode() {
        return (key != null) ? key.hashCode() : 0;
    }

    /**
     * Compares this object with the specified object for order.  Returns
     * a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     *
     * <p>
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the
     * mathematical <i>signum</i> function, which is defined to return
     * one of <tt>-1</tt>, <tt>0</tt>, or <tt>1</tt> according to
     * whether the value of <i>expression</i> is negative, zero or
     * positive. The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.
     * (This implies that <tt>x.compareTo(y)</tt> must throw an
     * exception iff <tt>y.compareTo(x)</tt> throws an exception.)
     * </p>
     *
     * <p>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt>
     * implies <tt>x.compareTo(z)&gt;0</tt>.
     * </p>
     *
     * <p>
     * Finally, the implementer must ensure that
     * <tt>x.compareTo(y)==0</tt> implies that <tt>sgn(x.compareTo(z))
     * == sgn(y.compareTo(z))</tt>, for all <tt>z</tt>.
     * </p>
     *
     * <p>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally
     * speaking, any class that implements the <tt>Comparable</tt>
     * interface and violates this condition should clearly indicate
     * this fact.  The recommended language is "Note: this class has a
     * natural ordering that is inconsistent with equals."
     * </p>
     *
     * @param o the Object to be compared.
     *
     * @return a negative integer, zero, or a positive integer as this
     *         object     is less than, equal to, or greater than the
     *         specified object.
     *
     * @throws ClassCastException if the specified object's type prevents
     *         it from being compared to this Object.
     */
    public int compareTo(K o) {
        return key.compareTo(o);
    }

    /**
     * Creates a human readable string representation of the object.
     *
     * @return the string representation
     */
    public String toString() {
        return "<" + key + ":" + value + ">";
    }
}
