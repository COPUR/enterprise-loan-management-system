package com.bank.loanmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "graphql")
public class GraphQLProperties {

    private Playground playground = new Playground();
    private Settings settings = new Settings();

    public Playground getPlayground() {
        return playground;
    }

    public void setPlayground(Playground playground) {
        this.playground = playground;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public static class Playground {
        private String endpoint;
        private String subscriptionEndpoint;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSubscriptionEndpoint() {
            return subscriptionEndpoint;
        }

        public void setSubscriptionEndpoint(String subscriptionEndpoint) {
            this.subscriptionEndpoint = subscriptionEndpoint;
        }
    }

    public static class Settings {
        private String editorTheme;
        private Integer editorFontSize;
        private Integer timeoutSeconds;

        public String getEditorTheme() {
            return editorTheme;
        }

        public void setEditorTheme(String editorTheme) {
            this.editorTheme = editorTheme;
        }

        public Integer getEditorFontSize() {
            return editorFontSize;
        }

        public void setEditorFontSize(Integer editorFontSize) {
            this.editorFontSize = editorFontSize;
        }

        public Integer getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(Integer timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}