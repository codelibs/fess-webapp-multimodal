# Fess Multimodal Search Plugin

[![Java CI with Maven](https://github.com/codelibs/fess-webapp-multimodal/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-webapp-multimodal/actions/workflows/maven.yml)
[![Maven Repository](https://img.shields.io/badge/Maven-maven.codelibs.org-blue)](https://maven.codelibs.org/release/org/codelibs/fess/fess-webapp-multimodal/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A multimodal search plugin for [Fess](https://fess.codelibs.org/) that lets you find images with plain-language, natural-language queries. It adds a CLIP (Contrastive Language-Image Pre-training) embedding provider and an image ingestion path; everything else -- the vector field, the mapping, the KNN query, the searcher plumbing -- is Fess 15.8 core's own content-chunk-vector infrastructure.

## Architecture

Starting with Fess 15.8, content-chunk embedding (vector field, mapping, KNN query assembly, the semantic searcher) moved into Fess core itself. This plugin no longer owns any of that. It supplies exactly two things that core does not: a CLIP embedding provider, and an image ingestion path.

- **`CasClient`** (`org.codelibs.fess.multimodal.client`) -- talks to the CLIP-as-a-Service (CAS) server from the crawler process to turn an image into a vector. Runs in the crawler child process, which has no `embeddingClientManager`, so it cannot go through core's `EmbeddingClient` abstraction.
- **`CasExtractor`** (`org.codelibs.fess.multimodal.crawler.extractor`) -- a `TikaExtractor` subclass registered for `image/gif`, `image/jpeg`, `image/png`. It calls `CasClient` during crawling and stages the base64-encoded embedding in a transient crawler metadata field.
- **`EmbeddingIngester`** (`org.codelibs.fess.multimodal.ingest`) -- at index time, decodes the staged embedding and writes it into Fess core's nested **`content_chunk_vector`** field (subfield `vector`), then sets **`content_chunk_status=done`** on the document. That status is what keeps core's Content Chunk Vector Indexer job from ever touching image documents (see "Content Chunk Vector Indexer job" below).
- **`ClipEmbeddingClient`** (`org.codelibs.fess.multimodal.embedding`) -- a core `EmbeddingClient` implementation (DI name `clipEmbeddingClient`, `getName()` returns `"clip"`) that embeds *query text* through the CLIP server's text tower, so a text query lands in the same vector space as the image embeddings above. Core's `EmbeddingClientManager` resolves it by taking `content_chunker.embedding.name` and appending `EmbeddingClient` as the component name.
- **`ClipChunkSearcher`** (`org.codelibs.fess.multimodal.rank.fusion`) -- replaces core's `SemanticChunkSearcher` (DI component name `semanticChunkSearcher`). Its `searcher` provenance value is **`multi_modal`**, not `semantic_chunk` -- core's own logging and documentation refer to `semantic_chunk`, but on this plugin's deployment `multi_modal` is what actually appears in the `searcher` field of a result. See "Facets, labels, and sort" below for what it adds on top of core's searcher.
- **`StructuredQuerySplitter`** (`org.codelibs.fess.multimodal.query`) -- a pure helper `ClipChunkSearcher` uses to split an assembled query string into free text plus structured conditions.

There is no plugin-owned vector field, no plugin-owned KNN query builder, and no plugin-owned index mapping/setting rewrite. All ten production classes are listed under `src/main/java/org/codelibs/fess/multimodal/`.

## Facets, labels, and sort

Core's `SemanticChunkSearcher` refuses any query that contains search syntax, and a facet or label selection is folded into the query string as `label:"x"` -- so on a faceted UI the vector branch silently disappears the moment a filter is applied. `ClipChunkSearcher` recovers both halves: the free text is embedded on its own, and the recovered conditions are applied as real filters, in both the approximate-KNN mode and the exact (script-score) mode. Supported condition fields are `label`, `host`, `site`, `filetype`, `mimetype`, and `lang`.

`sort=` is not one of those fields. A sort selection is embedded by core's `QueryStringBuilder` as a `sort:<field>` term, which `StructuredQuerySplitter` does not recognize, so the whole query is treated as non-splittable and the vector branch is skipped for that request -- the same as core's own behavior. This is deliberate: the vector branch cannot honor a user-requested sort order, so it steps aside rather than silently ignoring the sort.

## Requirements

- **Fess**: 15.8 or higher
- **Java**: 21 or higher
- **OpenSearch**: with the version Fess 15.8 ships (KNN capability is used through Fess core's own mapping, not configured by this plugin)
- **Docker** (optional): for running the bundled CLIP server
- **GPU** (optional): for faster embedding generation

## Installation

This plugin is **not published to Maven Central**. It is distributed through `maven.codelibs.org`, the same repository as other Fess plugins (see `pom.xml`'s `distributionManagement`).

1. Download the plugin JAR from the [Maven repository](https://maven.codelibs.org/release/org/codelibs/fess/fess-webapp-multimodal/).
2. Place it in your Fess plugin directory (`WEB-INF/plugin`), or install it from **Administration > Plugin** in the Fess admin UI.
3. Restart Fess.

See the [Plugin Administration Guide](https://fess.codelibs.org/15.8/admin/plugin-guide.html) for details.

Or add it as a Maven dependency, pointing at the CodeLibs repository:

```xml
<repository>
    <id>codelibs-repo</id>
    <url>https://maven.codelibs.org/release/</url>
</repository>
```

```xml
<dependency>
    <groupId>org.codelibs.fess</groupId>
    <artifactId>fess-webapp-multimodal</artifactId>
    <version>15.8.0</version>
</dependency>
```

## Quick Start

### 1. Start the CLIP service

```bash
git clone https://github.com/codelibs/fess-webapp-multimodal.git
cd fess-webapp-multimodal/docker
docker compose up -d
```

The CLIP API is available at `http://localhost:51000` by default.

### 2. Configure Fess

All settings below are read from the **`content_chunker.*`** system-property channel: `conf/system.properties`, or a `-Dfess.system.<key>=<value>` JVM argument. They are **not** `fess_config.properties` keys, so setting them there has no effect. Because this channel is the mutable `systemProperties` component, most of these are picked up live (no restart) -- the one exception noted below is startup-only.

```properties
# Required
content_chunker.enabled=true
content_chunker.embedding.name=clip
content_chunker.embedding.dimension=512

# Required to activate the semantic searcher, and startup-only (see note below)
content_chunker.search.enabled=true

# CLIP server connection (defaults shown)
content_chunker.embedding.clip.api.url=http://localhost:51000
content_chunker.embedding.clip.image.width=224
content_chunker.embedding.clip.image.height=224
content_chunker.embedding.clip.image.max_width=3000
content_chunker.embedding.clip.image.max_height=2000
content_chunker.embedding.clip.image.format=png

# Optional: minimum cosine similarity for a hit to be returned (see "min_score" note below)
content_chunker.search.min_score=0.12
```

**Every one of the first four is required, and failure is not graceful for two of them:**

- `content_chunker.enabled=true` -- if this is `false` (the default), core's `EmbeddingClientManager` throws an `EmbeddingException` the moment a search tries to use it. Vector search simply does not work until this is `true`.
- `content_chunker.embedding.dimension=<N>` -- there is no fallback. If it is unset, non-numeric, or not positive, `AbstractEmbeddingClient.getDimension()` throws. Set it to the dimension your CLIP model actually produces (the default CLIP `ViT-B/32` model used by the bundled `docker/clip_config.yaml` produces 512-dimensional vectors).
- `content_chunker.embedding.name=clip` -- tells core's `EmbeddingClientManager` to resolve the `clipEmbeddingClient` component (`content_chunker.embedding.name` value + `EmbeddingClient`).
- `content_chunker.search.enabled=true` -- **this one is startup-only.** It is read once, in `SemanticChunkSearcher`'s (here, `ClipChunkSearcher`'s) `@PostConstruct`, to decide whether to register with the rank fusion processor at all. Flipping it after Fess has started has no effect until Fess restarts.

**`content_chunker.search.min_score` is a cosine similarity (0..1), not the raw engine score.** Core converts your configured cosine cutoff to whatever scale the active query mode actually uses -- `(1 + cosine) / 2` for the common `lucene` + `cosinesimil` approximate-KNN combination, or `cosine + 1.0` for the exact (script-score) fallback mode. If you are migrating a threshold you tuned under the old `fess.multimodal.min_score` (which *was* the raw engine score), convert it: an old engine-score threshold of `0.56` corresponds to a new cosine threshold of `0.12` (`0.56 = (1 + 0.12) / 2`).

### 3. Apply configuration and crawl

1. **Scheduler** -> run **Config Reloader** (unless you set the properties before first boot).
2. **Maintenance** -> recreate/reindex the search index so it picks up core's `content_chunk_vector` mapping and, if applicable, the `index.knn` index setting.
3. Restart Fess if you changed `content_chunker.search.enabled`.
4. Crawl a directory or site containing images (`image/gif`, `image/jpeg`, `image/png`). Text documents are crawled and indexed as usual; only images go through this plugin's embedding path.

## Migrating from older `fess.multimodal.*` / `clip.*` keys

Every system property this plugin used before Fess 15.8 core-embedding support has been replaced or removed. There is no compatibility shim -- update `system.properties` (or your `-Dfess.system.*` arguments) to the new keys.

| Old key | Status | New key / replacement |
|---|---|---|
| `fess.multimodal.content.field` | **Removed.** The vector field is now Fess core's fixed `content_chunk_vector` (nested, subfield `vector`); there is no plugin-side field name to configure. | n/a |
| `fess.multimodal.content.dimension` | Replaced | `content_chunker.embedding.dimension` |
| `fess.multimodal.content.method` / `.content.engine` / `.content.space_type` | Replaced | `content_chunker.search.knn.*` (owned by Fess core, not this plugin) |
| `fess.multimodal.min_score` | Replaced, **unit changed** | `content_chunker.search.min_score` -- now a cosine similarity (0..1), not the raw engine score. See the conversion above (old `0.56` -> new `0.12`). |
| `clip.server.endpoint` | Replaced | `content_chunker.embedding.clip.api.url` |
| `clip.image.width` / `.height` / `.max_width` / `.max_height` / `.format` | Replaced | `content_chunker.embedding.clip.image.width` / `.height` / `.max_width` / `.max_height` / `.format` |

Classes that used to implement this configuration -- `MultiModalSearchHelper` (vector field mapping/query rewriting) and `KNNQueryBuilder` (KNN query assembly) -- no longer exist. Their responsibilities now live entirely in Fess core.

### Migrating from Fess 15.7

If you were running this plugin's 15.7-era code:

- The vector field name changes from `content_vector` (or whatever you had configured via `fess.multimodal.content.field`) to core's fixed `content_chunk_vector`. A reindex/recrawl is required either way (see below).
- The reindex workaround previously needed to preserve vectors across a mapping change is **no longer necessary** -- core owns the mapping now, so a normal index recreation carries it correctly.

## The Content Chunk Vector Indexer job

Fess core ships a scheduled job, **Content Chunk Vector Indexer**, that embeds the text content of documents that have no `content_chunk_status` yet. **Leave it disabled** (the default) for a deployment using this plugin, unless you specifically want text documents embedded too.

If you enable it:

- Text documents get embedded through the CLIP **text** tower (the same `clipEmbeddingClient` this plugin registers), joining the same vector space as the images.
- Image documents are unaffected: `EmbeddingIngester` already stamped them `content_chunk_status=done` at ingest time, and core's job only ever selects documents where `content_chunk_status` is absent (or `chunked`/`fail`) -- so a `done` image document is permanently outside the job's query and cannot be overwritten with a text embedding.

## Changing the model or dimension

If you switch CLIP models (a different `docker/clip_config.yaml` executor) or change `content_chunker.embedding.dimension`, **you must recreate the index and recrawl**. Fess's reindex operation copies existing documents as-is; it does not recompute embeddings. A dimension mismatch between old and new vectors in the same index is not something reindexing will fix for you.

## Testing

```bash
mvn clean test
```

## Development

### Building from source

```bash
git clone https://github.com/codelibs/fess-webapp-multimodal.git
cd fess-webapp-multimodal
mvn clean package
```

Building against a local Fess 15.8 checkout may require installing `fess-parent` locally first; see the CI workflow (`.github/workflows/maven.yml`) for the exact steps this project's own build uses.

### Project structure

```
src/main/java/org/codelibs/fess/multimodal/
├── client/             # CasClient, CasProtocol - talks to the CLIP server
├── crawler/extractor/  # CasExtractor - image extraction during crawling
├── embedding/          # ClipEmbeddingClient - core EmbeddingClient for query text
├── exception/          # CasAccessException
├── ingest/             # EmbeddingIngester - writes content_chunk_vector at index time
├── query/              # StructuredQuerySplitter - recovers free text + conditions
├── rank/fusion/        # ClipChunkSearcher - core SemanticChunkSearcher replacement
└── util/               # EmbeddingUtil - vector encode/decode helpers
```

DI wiring (LastaDi) is under `src/main/resources/`:

- `app++.xml` -- registers `casClient` (needed by the crawler process, which has no `embeddingClientManager`)
- `crawler/extractor++.xml` -- registers `CasExtractor` for `image/gif`, `image/jpeg`, `image/png`
- `fess_embedding++.xml` -- registers `clipEmbeddingClient`
- `fess_ingest++.xml` -- registers `embeddingIngester`
- `fess_rankfusion+semanticChunkSearcher.xml` -- replaces core's `semanticChunkSearcher` component with `ClipChunkSearcher`

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Documentation

- [Fess Documentation](https://fess.codelibs.org/)
- [CLIP Paper](https://arxiv.org/abs/2103.00020)

## Troubleshooting

**Vector search returns no semantic results / falls back to keyword-only**
- Confirm `content_chunker.enabled=true` and `content_chunker.search.enabled=true`, and that Fess was **restarted** after setting the latter.
- Confirm `content_chunker.embedding.dimension` matches your CLIP model's actual output size.
- Check that the CLIP server is reachable: `curl http://localhost:51000/health` (or whatever `content_chunker.embedding.clip.api.url` points to).
- A query using Fess search syntax (quotes, boolean operators, ranges, `sort=`) intentionally skips the vector branch.

**CLIP service connection failed**
```bash
curl http://localhost:51000/health
docker logs clip_server
```

**Images are not being embedded**
- Only `image/gif`, `image/jpeg`, and `image/png` are extracted by `CasExtractor`.
- Check the crawler log for `Failed to convert an image to a vector.` warnings.

**Changed the model/dimension and search looks wrong**
- Recreate the index and recrawl; see "Changing the model or dimension" above.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [OpenAI CLIP](https://github.com/openai/CLIP) for the foundational multimodal model
- [Jina AI](https://github.com/jina-ai) for the CLIP server implementation
- [CodeLibs](https://www.codelibs.org/) for the Fess search platform

## Support

- **Issues**: [GitHub Issues](https://github.com/codelibs/fess-webapp-multimodal/issues)
- **Documentation**: [Fess Official Docs](https://fess.codelibs.org/)
