package se.wikimedia.wikispeech.segmenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmenterServlet extends HttpServlet {

    private Map<String, SentenceDetectorME> sentenceDetectors = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void init() throws ServletException {
        try {
            addSentenceDetector("sv");
            addSentenceDetector("en");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void addSentenceDetector(String language) throws IOException {
        InputStream resource = this.getClass().getResourceAsStream("/models/sentence/" + language + "/model.bin");
        if (resource == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        SentenceModel model = new SentenceModel(resource);
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
        sentenceDetectors.put(language, sentenceDetector);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doProcess(req, resp);
    }

    @Data
    private static class Segment {
        private String text;
        private int startOffset;
        private int endOffset;
        private String type;
        private double probability;
    }

    private void doProcess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String text = req.getParameter("text");
        String language = req.getParameter("language");
        SentenceDetectorME sentenceDetector = sentenceDetectors.get(language);
        if (sentenceDetector == null) {
            resp.sendError(400, "Language " + language + " is not supported");
        } else {
            Span[] spans = sentenceDetector.sentPosDetect(text);
            List<Segment> segments = new ArrayList<>(spans.length);
            for (Span span : spans) {
                Segment segment = new Segment();
                segment.setText(span.getCoveredText(text).toString());
                segment.setStartOffset(span.getStart());
                segment.setEndOffset(span.getEnd());
                segment.setType(span.getType());
                segment.setProbability(span.getProb());
                segments.add(segment);
            }
            resp.setContentType("application/json");

            objectMapper.writeValue(resp.getWriter(), segments);
        }
    }
}
