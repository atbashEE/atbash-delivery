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
package be.atbash.config.mp.sources;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a {@link ConfigSourceProvider} for getting a configSource for the specified location. This class
 * cannot be used through ServiceLoader mechanism as it requires a constructor parameter.
 * <p/>
 * based on code by Jeff Mesnil (c) 2017 Red Hat inc.
 */
public class PropertiesConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {

    private final List<ConfigSource> configSources = new ArrayList<>();

    public PropertiesConfigSourceProvider(String location) {
        this.configSources.addAll(loadConfigSources(new String[]{location}, ConfigSource.DEFAULT_ORDINAL));
    }


    @Override
    public List<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        return configSources;
    }

    @Override
    protected ConfigSource loadConfigSource(URL url, int ordinal) throws IOException {
        return new PropertiesConfigSource(url, ordinal);
    }
}
