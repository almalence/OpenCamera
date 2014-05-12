package com.almalence.plugins.export.standard.ExifDriver.Values;

/**
 * Common abstract superclass for ValueAscii and ValueUndefined, which are
 * treated as simple byte arrays
 */
public class ValueByteArray extends ExifValue {

	private byte[] value;

	public ValueByteArray(int _dataType) {
		super(_dataType);
	}
	
	public int getNbfComponents() {
		if (value != null) {
			return value.length;
		} else {
			return 0;
		}
	}

	public byte[] getBytes(){
		return value;
	}
	
	public void setBytes(byte[] _bytes){
		value=_bytes;
	}

	public void readValueFromData(byte[] _data, int _offset, int _count, int _align) {
		value = new byte[_count];
		System.arraycopy(_data, _offset, value, 0, _count);
	}

	@Override
	protected void writeValues(byte[] _data, int _offset) {
		System.arraycopy(value, 0, _data, _offset, value.length);
	}
}
