/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulation of a list of items to be processed and possibly a list of failed items to
 * be skipped. To mark an item as skipped clients should iterate over the chunk using the
 * {@link #iterator()} method, and if there is a failure call
 * {@link Chunk.ChunkIterator#remove()} on the iterator. The skipped items are then
 * available through the chunk.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class Chunk<W> implements Iterable<W>, Serializable {

	private List<W> items = new ArrayList<>();

	private List<SkipWrapper<W>> skips = new ArrayList<>();

	private List<Exception> errors = new ArrayList<>();

	private Object userData;

	private boolean end;

	private boolean busy;

	public Chunk(W... items) {
		this(Arrays.stream(items).toList());
	}

	public static <W> Chunk<W> of(W... items) {
		return new Chunk<>(items);
	}

	public Chunk(List<? extends W> items) {
		this(items, null);
	}

	public Chunk(List<? extends W> items, List<SkipWrapper<W>> skips) {
		super();
		if (items != null) {
			this.items = new ArrayList<>(items);
		}
		if (skips != null) {
			this.skips = new ArrayList<>(skips);
		}
	}

	/**
	 * Add the item to the chunk.
	 * @param item the item to add
	 */
	public void add(W item) {
		items.add(item);
	}

	/**
	 * Add all items to the chunk.
	 * @param items the items to add
	 */
	public void addAll(List<W> items) {
		this.items.addAll(items);
	}

	/**
	 * Clear the items down to signal that we are done.
	 */
	public void clear() {
		items.clear();
		skips.clear();
		userData = null;
	}

	/**
	 * @return a copy of the items to be processed as an unmodifiable list
	 */
	public List<W> getItems() {
		return Collections.unmodifiableList(new ArrayList<>(items));
	}

	/**
	 * @return a copy of the skips as an unmodifiable list
	 */
	public List<SkipWrapper<W>> getSkips() {
		return Collections.unmodifiableList(skips);
	}

	/**
	 * @return a copy of the anonymous errors as an unmodifiable list
	 */
	public List<Exception> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	/**
	 * Register an anonymous skip. To skip an individual item, use
	 * {@link ChunkIterator#remove()}.
	 * @param e the exception that caused the skip
	 */
	public void skip(Exception e) {
		errors.add(e);
	}

	/**
	 * @return true if there are no items in the chunk
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * Get an unmodifiable iterator for the underlying items.
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public ChunkIterator iterator() {
		return new ChunkIterator(items);
	}

	/**
	 * @return the number of items (excluding skips)
	 */
	public int size() {
		return items.size();
	}

	/**
	 * Flag to indicate if the source data is exhausted.
	 * @return true if there is no more data to process
	 */
	public boolean isEnd() {
		return end;
	}

	/**
	 * Set the flag to say that this chunk represents an end of stream (there is no more
	 * data to process).
	 */
	public void setEnd() {
		this.end = true;
	}

	/**
	 * Query the chunk to see if anyone has registered an interest in keeping a reference
	 * to it.
	 * @return the busy flag
	 */
	public boolean isBusy() {
		return busy;
	}

	/**
	 * Register an interest in the chunk to prevent it from being cleaned up before the
	 * flag is reset to false.
	 * @param busy the flag to set
	 */
	public void setBusy(boolean busy) {
		this.busy = busy;
	}

	/**
	 * Clear only the skips list.
	 */
	public void clearSkips() {
		skips.clear();
	}

	public Object getUserData() {
		return userData;
	}

	public void setUserData(Object userData) {
		this.userData = userData;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("[items=%s, skips=%s]", items, skips);
	}

	/**
	 * Special iterator for a chunk providing the {@link #remove(Throwable)} method for
	 * dynamically removing an item and adding it to the skips.
	 *
	 * @author Dave Syer
	 *
	 */
	public class ChunkIterator implements Iterator<W> {

		final private Iterator<W> iterator;

		private W next;

		public ChunkIterator(List<W> items) {
			iterator = items.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public W next() {
			next = iterator.next();
			return next;
		}

		public void remove(Throwable e) {
			remove();
			skips.add(new SkipWrapper<>(next, e));
		}

		@Override
		public void remove() {
			if (next == null) {
				if (iterator.hasNext()) {
					next = iterator.next();
				}
				else {
					return;
				}
			}
			iterator.remove();
		}

		@Override
		public String toString() {
			return String.format("[items=%s, skips=%s]", items, skips);
		}

	}

}
