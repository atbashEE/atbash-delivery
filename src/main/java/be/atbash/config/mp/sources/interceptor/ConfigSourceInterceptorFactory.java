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
package be.atbash.config.mp.sources.interceptor;


import java.util.OptionalInt;

/**
 * This ConfigSourceInterceptorFactory allows to initialize a {@link ConfigSourceInterceptor}, with access to the
 * current {@link ConfigSourceInterceptorContext}.
 * <p>
 * <p>
 * Interceptors in the chain are initialized in priority order and the current
 * {@link ConfigSourceInterceptorContext} contains the current interceptor, plus all other interceptors already
 * initialized.
 * <p>
 * <p>
 * Instances of this interface will be discovered by {@code AtbashConfigBuilder#addDiscoveredInterceptors()} via the
 * {@link java.util.ServiceLoader} mechanism and can be registered by providing a
 * {@code META-INF/services/be.atbash.runtime.config.mp.sources.interceptor.ConfigSourceInterceptorFactory} which contains the fully qualified class
 * name of the custom {@link ConfigSourceInterceptor} implementation.
 * <p>
 * Based on code from SmallRye Config.
 */
public interface ConfigSourceInterceptorFactory {
    /**
     * The default priority value, {@link Priorities#APPLICATION}.
     */
    int DEFAULT_PRIORITY = Priorities.APPLICATION;

    /**
     * Gets the {@link ConfigSourceInterceptor} from the ConfigSourceInterceptorFactory. Implementations of this
     * method must provide the instance of the {@link ConfigSourceInterceptor} to add into the Config Interceptor Chain.
     *
     * @param context the current {@link ConfigSourceInterceptorContext} with the interceptors already initialized.
     * @return the {@link ConfigSourceInterceptor} to add to Config Interceptor Chain and initialize.
     */
    ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context);

    /**
     * Returns the interceptor priority. This is required, because the interceptor priority needs to be sorted
     * before doing initialization.
     *
     * @return the priority value.
     */
    default OptionalInt getPriority() {
        return OptionalInt.empty();
    }
}
