package com.iread.backend.training.analysis;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KomoranMorphAnalyzer {

    private volatile Komoran komoran;

    public List<MorphemeAnalysis> analyze(String text) {
        return instance().analyze(text).getTokenList().stream()
                .map(token -> new MorphemeAnalysis(
                        token.getMorph(),
                        token.getPos(),
                        token.getBeginIndex(),
                        token.getEndIndex()
                ))
                .toList();
    }

    private Komoran instance() {
        Komoran current = komoran;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (komoran == null) {
                komoran = new Komoran(DEFAULT_MODEL.LIGHT);
            }
            return komoran;
        }
    }
}
