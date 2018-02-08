package uk.ac.hw.ilab.fel_server.model;

import com.google.common.collect.Multimap;

import java.util.List;

public class LinkerRequest {
    private String text;
    private List<String> types;
    private Multimap<String, String> context;

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

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    @Override
    public String toString() {
        return "LinkerRequest{" +
                "text='" + text + '\'' +
                ", types=" + types +
                ", context=" + context +
                '}';
    }
}
