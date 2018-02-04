package uk.ac.hw.ilab.fel_server.model;

public class LinkerRequest {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "LinkerRequest{" +
                "text='" + text + '\'' +
                '}';
    }
}
