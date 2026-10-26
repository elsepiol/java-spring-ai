package dojo.docker.l1;

/** Reference solution for the ImageRef kata. */
public record ImageRef(String registry, String repository, String tag, String digest) {

	public static ImageRef parse(String reference) {
		if (reference == null || reference.isBlank() || !reference.equals(reference.strip())) {
			throw new IllegalArgumentException("empty image reference");
		}
		String rest = reference;
		String digest = null;
		int at = rest.indexOf('@');
		if (at >= 0) {
			digest = rest.substring(at + 1);
			rest = rest.substring(0, at);
			if (!digest.startsWith("sha256:") || digest.length() == "sha256:".length()) {
				throw new IllegalArgumentException("bad digest: " + digest);
			}
		}
		String registry = "docker.io";
		int slash = rest.indexOf('/');
		if (slash >= 0) {
			String first = rest.substring(0, slash);
			if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
				registry = first;
				rest = rest.substring(slash + 1);
			}
		}
		String tag = null;
		int colon = rest.lastIndexOf(':');
		if (colon >= 0) {
			tag = rest.substring(colon + 1);
			rest = rest.substring(0, colon);
			if (tag.isEmpty()) {
				throw new IllegalArgumentException("empty tag");
			}
		}
		else if (digest == null) {
			tag = "latest";
		}
		if (rest.isEmpty() || rest.startsWith("/") || rest.endsWith("/") || rest.contains("//") || !rest.equals(rest.toLowerCase())) {
			throw new IllegalArgumentException("bad repository: " + rest);
		}
		if (registry.equals("docker.io") && !rest.contains("/")) {
			rest = "library/" + rest;
		}
		return new ImageRef(registry, rest, tag, digest);
	}

	public boolean pinned() {
		return digest != null;
	}

}
