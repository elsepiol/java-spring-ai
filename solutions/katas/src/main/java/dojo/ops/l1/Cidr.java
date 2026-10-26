package dojo.ops.l1;

/** Reference solution for the Cidr kata: addresses as unsigned 32-bit values in a long. */
public record Cidr(long network, int prefix) {

	private static final Cidr[] PRIVATE = { new Cidr(0x0A000000L, 8), new Cidr(0xAC100000L, 12), new Cidr(0xC0A80000L, 16) };

	public static Cidr parse(String text) {
		if (text == null) {
			throw new IllegalArgumentException("null");
		}
		int slash = text.indexOf('/');
		if (slash < 0 || slash != text.lastIndexOf('/')) {
			throw new IllegalArgumentException("not a CIDR block: " + text);
		}
		int prefix;
		try {
			prefix = Integer.parseInt(text.substring(slash + 1));
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("bad prefix: " + text);
		}
		if (prefix < 0 || prefix > 32) {
			throw new IllegalArgumentException("bad prefix: " + text);
		}
		return new Cidr(ip(text.substring(0, slash)) & mask(prefix), prefix);
	}

	public long size() {
		return 1L << (32 - prefix);
	}

	public boolean contains(String ip) {
		return (ip(ip) & mask(prefix)) == network;
	}

	public boolean overlaps(Cidr other) {
		int shorter = Math.min(prefix, other.prefix);
		return (network & mask(shorter)) == (other.network & mask(shorter));
	}

	public boolean isPrivate() {
		for (Cidr range : PRIVATE) {
			if (prefix >= range.prefix && (network & mask(range.prefix)) == range.network) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return ((network >> 24) & 255) + "." + ((network >> 16) & 255) + "." + ((network >> 8) & 255) + "." + (network & 255) + "/" + prefix;
	}

	private static long mask(int prefix) {
		return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
	}

	static long ip(String dotted) {
		String[] parts = dotted.split("\\.", -1);
		if (parts.length != 4) {
			throw new IllegalArgumentException("bad address: " + dotted);
		}
		long value = 0;
		for (String part : parts) {
			if (part.isEmpty() || part.length() > 3 || !part.chars().allMatch(Character::isDigit)) {
				throw new IllegalArgumentException("bad address: " + dotted);
			}
			int octet = Integer.parseInt(part);
			if (octet > 255) {
				throw new IllegalArgumentException("bad address: " + dotted);
			}
			value = (value << 8) | octet;
		}
		return value;
	}

}
