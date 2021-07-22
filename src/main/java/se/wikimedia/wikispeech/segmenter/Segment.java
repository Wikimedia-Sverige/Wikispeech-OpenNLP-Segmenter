package se.wikimedia.wikispeech.segmenter;

import lombok.Data;
import opennlp.tools.util.Span;

@Data
public class Segment {

    private String input;
    private Span span;

    @Override
    public String toString() {
        return span.getCoveredText(input).toString();
    }

}
