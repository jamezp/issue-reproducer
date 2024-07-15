/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.shared.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;

import org.jboss.logging.Logger;
import org.wildfly.security.SecurityFactory;
import org.wildfly.security.ssl.SSLContextBuilder;

/**
 * This is only meant for use in tests and should not be used outside of tests.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SslUtil {

    private static final String WORK_DIR = "/tmp/test-ssl-config";

    private static final Path SHARED_KEYSTORE = Path.of(WORK_DIR, "localhost.jks");
    private static final Path CERT = Path.of(WORK_DIR, "shared.cert");
    private static final String KEYSTORE_PASSWORD = "change.it.12345";

    /**
     * Creates an {@link SSLContext} for a server.
     *
     * @return the SSL context
     *
     * @throws GeneralSecurityException if an error occurs creating the SSL context
     */
    public static SSLContext createServerSslContext(final boolean manual) throws GeneralSecurityException {
        if (!manual) {
            setupOnce();
        }
        final SecurityFactory<SSLContext> ssl = new SSLContextBuilder()
                .setClientMode(false)
                .setKeyManager(getKeyManager(SHARED_KEYSTORE))
                .build();
        return ssl.create();
    }

    /**
     * Deletes the directory where the SSL configuration is stored.
     */
    public static void clean() {
        final var dir = Path.of(WORK_DIR);
        if (Files.exists(dir)) {
            try {
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void setupOnce() {
        final Path workDir = Path.of(WORK_DIR);
        if (Files.notExists(workDir)) {
            try {
                Files.createDirectories(workDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // Use the keytool to generate the shared keystore and cert
            final var dname = "CN=localhost, OU=jakarta, O=eclipse, L=amsterdam, S=holland, C=nl";
            executeKeyTool("keytool",
                    "-v",
                    "-genkeypair",
                    "-alias", "wildfly-test",
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-dname", dname,
                    "-storepass", KEYSTORE_PASSWORD,
                    "-keystore", SHARED_KEYSTORE.toString()
            );
            executeKeyTool("keytool",
                    "-v",
                    "-export",
                    "-alias", "wildfly-test",
                    "-storepass", KEYSTORE_PASSWORD,
                    "-keystore", SHARED_KEYSTORE.toString(),
                    "-file", CERT.toString()
            );

        }
    }

    private static X509ExtendedKeyManager getKeyManager(final Path ksFile) {
        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(loadKeyStore(ksFile), KEYSTORE_PASSWORD.toCharArray());

            for (KeyManager current : keyManagerFactory.getKeyManagers()) {
                if (current instanceof X509ExtendedKeyManager) {
                    return (X509ExtendedKeyManager) current;
                }
            }
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Failed to create KeyManager " + ksFile, e);
        }
        throw new IllegalStateException("Unable to obtain X509ExtendedKeyManager.");
    }

    private static KeyStore loadKeyStore(final Path ksFile) {
        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream in = Files.newInputStream(ksFile)) {
                ks.load(in, KEYSTORE_PASSWORD.toCharArray());
            }
            return ks;
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load KeyStore " + ksFile, e);
        }
    }

    private static void executeKeyTool(final String... command) {
        final StringBuilder sb = new StringBuilder();
        List.of(command).forEach(value -> sb.append(value).append(" "));
        Logger logger = Logger.getLogger(SslUtil.class);
        logger.infof("Executing %s", sb);
        try {
            final var process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .inheritIO()
                    .start();
            final var exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("KeyTool failed with exit code " + exitCode);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
