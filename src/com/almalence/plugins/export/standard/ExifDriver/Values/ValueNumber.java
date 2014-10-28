package com.almalence.plugins.export.standard.ExifDriver.Values;

import com.almalence.plugins.export.standard.ExifDriver.ExifDriver;

/**
 * Basic class for scalar number data types. Implements Methods for Reading and
 * writing signed and unsigned values, same as constructor from value.
 */
public class ValueNumber extends ExifValue {

	int[] values;
	
	public ValueNumber(int _dataType, int _initValue) {
		super(_dataType);
		values=new int[]{_initValue};
	}

	public ValueNumber(int _dataType) {
		super(_dataType);
	}
	@Override
	public int[] getIntegers() {
		return values;
	}
	
	@Override
	public void  setIntegers(int[] _values){
		values=_values;
	}

	/**
	 * Read values from source data. It uses own information about component's
	 * size to determine, how much bytes read for each component.
	 * 
	 * @param _data
	 *          Source byte array
	 * @param _offset
	 *          Where to start the reading
	 * @param _count
	 *          How much components to read
	 * @param _align
	 *          - Endian
	 * @return Array of read components
	 */
	public void readValueFromData(byte[] _data, int _offset, int _count, int _align) {
		values = new int[_count];
		int componentSize = getComponentSize();
		for (int i = 0; i < _count; i++) {
			int iOffs = _offset + componentSize * i;
			switch (dataType) {
			case ExifDriver.FORMAT_SIGNED_BYTE:
			case ExifDriver.FORMAT_SIGNED_SHORT:
			case ExifDriver.FORMAT_SIGNED_LONG:
				values[i] = ExifDriver.readSInt(_data, iOffs, componentSize, _align);
				break;
			case ExifDriver.FORMAT_UNSIGNED_BYTE:
			case ExifDriver.FORMAT_UNSIGNED_SHORT:
			case ExifDriver.FORMAT_UNSIGNED_LONG:
				values[i] = ExifDriver.readUInt(_data, iOffs, componentSize, _align);
				break;
			default:
				break;
			}

		}
	}

	@Override
	protected void writeValues(byte[] _data, int _offset) {
		for (int i = 0; i < values.length; i++) {
			ExifDriver.writeNumber(_data, _offset + getComponentSize() * i,
			    values[i], getComponentSize());
		}
	}

	@Override
	public int getNbfComponents() {
		if (values != null) {
			return values.length;
		}
		return 0;
	}
}
