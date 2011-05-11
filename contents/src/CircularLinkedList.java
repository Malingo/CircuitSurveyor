/*
 * CircuitSurveyor is released to the public under the terms of the GNU General public license, version
 * 3. There is no warranty. For the full terms, see the LICENSE.txt file included in this distribution.
 */

import java.util.Iterator;

/**
 * A doubly-linked list in which the last node points back to the first.
 * 
 * @author Noah Morris
 * @param <T>
 *            the class of elements of which this is a list
 */
public class CircularLinkedList<T> implements Iterable<T> {

	/* *************** DATA MEMBERS *************** */

	private int size;
	private Node<T> tail;

	/* *************** CONSTRUCTOR *************** */

	/**
	 * Constructs an empty circular linked-list.
	 */
	public CircularLinkedList() {
		size = 0;
		tail = null;
	}

	/* *************** PUBLIC METHODS *************** */

	/**
	 * Returns the size of this list.
	 * 
	 * @return the number of elements in the list.
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns whether this list is empty.
	 * 
	 * @return true if this list contains no elements, false otherwise
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Adds the specified element to the list, directly after the most recently added element.
	 * 
	 * @param data
	 *            the element to be added to the list
	 * @return true (per the contract of <code>Collection.add()</code>)
	 */
	public boolean add(T data) {
		if (data == null)
			throw new NullPointerException();
		Node<T> node = new Node<T>(data);
		if (isEmpty()) {
			tail = node;
			node.next = node;
			node.prev = node;
		} else {
			node.prev = tail;
			node.next = tail.next;
			tail = node;
			tail.next.prev = tail;
			tail.prev.next = tail;
		}
		size++;
		return true;
	}

	/**
	 * Returns the first element that was added to the list.
	 * 
	 * @return the first element of the list
	 */
	public T getFirst() {
		if (size > 0)
			return tail.next.data;
		else return null;
	}

	/**
	 * Returns an Iterator over the elements of the list.
	 * 
	 * @return an Iterator over the elements of the list, beginning with the first to be added and
	 *         iterating in the order the elements were added
	 */
	public Iterator<T> iterator() {
		return iterator(tail.next.data, true);
	}

	/**
	 * Returns an Iterator over the elements of the list, starting in the specified spot.
	 * 
	 * @param start
	 *            the element at which to begin iterating
	 * @return an Iterator over the elements of the list, beginning with <code>start</code> and
	 *         iterating in the order the elements were added (looping around if necessary from the
	 *         last to the first)
	 */
	public Iterator<T> iterator(final T start, boolean forward) {
		if (forward)
			return iteratorForward(start);
		else return iteratorBackward(start);
	}

	private Iterator<T> iteratorForward(final T start) {
		return new Iterator<T>() {

			boolean isStart = true;
			Node<T> head = find(start);
			Node<T> node = head.prev;

			public boolean hasNext() {
				return node.next != head || isStart;
			}

			public T next() {
				isStart = false;
				node = node.next;
				return node.data;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	private Iterator<T> iteratorBackward(final T start) {
		return new Iterator<T>() {

			boolean isStart = true;
			Node<T> head = find(start);
			Node<T> node = head.next;

			public boolean hasNext() {
				return node.prev != head || isStart;
			}

			public T next() {
				isStart = false;
				node = node.prev;
				return node.data;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override
	public String toString() {
		String str = "[";
		if (!isEmpty()) {
			Node<T> node = tail.next;
			do {
				str += node.toString();
				node = node.next;
			} while (node != tail.next);
		}
		return str + "]";
	}

	/* *************** PRIVATE METHODS *************** */

	/**
	 * Find the specified element in the list, and return the node to which it belongs.
	 * 
	 * @param data
	 *            the element to find in the list
	 * @return the node in the list that <code>data</code> belongs to
	 */
	private Node<T> find(T data) {
		Node<T> node = tail;
		do {
			if (node.data.equals(data))
				return node;
			node = node.next;
		} while (node != tail);
		return null;
	}

	/* *************** HELPER CLASS *************** */

	/**
	 * A node for use within a linked list.
	 * 
	 * @author Noah Morris
	 * @param <E>
	 *            the class of the element this node houses
	 */
	private class Node<E> {
		E data;
		Node<E> next;
		Node<E> prev;

		/**
		 * Constructs a new node out of the specified data
		 * 
		 * @param data
		 */
		public Node(E data) {
			this.data = data;
		}

		@Override
		public String toString() {
			return "(" + data.toString() + ")";
		}
	}
}