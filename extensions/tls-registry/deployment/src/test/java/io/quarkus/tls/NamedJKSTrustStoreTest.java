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
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class NamedJKSTrustStoreTest {

    private static final String configuration = """
            quarkus.tls.http.trust-store.jks.path=target/certs/test-formats-truststore.jks
            quarkus.tls.http.trust-store.jks.password=password
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
        TlsConfiguration named = certificates.get("http").orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNull();
        assertThat(def.getTrustStore()).isNull();

        assertThat(named.getTrustStoreOptions()).isNotNull();
        assertThat(named.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) named.getTrustStore().getCertificate("test-formats");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }
}
