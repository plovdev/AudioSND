# AudioSND

[![License: Free To Use](https://img.shields.io/badge/License-Free_To_Use-blue.svg)]

## !!! Движок находиться на стадии разработки!!!

##

**AudioSND** - мощный движок для работы с аудио на Java, с мнимальной задержкой(прямой слив в устройтсво вывода дал
результат 10мс)
и гибкой архитектурой.
Подходит для любого вида работ с аудио: будь то простой плеер, или realtime музыкальная игра - AudioSND справиться
всегда и везде.

Ядро движка написано на Java для простого и комфортного использования,
а реализация плеера и других важных частей сделано на C++, что дает неограниченную мощность нативного кода, и низкую
задержку.

Цель движка - подарить пользователям полноценный инструмент работы со звуком.
Он покрывает все возможности современного звука, от простого воспроизведения, до сложных действий и pipeline'ов.

## Основные возможности

1. Поддержка большинства известных аудио форматов с наружи, а внутри всевозможные PCM вариации.
2. Низкая задержка даже на старых системах(движок разрабатываеться на машине MacBook Air 2012).
3. Сложные pipline'ы действий со звуком.
4. Поддержка профессианального 3D-звука(в светлом будущем).
5. Легкая интеграция в проекты, и легкость использования даже в сложных сценариях.

## Устанвка и использвание

По сколько это полностью свободный движок, то тут есть несколько способов установки:

1. Maven dependency:
    ```xml
    <groupId>org.plovdev.audiosnd</groupId>
    <artifactId>core</artifactId>
    <version>1.0.0-BETA</version>
    ```
2. Gradle
   `implementation'org.plovdev.audiosnd:core:1.0.0-BETA'`
3. Отдельный форк движка:
   `git clone "https://github.com/anton-1488/AudioSND.git"`
4. Ваш оригинальный способ установки и сборки тут:)

Простой пример использования:

```Java
package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.player.TrackPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    void main() {
        // инициализируем движок
        try (AudioEngine engine = new NativeAudioEngine()) {
            // загружаем трек/песню
            Track track = engine.loadTrack("test1.wav");
            // получаем плеер(или ручками через OutputDevice)
            TrackPlayer player = engine.getTrackPlayer(track);
            // запускаем воспроизведение
            player.play();
            // ждем окончания(условно)
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }
}
```

## Сборка движка

Сборки движка доступны в папке builds в корне проекта, а так же на GitHub публикуються релизные версии, доступные для
скачивания и использования.

Однако можно собрать движок самостоятельно.

1. Скачать(клонировать) этот репозиторий себе.
2. выполнить скрипт build.sh(или build.bat на windows)
3. Подождать окончания результата. Сборка появиться в папке builds.

## Требования

1. Java 25+.
2. cmake/g++ 12 для сборки нативного кода.

Движок не требует жестких ресурсов от железа, и работает даже на старых устройствах.

## Примеры и документация

Посмотреть примеры, ознакомиться с документацией, туториалы и многое другое вы можете найти на странице движка:

1. [AudioSND](https://anton-1488.github.io/audiosnd-doc) - main
2. [AudioSND](https://anton-1488.github.io/audiosnd-doc/help) - help

## Лицензия

Абсолюный The Unlicense

Я с городстью ставлю такую лицензию на своих библиотеки потому что я ЗА true open source!

*(c) PlovDev 2026*

[Telegram](https://t.me/plovdev_official)