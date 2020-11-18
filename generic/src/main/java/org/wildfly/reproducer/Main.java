/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@CommandLine.Command()
public class Main implements Callable<Integer> {


    @Option(required = true, names = {"-u", "--user"}, description = "The user to authenticate with.", interactive = true)
    private String user;

    @Option(required = true, names = {"-p", "--password"}, description = "The password to authenticate with.", interactive = true)
    private char[] password;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean usageHelpRequested;

    @Parameters(paramLabel = "URL", description = "The URL or URL's to execute the rest client on.", index = "0..*")
    private String[] urls;

    public static void main(final String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);

        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try (StdoutClient client = new StdoutClient(user, password)) {
            for (String url : urls) {
                client.accept(url);
            }
        }
        return 0;
    }
}
