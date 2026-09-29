# Artwork

Reuse the six bundled cap icons in [catalog](catalog.md), with source and checksums in [manifest](manifest.json). Keep lossless exact WebP in `app/src/main/res/drawable-nodpi`, preserve full canvases and backgrounds, and use explicit Compose bounds. Do not derive reward rules from artwork names.

## Real report pet artwork (2026-09-29)

`feature/report/src/main/res/drawable-nodpi/ryzhik_*` and the report's
`PetArtwork.kt` / `RewardCapArtwork.kt` reuse the main LCTApp artwork without
image edits. The 252 resources cover saved age, fur, selected accessories
(including parent caps) and special states. Main-app drawable aliases for teen
copper/sand base poses resolve to the same underlying files. Unknown looks and
WORRIED do not substitute an unrelated pet image, matching the game behavior.

## Иконка приложения «Родительские лапки»

По запросу пользователя от 2026-09-29 название приложения — «Родительские
лапки», основа иконки — приложенная иллюстрация енота с банкой монет.
Композиция адаптирована встроенным imagegen для мобильной иконки.

- [Каталог ресурсов](catalog.md)
- [Размеры, SHA-256 и параметры конвертации](manifest.json)
- [Оригинал вложения](sources/launcher-raccoon-jar/source.png)
- [Адаптированный мастер](sources/launcher-raccoon-jar/adaptive-source.png)
- [Точный запрос imagegen](sources/launcher-raccoon-jar/prompt.md)
- [Предпросмотр круглой и квадратной масок](sources/launcher-raccoon-jar/preview.png)
- [SVG одноцветного варианта](sources/launcher-raccoon-jar/monochrome.svg)

Оба PNG имеют размер 1254 × 1254. Мастер экспортируется целиком в слой
адаптивной иконки 108 dp; центральные 72 dp задают номинальную видимую область.
Внешние поля заполнены садом без растяжения крайних пикселей. Уши, лицо,
лапы и банка помещаются в круглую маску предпросмотра. Цветная сцена
используется как непрозрачный фон с прозрачным foreground; отдельного
параллакса персонажа нет. Форму адаптивной иконки задаёт Android.

Для Android 7 подготовлены круглые и скруглённые квадратные иконки из той же
центральной области. Для тематических иконок используется вектор банки с
лапкой, перенесённый вместе с SVG из основного LCTApp
(`docs/design/assets/sources/launcher-fox-jar/monochrome.svg`). Схема упаковки
и скрипт экспорта также основаны на LCTApp. Это технические решения адаптации,
а не отдельные утверждённые пользователем варианты дизайна.

Растровые ресурсы хранятся в `app/src/main/res/mipmap-*` как lossless exact
WebP, ресемплинг Lanczos выполняется один раз от мастера для каждого файла.
Оригинал и мастер сохраняются без изменений за пределами Android-ресурсов.
Файлы `ic_launcher.xml` и `ic_launcher_round.xml` связывают слои; манифест
приложения уже ссылается на эти имена. Имя пакета остаётся `ru.nksk.parentsapp`.

Повторный экспорт (Python 3, Pillow 12.3.0) и проверка:

```sh
python3 scripts/import_launcher_icon.py
./gradlew :app:assembleDebug :app:lintDebug
```

Предпросмотр — симуляция масок и размеров, не снимок запущенного приложения.
