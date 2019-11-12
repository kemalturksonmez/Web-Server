import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;

public class NetUtil {

	/**
	 * Copies all bytes, up to the end-of-stream, from an input stream to an output stream.
	 * Does not flush the output stream, and does not close the streams.
	 * @param in the stream from which all bytes are read, up to end-of-stream
	 * @param out the output stream to which bytes are written
	 * @throws IOException if any IO occurs during the copying
	 */
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[10000];
		while (true) {
			int count = in.read(buffer);
			if (count == -1)
				break;  // at end of stream
			out.write(buffer,0,count);
		}
	}

	/**
	 * Writes a string of ASCII characters to an output stream.  Does not flush the stream.
	 * @param str a string containing the ASCII characters.  Note that only the lower 8
	 *    bits of the characters are used.  If a character is not in fact a 7-bit ASCII
	 *    value, it will not be represented correctly in the output to the stream.
	 * @throws IOException if any IO exception occurs while writing the characters
	 */
	public static void writeASCII(String str, OutputStream out) throws IOException {
		byte[] buffer = new byte[str.length()];
		for (int i = 0; i < str.length(); i++)
			buffer[i] = (byte)str.charAt(i);
		out.write(buffer);
	}

	/**
	 * Encodes a string in the format required for URLs, using UTF-8 URL encoding.
	 */
	public static String encodeURL(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			return "";  // This can't happen, since UTF-8 is always supported.
		}
	}

	/**
	 * Decodes a string that has been encoded using the web-standard UTF8 URL encoding.
	 */
	public static String decodeURL(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			return ""; // this can't happen, since UTF-8 is always supported.
		}
	}

	/**
	 * Write two bytes to an output stream, representing the ASCII carriage return
	 * and line feed (characters 13 and 10).  Does not flush the stream.
	 * @throws IOException if an IO error occurs while writing the bytes
	 */
	public static void writeCRLF(OutputStream out) throws IOException {
		byte[] buffer = new byte[] { '\r', '\n' };
		out.write(buffer);
	}

	/**
	 * Reads bytes up to a line feed or end-of-stream.  The line feed, if 
	 * present, is read and discarded.  If the line feed or end-of-stream
	 * was preceded by a carriage return, that is also discarded.  The
	 * remaining bytes are converted into a string by treating the bytes
	 * as a string using the UTF-8 charset. 
	 * @throws IOException if an IO error occurs while reading the bytes.
	 */
	public static String readLine(InputStream in) throws IOException {
		byte[] buffer = new byte[128];
		int length = 0;
		while (true) {
			int b = in.read();
			if (b == -1)
				break;
			else if (b == '\n')
				break;
			if (length == buffer.length)
				buffer = Arrays.copyOf(buffer, 2*buffer.length);
			buffer[length] = (byte)b;
			length++;
		}
		if (length > 0 && buffer[length-1] == '\r')
			length--;
		String line = new String(buffer,0,length,"UTF-8");
		return line;
	}

}
