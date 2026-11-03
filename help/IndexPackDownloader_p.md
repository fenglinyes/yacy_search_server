---
page: htroot/IndexPackDownloader_p.html
help: help/IndexPackDownloader_p.md
title: Index Pack Downloader
package: import-export-federation
access: admin
kind: admin-page
backend_java: source/net/yacy/htroot/IndexPackDownloader_p.java
---

# Index Pack Downloader

## Purpose

Index Pack Downloader retrieves prepared YaCy index packs from Hugging Face datasets and saves them in `DATA/PACKS/hold`. Downloading a pack does not import it into the index; use Pack Manager to load the completed file.

## What You Can Do Here

- Browse available packs and their Hugging Face repositories.
- Download a pack, including archives of 20 GB or more, without buffering the complete file in Java memory.
- See where an already downloaded pack is stored, or read an error message if a download fails.

## Page Architecture

The download runs synchronously in the page request. The page waits until the transfer finishes; there is no background job or live progress display. The existing HTTP connection and socket inactivity timeouts still apply, rather than a total transfer-duration limit.

## Correct Use

Select **download** beside a pack and allow the request to finish. Ensure the hold filesystem has enough free space for the archive. Files are streamed to a unique `.part` file in the destination directory. Only a successful, nonempty transfer is renamed to the final filename; the advertised content length is checked when available. Temporary files are removed on handled failures, and an existing completed pack is preserved when a transfer fails. A process crash can leave an ignored `.part` file that may be removed manually when no download is running.

After completion, open Pack Manager and select **load** to import the pack. Importing requires additional disk space for the index. Avoid refreshing or repeating a download request while it is running, as each request starts a separate transfer.

## Access And Safety

Administrator access is required. YaCy protects `_p` pages as administration pages.

Protected related endpoint(s): `/IndexPackDownloader_p.html`.

## Automation And API

Page backend: `source/net/yacy/htroot/IndexPackDownloader_p.java`.

| Endpoint | Method | Access | Backend |
| --- | --- | --- | --- |
| `/IndexPackDownloader_p.html` | `GET or POST` | admin | `source/net/yacy/htroot/IndexPackDownloader_p.java` |

### Parameter Guide

| Parameter | Meaning and valid values | Care |
| --- | --- | --- |
| `dlsource` | `Huggingface` | Case-sensitive. Omit download parameters to list packs only. |
| `dlrepoid` | Dataset repository ID, such as `yacy/YaCy-Pack_scroll-common-web_wikipedia_20261001` | Uses the repository's `main` revision. |
| `dlfile` | Repository-relative pack filename | Must resolve inside the configured hold directory. URL-encode parameter values. |

Example request shape:

```http
GET /IndexPackDownloader_p.html?dlsource=Huggingface&dlrepoid=yacy%2FYaCy-Pack_scroll-common-web_wikipedia_20261001&dlfile=YaCy-Pack_scroll-common-web_wikipedia-de_20261001.jsonl.gz
```

Both GET and POST accept these parameters and start the same synchronous download. Configure automation clients to allow the full transfer duration. A successful response lists the pack's local location and refreshes to the parameter-free listing after five seconds, avoiding an automatic repeat of the download.

The HTML response displays **Download failed:** followed by an escaped error detail on failure; HTTP status alone does not indicate download success. Template properties include `download-error` (`0` or `1`), `download-error_message` on failure, `reload` (`1` on success), and the `packs-source` listing. Automation should check for the error message and confirm that the final pack appears in the listing or hold directory before requesting an import. `.part` files are not loadable packs.

## What To Expect

Transfers use bounded memory and long file sizes, so the previous byte-array limit of approximately 2 GiB does not apply. Download time depends on the archive size, network, and disk throughput. Downloading leaves the pack in hold until explicitly loaded.

## Related Pages

- [Pack Manager](IndexPackManager_p.md) loads downloaded packs into the index.
- [Pack Generator](IndexPackGenerator_p.md) creates packs from local index data.
