# i2p-messenger — быстрый старт (Alice ↔ Bob)

## Требования

* Запущенный I2P-роутер с включённым I2CP.
* Java 21+.
* Собранный CLI-jar: `target/i2p-messenger-1.0-SNAPSHOT-cli.jar`.

Собрать (если нужно):

```bash
mvn -DskipTests package
```

## 0) Проверка роутера I2P

```bash
# веб-консоль роутера
curl -sI http://127.0.0.1:7657 | head -n1       # ожидаем "HTTP/1.1 200" или "307"

# порт I2CP
netstat -ano | grep -E ":7654" | grep -E "LISTEN|LISTENING"
```

Если `7654` не слушает — включите I2CP в настройках роутера и перезапустите его.

## 1) Раздельные профили (ключи/входящие) для Alice и Bob

```bash

export ALICE_HOME=/c/tmp/i2p-alice
export BOB_HOME=/c/tmp/i2p-bob
mkdir -p "$ALICE_HOME" "$BOB_HOME"
```

## 2) Получить адрес Alice (b32)

```bash
cd /c/Projects/i2p-messenger/i2p-messenger

java -Dfile.encoding=UTF-8 \
  -Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
  -Di2p.messenger.home="$ALICE_HOME" \
  -jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar address
```

Скопируйте напечатанный адрес вида `xxxxxxxx...xxxx.b32.i2p`, либо поместите его в переменную автоматически:

```bash
export ALICE_B32=$(java -Dfile.encoding=UTF-8 \
  -Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
  -Di2p.messenger.home="$ALICE_HOME" \
  -jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar address)
echo "ALICE_B32=$ALICE_B32"
```

## 3) Запустить Alice на приём

Оставьте это окно открытым — оно будет слушать входящие соединения.

```bash
java -Dfile.encoding=UTF-8 \
  -Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
  -Di2p.messenger.home="$ALICE_HOME" \
  -jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar listen
```

Ожидаемый вывод:

```
Listening on: <ALICE_B32>
```

## 4) Отправить текст от Bob → Alice

В новом окне терминала:

```bash
# убедимся, что переменные заданы
export BOB_HOME=/c/tmp/i2p-bob
echo "$ALICE_B32"

java -Dfile.encoding=UTF-8 \
  -Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
  -Di2p.messenger.home="$BOB_HOME" \
  -jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar send-text "$ALICE_B32" "HALLO FROM BERLIN"
```

Проверить на стороне Alice (где запущен `listen`), что в инбоксе появился новый файл:

```bash
ls -la "$ALICE_HOME/inbox"
latest=$(ls -1t "$ALICE_HOME/inbox" | head -n1); echo "LATEST=$latest"
# посмотреть первые байты/метку протокола
od -An -t x1 -N 16 "$ALICE_HOME/inbox/$latest"
```

## 5) Отправить картинку (PNG) от Bob → Alice (опционально)

Создадим минимальный PNG и отправим его:

```bash
# tiny 1x1 PNG
printf '%s\n' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=' \
  | base64 -d > /c/tmp/dot.png

java -Dfile.encoding=UTF-8 \
  -Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
  -Di2p.messenger.home="$BOB_HOME" \
  -jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar send-image "$ALICE_B32" /c/tmp/dot.png
```

Проверьте, что у Alice появился новый файл в `inbox` и его начало соответствует PNG-сигнатуре (`89 50 4E 47 0D 0A 1A 0A`):

```bash
latest=$(ls -1t "$ALICE_HOME/inbox" | head -n1)
od -An -t x1 -N 8 "$ALICE_HOME/inbox/$latest"
```

## Подсказки и ошибки

* **`Unknown I2P host:`** — переменная адреса пуста. Проверьте `echo "$ALICE_B32"`.
* **`I2PSocketManagerFactory returned null`** — роутер/I2CP ещё не готов. Подождите пару секунд и повторите команду.
* **`duplicate destination`** — два процесса используют **один и тот же** `messenger-keys.dat`. Для Alice и Bob должны быть **разные** `i2p.messenger.home`.
* Локации по умолчанию:

    * Ключи/инбокс берутся из `-Di2p.messenger.home=<dir>`, внутри будут `messenger-keys.dat` и `inbox/`.
    * Адрес роутера/I2CP — `-Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654`.

