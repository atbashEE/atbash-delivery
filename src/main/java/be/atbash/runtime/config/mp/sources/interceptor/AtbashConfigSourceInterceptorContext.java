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
package be.atbash.runtime.config.mp.sources.interceptor;

import org.eclipse.microprofile.config.ConfigValue;

import java.util.Iterator;

/**
 * FIXME This just passes the code execution to the next interceptor. Reason to have this?
 */
public class AtbashConfigSourceInterceptorContext implements ConfigSourceInterceptorContext {
    private static final long serialVersionUID = 6654406739008729337L;

    private final ConfigSourceInterceptor interceptor;
    private final ConfigSourceInterceptorContext next;

    public AtbashConfigSourceInterceptorContext(ConfigSourceInterceptor interceptor, ConfigSourceInterceptorContext next) {
        this.interceptor = interceptor;
        this.next = next;
    }

    @Override
    public ConfigValue proceed(String name) {
        return interceptor.getValue(next, name);
    }

    @Override
    public Iterator<String> iterateNames() {
        return interceptor.iterateNames(next);
    }

}