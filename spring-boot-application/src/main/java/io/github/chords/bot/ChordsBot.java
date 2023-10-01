package io.github.chords.bot;

import com.github.kshashov.telegram.api.MessageType;
import com.github.kshashov.telegram.api.TelegramMvcController;
import com.github.kshashov.telegram.api.bind.annotation.BotController;
import com.github.kshashov.telegram.api.bind.annotation.BotPathVariable;
import com.github.kshashov.telegram.api.bind.annotation.BotRequest;
import com.github.kshashov.telegram.api.bind.annotation.request.MessageRequest;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import io.github.chords.bot.source.api.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;

@BotController
@SpringBootApplication
@Slf4j
public class ChordsBot implements TelegramMvcController {
    @Value("${bot.token:}")
    private String token;
    @Autowired(required = false)
    private List<Source> chordsSources;

    @Override
    public String getToken() {
        return token;
    }

    @BotRequest(value = "/start", type = { MessageType.CALLBACK_QUERY, MessageType.MESSAGE })
    public BaseRequest<SendMessage, SendResponse> hello(User user, Chat chat) {
        return new SendMessage(chat.id(), "Hello, `" + user.firstName() + "`!")
                .parseMode(ParseMode.Markdown);
    }

    @MessageRequest("{song:[\\S].*}")
    public BaseRequest<SendMessage, SendResponse> findChords(@BotPathVariable("song") String song, Chat chat) {
        var songName = song;
        var parsedSource = Optional.empty();
        Optional<Integer> parsedIndex = Optional.empty();
        if (song.contains("#") && song.contains("[")) {
            songName = song.substring(0, song.indexOf('#') - 1);
            parsedIndex = Optional.of(Integer.parseInt(song.substring(song.indexOf('#') + 1, song.indexOf('[') - 1)));
            parsedSource = Optional.of(song.substring(song.indexOf('[') + 1, song.indexOf(']')));
        }


        final var finalSongName = songName;
        var allResults = chordsSources.parallelStream()
                .map(source -> {
                    log.info("Поиск {} в источнике {}", finalSongName, source.getName());
                    var chords = source.getChords(finalSongName);
                    var index = 1;
                    var result = new HashMap<String, List<Chords>>();
                    for (var res : chords) {
                        result.putIfAbsent(source.getName(), new ArrayList<>());
                        result.get(source.getName()).add(new Chords(source.getName(), index++, res));
                    }
                    return result;
                })
                .reduce(new HashMap<>(), (l, r) -> {
                    l.putAll(r);
                    return l;
                });
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(new String[0]);
        keyboardMarkup.oneTimeKeyboard(true);
        allResults.values()
                .stream().flatMap(Collection::stream)
                .forEach(r -> {
                    keyboardMarkup.addRow(finalSongName + " #" + r.index + " [" + r.source + "]");
                });

        if (parsedIndex.isPresent()) {
            List<Chords> sourceChords = allResults.get(parsedSource.get());
            for (var chords : sourceChords) {
                if (chords.index == parsedIndex.get()) {
                    return new SendMessage(chat.id(), chords.text).parseMode(ParseMode.MarkdownV2);
                }
            }
        } else {
            return new SendMessage(chat.id(), "Выберите вариант:").replyMarkup(keyboardMarkup).parseMode(ParseMode.MarkdownV2);
        }

        return new SendMessage(chat.id(), "Не найдено");
    }

    public static void main(String... agrs) {
        SpringApplication.run(ChordsBot.class, agrs);
    }

    record Chords(String source, int index, String text){};
}
