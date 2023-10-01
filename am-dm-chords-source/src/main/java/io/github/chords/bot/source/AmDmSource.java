package io.github.chords.bot.source;

import io.github.chords.bot.source.api.Source;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * amdm.ru chords source
 */
@Service
@Slf4j
public class AmDmSource implements Source {
    @Override
    public String getName() {
        return "amdm.ru";
    }

    @Override
    public List<String> getChords(String song) {
        try {
            var doc = Jsoup.connect("https://amdm.ru/search/?q=" + URLEncoder.encode(song, StandardCharsets.UTF_8))
                    .get();
            var table = Optional.ofNullable(doc.selectFirst(".items"));
            var tableBody = table.map(t -> t.selectFirst("tbody"));
            var rows = tableBody.map(body -> body.select("tr"));
            var result = new ArrayList<String>();
            int i = 0;
            for (var row : rows.map(Elements::next).orElse(new Elements())) {
                var artist = Optional.ofNullable(row.selectFirst(".artist_name"));
                var songUrl = artist.map(a -> a.select(".artist").last());
                if (songUrl.isPresent()) {
                    result.add(loadText(songUrl.get().attr("href")));
                }
                if (i++ == 2) {
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            log.error("Ошибка поиска аккордов", e);
            return Collections.emptyList();
        }
    }

    private String loadText(String ref) throws IOException {
        var doc = Jsoup.connect(ref).get();
        var text = Optional.ofNullable(doc.selectFirst(".podbor__text"));

        StringBuilder songText = new StringBuilder();
        for (var row : text.map(Node::childNodes).orElse(Collections.emptyList())) {
            var nodeValue = row.toString();
            if (nodeValue.startsWith("<")) {
                var parsedNodeValue = Jsoup.parse(nodeValue);
                if (parsedNodeValue.selectFirst(".podbor__keyword") != null) {
                    songText.append(parsedNodeValue.selectFirst(".podbor__keyword").text()).append(System.lineSeparator());
                }
                if (parsedNodeValue.selectFirst(".podbor__chord") != null) {
                    songText.append(parsedNodeValue.selectFirst(".podbor__chord").attr("data-chord"));
                }
            } else {
                songText.append(nodeValue);
            }
        }
        return wrapToMonospaced(splitLongLines(songText.toString()));
    }

    private String wrapToMonospaced(String originalString) {
        return "`" + originalString + "`";
    }

    private String splitLongLines(String originalString) {
        String[] prevStringEnd = new String[1];
        return Arrays.stream(originalString.split(System.lineSeparator()))
                .flatMap(s -> {
                    var resultStream = Stream.<String>empty();
                    boolean isOneString = true;
                    if (s.length() > 32) {
                        var tempString = "";
                        for (var part : s.split(" ")) {
                            if (tempString.length() < 28) {
                                tempString += (part + " ");
                            } else {
                                resultStream = Stream.concat(resultStream, Stream.of(tempString));
                                if (prevStringEnd[0] != null) {
                                    resultStream = Stream.concat(resultStream, Stream.of(prevStringEnd[0]));
                                    prevStringEnd[0] = null;
                                }
                                tempString = part + " ";
                                isOneString = false;
                            }
                        }
                        if (!isOneString) {
                            prevStringEnd[0] = tempString;
                            return resultStream;
                        }
                    }

                    if (prevStringEnd[0] != null) {
                        resultStream = Stream.concat(resultStream, Stream.of(prevStringEnd[0]));
                        prevStringEnd[0] = null;
                    }
                    return Stream.concat(resultStream, Stream.of(s));
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
