package dojo.docker.l1;

import dojo.Kata;

/**
 * KATA docker L1 "ImageRef" (tracks/11-docker-deploy/L1-basics.md, section 3).
 *
 * Parse an image reference the way Docker does: [registry/]repository[:tag][@digest]
 * - registry: the first path component, but ONLY if it contains a "." or a ":" or is "localhost"
 *   ("ghcr.io/acme/shop", "localhost:5000/shop"); otherwise there is no registry part and it is
 *   "docker.io" ("acme/shop" is a Docker Hub user "acme");
 * - Docker Hub images without a "/" live under "library/": "postgres" -> "library/postgres";
 * - tag: after the LAST ":" of the repository part (not the registry's port!), default "latest"
 *   when there is no tag AND no digest; null when only a digest is given;
 * - digest: after "@", must start with "sha256:"; null if absent;
 * - blank input, uppercase letters in the repository, or an empty part -> IllegalArgumentException.
 * pinned() is true when a digest is present: the only reference that cannot change under you.
 */
public record ImageRef(String registry, String repository, String tag, String digest) {

	public static ImageRef parse(String reference) {
		return Kata.todo("tracks/11-docker-deploy/L1-basics.md section 3");
	}

	public boolean pinned() {
		return digest != null;
	}

}
