
## Gradle MRJAR Plugin

> [!WARNING]  
> This plug-in is experimental; it is not yet clear what performance benefits fat MRJARs yield, if any.

This plug-in enables enhanced [Multi-Release JAR][0] builds for Gradle JVM projects; JARs are made compatible with JPMS,
and additionally **recompiled in full** at each bytecode tier declared for support. In effect, this enables a JAR which
is optimized as highly as possible for the runtime that eventually loads it.

Multi-release JARs, also known as "MRJARs," are defined by [JSR-328][0], and originally were imagined as a way for
libraries to ship support for JPMS without a root `module-info.class`. MRJARs are different from regular JARs in the
following ways:

- The JAR manifest has the `Multi-Release: true` attribute set.
- The JAR includes a `META-INF/versions` directory, with sub-directories for each targeted JVM bytecode version
- The JAR includes a `META-INF/versions/<version>/<...class...>` for each class which should override for a JVM version

When prepared this way, the JVM will notice the attribute when the JAR is loaded, and will prefer loading classes from
the highest-supported version path declared in the JAR.

### How does this optimize anything?

While the JARs produced by this plug-in are larger _on-disk_ (they contain, potentially, several copies of each class),
the bytecode loaded at runtime has no footprint change, modulo differences in JVM bytecode target standards.

JVM bytecode is very good at accomplishing forward compatibility. You can still run JVM 6 bytecode on JVM 21; but this
is not true in reverse, and for good reason: bytecode standards don't change much from version to version, but they do
change, and when they do, it is usually to introduce optimization potential.

By using this plug-in to create your JARs, you can **retain compatibility with Java versions back to 1.8**, while still
only loading and running the most optimized bytecode possible.

### Do I need to use JPMS to leverage this?

No! While MRJARs were imagined as a way to ship a `module-info.class` without causing issues with JVM 8, you could just
not ship one at all and still use the MRJAR feature's bytecode loading behavior.

### What about `jmod` support?

There is no equivalent for a multi-release JAR in the post-JPMS world. This is because `jmod` artifacts are compile-time
only. Thus, final linkage via `jlink` or similar tools still has a chance to rewrite and upgrade bytecode before it is
shipped in a form that an end-user runs.

If you want to build `jmod` artifacts anyway, take a look at the [`jmod` plugin](../jmod).

[0]: https://openjdk.org/jeps/238
