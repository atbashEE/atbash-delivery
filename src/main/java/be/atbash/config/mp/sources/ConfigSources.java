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

import be.atbash.config.mp.AtbashConfigBuilder;
import be.atbash.config.mp.ConfigValueImpl;
import be.atbash.config.mp.sources.interceptor.*;
import be.atbash.config.mp.util.AnnotationUtil;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.io.Serializable;
import java.util.*;

import static be.atbash.config.mp.sources.interceptor.ConfigSourceInterceptorFactory.DEFAULT_PRIORITY;

/**
 * This is kinda part of the ConfigBuilder, but handles all the (complex) logic around finding {@link ConfigSource}
 * and the chain for traveling the sources to find a value.
 */
public class ConfigSources implements Serializable {

    public static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES = "META-INF/microprofile-config.properties";
    public static final String ATBASH_CONFIG_LOCATIONS = "atbash.config.locations";

    private final List<ConfigSource> sources;
    private final ConfigSourceInterceptorContext interceptorChain;

    /**
     * Builds a representation of Config Sources, Interceptors and the Interceptor chain to be used in Config. Note
     * that this constructor must be used when the Config object is being initialized, because interceptors also
     * require initialization.
     */
    public ConfigSources(AtbashConfigBuilder builder) {
        // Add all sources (specified through the builder, the discovered and the default ones)
        // except for ConfigurableConfigSource types. These are initialized later.
        List<ConfigSource> sourcesFromBuilder = buildSources(builder);

        // Add all interceptors
        List<InterceptorWithPriority> interceptorWithPriorities = buildInterceptors(builder);

        // Create the initial chain with initial sources and all interceptors

        // 1. Termination point
        AtbashConfigSourceInterceptorContext current = new AtbashConfigSourceInterceptorContext(ConfigSourceInterceptor.EMPTY, null);


        // 2. An interceptor that is capable of retrieving the value from a ConfigSource.

        List<ConfigSourceWithPriority> sourcesWithPriority = mapSources(sourcesFromBuilder);
        // ConfigSource with high priority should be considered first.
        sourcesWithPriority.sort(Collections.reverseOrder());
        current = new AtbashConfigSourceInterceptorContext(new ConfigValueRetrievalInterceptor(sourcesWithPriority), current);

        List<ConfigSourceInterceptor> interceptors = new ArrayList<>();  // We need this for the next step, retrieval of profile.

        // 3. Add the interceptors (discovered and default)
        for (InterceptorWithPriority interceptorWithPriority : interceptorWithPriorities) {
            ConfigSourceInterceptor interceptor = interceptorWithPriority.getInterceptor(current);
            interceptors.add(interceptor);
            current = new AtbashConfigSourceInterceptorContext(interceptor, current);
        }

        // Init all late sources
        List<String> profiles = getProfiles(interceptors);
        List<ConfigSourceWithPriority> sourcesWithPriorities = mapLateSources(current, sourcesFromBuilder, profiles);

        // Rebuild the chain with the late sources and new instances of the interceptors
        // The new instance will ensure that we get rid of references to factories and other stuff and keep only
        // the resolved final source or interceptor to use.
        current = new AtbashConfigSourceInterceptorContext(ConfigSourceInterceptor.EMPTY, null);
        current = new AtbashConfigSourceInterceptorContext(new ConfigValueRetrievalInterceptor(sourcesWithPriorities), current);
        for (ConfigSourceInterceptor interceptor : interceptors) {
            current = new AtbashConfigSourceInterceptorContext(interceptor, current);
        }

        // Adds the PropertyNamesConfigSourceInterceptor
        List<ConfigSource> configSources = getSources(sourcesWithPriorities);
        ConfigSourceInterceptor propertyNamesInterceptor = createPropertyNamesInterceptor(sourcesFromBuilder, current);
        current = new AtbashConfigSourceInterceptorContext(propertyNamesInterceptor, current);

        this.sources = configSources;
        this.interceptorChain = current;
    }

    public List<ConfigSource> getSources() {
        return sources;
    }

    public ConfigSourceInterceptorContext getInterceptorChain() {
        return interceptorChain;
    }

