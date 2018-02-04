package uk.ac.hw.ilab.fel_server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@ConfigurationProperties(prefix = "stanford")
public class StanfordProperties {
    private String annotators;

    public String getAnnotators() {
        return annotators;
    }

    public void setAnnotators(String annotators) {
        this.annotators = annotators;
    }

    public Properties getProperties() {
        Properties props = new Properties();

        props.setProperty("annotators", this.annotators);

        return props;
    }
}