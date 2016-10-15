package no.difi.vefa.peppol.lookup.reader;

import no.difi.vefa.peppol.common.lang.PeppolRuntimeException;
import no.difi.vefa.peppol.common.model.*;
import no.difi.vefa.peppol.common.util.DomUtils;
import no.difi.vefa.peppol.lookup.api.FetcherResponse;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.lookup.api.MetadataReader;
import no.difi.vefa.peppol.security.api.PeppolSecurityException;
import no.difi.vefa.peppol.security.xmldsig.XmldsigVerifier;
import org.apache.commons.codec.binary.Base64;
import org.busdox.servicemetadata.publishing._1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class BusdoxReader implements MetadataReader {

    private static Logger logger = LoggerFactory.getLogger(BusdoxReader.class);

    public static final String NAMESPACE = "http://busdox.org/serviceMetadata/publishing/1.0/";

    private static JAXBContext jaxbContext;

    private static CertificateFactory certificateFactory;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ServiceGroupType.class, SignedServiceMetadataType.class,
                    ServiceMetadataType.class);
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (JAXBException | CertificateException e) {
            throw new PeppolRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentTypeIdentifier> parseDocumentIdentifiers(FetcherResponse fetcherResponse)
            throws LookupException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ServiceGroupType serviceGroup = unmarshaller.unmarshal(
                    new StreamSource(fetcherResponse.getInputStream()), ServiceGroupType.class).getValue();
            List<DocumentTypeIdentifier> documentTypeIdentifiers = new ArrayList<>();

            for (ServiceMetadataReferenceType reference :
                    serviceGroup.getServiceMetadataReferenceCollection().getServiceMetadataReference()) {
                String hrefDocumentTypeIdentifier =
                        URLDecoder.decode(reference.getHref().split("/services/")[1], "UTF-8");
                String[] parts = hrefDocumentTypeIdentifier.split("::", 2);

                try {
                    documentTypeIdentifiers.add(
                            new DocumentTypeIdentifier(parts[1], Scheme.of(parts[0]), URI.create(reference.getHref())));
                } catch (ArrayIndexOutOfBoundsException e) {
                    logger.warn("Unable to parse '{}'.", hrefDocumentTypeIdentifier);
                }
            }

            return documentTypeIdentifiers;
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new LookupException(e.getMessage(), e);
        }
    }

    @Override
    public ServiceMetadata parseServiceMetadata(FetcherResponse fetcherResponse)
            throws LookupException, PeppolSecurityException {
        try {
            Document doc = DomUtils.parse(fetcherResponse.getInputStream());

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<?> result = (JAXBElement) unmarshaller.unmarshal(new DOMSource(doc));
            Object o = result.getValue();

            X509Certificate signer = null;
            if (o instanceof SignedServiceMetadataType) {
                signer = XmldsigVerifier.verify(doc);
                o = ((SignedServiceMetadataType) o).getServiceMetadata();
            }

            if (!(o instanceof ServiceMetadataType))
                throw new LookupException("ServiceMetadata element not found.");

            ServiceInformationType serviceInformation = ((ServiceMetadataType) o).getServiceInformation();

            List<Endpoint> endpoints = new ArrayList<>();
            for (ProcessType processType : serviceInformation.getProcessList().getProcess()) {
                for (EndpointType endpointType : processType.getServiceEndpointList().getEndpoint()) {
                    endpoints.add(Endpoint.of(
                            ProcessIdentifier.of(
                                    processType.getProcessIdentifier().getValue(),
                                    Scheme.of(processType.getProcessIdentifier().getScheme())
                            ),
                            TransportProfile.of(endpointType.getTransportProfile()),
                            endpointType.getEndpointReference().getAddress().getValue(),
                            certificateInstance(Base64.decodeBase64(endpointType.getCertificate()))
                    ));
                }
            }

            return ServiceMetadata.of(
                    ParticipantIdentifier.of(
                            serviceInformation.getParticipantIdentifier().getValue(),
                            Scheme.of(serviceInformation.getParticipantIdentifier().getScheme())
                    ),
                    DocumentTypeIdentifier.of(
                            serviceInformation.getDocumentIdentifier().getValue(),
                            Scheme.of(serviceInformation.getDocumentIdentifier().getScheme())
                    ),
                    endpoints,
                    signer
            );
        } catch (JAXBException | CertificateException | IOException | SAXException | ParserConfigurationException e) {
            throw new LookupException(e.getMessage(), e);
        }
    }

    private X509Certificate certificateInstance(byte[] content) throws CertificateException {
        return (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(content));
    }
}
