package io.github.chords.bot.source;

import io.github.chords.bot.source.api.Source;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * akkords.pro chords source implementation
 */
@Service
@Slf4j
public class AkkordsProSource implements Source {
    @Override
    public String getName() {
        return "akkords.pro";
    }

    @Override
    public List<String> getChords(String songName) {
        try {
            var doc = Jsoup.connect("https://akkords.pro/?s=" + URLEncoder.encode(songName, StandardCharsets.UTF_8))
                    .get();
            var results = Optional.ofNullable(doc.selectFirst(".post-cards"));
            var names = results.map(r -> r.select(".post-card__title"));
            var songTexts = new ArrayList<String>();
            if (names.isPresent()) {
                for (var name : names.get()) {
                    var ref = Optional.ofNullable(name.selectFirst("a")).map(r -> r.attr("href"));
                    if (ref.isPresent()) {
                        var songPage = Jsoup.connect(ref.get()).get();
                        var songText = songPage.selectFirst(".chords");
                        songTexts.add(loadText(songText));
                    }
                }
            }
            return songTexts;
        } catch (Exception e) {
            log.error("Ошибка поиска аккордов", e);
            return Collections.emptyList();
        }
    }

    private String loadText(Element songText) {
        StringBuilder result = new StringBuilder("`");
        if (songText != null) {
            for (Node node : songText.childNodes()) {
                if (!node.outerHtml().contains("<span>") && !node.outerHtml().replace(" ", "").equals("")) {
                    result.append(System.lineSeparator());
                }
                result.append(node.outerHtml()
                        .replace("<span>", "")
                        .replace("</span>", "")
                        .replace("<b>", "")
                        .replace("</b>", "")
                );
                if (!node.outerHtml().contains("<span>") && !node.outerHtml().replace(" ", "").equals("")) {
                    result.append(System.lineSeparator());
                }
            }
        }
        return result.append("`").toString();
    }
}