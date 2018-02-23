package uk.ac.hw.ilab.fel_server.model;

import com.google.common.collect.Multimap;
import uk.ac.hw.ilab.fel_server.model.properties.WikidataProperties;

import java.util.Collection;
import java.util.Objects;

public class EntityLink {
    private KnowledgeBase kb;
    private String identifier;
    private Multimap<String, String> properties;

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

    public Multimap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Multimap<String, String> properties) {
        this.properties = properties;
    }

    public Collection<String> getValuesForProperty(String propertyURI) {
        return this.properties.get(propertyURI);
    }

    public Collection<String> getTypes() {
        switch (kb) {
            case WIKIDATA:
                return properties.get(WikidataProperties.ENTITY_TYPE);
            default:
                return null;
        }
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
                ", properties=" + properties +
                '}';
    }
}
