package uk.ac.hw.ilab.fel_server.model;

import com.google.common.collect.Multimap;

public class LinkerRequest {
    private String text;
    private Multimap<String, String> properties;
    private Multimap<String, String> context;
    private Multimap<String, String> profanity;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Multimap<String, String> getContext() {
        return context;
    }

    public void setContext(Multimap<String, String> context) {
        this.context = context;
    }

    public Multimap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Multimap<String, String> properties) {
        this.properties = properties;
    }

    public Multimap<String, String> getProfanity() {
        return profanity;
    }

    public void setProfanity(Multimap<String, String> profanity) {
        this.profanity = profanity;
    }

    @Override
    public String toString() {
        return "LinkerRequest{" +
                "text='" + text + '\'' +
                ", properties= " + properties +
                ", context=" + context +
                ", profanity=" + profanity +
                '}';
    }
}
