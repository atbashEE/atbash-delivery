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
package be.atbash.config.mp.util;

import net.jadler.Jadler;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class ConfigSourceUtilTest {

    @Test
    void propertiesToMap() {
        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        properties.setProperty("key3", "value3");
        Map<String, String> map = ConfigSourceUtil.propertiesToMap(properties);
        Assertions.assertThat(map).hasSize(3);
        Assertions.assertThat(map).containsEntry("key1", "value1");
        Assertions.assertThat(map).containsEntry("key2", "value2");
        Assertions.assertThat(map).containsEntry("key3", "value3");
    }

    @Test
    void getOrdinalFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put(ConfigSource.CONFIG_ORDINAL, "123");
        map.put("key3", "value3");
        int ordinal = ConfigSourceUtil.getOrdinalFromMap(map, 200);
        Assertions.assertThat(ordinal).isEqualTo(123);
    }

    @Test
    void getOrdinalFromMap_noValueDefined() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key3", "value3");
        int ordinal = ConfigSourceUtil.getOrdinalFromMap(map, 200);
        Assertions.assertThat(ordinal).isEqualTo(200);
    }

    @Test
    void getOrdinalFromMap_notANumber() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put(ConfigSource.CONFIG_ORDINAL, "Atbash");
        map.put("key3", "value3");

        // FIXME Add the test to see if we have the logging entry
        int ordinal = ConfigSourceUtil.getOrdinalFromMap(map, 200);
        Assertions.assertThat(ordinal).isEqualTo(200);
    }

    @Test
    void urlToMap() throws IOException {
        Jadler.initJadler();

        String data = "key=value\nruntime=Atbash";

        Jadler.onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/data")
                .respond()
                .withStatus(200)
                .withHeader("Content-Type", "text/plain")
                .withBody(data);

        Map<String, String> map = ConfigSourceUtil.urlToMap(new URL("http://localhost:" + Jadler.port() + "/data"));
        Assertions.assertThat(map).hasSize(2);

        Assertions.assertThat(map).containsEntry("key", "value");
        Assertions.assertThat(map).containsEntry("runtime", "Atbash");
    }

    @AfterEach
    public void tearDown() {
        Jadler.closeJadler();
    }

}