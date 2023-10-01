package io.github.chords.bot.source.api;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Chords source interface
 */
@Service
public interface Source {
    /**
     * @return chords source name
     */
    String getName();

    /**
     * get chords by song name
     * 
     * @param songName
     * @return chords
     */
    List<String> getChords(String songName);
}
