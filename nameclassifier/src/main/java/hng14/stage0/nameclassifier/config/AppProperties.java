package hng14.stage0.nameclassifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "github.oauth")
public class AppProperties {
    private OAuthClient web = new OAuthClient();
    private OAuthClient cli = new OAuthClient();
    private String scope;

    public OAuthClient getWeb() {
        return web;
    }

    public void setWeb(OAuthClient web) {
        this.web = web;
    }

    public OAuthClient getCli() {
        return cli;
    }

    public void setCli(OAuthClient cli) {
        this.cli = cli;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public static class OAuthClient {
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}