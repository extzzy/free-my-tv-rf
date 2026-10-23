<div align="center">

<img src="docs/assets/icon-512.png" width="96" alt="">

# Free My TV — без TMDB

**Форк [simbasjungles/free-my-tv](https://github.com/simbasjungles/free-my-tv): быстрый главный экран без рекламы для Google TV и Android TV, с фото ночного неба вместо подборок фильмов.**

[Исходный проект](https://github.com/simbasjungles/free-my-tv) · [Что изменено](#что-изменено) · [Установка](#установка) · [Как вернуть заводской экран](#как-вернуть-заводской-экран) · [Авторы и лицензии](#авторы-и-лицензии)

</div>

## Об этом форке

Free My TV — главный экран для Android TV и Google TV от [@simbasjungles](https://github.com/simbasjungles): без рекламы, с вашими приложениями на полке, погодой и курсами, с откатом к заводскому экрану одной кнопкой. Полное описание исходного проекта — в [его README](https://github.com/simbasjungles/free-my-tv#readme).

Этот форк убирает из версии для Android TV всё, что связано с TMDB (подборки фильмов и сериалов), и делает экран проще и спокойнее. Ключи API не нужны вовсе.

Основан на коммите [`4d73951`](https://github.com/simbasjungles/free-my-tv/commit/4d73951) исходного репозитория. Проверен на Xiaomi Mi TV (MiTV-AYFR0, Android TV 14).

## Что изменено

### TMDB и Watchmode удалены

- **Главный экран.** Нет заголовка «Для вас», вкладок «Обзор / Фильмы / Сериалы», карточек с фильмами и размытых кадров из фильмов на фоне. Вместо них — дата, большие часы, погода и курсы.
- **Заставка.** Нет слайд-шоу с кадрами фильмов: только часы, дата, погода и курсы.
- **Настройки.** Нет блока «Рекомендации» с состоянием TMDB.
- **Watchmode.** Нет открытия страницы конкретного фильма в приложении.
- **Настройки сборки.** Из `config.properties` и сборки убраны `tmdb_key`, `tmdb_region`, `watchmode_key`, `hide_services` и `hide_animation`.
- **Код.** Удалены `Recs.java` (подборки) и `Backdrop.java` (фоны из кадров), шейдер стеклянных часов заставки и неиспользуемые строки переводов. Всего около 3000 строк.

### Фон — фото ночного неба

- **Фон главного экрана и заставки** — статичное фото Млечного Пути ([`app/assets/sky.jpg`](app/assets/sky.jpg), CC0, см. [авторов](#авторы-и-лицензии)). На заставке фото немного затемнено, чтобы часы читались лучше.
- **Всегда тёмный вид:** белый текст и дымчатое стекло кнопок. Переключатель «Тема» в настройках Free My TV на главный экран больше не влияет.
- **Полка приложений без стеклянной подложки:** плитки стоят прямо на фоне, на тех же местах.

### Звуки навигации как на заводском экране

- Вместо своего синтезированного щелчка играют системные звуки Android TV (`Effect_Tick`, как у заводского лаунчера) — и при перелистывании, и при открытии.
- Звуки играют на максимальной громкости, которую Android даёт приложению. Это +6 дБ к системному уровню: по умолчанию Android приглушает эти звуки до половины.
- Они по-прежнему слушаются системного переключателя звуков интерфейса и пункта «Звуки навигации» в настройках Free My TV.

### Что осталось как в исходном проекте

- Переключение главного экрана и откат на *Original*, страховка от сбоев (watchdog), отключение рекламных приложений, полка и её редактор, нижняя панель с входами, погодой и курсами, кнопки пульта на TCL, Sony и других.
- **Версии для LG (webOS) и Samsung (Tizen)** в папке [`web/`](web) этот форк не трогал: там подборки TMDB остались как в исходном проекте. Ключа TMDB в `config.properties` больше нет, поэтому эти сборки работают без подборок.

## Установка

Около десяти минут. Нужен Google TV или Android TV (Android 8 или новее) и компьютер в той же сети Wi-Fi.

1. **На ТВ** включите режим разработчика: *Настройки → Система → Об устройстве* → 7 раз нажмите *Сборка ОС Android TV*. Затем *Настройки → Система → Для разработчиков* → включите **Отладку по USB**. Запишите IP-адрес ТВ (*Настройки → Сеть и Интернет*).
2. **Инструменты на компьютере:** JDK 17+, Android SDK command-line tools (build-tools 34, platform android-34) и adb. На Mac:
   ```bash
   brew install openjdk@17
   ```
   ```bash
   brew install --cask android-commandlinetools android-platform-tools
   ```
   ```bash
   sdkmanager "build-tools;34.0.0" "platforms;android-34"
   ```
   Если `java` не находится, укажите JDK: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
3. **Сборка:**
   ```bash
   ./build.sh
   ```
   Первый запуск создаёт `config.properties`. Впишите туда свой город (`latitude`, `longitude`, `city`) для погоды и запустите сборку ещё раз. Ключи API не нужны.
4. **Установка** (вместо `192.168.1.50` — IP вашего ТВ):
   ```bash
   adb connect 192.168.1.50:5555
   ```
   ```bash
   adb install -r FreeMyTV.apk
   ```
   ТВ спросит *Разрешить отладку по USB?* — отметьте *Всегда разрешать* и подтвердите.
5. **На ТВ** откройте *Free My TV → Home Screen → Free My TV*. ТВ ещё раз спросит про отладку — теперь для самого Free My TV.

Если Wi-Fi на ТВ был сохранён при первой настройке, переключение попросит забыть сеть и подключиться к ней заново (*Настройки → Сеть и Интернет*). Так Free My TV защищает сохранённую сеть от удаления. На TCL нужен ещё один шаг — см. [исходный README](https://github.com/simbasjungles/free-my-tv#the-manual-way).

Сохраните `freemytv.keystore`: обновления подписываются этим ключом. Он, как и `config.properties`, не попадает в git.

## Как вернуть заводской экран

На ТВ: *Free My TV → Home Screen → Original*. Заводской главный экран, заставка и все отключённые приложения вернутся как были. Подробнее — в [исходном README](https://github.com/simbasjungles/free-my-tv#going-back).

## Авторы и лицензии

- **Free My TV** — [@simbasjungles](https://github.com/simbasjungles) и участники [исходного проекта](https://github.com/simbasjungles/free-my-tv), лицензия [MIT](LICENSE). Изменения этого форка распространяются на тех же условиях.
- **Фото ночного неба** — Guillaume ([Unsplash](https://unsplash.com/)), [«Milky Way Night Sky»](https://commons.wikimedia.org/wiki/File:Milky_Way_Night_Sky_(Unsplash).jpg) на Wikimedia Commons, [CC0](https://creativecommons.org/publicdomain/zero/1.0/). Кадрировано до 16:9.
- **Звуки навигации** — системные звуки самого ТВ, в приложение не входят.
- Погода — [Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0), курсы — открытый API [Binance](https://www.binance.com/). Иконки — [Material Symbols](https://fonts.google.com/icons) (Apache 2.0), шрифт — [Inter](https://rsms.me/inter/) (SIL OFL 1.1), логотипы монет — [cryptocurrency-icons](https://github.com/spothq/cryptocurrency-icons) (CC0). Остальное — в [исходном README](https://github.com/simbasjungles/free-my-tv#credits).
- Не связан с Google, Xiaomi, TCL, LG, Samsung и стриминговыми сервисами. Названия и логотипы принадлежат их владельцам.
