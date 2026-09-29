# Artwork

Reuse the six bundled cap icons in [catalog](catalog.md), with source and checksums in [manifest](manifest.json). Keep lossless exact WebP in `app/src/main/res/drawable-nodpi`, preserve full canvases and backgrounds, and use explicit Compose bounds. Do not derive reward rules from artwork names.

## Real report pet artwork (2026-09-29)

`feature/report/src/main/res/drawable-nodpi/ryzhik_*` and the report's
`PetArtwork.kt` / `RewardCapArtwork.kt` reuse the main LCTApp artwork without
image edits. The 252 resources cover saved age, fur, selected accessories
(including parent caps) and special states. Main-app drawable aliases for teen
copper/sand base poses resolve to the same underlying files. Unknown looks and
WORRIED do not substitute an unrelated pet image, matching the game behavior.
