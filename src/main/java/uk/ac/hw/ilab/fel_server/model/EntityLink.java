package uk.ac.hw.ilab.fel_server.model;

public class EntityLink {
    private KnowledgeBase kb;
    private String identifier;

    public EntityLink(KnowledgeBase kb, String identifier) {
        this.kb = kb;
        this.identifier = identifier;
    }

    public KnowledgeBase getKb() {
        return kb;
    }

    public void setKb(KnowledgeBase kb) {
        this.kb = kb;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
