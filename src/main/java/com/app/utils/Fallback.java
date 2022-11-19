package com.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class Fallback<T> {
	
	public static <E extends Exception> void verify(final boolean condition, final Supplier<E> exceptionSupplier)
			throws E {
		if (!condition) {
			throw exceptionSupplier.get();
		}
	}


	private static class Found<T> implements Value<T> {

		private final T value;

		public Found(T value) {
			this.value = value;
		}

		@Override
		public String getError() {
			return null;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public boolean isFound() {
			return true;
		}

	}

	private static class NotFound<T> implements Value<T> {

		private final String reason;

		public NotFound(String reason) {
			this.reason = reason;
		}

		@Override
		public String getError() {
			return reason;
		}

		@Override
		public T getValue() {
			throw new NoSuchElementException(reason);
		}

		@Override
		public boolean isFound() {
			return false;
		}

	}

	public static class Result<T> {

		private final List<String> errors;
		private final Value<T> value;

		public Result(Value<T> value, List<String> errors) {
			this.value = value;
			this.errors = errors;
		}

		public List<String> getErrors() {
			return errors;
		}

		public T getValue() {
			return value.getValue();
		}

		public boolean isEmpty() {
			return !value.isFound();
		}

	}

	public static interface Value<T> {

		public String getError();

		public T getValue();

		public boolean isFound();

	}

	public static <T> Value<T> found(T value) {
		return new Found<>(value);
	}

	public static <T> Value<T> notFound(String reason) {
		return new NotFound<>(reason);
	}

	private final List<Supplier<Fallback.Value<T>>> suppliers = new ArrayList<>();

	public Result<T> execute() {
		verify(suppliers.size() > 0, () -> new IllegalStateException(
				"Tried to call a fallback's value but no supplier were given. User Fallback::from to provider suppliers."));

		Value<T> current = null;
		List<String> errors = new ArrayList<>();

		for (Supplier<Value<T>> supplier : suppliers) {
			current = supplier.get();

			if (current.isFound()) {
				return new Result<>(current, errors);
			}

			errors.add(current.getError());
		}

		return new Result<>(current, errors);
	}

	public Fallback<T> from(Supplier<Fallback.Value<T>> supplier) {
		this.suppliers.add(supplier);
		return this;
	}

	public List<String> getErrors() {
		return execute().getErrors();
	}

	public T getValue() {
		return execute().getValue();
	}

}
