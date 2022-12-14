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

import be.atbash.config.mp.sources.interceptor.ConfigSourceInterceptor;
import be.atbash.config.mp.sources.interceptor.ConfigSourceInterceptorContext;
import org.eclipse.microprofile.config.ConfigValue;

import java.util.*;

/**
 * This interceptor actually looks up the required information from the ConfigSource.
 * <p>
 * Based on code from SmallRye Config.
 */
public class ConfigValueRetrievalInterceptor implements ConfigSourceInterceptor {

    private final List<ConfigValueConfigSource> configSources = new ArrayList<>();

    public ConfigValueRetrievalInterceptor(List<ConfigSources.ConfigSourceWithPriority> configSourcesWithPriorities) {
        for (ConfigSources.ConfigSourceWithPriority configSource : configSourcesWithPriorities) {
            configSources.add(ConfigValueConfigSourceWrapper.wrap(configSource.getSource()));
        }
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        for (ConfigValueConfigSource configSource : configSources) {
            ConfigValue configValue = configSource.getConfigValue(name);

            if (configValue != null) {
                return configValue;
            }

        }
        return null;
    }

    @Override
    public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        Set<String> names = new HashSet<>();
        for (ConfigValueConfigSource configSource : configSources) {
            Set<String> propertyNames = configSource.getPropertyNames();
            if (propertyNames != null) {
                names.addAll(propertyNames);
            }
        }
        return names.iterator();
    }

}
