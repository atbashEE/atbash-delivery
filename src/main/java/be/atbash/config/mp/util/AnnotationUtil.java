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

import jakarta.annotation.Priority;

import java.util.OptionalInt;

/**
 * Based on code from SmallRye Config.
 */
public final class AnnotationUtil {
    private AnnotationUtil() {
    }

    public static OptionalInt getPriority(Class<?> aClass) {
        Priority priorityAnnotation = aClass.getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            return OptionalInt.of(priorityAnnotation.value());
        } else {
            if (aClass != Object.class) {
                return getPriority(aClass.getSuperclass());
            }
            return OptionalInt.empty();
        }
    }
}
