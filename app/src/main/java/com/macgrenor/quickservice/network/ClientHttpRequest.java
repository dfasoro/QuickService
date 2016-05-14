package com.macgrenor.quickservice.network;
/**
 * <p>Title: MyJavaTools: Client HTTP Request class</p>
 * <p>Description: this class helps to send POST HTTP requests with various form data,
 * including files. Cookies can be added to be included in the request.</p>
 *
 * <p>Copyright: This is public domain;
 * The right of people to use, distribute, copy or improve the contents of the
 * following may not be restricted.</p>
 *
 * @author Vlad Patryshev, Alexei Trebounskikh
 * @version 1.4
 */
//package com.myjavatools.web;

import android.net.Uri;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.InputStream;
import java.util.Random;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("rawtypes")
public class ClientHttpRequest {
	URLConnection _connection;
	OutputStream _os = null;
	InputStream _is = null;
	
	Map _cookies = new HashMap();
	String _rawCookies = "";
	
	public void closeAll() {		
		try {
			_os.close();
		} catch (Exception e) {
			//e.printStackTrace();
		}
		try {
			_is.close();
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		try {
			_connection = null;
			_cookies.clear();
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	protected void connect() throws IOException {
		if (_os == null) {
			_os = _connection.getOutputStream();
		}
	}

	protected void write(String s) throws IOException {
		connect();
		_os.write(s.getBytes());
	}

	public String postAndRetrieve() throws IOException {
		String s = "";
		//try {
			InputStream serverInput = this.post();

			byte[] output;
			int readbytes = 0;
			int outputDefSize = 1024;

			while (true) {
				output = new byte[outputDefSize];

				readbytes = serverInput.read(output);

				if (readbytes != -1) {
					String x = new String(output, 0, readbytes);
					s += x;
				}
				else break;
			}
			
			serverInput.close();
			
			return s;
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
		//return null;
	}

	/**
	 * Creates a new multipart POST HTTP request on a freshly opened URLConnection
	 *
	 * @param connection an already open URL connection
	 * @throws IOException
	 */
	public ClientHttpRequest(URLConnection connection, String mode) throws IOException {
		this._connection = connection;
		connection.setDoOutput(true);
		connection.setDoInput(true);
		if (connection instanceof HttpURLConnection) {
			((HttpURLConnection)connection).setRequestMethod(mode);
		}
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL
	 *
	 * @param url the URL to send request to
	 * @throws IOException
	 */
	public ClientHttpRequest(URL url, String mode) throws IOException {
		this(url.openConnection(), mode);
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL string
	 *
	 * @param urlString the string representation of the URL to send request to
	 * @throws IOException
	 */
	public ClientHttpRequest(String urlString, String mode) throws IOException {
		this(new URL(urlString), mode);
	}


	/**
	 * Creates a new multipart POST HTTP request on a freshly opened URLConnection
	 *
	 * @param connection an already open URL connection
	 * @throws IOException
	 */
	public ClientHttpRequest(URLConnection connection, String mode, int timeout) throws IOException {
		this(connection, mode);
		this._connection.setConnectTimeout(timeout);
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL
	 *
	 * @param url the URL to send request to
	 * @throws IOException
	 */
	public ClientHttpRequest(URL url, String mode, int timeout) throws IOException {
		this(url.openConnection(), mode);
		this._connection.setConnectTimeout(timeout);
	}

	/**
	 * Creates a new multipart POST HTTP request for a specified URL string
	 *
	 * @param urlString the string representation of the URL to send request to
	 * @throws IOException
	 */
	public ClientHttpRequest(String urlString, String mode, int timeout) throws IOException {
		this(new URL(urlString), mode);
		this._connection.setConnectTimeout(timeout);
	}

	private void postCookies() {
		StringBuffer cookieList = new StringBuffer(_rawCookies);

		for (Iterator i = _cookies.entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry)(i.next());
			cookieList.append(entry.getKey().toString() + "=" + entry.getValue());

			if (i.hasNext()) {
				cookieList.append("; ");
			}
		}
		if (cookieList.length() > 0) {
			_connection.setRequestProperty("Cookie", cookieList.toString());
		}
	}

	public void setCookies(String rawCookies) throws IOException {
		this._rawCookies = (rawCookies == null) ? "" : rawCookies;
		_cookies.clear();
	}

	/**
	 * adds a cookie to the requst
	 * @param name cookie name
	 * @param value cookie value
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void setCookie(String name, String value) throws IOException {
		_cookies.put(name, value);
	}

	/**
	 * adds cookies to the request
	 * @param cookies the cookie "name-to-value" map
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void setCookies(Map cookies) throws IOException {
		if (cookies == null) return;
		this._cookies.putAll(cookies);
	}

	/**
	 * adds cookies to the request
	 * @param cookies array of cookie names and values (cookies[2*i] is a name, cookies[2*i + 1] is a value)
	 * @throws IOException
	 */
	public void setCookies(String[] cookies) throws IOException {
		if (cookies == null) return;
		for (int i = 0; i < cookies.length - 1; i+=2) {
			setCookie(cookies[i], cookies[i+1]);
		}
	}

	/**
	 * adds a string parameter to the request
	 * @param name parameter name
	 * @param value parameter value
	 * @throws IOException
	 */
	private boolean writtenBefore;
	public void setParameter(String name, String value) throws IOException {
		String text = (writtenBefore ? "&" : "") + Uri.encode(name) + "=" + Uri.encode(value);
		write(text);
		writtenBefore = true;
	}

	/**
	 * adds parameters to the request
	 * @param parameters "name-to-value" map of parameters; if a value is a file, the file is uploaded, otherwise it is stringified and sent in the request
	 * @throws IOException
	 */
	public void setParameters(Map<String, String> parameters) throws IOException {
		if (parameters != null) {
			for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
				Map.Entry<String, String> entry = (Map.Entry)i.next();
				setParameter(entry.getKey().toString(), entry.getValue());
			}
		}
	}

	/**
	 * adds parameters to the request
	 * @param parameters array of parameter names and values (parameters[2*i] is a name, parameters[2*i + 1] is a value); if a value is a file, the file is uploaded, otherwise it is stringified and sent in the request
	 * @throws IOException
	 */
	public void setParameters(String[] parameters) throws IOException {
		if (parameters != null) {
			for (int i = 0; i < parameters.length - 1; i += 2) {
				setParameter(parameters[i].toString(), parameters[i + 1]);
			}
		}
	}

	/**
	 * posts the requests to the server, with all the cookies and parameters that were added
	 * @return input stream with the server response
	 * @throws IOException
	 */
	private InputStream doPost() throws IOException {
		_os.close();

		_is = _connection.getInputStream();
		return _is;
	}

	/**
	 * posts the requests to the server, with all the cookies and parameters that were added
	 * @return input stream with the server response
	 * @throws IOException
	 */
	public InputStream post() throws IOException {
		postCookies();
		return doPost();
	}

	public InputStream post(Map<String, String> parameters) throws IOException {
		postCookies();
		setParameters(parameters);
		return doPost();
	}

	public InputStream post(String[] parameters) throws IOException {
		postCookies();
		setParameters(parameters);
		return doPost();
	}

	public InputStream post(Map cookies, Map parameters) throws IOException {
		setCookies(cookies);
		postCookies();
		setParameters(parameters);
		return doPost();
	}

	public InputStream post(String raw_cookies, Map parameters) throws IOException {
		setCookies(raw_cookies);
		postCookies();
		setParameters(parameters);
		return doPost();
	}
}
