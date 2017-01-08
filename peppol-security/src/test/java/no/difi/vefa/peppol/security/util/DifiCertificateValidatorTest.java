/*
 * Copyright 2016-2017 Direktoratet for forvaltning og IKT
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.vefa.peppol.security.util;

import com.typesafe.config.ConfigFactory;
import no.difi.vefa.peppol.common.lang.PeppolLoadingException;
import no.difi.vefa.peppol.mode.Mode;
import org.testng.annotations.Test;

public class DifiCertificateValidatorTest {

    @Test(expectedExceptions = PeppolLoadingException.class)
    public void loadingException() throws PeppolLoadingException{
        // Create invalid configuration and mode.
        Mode mode = Mode.of(ConfigFactory.parseString("security.pki = /testing.txt"), null);

        // Initiate validator without the required configuration.
        mode.initiate(DifiCertificateValidator.class);
    }
}
