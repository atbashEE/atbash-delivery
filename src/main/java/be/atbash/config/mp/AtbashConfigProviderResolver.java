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
package be.atbash.config.mp;

import be.atbash.config.mp.sources.ConfigSources;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

/**
 * Implementation of the {@code ConfigProviderResolver} of Microprofile Config
 * <p/>
 * Based on code by Jeff Mesnil (Red Hat) and David M. Lloyd (Red Hat)
 */
public class AtbashConfigProviderResolver extends ConfigProviderResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSources.class.getName());

    private final Map<ClassLoader, Config> configsForClassLoader = new ConcurrentHashMap<>();

    static final ClassLoader SYSTEM_CL = calculateSystemClassLoader();

    private static ClassLoader calculateSystemClassLoader() {
        ClassLoader result = ClassLoader.getSystemClassLoader();
        if (result == null) {
            // non-null ref that delegates to the system
            result = new ClassLoader(null) {
            };
        }
        return result;
    }

    @Override
    public Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Config config;
        synchronized (configsForClassLoader) {
            config = configsForClassLoader.computeIfAbsent(realClassLoader, this::getConfigFor);
        }
        return config;
    }

    private AtbashConfig getConfigFor(ClassLoader classLoader) {
        return new AtbashConfigBuilder().forClassLoader(classLoader)
                .addDefaultInterceptors()
                .addDefaultSources()
                .addDiscoveredSources()
                .addDiscoveredConverters()
                .addDiscoveredInterceptors()
                .build();
    }

    @Override
    public ConfigBuilder getBuilder() {
        return new AtbashConfigBuilder().addDefaultInterceptors();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        if (config == null) {
            throw new IllegalArgumentException("MPCONFIG-016: Config cannot be null");

        }
        ClassLoader realClassLoader = getRealClassLoader(classLoader);

        synchronized (configsForClassLoader) {
            Config existing = configsForClassLoader.putIfAbsent(realClassLoader, config);
            if (existing != null) {

                throw new IllegalStateException("MPCONFIG-017: Configuration already registered for the given class loader");
            }
        }
    }

    @Override
    public void releaseConfig(Config config) {
        synchronized (configsForClassLoader) {
            configsForClassLoader.values().removeIf(v -> v == config);
            closeConverterIfNeeded(config);
            closeConfigSourceIfNeeded(config);
        }
    }

    private void closeConverterIfNeeded(Config config) {
        // Spec 6.5, if Converter implements AutoCloseable, it must be closed when Config is released.
        config.unwrap(AtbashConfig.class).getConverters()
                .stream()
                .filter(c -> c instanceof AutoCloseable)
                .forEach(c -> {
                    try {
                        ((AutoCloseable) c).close();
                    } catch (Exception e) {
                        LOGGER.warn(String.format("MPCONFIG-016: Failure when closing the Converter %s : %s", c.getClass().getName(), e.getLocalizedMessage()));
                        // ignore
                    }
                });
    }

    private void closeConfigSourceIfNeeded(Config config) {
        // Spec 5.7, if ConfigSource implements AutoCloseable, it must be closed when Config is released.
        StreamSupport.stream(config.unwrap(AtbashConfig.class).getConfigSources().spliterator(), false)
                .filter(c -> c instanceof AutoCloseable)
                .forEach(c -> {
                    try {
                        ((AutoCloseable) c).close();
                    } catch (Exception e) {
                        LOGGER.warn(String.format("MPCONFIG-018: Failure when closing the ConfigSource %s : %s", c.getClass().getName(), e.getLocalizedMessage()));

                        // ignore
                    }
                });
    }

    /**
     * Make sure we have a not null Classloader. If method parameter is null, try Context class loader or take System
     * Classloader.
     *
     * @param classLoader user supplied classloader.
     * @return a real classloader.
     */
    private static ClassLoader getRealClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = SYSTEM_CL;
        }
        return classLoader;
    }
}
