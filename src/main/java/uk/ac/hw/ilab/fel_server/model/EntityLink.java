package uk.ac.hw.ilab.fel_server.model;

import java.util.List;
import java.util.Objects;

public class EntityLink {
    private KnowledgeBase kb;
    private String identifier;
    private List<String> types;

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

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityLink that = (EntityLink) o;
        return kb == that.kb &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {

        return Objects.hash(kb, identifier);
    }

    @Override
    public String toString() {
        return "EntityLink{" +
                "kb=" + kb +
                ", identifier='" + identifier + '\'' +
                ", types=" + types +
                '}';
    }
}
