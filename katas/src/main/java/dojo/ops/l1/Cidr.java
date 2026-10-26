package dojo.ops.l1;

import dojo.Kata;

/**
 * KATA ops L1 "Cidr" (tracks/14-git-linux-networking/L1-basics.md, section 6).
 *
 * IPv4 addresses are 32-bit numbers; a CIDR block "10.20.0.0/16" is every address whose first 16
 * bits equal those of 10.20.0.0.
 * - parse: "a.b.c.d/n" with 0 <= octets <= 255 and 0 <= n <= 32; a block whose address has host
 *   bits set ("10.20.1.5/16") is normalised to its network address (10.20.0.0/16). Anything
 *   else -> IllegalArgumentException.
 * - size: number of addresses (2^(32-n), so /0 has 4294967296: use long).
 * - contains(ip): is the dotted address inside the block?
 * - overlaps(other): do two blocks share any address? (Subnets of one VPC must not.)
 * - isPrivate: entirely inside 10.0.0.0/8, 172.16.0.0/12 or 192.168.0.0/16 (RFC 1918).
 * - toString: "network/prefix".
 */
public record Cidr(long network, int prefix) {

	public static Cidr parse(String text) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 6");
	}

	public long size() {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 6");
	}

	public boolean contains(String ip) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 6");
	}

	public boolean overlaps(Cidr other) {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 6");
	}

	public boolean isPrivate() {
		return Kata.todo("tracks/14-git-linux-networking/L1-basics.md section 6");
	}

}
