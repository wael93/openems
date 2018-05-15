package io.openems.edge.common.channel;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.TypeUtils;

public interface WriteChannel<T> extends Channel<T> {

	/**
	 * Updates the 'next' write value of Channel.
	 * 
	 * @param value
	 */
	public default void setNextWriteValue(T value) throws OpenemsException {
		this.setNextWriteValueFromObject(value);
	}

	/**
	 * Updates the 'next' write value of Channel from an Object value. Use this
	 * method if the value is not yet in the correct Type. Otherwise use
	 * setNextWriteValue() directly.
	 * 
	 * @param value
	 */
	public default void setNextWriteValueFromObject(Object value) throws OpenemsException {
		T typedValue = TypeUtils.<T>getAsType(this.getType(), value);
		// set the write value
		this._setNextWriteValue(typedValue);
		this.getOnSetNextWrites().forEach(callback -> callback.accept(typedValue));
	}

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public void _setNextWriteValue(T value);

	/**
	 * Gets the next write value and resets it.
	 * 
	 * @return
	 */
	public default Optional<T> getNextWriteValueAndReset() {
		Optional<T> valueOpt = this._getNextWriteValue();
		this._setNextWriteValue(null);
		return valueOpt;
	}

	public Optional<T> _getNextWriteValue();

	/**
	 * Add an onSetNextWrite callback. It is called when a 'next write value' was
	 * set.
	 */
	public void onSetNextWrite(Consumer<T> callback);

	public List<Consumer<T>> getOnSetNextWrites();

}
