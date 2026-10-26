package dojo.ops.l1;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;

/** Reference solution for the GitObjects kata. */
public final class GitObjects {

	private GitObjects() {
	}

	public static String blobId(byte[] content) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.update(("blob " + content.length + "\0").getBytes(StandardCharsets.US_ASCII));
			sha1.update(content);
			return HexFormat.of().formatHex(sha1.digest());
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String shortId(String id, Collection<String> allIds) {
		for (int length = 4; length < id.length(); length++) {
			String prefix = id.substring(0, length);
			long matches = allIds.stream().filter(other -> other.startsWith(prefix)).count();
			if (matches == 1) {
				return prefix;
			}
		}
		return id;
	}

}