    private List<ConfigSource> buildSources(AtbashConfigBuilder builder) {
        // ConfigSources added to the ConfigBuilder
        List<ConfigSource> result = new ArrayList<>();

        result.addAll(builder.getSources());  // Not using ArrayList constructor as that sets the size and expansion
        //is required.
        if (builder.isAddDiscoveredSources()) {
            result.addAll(discoverSources(builder.getClassLoader()));
        }
        if (builder.isAddDefaultSources()) {
            result.addAll(getDefaultSources(builder.getClassLoader()));
        }

        return result;
    }

    private List<ConfigSource> getDefaultSources(ClassLoader classLoader) {
        List<ConfigSource> defaultSources = new ArrayList<>();

        defaultSources.add(new EnvConfigSource());
        defaultSources.add(new SysPropConfigSource());

        PropertiesConfigSourceProvider configSourceProvider = new PropertiesConfigSourceProvider(META_INF_MICROPROFILE_CONFIG_PROPERTIES);
        defaultSources.addAll(configSourceProvider.getConfigSources(classLoader));
        return defaultSources;
    }

    private List<ConfigSource> discoverSources(ClassLoader classLoader) {
        List<ConfigSource> discoveredSources = new ArrayList<>();
        ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class);
        for (ConfigSource source : configSourceLoader) {
            discoveredSources.add(source);
        }

