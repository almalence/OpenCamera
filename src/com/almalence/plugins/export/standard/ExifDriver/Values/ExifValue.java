package com.almalence.plugins.export.standard.ExifDriver.Values;

import com.almalence.plugins.export.standard.ExifDriver.ExifDriver;

/**
 * Parent class of all EXIF datatypes.
 */
public abstract class ExifValue {
	protected int dataType;

	public ExifValue(int _dataType) {
		dataType = _dataType;
	}

	/**
	 * Read it's value from a given source
	 * 
	 * @param _data
	 *          Source array to read from
	 * @param _offset
	 *          Offset where to start reading
	 * @param _count
	 *          Amount of bytes to read
	 * @param _align
	 *          Endian - is ignored by ASCII and UNDEFINED
	 */
	public abstract void readValueFromData(byte[] _data, int _offset, int _count,
	    int _align);

	/**
	 * Returns byte array of components
	 * 
	 * @return Array of components
	 * @throws UndefinedValueAccessException
	 */
	public byte[] getBytes() throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Returns int [] of components
	 * 
	 * @return Array of components
	 * @throws UndefinedValueAccessException
	 */
	public int[] getIntegers() throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Returns int [][2] of components
	 * 
	 * @return Array of components
	 * @throws UndefinedValueAccessException
	 */
	public int[][] getRationals() throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Set byte array of values
	 * 
	 * @param _values
	 * @throws UndefinedValueAccessException
	 */
	public void setBytes(byte[] _bytes) throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Set int[] of values
	 * 
	 * @param _values
	 * @throws UndefinedValueAccessException
	 */
	public void setIntegers(int[] _values) throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Set int[][] array of values
	 * 
	 * @param _values
	 * @throws UndefinedValueAccessException
	 */
	public void setRationals(int[][] _values)
	    throws UndefinedValueAccessException {
		throw new UndefinedValueAccessException("Function is undefined in "
		    + getClass().getSimpleName());
	}

	/**
	 * Returns size of one single component. For example if Value holds components
	 * of type UNSIGNED_BYTE, it returns 1.
	 * 
	 * @return Size of one single component
	 * @throws UndefinedValueAccessException
	 */
	public final int getComponentSize() {
		return ExifDriver.COMP_WIDTHS[dataType];
	}

	/**
	 * Total size of components. In case that value holds 4 components of type
	 * UNSIGNED_SHORT, it returns 8;
	 * 
	 * @return Total size of components
	 * @throws UndefinedValueAccessException
	 */
	public final int getTotalSize() {
		return getNbfComponents() * getComponentSize();
	}

	/**
	 * Returns of extra space that this value needs. All values store their
	 * components right into directory if their total amount is less then 4B. In
	 * opposite case they stores to directory only offset, where the data are
	 * stored. Extra space is therefore 0 or totalLength of data.
	 * 
	 * @return Extra space that this value uses
	 */
	public final int getExtraSize() {
		if (getTotalSize() > 4) {
			return getTotalSize();
		} else {
			return 0;
		}
	}

	/**
	 * Returns number of value's components
	 * 
	 * @return Extra space that this value uses
	 * @throws UndefinedValueAccessException
	 */
	public abstract int getNbfComponents();

	/**
	 * This method implements the special way, in which the value saves it's
	 * components.
	 * 
	 * @param _data
	 *          - output array
	 * @param _offset
	 *          - offset where to save the components
	 */
	protected abstract void writeValues(byte[] _data, int _offset);

	/**
	 * Write this component to specified place in output data.
	 * 
	 * @param _data
	 *          Output data array
	 * @param _itemOffset
	 *          Offset of IFD directory record
	 * @param _valuesOffset
	 *          Offset of the first free byte in IFD's data
	 * @return Offset of the first free byte in IFD's data. In case, that the
	 *         value does not use any extra space, return value will equal to
	 *         _valuesOffset.
	 * @throws UndefinedValueAccessException
	 */
	public final int writeToData(byte[] _data, int _itemOffset, int _valuesOffset) {
		ExifDriver.writeNumber(_data, _itemOffset + 2, dataType, 2);
		ExifDriver.writeNumber(_data, _itemOffset + 4, getNbfComponents(), 4);
		int valueOffset = _itemOffset + 8;
		int extraSpace = getExtraSize();
		if (extraSpace > 0) {
			ExifDriver.writeNumber(_data, valueOffset, _valuesOffset, 4);
			writeValues(_data, _valuesOffset);
			return _valuesOffset + extraSpace;
		} else {
			writeValues(_data, valueOffset);
			return _valuesOffset;
		}
	}

	/**
	 * Returns the datatype of this value
	 * 
	 * @return Datatype
	 */
	public final int getDataType() {
		return dataType;
	}

}
