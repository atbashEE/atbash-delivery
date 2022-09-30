# MicroProfile Config for Java SE

Base implementation of MicroProfile Config 3.x for use in plain Java SE (Java 11+).

## Supported

Following concepts and features are supported in this SE only (non-CDI) version.

- `ConfigSource`, the 3 default implementations with their default ordinal values and the possibility to define custom ones through the _ServiceLoader_ mechanism.
- Custom `ConfigSourceProvider`'s can be loaded through the _ServiceLoader_ mechanism.
- `Converter`, the implicit defined one as specified in the specification and the possibility to define custom converters using the _ServiceLoader_ mechanism.
- Support for optional values, expressions
- Support for Config Profile.
- Support for `ConfigBuilder` and creating custom `Config` instances.

Things that are explicitly not supported:

- Injection of a config value into a CDI bean.
- `@ConfigProperties`.

However, these are supported within Atbash Runtime with the MP Config module.