        // load all ConfigSources from ConfigSourceProviders
        ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class);
        for (ConfigSourceProvider configSourceProvider : configSourceProviderLoader) {
            for (ConfigSource configSource : configSourceProvider.getConfigSources(classLoader)) {
                discoveredSources.add(configSource);
            }
        }


        // TODO Documentation on how to use this type of ConfigSource provisioning.
        ServiceLoader<ConfigSourceFactory> configSourceFactoryLoader = ServiceLoader.load(ConfigSourceFactory.class);
        for (ConfigSourceFactory factory : configSourceFactoryLoader) {
            discoveredSources.add(new ConfigurableConfigSource(factory));
        }

        return discoveredSources;
    }

    private List<InterceptorWithPriority> buildInterceptors(AtbashConfigBuilder builder) {
        final List<InterceptorWithPriority> interceptors = new ArrayList<>();
        // Add the discovered one through ServiceLoader
        if (builder.isAddDiscoveredInterceptors()) {
            interceptors.addAll(discoverInterceptors(builder.getClassLoader()));
        }
        if (builder.isAddDefaultInterceptors()) {
            interceptors.addAll(getDefaultInterceptors());
        }

        interceptors.sort(null);
        return interceptors;
    }

    private List<InterceptorWithPriority> getDefaultInterceptors() {
        List<InterceptorWithPriority> interceptors = new ArrayList<>();
        // Intercept that handles Profiles.
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
                return new ProfileConfigSourceInterceptor(context);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 200);
            }
        }));

        // Interceptor that handles Expressions
        interceptors.add(new InterceptorWithPriority(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
                return new ExpressionConfigSourceInterceptor(context);
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 300);
            }
        }));

        return interceptors;
    }

    private List<InterceptorWithPriority> discoverInterceptors(ClassLoader classLoader) {
        List<InterceptorWithPriority> interceptors = new ArrayList<>();
        ServiceLoader<ConfigSourceInterceptor> interceptorLoader = ServiceLoader.load(ConfigSourceInterceptor.class,
                classLoader);
        for (ConfigSourceInterceptor configSourceInterceptor : interceptorLoader) {
            interceptors.add(new InterceptorWithPriority(configSourceInterceptor));
        }

        ServiceLoader<ConfigSourceInterceptorFactory> interceptorFactoryLoader = ServiceLoader
                .load(ConfigSourceInterceptorFactory.class, classLoader);
        for (ConfigSourceInterceptorFactory interceptorFactory : interceptorFactoryLoader) {
            interceptors.add(new InterceptorWithPriority(interceptorFactory));
        }

        return interceptors;
    }

    private List<ConfigSourceWithPriority> mapSources(List<ConfigSource> sources) {
        List<ConfigSourceWithPriority> sourcesWithPriority = new ArrayList<>();
        for (ConfigSource source : sources) {
            if (!(source instanceof ConfigurableConfigSource)) {
                sourcesWithPriority.add(new ConfigSourceWithPriority(source));
            }
        }
        return sourcesWithPriority;
    }

    private List<String> getProfiles(List<ConfigSourceInterceptor> interceptors) {
        for (ConfigSourceInterceptor interceptor : interceptors) {
            if (interceptor instanceof ProfileConfigSourceInterceptor) {
                return Arrays.asList(((ProfileConfigSourceInterceptor) interceptor).getProfiles());
            }
        }
        return Collections.emptyList();
    }

    private List<ConfigSourceWithPriority> mapLateSources(AtbashConfigSourceInterceptorContext initChain
            , List<ConfigSource> sources
            , List<String> profiles) {

        ConfigSourceWithPriority.resetLoadPriority();

        List<ConfigurableConfigSource> lateSources = new ArrayList<>();
        for (ConfigSource source : sources) {
            if (source instanceof ConfigurableConfigSource) {
                lateSources.add((ConfigurableConfigSource) source);
            }
        }
        lateSources.sort(Comparator.comparingInt(ConfigurableConfigSource::getOrdinal));

        List<ConfigSourceWithPriority> sourcesWithPriority = new ArrayList<>();
        for (ConfigurableConfigSource configurableSource : lateSources) {
            final List<ConfigSource> configSources = configurableSource.getConfigSources(new ConfigSourceContext() {
                @Override
                public ConfigValue getValue(String name) {
                    ConfigValue value = initChain.proceed(name);
                    return value != null ? value : ConfigValueImpl.builder().withName(name).build();
                }

                @Override
                public List<String> getProfiles() {
                    return profiles;
                }

            });

            for (ConfigSource configSource : configSources) {
                sourcesWithPriority.add(new ConfigSourceWithPriority(configSource));
            }
        }

        sourcesWithPriority.addAll(mapSources(sources));
        sourcesWithPriority.sort(Collections.reverseOrder());

        return sourcesWithPriority;
    }

    private List<ConfigSource> getSources(List<ConfigSourceWithPriority> sourceWithPriorities) {
        final List<ConfigSource> configSources = new ArrayList<>();
        for (ConfigSourceWithPriority configSourceWithPriority : sourceWithPriorities) {
            configSources.add(configSourceWithPriority.getSource());
        }
        return Collections.unmodifiableList(configSources);
    }

    private ConfigSourceInterceptor createPropertyNamesInterceptor(List<ConfigSource> sources, AtbashConfigSourceInterceptorContext current) {
        final Set<String> properties = new HashSet<>();
        final Iterator<String> iterateNames = current.iterateNames();
        while (iterateNames.hasNext()) {
            String name = iterateNames.next();
            properties.add(name);
        }
        return new PropertyNamesConfigSourceInterceptor(properties, sources);
    }


    public static class InterceptorWithPriority implements Comparable<InterceptorWithPriority> {
        private final ConfigSourceInterceptorFactory factory;
        private final int priority;

        InterceptorWithPriority(ConfigSourceInterceptor interceptor) {
            this(new ConfigSourceInterceptorFactory() {
                @Override
                public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
                    return interceptor;
                }

                @Override
                public OptionalInt getPriority() {
                    OptionalInt parentPriority = ConfigSourceInterceptorFactory.super.getPriority();
                    if (parentPriority.isPresent()) {
                        return parentPriority;
                    }

                    return AnnotationUtil.getPriority(interceptor.getClass());
                }
            });
        }

        InterceptorWithPriority(ConfigSourceInterceptorFactory factory) {
            this.factory = factory;
            this.priority = factory.getPriority().orElse(DEFAULT_PRIORITY);
        }

        ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
            return factory.getInterceptor(context);
        }

        @Override
        public int compareTo(InterceptorWithPriority other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    public static class ConfigSourceWithPriority implements Comparable<ConfigSourceWithPriority>, Serializable {

        private final ConfigSource source;
        private final int priority;
        private final int loadPriority = loadPrioritySequence++;

        ConfigSourceWithPriority(ConfigSource source) {
            this.source = source;
            this.priority = source.getOrdinal();
        }

        public ConfigSource getSource() {
            return source;
        }

        @Override
        public int compareTo(ConfigSourceWithPriority other) {
            int res = Integer.compare(this.priority, other.priority);
            return res != 0 ? res : Integer.compare(other.loadPriority, this.loadPriority);
        }

        private static int loadPrioritySequence = 0;

        static void resetLoadPriority() {
            loadPrioritySequence = 0;
        }
    }
}


