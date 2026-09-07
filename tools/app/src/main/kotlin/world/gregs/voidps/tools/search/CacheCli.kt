package world.gregs.voidps.tools.search

import kotlinx.coroutines.runBlocking
import world.gregs.voidps.cache.Definition
import world.gregs.voidps.tools.search.screen.view.tab.DefinitionTab
import world.gregs.voidps.tools.search.screen.view.tab.buildTabs
import java.lang.reflect.Array as JavaArray
import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KProperty1
import kotlin.system.exitProcess

object CacheCli {
    fun run(args: Array<String>) {
        try {
            val options = parse(args)
            if (options.help) {
                printUsage()
                return
            }
            require(options.cache != null) { "Missing required option: --cache=<path>" }
            inspect(options)
        } catch (e: IllegalArgumentException) {
            System.err.println("error: ${e.message}")
            printUsage()
            exitProcess(2)
        } catch (e: Exception) {
            System.err.println("error: ${e.message ?: e::class.simpleName}")
            exitProcess(1)
        }
    }

    private fun inspect(options: Options) {
        val path = options.cache!!
        val (selectedTabs, loaded) = silenceStdout {
            val tabs = buildTabs(path).getOrElse { throw IllegalArgumentException(it.message ?: it::class.simpleName) }
            val selectedTabs = selectTabs(tabs, options.type)
            selectedTabs to runBlocking { loadTabs(selectedTabs) }
        }
        val requestedType = options.type.orEmpty()
        val includeAllTypes = requestedType.isEmpty() || requestedType.equals("all", ignoreCase = true)
        val resultTypes = selectedTabs
            .filter { includeAllTypes || canonicalType(it.label) == canonicalType(requestedType) }
            .map { it.label }
        val results = loaded.asSequence()
            .filter { (tab, _) -> tab.label in resultTypes }
            .flatMap { (tab, definitions) ->
                definitions.asSequence()
                    .filter { definition -> matches(definition, tab.clazz, options) }
                    .map { definition -> Result(tab.label, fields(tab.clazz, definition, options.fields)) }
            }
            .take(options.limit)
            .toList()
        when (options.format) {
            Format.JSON -> printJson(path, resultTypes, results)
            Format.TEXT -> printText(path, results)
        }
    }

    private fun selectTabs(tabs: List<DefinitionTab<*>>, type: String?): List<DefinitionTab<*>> {
        if (type == null || type.equals("all", ignoreCase = true)) {
            return tabs
        }
        val selected = tabs.firstOrNull { canonicalType(it.label) == canonicalType(type) }
            ?: throw IllegalArgumentException("Unknown type '$type'. Use --help to list supported types.")
        return tabs.filter { it.label == selected.label || it.label in selected.dependsOn }
    }

    private suspend fun loadTabs(tabs: List<DefinitionTab<*>>): List<Pair<DefinitionTab<*>, List<Definition>>> {
        val byLabel = tabs.associateBy { it.label }
        val loaded = mutableMapOf<String, List<Definition>>()

        suspend fun load(tab: DefinitionTab<*>): List<Definition> {
            loaded[tab.label]?.let { return it }
            tab.dependsOn.forEach { dependency ->
                byLabel[dependency]?.let { load(it) }
            }
            val definitions = tab.loader()
            loaded[tab.label] = definitions
            return definitions
        }

        return tabs.map { tab -> tab to load(tab) }
    }

    private fun <T> silenceStdout(block: () -> T): T {
        val previous = System.out
        val sink = PrintStream(OutputStream.nullOutputStream())
        System.setOut(sink)
        return try {
            block()
        } finally {
            System.setOut(previous)
            sink.close()
        }
    }

    private fun matches(definition: Definition, clazz: Class<*>, options: Options): Boolean {
        options.id?.let { id ->
            if (definition.id.toString() != id && !fieldValue(clazz, definition, "stringId").toString().equals(id, ignoreCase = true)) {
                return false
            }
        }
        options.query?.takeIf { it.isNotBlank() }?.let { query ->
            val text = getProperties(clazz).joinToString(" ") { property ->
                displayValue(readProperty(property, definition), property.name == "params")
            }
            if (!text.contains(query, ignoreCase = true)) {
                return false
            }
        }
        return true
    }

    private fun fields(clazz: Class<*>, definition: Definition, requestedFields: Set<String>): Map<String, Any?> =
        getProperties(clazz)
            .filter { requestedFields.isEmpty() || it.name in requestedFields }
            .associate { it.name to readProperty(it, definition) }

    private fun fieldValue(clazz: Class<*>, definition: Definition, name: String): Any? =
        getProperties(clazz).firstOrNull { it.name == name }?.let { readProperty(it, definition) }

    private fun readProperty(property: KProperty1<*, *>, definition: Definition): Any? =
        try {
            @Suppress("UNCHECKED_CAST")
            (property as KProperty1<Definition, *>).get(definition)
        } catch (_: Exception) {
            null
        }

    private fun printJson(cache: String, types: List<String>, results: List<Result>) {
        val output = jsonObject(
            listOf(
                "cache" to cache,
                "types" to types,
                "count" to results.size,
                "results" to results.map { result ->
                    linkedMapOf("type" to result.type, "fields" to result.fields)
                },
            ),
        )
        println(output)
    }

