package com.ibm.expiremental.jaxws.cache;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class JaxwsIdGeneratorSHA256S extends AbstractIdGenerator {
	private static final ThreadLocal<MessageDigest> hasher = new ThreadLocal<MessageDigest>() {
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		};
	};

	@Override
	protected String generateId(byte[] reqContent) {
		byte[] reqHash = hasher.get().digest(reqContent);
		return Base64.getEncoder().encodeToString(reqHash);
	}
}
