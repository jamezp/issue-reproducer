/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

package org.wildfly.reproducer.web.servlet;

import java.io.IOException;
import java.util.Properties;
import java.util.TreeSet;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebServlet("/default")
public class DefaultServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try (JsonGenerator generator = Json.createGenerator(resp.getWriter())) {
            generator.writeStartObject();
            writeSystemProperties(generator);
            generator.writeEnd();
        }
    }

    private void writeSystemProperties(final JsonGenerator generator) {
        generator.writeStartObject("systemProperties");
        final Properties props = System.getProperties();
        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            final String value = props.getProperty(key);
            if (value == null) {
                generator.writeNull(key);
            } else {
                generator.write(key, value);
            }
        }
        generator.writeEnd();
    }
}
