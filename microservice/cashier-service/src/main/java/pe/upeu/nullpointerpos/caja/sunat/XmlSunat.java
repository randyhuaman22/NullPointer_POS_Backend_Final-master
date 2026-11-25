package pe.upeu.nullpointerpos.caja.sunat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/** Firma XML y lectura segura de las respuestas de SUNAT. */
final class XmlSunat {
    private XmlSunat() {}

    static Document leer(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    static String texto(Document doc, String nombre) {
        var nodos = doc.getElementsByTagNameNS("*", nombre);
        return nodos.getLength() == 0 ? "" : nodos.item(0).getTextContent().trim();
    }

    static String firmar(String xml, ConfiguracionSunat config) throws Exception {
        var almacen = KeyStore.getInstance("PKCS12");
        char[] clave = config.getClaveCertificado().toCharArray();
        try (var entrada = Files.newInputStream(Path.of(config.getCertificadoRuta()))) {
            almacen.load(entrada, clave);
        }
        String alias = config.getCertificadoAlias();
        if (alias.isBlank()) {
            var aliases = almacen.aliases();
            while (aliases.hasMoreElements()) {
                String candidato = aliases.nextElement();
                if (almacen.isKeyEntry(candidato)) { alias = candidato; break; }
            }
        }
        var key = (PrivateKey) almacen.getKey(alias, clave);
        var cert = (X509Certificate) almacen.getCertificate(alias);
        if (key == null || cert == null) throw new IllegalArgumentException("Certificado sin clave privada");
        cert.checkValidity();
        Document doc = leer(xml.getBytes(StandardCharsets.UTF_8));
        // Las versiones anteriores guardaban un bloque de firma vacio.
        var firmasAnteriores = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        while (firmasAnteriores.getLength() > 0) {
            var firma = firmasAnteriores.item(0);
            firma.getParentNode().removeChild(firma);
        }
        var factory = XMLSignatureFactory.getInstance("DOM");
        var reference = factory.newReference("", factory.newDigestMethod(DigestMethod.SHA256, null),
            List.of(factory.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                factory.newTransform(CanonicalizationMethod.INCLUSIVE, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)), null, null);
        var info = factory.newSignedInfo(factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
            (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
            factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null), List.of(reference));
        var keyFactory = factory.getKeyInfoFactory();
        KeyInfo keyInfo = keyFactory.newKeyInfo(List.of(keyFactory.newX509Data(List.of(cert))));
        var parent = doc.getElementsByTagNameNS("urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "ExtensionContent").item(0);
        var context = new DOMSignContext(key, parent);
        context.setDefaultNamespacePrefix("ds");
        factory.newXMLSignature(info, keyInfo, null, "SignatureNullPointer", null).sign(context);
        var transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var salida = new StringWriter();
        transformerFactory.newTransformer().transform(new DOMSource(doc), new StreamResult(salida));
        return salida.toString();
    }

    static Document leerCdr(byte[] zip) throws Exception {
        try (var entrada = new ZipInputStream(new ByteArrayInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = entrada.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".xml")) {
                    byte[] bytes = entrada.readNBytes(2_000_001);
                    if (bytes.length > 2_000_000) throw new IOException("CDR demasiado grande");
                    return leer(bytes);
                }
            }
        }
        throw new IOException("Respuesta sin XML de CDR");
    }

    static String escapar(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
