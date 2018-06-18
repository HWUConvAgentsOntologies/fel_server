package uk.ac.hw.ilab.fel_server.model;

import com.google.common.collect.Multimap;

public class LinkerRequest {
    private String text;
    private Multimap<String, String> properties;
    private Multimap<String, String> context;
    private Multimap<String, String> profanity;
    private Double annotationScore;
    private Double candidateScore;

    public LinkerRequest() {
        annotationScore = null;
        candidateScore = null;
    }

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

    public Double getAnnotationScore() {
        return annotationScore;
    }

    public void setAnnotationScore(Double annotationScore) {
        this.annotationScore = annotationScore;
    }

    public Double getCandidateScore() {
        return candidateScore;
    }

    public void setCandidateScore(Double candidateScore) {
        this.candidateScore = candidateScore;
    }

    @Override
    public String toString() {
        return "LinkerRequest{" +
                "text='" + text + '\'' +
                ", properties=" + properties +
                ", context=" + context +
                ", profanity=" + profanity +
                ", annotationScore=" + annotationScore +
                ", candidateScore=" + candidateScore +
                '}';
    }
}
