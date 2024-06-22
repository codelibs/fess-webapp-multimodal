# Fess for Multimodal Search
[![Java CI with Maven](https://github.com/codelibs/fess-webapp-multimodal/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-webapp-multimodal/actions/workflows/maven.yml)

## Overview

This is a multimodal-search plugin for Fess, enabling the crawling and indexing of various media formats such as text, images, audio, and video.

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-webapp-multimodal/).

## Installation

See [Plugin](https://fess.codelibs.org/14.15/admin/plugin-guide.html) of Administration guide.

## Usage

After installing the plugin, follow these steps to configure and use it:

1. **Start Fess**: Launch Fess and log in to the administration console.

2. **Configure System Properties**: Add the following properties to the general settings under system properties:
   ```
   fess.multimodal.content.field=content_vector
   fess.multimodal.content.dimension=512
   fess.multimodal.content.method=hnsw
   fess.multimodal.content.engine=lucene
   fess.multimodal.content.space_type=cosinesimil
   fess.multimodal.min_score=0.5
   ```

3. **Update Settings**: Navigate to the scheduler page and execu te Config Reloader.

4. **Re-indexing**: Navigate to the maintenance page and execute re-indexing.

5. **Setup CLIP as Service**: For image embedding, use CLIP as Service. Start the CLIP API using Docker:
   ```sh
   git clone https://github.com/codelibs/fess-webapp-multimodal.git
   cd fess-webapp-multimodal/docker
   docker compose up -d
   ```
   This will make the CLIP API accessible at `localhost:51000`.

6. **Crawl Directories**: Crawl directories containing image files.

7. **Test Data**: If you need test data, you can download the Open Images Dataset. For example:
   ```sh
   pip install fiftyone
   fiftyone zoo datasets load open-images-v7 --split validation --kwargs max_samples=1000 -d fiftyone
   ```
   This will download 1000 images into the `fiftyone` directory.

## Contributing

We welcome contributions to enhance the functionality of this plugin. Please fork the repository and submit pull requests.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

