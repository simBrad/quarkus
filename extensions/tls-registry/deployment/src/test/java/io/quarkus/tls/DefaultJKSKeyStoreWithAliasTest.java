package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Certificate;
import me.escoffier.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-alias-jks", password = "password", formats = { Format.JKS }, aliases = {
                @me.escoffier.certs.junit5.Alias(name = "alias1", password = "alias-password", subjectAlternativeNames = "dns:acme.org"),
                @me.escoffier.certs.junit5.Alias(name = "alias2", password = "alias-password-2")
        })
})
public class DefaultJKSKeyStoreWithAliasTest {

    private static final String configuration = """
            quarkus.tls.key-store.jks.path=target/certs/test-alias-jks-keystore.jks
            quarkus.tls.key-store.jks.password=password
            quarkus.tls.key-store.jks.alias=alias1
            quarkus.tls.key-store.jks.alias-password=alias-password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    Registry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate("alias1");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("dns:acme.org");
        });
    }
}
