/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.wildfly.reproducer;

import java.io.Writer;
import java.util.Map;

import org.jboss.logmanager.formatters.JsonFormatter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class CustomJsonFormatter1 extends JsonFormatter {
    @Override
    protected Generator createGenerator(final Writer writer) {
        return new DelegateGenerator(super.createGenerator(writer));
    }

    private static class DelegateGenerator implements Generator {
        private final Generator delegate;

        private DelegateGenerator(final Generator delegate) {
            this.delegate = delegate;
        }

        @Override
        public Generator begin() throws Exception {
            delegate.begin();
            return this;
        }

        @Override
        public Generator add(final String key, final int value) throws Exception {
            delegate.add(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final long value) throws Exception {
            delegate.add(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) throws Exception {
            if ("mdc".equals(key) && value != null) {
                value.forEach((k, v) -> {
                    try {
                        add(k, v == null ? null : String.valueOf(v));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                delegate.add(key, value);
            }
            return this;
        }

        @Override
        public Generator add(final String key, final String value) throws Exception {
            delegate.add(key, value);
            return this;
        }

        @Override
        public Generator addMetaData(final Map<String, String> metaData) throws Exception {
            delegate.addMetaData(metaData);
            return this;
        }

        @Override
        public Generator startObject(final String key) throws Exception {
            delegate.startObject(key);
            return this;
        }

        @Override
        public Generator endObject() throws Exception {
            delegate.endObject();
            return this;
        }

        @Override
        public Generator startArray(final String key) throws Exception {
            delegate.startArray(key);
            return this;
        }

        @Override
        public Generator endArray() throws Exception {
            delegate.endArray();
            return this;
        }

        @Override
        public Generator addAttribute(final String name, final int value) throws Exception {
            delegate.addAttribute(name, value);
            return this;
        }

        @Override
        public Generator addAttribute(final String name, final String value) throws Exception {
            delegate.addAttribute(name, value);
            return this;
        }

        @Override
        public Generator end() throws Exception {
            delegate.end();
            return this;
        }

        @Override
        public boolean wrapArrays() {
            return delegate.wrapArrays();
        }
    }
}
