package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ExtendedByteArrayOutputStream extends ByteArrayOutputStream {

	public ExtendedByteArrayOutputStream() {
		this(32);
	}

	public ExtendedByteArrayOutputStream(int size) {
		super(size);
	}


	/**
	 * Returns an instance of ByteArrayInputStream initialized with the data written into this stream. \n
	 * It does this without copying the buffer thus the runtime of this method is O(1). \n
	 *
	 *
	 * @return an inputstream that holds the data written onto this output stream.
	 */
	public InputStream toInputStream() {
		return new ByteArrayInputStream(this.buf, 0, this.count);
	}

}
