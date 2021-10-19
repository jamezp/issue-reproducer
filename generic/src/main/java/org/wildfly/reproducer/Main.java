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

import java.io.IOException;
import java.security.Provider;
import java.util.concurrent.Callable;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.ContextualModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.sasl.SaslMechanismSelector;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Command(name = "reproducer", description = "A generic command line reproducer.",
        showDefaultValues = true)
public class Main implements Callable<Integer> {
    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @Option(names = {"-c", "--configure"}, defaultValue = "false", description = "Whether or not to configure the server")
    private boolean configure;

    @Option(names = {"--host"}, defaultValue = "localhost", description = "The host name the server is running on.")
    private String host;

    @Option(names = {"-p", "--password"}, defaultValue = "admin.12345", interactive = true, description = "Client password")
    private char[] password;

    @Option(names = {"--port"}, defaultValue = "9990", description = "The port name the server is running on.")
    private int port;

    @Option(names = {"-r", "--realm"}, defaultValue = "testRealm", description = "The name of the realm")
    private String realmName;

    @SuppressWarnings("unused")
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean usageHelpRequested;

    @Option(names = {"-u", "--user"}, defaultValue = "test-admin", description = "Client user name")
    private String user;

    @Spec
    private CommandSpec spec;

    public static void main(final String[] args) {
        final CommandLine commandLine = new CommandLine(new Main());
        final int exitStatus = commandLine.execute(args);
        System.exit(exitStatus);
    }

    @Override
    public Integer call() {
        final Logger logger = Logger.getLogger(Main.class.getPackageName());
        try {
            if (configure) {
                configure();
            } else {
            final AuthenticationConfiguration authnCfg = AuthenticationConfiguration.empty()
                    .useDefaultProviders()
                    .setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("DIGEST-MD5"))
                    //.useRealm(realmName)
                    .useName(user)
                    .usePassword(password);
            final AuthenticationContext context = AuthenticationContext.captureCurrent()
                    .with(MatchRule.ALL, authnCfg);
            try (ModelControllerClient client = new ContextualModelControllerClient(ModelControllerClient.Factory.create(host, port), context)) {
                final ModelNode op = Operations.createOperation("whoami");
                op.get("verbose").set(true);
                final ModelNode identity = executeCommand(client, op);
                System.out.printf("Identity %s%n", identity);
            }
            }
        } catch (Exception e) {
            logger.error("Failed to connect to client", e);
            return 1;
        }
        return 0;
    }

    private void configure() throws IOException {
        try (ModelControllerClient client = ModelControllerClient.Factory.create(host, port)) {
            final CompositeOperationBuilder builder = CompositeOperationBuilder.create();

            final ModelNode realmAddress = Operations.createAddress("subsystem", "elytron", "filesystem-realm", realmName);
            ModelNode op = Operations.createAddOperation(realmAddress);
            op.get("path").set("fs-realm-users");
            op.get("relative-to").set("jboss.server.config.dir");
            builder.addStep(op);

            op = Operations.createOperation("add-identity", realmAddress);
            op.get("identity").set(user);
            builder.addStep(op);

            op = Operations.createOperation("set-password", realmAddress);
            op.get("identity").set(user);
            final ModelNode clearPassword = op.get("clear").setEmptyObject();
            clearPassword.get("password").set(new String(password));
            builder.addStep(op);

            final ModelNode securityDomainAddress = Operations.createAddress("subsystem", "elytron",
                    "security-domain", "testSecurityDomain");
            op = Operations.createAddOperation(securityDomainAddress);
            final ModelNode realms = op.get("realms").setEmptyList();
            final ModelNode realm = new ModelNode().setEmptyObject();
            realm.get("realm").set(realmName);
            realms.add(realm);
            op.get("default-realm").set(realmName);
            op.get("permission-mapper").set("default-permission-mapper");
            builder.addStep(op);

            final ModelNode saslAddress = Operations.createAddress("subsystem", "elytron",
                    "sasl-authentication-factory", "test-sasl-auth");
            op = Operations.createAddOperation(saslAddress);
            op.get("sasl-server-factory").set("configured");
            op.get("security-domain").set("testSecurityDomain");
            final ModelNode mechConfigs = op.get("mechanism-configurations").setEmptyList();
            final ModelNode mechName = new ModelNode();
            mechName.get("mechanism-name").set("DIGEST-MD5");
            mechConfigs.add(mechName);
            final ModelNode mechConfig = new ModelNode().setEmptyObject();
            final ModelNode mechRealmCOnfig = mechConfig.get("mechanism-realm-configurations").setEmptyList();
            final ModelNode mechRealm = new ModelNode().setEmptyObject();
            mechRealm.get("realm-name").set(realmName);
            mechRealmCOnfig.add(mechRealm);
            mechConfigs.add(mechConfig);
            builder.addStep(op);

            final ModelNode mgmtInterfaceAddress = Operations.createAddress("core-service", "management",
                    "management-interface", "http-interface");
            builder.addStep(Operations.createWriteAttributeOperation(mgmtInterfaceAddress, "http-upgrade.sasl-authentication-factory", "test-sasl-auth"));

            executeCommand(client, builder.build());
        }
    }

    private ModelNode executeCommand(final ModelControllerClient client, final ModelNode op) throws IOException {
        return executeCommand(client, Operation.Factory.create(op));
    }

    private ModelNode executeCommand(final ModelControllerClient client, final Operation op) throws IOException {
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new CommandLine.ParameterException(spec.commandLine(), String.format("Failed to execute command: %s%n%s", op.getOperation(), Operations.getFailureDescription(result)
                    .asString()));
        }
        if (result.hasDefined("response-headers")) {
            final ModelNode responseHeaders = result.get("response-headers");
            if (responseHeaders.hasDefined("process-state")) {
                if (responseHeaders.get("process-state").asString().equals("reload-required")) {
                    client.execute(Operations.createOperation("reload"));
                }
            }
        }
        return Operations.readResult(result);
    }
}
