# Upgrading

Notes for moving an existing deployment of the Fess Multimodal Search Plugin to a newer version. For a fresh install, see [README.md](README.md).

## Migrating from older `fess.multimodal.*` / `clip.*` keys

Every system property this plugin used before Fess 15.8 core-embedding support has been replaced or removed. There is no compatibility shim -- update `system.properties` (or your `-Dfess.system.*` arguments) to the new keys.

If you are migrating a threshold you tuned under the old `fess.multimodal.min_score` (which *was* the raw engine score), convert it: an old engine-score threshold of `0.56` corresponds to a new cosine threshold of `0.12` (`0.56 = (1 + 0.12) / 2`).

| Old key | Status | New key / replacement |
|---|---|---|
| `fess.multimodal.content.field` | **Removed.** The vector field is now Fess core's fixed `content_chunk_vector` (nested, subfield `vector`); there is no plugin-side field name to configure. | n/a |
| `fess.multimodal.content.dimension` | Replaced | `content_chunker.embedding.dimension` |
| `fess.multimodal.content.method` / `.content.engine` / `.content.space_type` | Replaced | `content_chunker.search.knn.*` (owned by Fess core, not this plugin) |
| `fess.multimodal.min_score` | Replaced, **unit changed** | `content_chunker.search.min_score` -- now a cosine similarity (0..1), not the raw engine score. See the conversion above (old `0.56` -> new `0.12`). |
| `clip.server.endpoint` | Replaced | `content_chunker.embedding.clip.api.url` |
| `clip.image.width` / `.height` / `.max_width` / `.max_height` / `.format` | Replaced | `content_chunker.embedding.clip.image.width` / `.height` / `.max_width` / `.max_height` / `.format` |

Classes that used to implement this configuration -- `MultiModalSearchHelper` (vector field mapping/query rewriting) and `KNNQueryBuilder` (KNN query assembly) -- no longer exist. Their responsibilities now live entirely in Fess core.

## Migrating from Fess 15.7

If you were running this plugin's 15.7-era code:

- The vector field name changes from `content_vector` (or whatever you had configured via `fess.multimodal.content.field`) to core's fixed `content_chunk_vector`. A reindex/recrawl is required either way (see [Changing the model or dimension](README.md#changing-the-model-or-dimension)).
- The reindex workaround previously needed to preserve vectors across a mapping change is **no longer necessary** -- core owns the mapping now, so a normal index recreation carries it correctly.
