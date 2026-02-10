package com.enterprise.openfinance.businessfinancialdata.infrastructure.security;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.config.CorporateSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class DpopProofValidationFilterTest {

    private final CorporateSecurityProperties securityProperties = new CorporateSecurityProperties();
    private final Clock clock = Clock.systemUTC();
    private final DpopProofValidationFilter filter = new DpopProofValidationFilter(securityProperties, clock);

    @Test
    void shouldAcceptValidDpopProofForProtectedEndpoint() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts read_balances read_transactions");
        String dpopProof = SecurityTestTokenFactory.dpopProof(
                "GET",
                "http://localhost/open-finance/v1/corporate/accounts",
                accessToken
        );

        MockHttpServletRequest request = protectedRequest("GET", "/open-finance/v1/corporate/accounts");
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.addHeader("DPoP", dpopProof);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldRejectWhenDpopProofMissing() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts read_balances read_transactions");

        MockHttpServletRequest request = protectedRequest("GET", "/open-finance/v1/corporate/accounts");
        request.addHeader("Authorization", "Bearer " + accessToken);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldRejectWhenDpopMethodClaimDoesNotMatchRequestMethod() throws Exception {
        String accessToken = SecurityTestTokenFactory.accessToken("read_accounts read_balances read_transactions");
        String dpopProof = SecurityTestTokenFactory.dpopProof(
                "POST",
                "http://localhost/open-finance/v1/corporate/accounts",
                accessToken
        );

        MockHttpServletRequest request = protectedRequest("GET", "/open-finance/v1/corporate/accounts");
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.addHeader("DPoP", dpopProof);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    private static MockHttpServletRequest protectedRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        return request;
    }
}
