package com.almalence.plugins.export.standard.ExifDriver.Values;

import com.almalence.plugins.export.standard.ExifDriver.ExifDriver;

/**
 * Base class for rationals. Implements writting method, which is basically the
 * same for signed and unsigned variant.
 */
public class ValueRationals extends ExifValue {
	private final int NUMBER_WIDTH = 4;
	private int[][] values;

	public ValueRationals(int _dataType) {
		super(_dataType);
	}
	@Override
	public int[][] getRationals(){
		return values;
	}
	@Override
	public void  setRationals(int[][] _values){
		values=_values;
	}

	/**
	 * Read pairs of signed values from source byte array.
	 * 
	 * @param _data
	 *          Byte array to read from
	 * @param _offset
	 *          Offset where to start
	 * @param _count
	 *          Count of rationals values
	 * @param _align
	 *          Endian
	 * @return Array of two-values arrays represeting rationals
	 */
	public void readValueFromData(byte[] _data, int _offset, int _count, int _align) {
		values = new int[_count][2];
		for (int v = 0; v < _count; v++) {
			int iOffs = _offset + getComponentSize() * v;
			if (dataType == ExifDriver.FORMAT_SIGNED_RATIONAL) {
				values[v][0] = ExifDriver.readSInt(_data, iOffs, NUMBER_WIDTH, _align);
				values[v][1] = ExifDriver.readSInt(_data, iOffs + NUMBER_WIDTH,
				    NUMBER_WIDTH, _align);
			} else if (dataType == ExifDriver.FORMAT_UNSIGNED_RATIONAL) {
				values[v][0] = ExifDriver.readUInt(_data, iOffs, NUMBER_WIDTH, _align);
				values[v][1] = ExifDriver.readUInt(_data, iOffs + NUMBER_WIDTH,
				    NUMBER_WIDTH, _align);
			}
		}
	}

	@Override
	protected void writeValues(byte[] _data, int _offset) {
		for (int i = 0; i < values.length; i++) {
			int iOffs = _offset + i * getComponentSize();
			ExifDriver.writeNumber(_data, iOffs, values[i][0], NUMBER_WIDTH);
			ExifDriver.writeNumber(_data, iOffs + NUMBER_WIDTH, values[i][1],
			    NUMBER_WIDTH);
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
