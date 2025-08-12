package org.encinet.whosTheSpy.util;

import org.encinet.whosTheSpy.WhosTheSpy;
import java.util.*;

public class WordLoader {
    private final List<WordPair> wordList = new ArrayList<>();
    private final Random random = new Random();
    private final WhosTheSpy plugin;

    public WordLoader(WhosTheSpy plugin) {
        this.plugin = plugin;
    }

    public void loadWords() {
        wordList.clear();
        List<Map<?, ?>> words = plugin.getConfig().getMapList("words");
        for (Map<?, ?> wordMap : words) {
            String civilianWord = (String) wordMap.get("civilian");
            String spyWord = (String) wordMap.get("spy");
            if (civilianWord != null && spyWord != null) {
                wordList.add(new WordPair(civilianWord, spyWord));
            }
        }
    }

    public WordPair getRandomWordPair() {
        if (wordList.isEmpty())
            return null;
        return wordList.get(random.nextInt(wordList.size()));
    }

    public static record WordPair(String civilianWord, String spyWord) {
    }
}