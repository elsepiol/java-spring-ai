package dojo.build.l2;

import java.util.List;

/** A node of a dependency tree: groupId:artifactId:version and what it depends on. Given, not a kata. */
public record Dep(String groupId, String artifactId, String version, List<Dep> dependencies) {

	public Dep {
		dependencies = List.copyOf(dependencies);
	}

	/** "group:artifact:version" with optional children. */
	public static Dep of(String coordinates, Dep... dependencies) {
		String[] p = coordinates.split(":");
		return new Dep(p[0], p[1], p[2], List.of(dependencies));
	}

	/** The identity Maven mediates on: group and artifact, without the version. */
	public String ga() {
		return groupId + ":" + artifactId;
	}

}
