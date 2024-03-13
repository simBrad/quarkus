package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
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
public class PemTrustStoreFromClassPathTest {

    private static final String configuration = """
            quarkus.tls.trust-store.pem.certs=/certs/ca.pem
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new File("target/certs/test-formats-ca.crt"), "/certs/ca.pem")
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    Registry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        assertThat(def.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getTrustStore().getCertificate("cert-0");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }
}
