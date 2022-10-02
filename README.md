[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/be.atbash/mp-config-se/badge.svg)](https://maven-badges.herokuapp.com/maven-central/be.atbash/mp-config-se)

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

## Release notes

### 1.0.1

- Small code improvements
- Use SLF4J everywhere for logging
- Fix SNAPSHOT dependency