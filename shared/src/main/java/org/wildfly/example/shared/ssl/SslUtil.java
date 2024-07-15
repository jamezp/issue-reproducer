/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.example.shared.ssl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import org.wildfly.security.SecurityFactory;
import org.wildfly.security.ssl.SSLContextBuilder;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

/**
 * This is only meant for use in tests and should not be used outside of tests.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SslUtil {

    private static final String WORK_DIR = System.getProperty("java.io.tmpdir") + File.separator + "/test-ssl-config";

    private static final Path SHARED_KEYSTORE = Path.of(WORK_DIR, "localhost.jks");
    private static final Path CERT = Path.of(WORK_DIR, "shared.cert");
    private static final Path SERVER_KEYSTORE_FILE = Path.of(WORK_DIR, "server.keystore");
    private static final Path SERVER_TRUSTSTORE_FILE = Path.of(WORK_DIR, "server.truststore");
    private static final Path CLIENT_KEYSTORE_FILE = Path.of(WORK_DIR, "client.keystore");
    private static final Path CLIENT_TRUSTSTORE_FILE = Path.of(WORK_DIR, "client.truststore");
    private static final String ALIAS = "self-signed";
    private static final String CLIENT_DNS_STRING = "CN=localhost, OU=Test, L=Test, ST=Test, C=Test";
    private static final String SERVER_DNS_STRING = "CN=localhost, OU=Unknown, L=Unknown, ST=Unknown, C=Unknown";
    private static final String KEYSTORE_PASSWORD = "change.it.12345";

    /**
     * Creates an {@link SSLContext} for a server.
     *
     * @return the SSL context
     *
     * @throws GeneralSecurityException if an error occurs creating the SSL context
     */
    public static SSLContext createServerSslContext() throws GeneralSecurityException {
        setupOnce();
        final SecurityFactory<SSLContext> ssl = new SSLContextBuilder()
                .setClientMode(false)
                .setKeyManager(getKeyManager(SHARED_KEYSTORE))
                //.setKeyManager(getKeyManager(SERVER_KEYSTORE_FILE))
                //.setTrustManager(getTrustManager())
                .build();
        return ssl.create();
    }

    /**
     * Creates an {@link SSLContext} for a client.
     *
     * @return the SSL context
     *
     * @throws GeneralSecurityException if an error occurs creating the SSL context
     */
    public static SSLContext createClientSslContext() throws GeneralSecurityException {
        setupOnce();
        final SecurityFactory<SSLContext> ssl = new SSLContextBuilder()
                .setClientMode(true)
                .setKeyManager(getKeyManager(CLIENT_KEYSTORE_FILE))
                .setTrustManager(getTrustManager())
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
            final KeyStore clientKeyStore = loadKeyStore();
            final KeyStore clientTrustStore = loadKeyStore();
            final KeyStore serverKeyStore = loadKeyStore();
            final KeyStore serverTrustStore = loadKeyStore();

            createKeyStoreTrustStore(clientKeyStore, serverTrustStore, CLIENT_DNS_STRING);
            createKeyStoreTrustStore(serverKeyStore, clientTrustStore, SERVER_DNS_STRING);

            createTemporaryKeyStoreFile(clientKeyStore, CLIENT_KEYSTORE_FILE);
            createTemporaryKeyStoreFile(clientTrustStore, CLIENT_TRUSTSTORE_FILE);
            createTemporaryKeyStoreFile(serverKeyStore, SERVER_KEYSTORE_FILE);
            createTemporaryKeyStoreFile(serverTrustStore, SERVER_TRUSTSTORE_FILE);
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

    private static X509TrustManager getTrustManager() {
        try {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadKeyStore(CLIENT_TRUSTSTORE_FILE));

            for (TrustManager current : trustManagerFactory.getTrustManagers()) {
                if (current instanceof X509TrustManager) {
                    return (X509TrustManager) current;
                }
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Failed to create X509TrustManager", e);
        }
        throw new IllegalStateException("Unable to obtain X509TrustManager.");
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

    private static void createKeyStoreTrustStore(final KeyStore keyStore, final KeyStore trustStore, final String name) {
        final X500Principal principal = new X500Principal(name);
        final SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey = SelfSignedX509CertificateAndSigningKey
                .builder()
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName("SHA256withRSA")
                .setDn(principal)
                .setKeySize(2048)
                .build();
        final X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        try {
            keyStore.setKeyEntry(ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), KEYSTORE_PASSWORD.toCharArray(),
                    new X509Certificate[] {certificate});
            trustStore.setCertificateEntry(ALIAS, certificate);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to create TrustStore " + name, e);
        }
    }

    private static KeyStore loadKeyStore() {
        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            return ks;
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            throw new RuntimeException("Failed to load KeyStore.", e);
        }
    }

    private static void createTemporaryKeyStoreFile(final KeyStore keyStore, final Path outputFile) {
        try (OutputStream out = Files.newOutputStream(outputFile, StandardOpenOption.CREATE)) {
            keyStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        } catch (IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create KeyStore at " + outputFile, e);
        }
    }

    private static void executeKeyTool(final String... command) {
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
