README.md

0) Убедимся, что роутер I2P запущен
   curl -sI http://127.0.0.1:7657 | head -n1      # должно быть "HTTP/1.1 200" или "307"
   netstat -ano | grep -E ":7654" | grep -E "LISTEN|LISTENING"  # порт I2CP слушает


Если 7654 не слушает — включи I2CP в настройках роутера и перезапусти его.


1) Подготовим отдельные профили (ключи/инбокс) для Alice и Bob
   export ALICE_HOME=/c/tmp/i2p-alice
   export BOB_HOME=/c/tmp/i2p-bob

mkdir -p "$ALICE_HOME" "$BOB_HOME"


2) Получим адрес Alice (b32)
   cd /c/Projects/i2p-messenger/i2p-messenger

java -Dfile.encoding=UTF-8 \
-Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
-Di2p.messenger.home="$ALICE_HOME" \
-jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar address


Скопируй напечатанный адрес, вида xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.b32.i2p.
Для удобства положим его в переменную:

export ALICE_B32="ВСТАВЬ_СВОЙ_b32_АДРЕС_АЛИСЫ"


3) Запустим Alice на приём

Оставь это окно открытым — будет слушать входящие.

java -Dfile.encoding=UTF-8 \
-Di2p.i2cp.host=127.0.0.1 -Di2p.i2cp.port=7654 \
-Di2p.messenger.home="$ALICE_HOME" \
-jar target/i2p-messenger-1.0-SNAPSHOT-cli.jar listen


Увидишь что-то вроде:

Listening on: <ALICE_B32>


