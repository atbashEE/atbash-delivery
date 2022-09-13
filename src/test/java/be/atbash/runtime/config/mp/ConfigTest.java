/*
 * Copyright 2022 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.runtime.config.mp;


import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Smoke testing the MP Config implementation.  Since this is implementation is part
 * of Atbash Runtime MicroProfile Config which is tested through the TCK, this are
 * just a few basic tests.
 */
class ConfigTest {

    @AfterEach
    public void cleanup() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
        System.clearProperty("hello");
        System.clearProperty("var");
    }

    @Test
    void defineProvider() {
        Assertions.assertThat(ConfigProvider.getConfig()).isNotNull();
    }

    @Test
    void hasDefaultSources() {
        Iterable<ConfigSource> sources = ConfigProvider.getConfig().getConfigSources();
        List<String> names = new ArrayList<>();
        sources.forEach(cs -> names.add(processName(cs.getName())));

        Assertions.assertThat(names).containsOnly("SysPropConfigSource", "EnvConfigSource", "PropertiesConfigSource");
    }

    @Test
    void getValue() {
        String value = ConfigProvider.getConfig().getValue("hello", String.class);
        Assertions.assertThat(value).isEqualTo("world");
    }

    @Test
    void getConvertedValue() {
        Integer value = ConfigProvider.getConfig().getValue("answerToLife", Integer.class);
        Assertions.assertThat(value).isEqualTo(42);
    }

    @Test
    void overrideValue() {
        System.setProperty("hello", "overruled");
        String value = ConfigProvider.getConfig().getValue("hello", String.class);
        Assertions.assertThat(value).isEqualTo("overruled");
    }

    @Test
    void getOptionalValue() {
        Optional<String> value = ConfigProvider.getConfig().getOptionalValue("nonExistent", String.class);
        Assertions.assertThat(value).isEmpty();
    }

    @Test
    void getOptionalValue_Existing() {
        Optional<String> value = ConfigProvider.getConfig().getOptionalValue("hello", String.class);
        Assertions.assertThat(value).hasValue("world");
    }

    @Test
    void getValueNotFound() {
        Assertions.assertThatThrownBy(
                        () -> ConfigProvider.getConfig().getValue("NonExisting", String.class)
                ).isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("The config property 'NonExisting' is required but it could not be found in any config source");

    }

    @Test
    void getConvertedValueFails() {
        Assertions.assertThatThrownBy(
                        () -> ConfigProvider.getConfig().getValue("hello", Integer.class)
                ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The config property 'hello' with the config value 'world' threw an Exception whilst being converted")
                .hasMessageContaining("Expected a integer value, got 'world'");
    }

    @Test
    void getExpressionValue() {
        System.setProperty("var", "From System property");
        String value = ConfigProvider.getConfig().getValue("supportExpression", String.class);
        Assertions.assertThat(value).isEqualTo("Can resolve variable like this : From System property");
    }

    private String processName(String name) {
        int idx = name.indexOf('[');
        if (idx != -1) {
            return name.substring(0, idx);
        }
        return name;
    }
}