    private fun printText(cache: String, results: List<Result>) {
        println("cache=$cache")
        println("count=${results.size}")
        results.forEach { result ->
            println("[${result.type}] ${result.fields.entries.joinToString(" ") { (key, value) -> "$key=${displayValue(value, key == "params")}" }}")
        }
    }

    private fun jsonObject(entries: List<Pair<String, Any?>>): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${jsonString(key)}:${jsonValue(value)}" }

    private fun jsonValue(value: Any?, depth: Int = 0): String = when (value) {
        null -> "null"
        is String, is Char, is Enum<*> -> jsonString(value.toString())
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> jsonObject(value.entries.map { it.key.toString() to it.value })
        is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { jsonValue(it, depth + 1) }
        else -> {
            val type = value.javaClass
            if (type.isArray) {
                (0 until JavaArray.getLength(value)).joinToString(prefix = "[", postfix = "]") {
                    jsonValue(JavaArray.get(value, it), depth + 1)
                }
            } else if (depth < 2) {
                val properties = getProperties(type)
                if (properties.isEmpty()) jsonString(value.toString()) else {
                    jsonObject(properties.map { it.name to runCatching { it.get(value) }.getOrNull() })
                }
            } else {
                jsonString(value.toString())
            }
        }
    }

    private fun jsonString(value: String): String = buildString(value.length + 2) {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
        append('"')
    }

    private fun canonicalType(value: String): String = value.lowercase().removeSuffix("s")

    private fun parse(args: Array<String>): Options {
        var cache: String? = null
        var type: String? = null
        var query: String? = null
        var id: String? = null
        var limit = 20
        var format = Format.JSON
        var fields = emptySet<String>()
        var help = false
        var index = 0
        while (index < args.size) {
            val argument = args[index]
            when {
                argument == "--help" || argument == "-h" -> help = true
                argument == "--cli" -> Unit
                argument.startsWith("--cache=") -> cache = argument.substringAfter('=')
                argument == "--cache" -> cache = args.valueAfter(argument, ++index)
                argument.startsWith("--type=") -> type = argument.substringAfter('=')
                argument == "--type" -> type = args.valueAfter(argument, ++index)
                argument.startsWith("--query=") -> query = argument.substringAfter('=')
                argument == "--query" -> query = args.valueAfter(argument, ++index)
                argument.startsWith("--id=") -> id = argument.substringAfter('=')
                argument == "--id" -> id = args.valueAfter(argument, ++index)
                argument.startsWith("--limit=") -> limit = argument.substringAfter('=').toIntOrNull() ?: throw IllegalArgumentException("Invalid --limit")
                argument == "--limit" -> limit = args.valueAfter(argument, ++index).toIntOrNull() ?: throw IllegalArgumentException("Invalid --limit")
                argument.startsWith("--format=") -> format = Format.parse(argument.substringAfter('='))
                argument == "--format" -> format = Format.parse(args.valueAfter(argument, ++index))
                argument.startsWith("--fields=") -> fields = argument.substringAfter('=').split(',').filter { it.isNotBlank() }.toSet()
                argument == "--fields" -> fields = args.valueAfter(argument, ++index).split(',').filter { it.isNotBlank() }.toSet()
                else -> throw IllegalArgumentException("Unknown option '$argument'")
            }
            index++
        }
        require(limit > 0) { "--limit must be greater than zero" }
        return Options(cache, type, query, id, limit, format, fields, help)
    }

    private fun Array<String>.valueAfter(option: String, index: Int): String =
        getOrNull(index) ?: throw IllegalArgumentException("Missing value for $option")

    private fun printUsage() {
        println(
            """
            Cache inspector for AI-friendly cache lookups.

            Usage:
              ./gradlew tools:app:run --args="--cli --cache=<path> [options]"

            Options:
              --type=<name>       items, npcs, objs, anims, emotes, gfx, sounds,
                                  ifaces, components, enums, vars, invs, structs, or all
              --query=<text>      case-insensitive search across definition fields
              --id=<id>           exact numeric id or stringId lookup
              --fields=a,b        return only these fields (default: all fields)
              --limit=<n>         maximum results (default: 20)
              --format=json|text  output format (default: json)
              --help              show this help

            Examples:
              ./gradlew tools:app:run --args="--cli --cache=/path/to/cache --type=npcs --id=kalphite_queen"
              ./gradlew tools:app:run --args="--cli --cache=/path/to/cache --type=npcs --query=kalphite --format=text"
            """.trimIndent(),
        )
    }

    private data class Options(
        val cache: String?,
        val type: String?,
        val query: String?,
        val id: String?,
        val limit: Int,
        val format: Format,
        val fields: Set<String>,
        val help: Boolean,
    )

    private data class Result(val type: String, val fields: Map<String, Any?>)

    private enum class Format {
        JSON,
        TEXT,
        ;

        companion object {
            fun parse(value: String): Format = when (value.lowercase()) {
                "json" -> JSON
                "text" -> TEXT
                else -> throw IllegalArgumentException("Unknown format '$value'. Use json or text.")
            }
        }
    }
}
