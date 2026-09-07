# Void Cache Viewer

A simple app for reading void's cache and config information in a searchable way.

Note: Takes a minute to load everything on startup.

### Run
`gradle tools:app:run`


### CLI para IA
Para consultar definições do cache sem abrir a interface gráfica, passe `--cli` ao app. A saída padrão é JSON, adequada para ferramentas de IA:

`./gradlew tools:app:run --args="--cli --cache=/path/to/cache --type=npcs --query=kalphite"`

Opções disponíveis: `--type`, `--query`, `--id`, `--fields`, `--limit` e `--format=json|text`. Execute com `--help` para ver exemplos e todos os tipos suportados.

### Build
`gradle tools:app:packageReleaseUberJarForCurrentOS`

Artefacts will be produced in `build\compose\jars\`

## TODOs
- [] Loading screen
- [] Filter nested arrays
- [x] Scrollbars
- [] Fix filtering by null
- [] Select/copy from the details panel
- [] Enum replacement support e.g. component.type/contentType, anims.replayMode etc...
- [] Reverse lookup support e.g. all npcs with render emote X
- [] Column size adjusting
- [] Support non-definition types like Tables/Rows/Books