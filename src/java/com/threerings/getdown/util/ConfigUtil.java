//
// $Id$
//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2006 Three Rings Design, Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
// more details.
//
// You should have received a copy of the GNU General Public License along with
// this program; if not, write to the: Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA

package com.threerings.getdown.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;

import com.threerings.getdown.Log;

/**
 * Parses a file containing key/value pairs and returns a {@link HashMap} with the values. Keys may
 * be repeated, in which case they will be made to reference an array of values.
 */
public class ConfigUtil
{
    /**
     * Parses a configuration file containing key/value pairs. The file must be in the UTF-8
     * encoding.
     *
     * @return a list of <code>String[]</code> instances containing the key/value pairs in the
     * order they were parsed from the file.
     */
    public static List<String[]> parsePairs (File config, boolean checkPlatform)
        throws IOException
    {
        ArrayList<String[]> pairs = new ArrayList<String[]>();
        String osname = System.getProperty("os.name");
        osname = (osname == null) ? "" : osname.toLowerCase();

        // parse our configuration file
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(config);
            BufferedReader bin = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
            String line = null;
            while ((line = bin.readLine()) != null) {
                // nix comments
                int cidx = line.indexOf("#");
                if (cidx != -1) {
                    line = line.substring(0, cidx);
                }

                // trim whitespace and skip blank lines
                line = line.trim();
                if (StringUtil.isBlank(line)) {
                    continue;
                }

                // parse our key/value pair
                String[] pair = new String[2];
                int eidx = line.indexOf("=");
                if (eidx != -1) {
                    pair[0] = line.substring(0, eidx).trim();
                    pair[1] = line.substring(eidx+1).trim();
                } else {
                    pair[0] = line;
                    pair[1] = "";
                }

                // allow a value to have a [Linux]
                if (pair[1].startsWith("[")) {
                    cidx = pair[1].indexOf("]");
                    if (cidx == -1) {
                        Log.warning("Bogus platform specifier [key=" + pair[0] +
                                    ", value=" + pair[1] + "].");
                    } else {
                        String platform = pair[1].substring(1, cidx);
                        platform = platform.trim().toLowerCase();
                        pair[1] = pair[1].substring(cidx+1).trim();
                        if (checkPlatform) {
                            if (platform.startsWith("!")) {
                                platform = platform.substring(1);
                                if (osname.indexOf(platform) != -1) {
                                    Log.info("Skipping [platform=!" + platform +
                                             ", key=" + pair[0] + ", value=" + pair[1] + "].");
                                    continue;
                                }
                            } else if (osname.indexOf(platform) == -1) {
                                Log.info("Skipping [platform=" + platform +
                                         ", key=" + pair[0] + ", value=" + pair[1] + "].");
                                continue;
                            }
                        }
                    }
                }

                pairs.add(pair);
            }

        } finally {
            StreamUtil.close(fin);
        }

        return pairs;
    }

    /**
     * Parses a configuration file containing key/value pairs. The file must be in the UTF-8
     * encoding.
     *
     * @return a map from keys to values, where a value will be an array of strings if more than
     * one key/value pair in the config file was associated with the same key.
     */
    public static HashMap<String, Object> parseConfig (File config, boolean checkPlatform)
        throws IOException
    {
        List<String[]> pairs = parsePairs(config, checkPlatform);
        HashMap<String, Object> data = new HashMap<String, Object>();

        // I thought that we could use HashMap<String, String[]> and put new String[] {pair[1]} for
        // the null case, but it mysteriously dies on launch, so leaving it as HashMap<String,
        // Object> for now
        for (Iterator iter = pairs.iterator(); iter.hasNext(); ) {
            String[] pair = (String[])iter.next();
            Object value = data.get(pair[0]);
            if (value == null) {
                data.put(pair[0], pair[1]);
            } else if (value instanceof String) {
                data.put(pair[0], new String[] { (String)value, pair[1] });
            } else if (value instanceof String[]) {
                String[] values = (String[])value;
                String[] nvalues = new String[values.length+1];
                System.arraycopy(values, 0, nvalues, 0, values.length);
                nvalues[values.length] = pair[1];
                data.put(pair[0], nvalues);
            }
        }

        return data;
    }

    /**
     * Massages a single string into an array and leaves existing array values as is. Simplifies
     * access to parameters that are expected to be arrays.
     */
    public static String[] getMultiValue (HashMap data, String name)
    {
        Object value = data.get(name);
        if (value instanceof String) {
            return new String[] { (String)value };
        } else {
            return (String[])value;
        }
    }
}
