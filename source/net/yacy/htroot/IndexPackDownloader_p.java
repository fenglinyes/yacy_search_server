// IndexPackDownloader_p.java
// -----------------------
// (C) 2025 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.htroot;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.yacy.cora.protocol.RequestHeader;
import net.yacy.kelondro.util.HFClient;
import net.yacy.search.Switchboard;
import net.yacy.server.serverObjects;
import net.yacy.server.serverSwitch;

public class IndexPackDownloader_p {

    public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header, final serverObjects post, final serverSwitch env) {
        // return variable that accumulates replacements
        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        prop.put("reload", 0);
        prop.put("download-error", 0);

        if (post != null) {
            final String dlsource = post.get("dlsource", "");
            final String dlrepoid = post.get("dlrepoid", "");
            final String dlfile = post.get("dlfile", "");

            if ("Huggingface".equals(dlsource) && !dlrepoid.isEmpty() && !dlfile.isEmpty()) {
                try {
                    final File holdFile = new File(sb.packsHoldPath, dlfile).getCanonicalFile();
                    if (!holdFile.toPath().startsWith(sb.packsHoldPath.getCanonicalFile().toPath())) {
                        throw new IOException("pack destination must be inside the hold directory");
                    }
                    HFClient.downloadFile(dlrepoid, dlfile, holdFile);
                    prop.put("reload", 1);
                } catch (final IOException e) {
                    sb.log.warn("error downloading from HuggingFace: " + e.getMessage(), e);
                    prop.put("download-error", 1);
                    prop.putHTML("download-error_message", e.getMessage());
                }
            }
        }

        // show Pack folder contents
        int i = 0; boolean dark = true;
        Map<String, List<String>> hfmap;
        try {
            hfmap = HFClient.getAllPacks(true);
        } catch (final IOException e) {
            hfmap = Collections.emptyMap();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            hfmap = Collections.emptyMap();
        }
        final Map<String, String> packsMap = sb.packsMap();
        for (final Map.Entry<String, List<String>> entry : hfmap.entrySet()) {
            final String repoid = entry.getKey();
            final List<String> files = entry.getValue();
            for (final String file: files) {
                // skip empty files
                if (file == null || file.isEmpty()) continue;
                prop.put("packs-source_" + i + "_source", "Huggingface");
                prop.put("packs-source_" + i + "_repoid", repoid);
                prop.put("packs-source_" + i + "_repourl", "https://huggingface.co/datasets/" + repoid);
                prop.put("packs-source_" + i + "_file", file);
                final String localPath = packsMap.get(file);
                if (localPath == null) {
                    prop.put("packs-source_" + i + "_process",
                        "<a class=\"btn btn-primary\" href=\"IndexPackDownloader_p.html?dlsource=Huggingface&dlrepoid=" + repoid + "&dlfile=" + file + "\" role=\"button\">download</a>"
                    );
                } else {
                    prop.put("packs-source_" + i + "_process",
                        "file is downloaded; location: " + localPath
                    );
                }
                prop.put("packs-source_" + i + "_dark", dark ? "1" : "0");
                i++;
                dark = !dark;
            }
        }
        prop.put("packs-source", i);


        // return rewrite properties
        return prop;
    }

}
