package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Alias;
import me.escoffier.certs.junit5.Certificate;
import me.escoffier.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-credentials-provider-alias", password = "secret123!", formats = { Format.JKS,
                Format.PKCS12 }, aliases = @Alias(name = "my-alias", password = "alias-secret123!", subjectAlternativeNames = "dns:acme.org"))
})
public class NamedJKSKeyStoreWithAliasCredentialsProviderTest {

    private static final String configuration = """
            quarkus.tls.foo.key-store.jks.path=target/certs/test-credentials-provider-alias-keystore.jks
            quarkus.tls.foo.key-store.jks.alias=my-alias
            quarkus.tls.foo.key-store.credentials-provider.name=tls
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyCredentialProvider.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    Registry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.get("foo").orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate("my-alias");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("dns:acme.org");
        });
    }

    @ApplicationScoped
    public static class MyCredentialProvider implements CredentialsProvider {

        private final Map<String, Map<String, String>> credentials = Map.of("tls",
                Map.of(CredentialsProvider.PASSWORD_PROPERTY_NAME, "secret123!",
                        "alias-password", "alias-secret123!"));

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return credentials.get(credentialsProviderName);
        }
    }
}
