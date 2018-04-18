package com.ibm.expiremental.jaxws.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestWrapper;

import com.ibm.websphere.servlet.cache.CacheConfig;
import com.ibm.websphere.servlet.cache.IdGenerator;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;
import com.ibm.websphere.servlet.request.ServletInputStreamAdapter;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.wsspi.webcontainer.util.WSServletInputStream;

public abstract class AbstractIdGenerator implements IdGenerator {

	private final String CLASS = getClass().getName();
	private final Logger LOG = Logger.getLogger(CLASS);

	private static final String ACTION = "action=";

	/**
	 * Cache policy that governs if a response is cached or not.
	 * 
	 * Returning null is like telling Dynacache to not worry about caching this
	 * response or looking in the cache for this response.
	 * 
	 * Returning a non-null id will result in dynacache caching the response if
	 * the id does not exist in the cache (CACHE MISS) or simply returning the
	 * cached response for that id from the cache.
	 */
	@Override
	public String getId(ServletCacheRequest request) {

		if (LOG.isLoggable(Level.FINER)) {
			LOG.entering(CLASS, "getId", request);
		}

		String cacheId = null;

		if (request.getMethod().equals("POST")) {
			String soapAction = getSoapAction(request);

			if (soapAction != null && isValidAction(soapAction)) {
				try {
					chunkedSupport(request);

					request.setGeneratingId(true); // ** DO NOT REMOVE... THIS
													// IS NEEDED FOR PROPER
													// PARSING OF RESPONSE


					ServletInputStream is = request.getInputStream();
					byte[] reqContent = getBytes(is);

					cacheId = generateId(reqContent);
					if (LOG.isLoggable(Level.FINEST)) {
						LOG.fine("Create hash of SOAPEnvelope: " + cacheId);
					}

				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					request.setGeneratingId(false); // ** DO NOT REMOVE... THIS
													// IS NEEDED FOR PROPER
													// PARSING OF RESPONSE
				}

				cacheId = "SOAPAction=" + soapAction + ":SOAPEnvelope=" + cacheId;
			}
		}

		if (LOG.isLoggable(Level.FINER)) {
			LOG.exiting(CLASS, "getId", cacheId);
		}

		return cacheId;
	}
	protected abstract String generateId(byte[] reqContent);

	private void chunkedSupport(ServletCacheRequest request) throws IOException {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.entering(CLASS, "chunkedSupport", request);
		}
		
		SRTServletRequest req = (SRTServletRequest) ((ServletRequestWrapper) request).getRequest();
		
		if (req.getContentLength() < 0) {
			WSServletInputStream in = (WSServletInputStream) req.getInputStream();
			@SuppressWarnings("unchecked")
			HashMap<Object, Object> inStreamInfo = req.getInputStreamData();

			byte[] bytes = getBytes(in);
			try {
				in.close();
			} catch (Exception e) {
				if (LOG.isLoggable(Level.WARNING)) {
					LOG.log(Level.WARNING, "ServletInputStream didn't close", e);
				}
			}
			inStreamInfo.put("ContentDataLength", new Long(bytes.length));

			in.init(new ServletInputStreamAdapter(new ByteArrayInputStream(bytes)), req);
			req.setInputStreamData(inStreamInfo);
		}
		if (LOG.isLoggable(Level.FINER)) {
			LOG.exiting(CLASS, "chunkedSupport", request);
		}
	}

	/**
	 * Checks is SoapAction is to retrieve information.<br/>
	 * Only Retrieve information could be cached!
	 * @param soapAction
	 * @return
	 */
	protected boolean isValidAction(String soapAction) {
		return CacheEnable.isValid(soapAction.replaceAll("\"", ""));
	}

	private String getSoapAction(ServletCacheRequest request) {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.entering(CLASS, "getSoapAction", request);
		}

		String soapAction = request.getHeader("SOAPAction");
		if (soapAction == null) {
			// pull out the action from the http request content-type
			// Content-Type:
			// application/soap+xml;charset=UTF-8;action="getAccount"
			String contentType = request.getContentType();

			if (LOG.isLoggable(Level.FINEST)) {
				LOG.fine("Retrieved contentType " + contentType);
			}

			// parse the content-type with the ; delimiter
			StringTokenizer strToken = new StringTokenizer(contentType, ";", false);
			while (strToken.hasMoreTokens()) {
				String token = strToken.nextToken();
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.fine("Processing token " + token);
				}
				int index = token.indexOf(ACTION);
				if (index != -1) {
					soapAction = token.substring(index + ACTION.length());
				}
			}
		}

		if (LOG.isLoggable(Level.FINER)) {
			LOG.exiting(CLASS, "getSoapAction", soapAction);
		}

		return soapAction;
	}

	// Deprecated method ... do nothing
	@Override
	public int getSharingPolicy(ServletCacheRequest request) {
		// Dynacache runtime will not do anything
		return 0;
	}

	// Deprecated method ... do nothing
	@Override
	public void initialize(CacheConfig cc) {
		// Dynacache runtime will not do anything
	}

	public static byte[] getBytes(InputStream is) throws IOException {

		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream) {
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1)
				bos.write(buf, 0, len);
			buf = bos.toByteArray();
		}
		return buf;
	}
}
