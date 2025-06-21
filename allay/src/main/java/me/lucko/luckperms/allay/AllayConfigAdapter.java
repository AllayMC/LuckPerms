/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.allay;

import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllayConfigAdapter implements ConfigurationAdapter {
    private final LPAllayPlugin plugin;
    private final Path path;

    private Map<String, Object> config = new HashMap<>();

    public AllayConfigAdapter(LPAllayPlugin plugin, Path path) {
        this.plugin = plugin;
        this.path = path;
        this.reload();
    }

    @Override
    public void reload() {
        try (var inputStream = Files.newInputStream(path)) {
            var yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            var loaded = yaml.load(inputStream);
            if (loaded instanceof Map) {
                this.config = (Map<String, Object>) loaded;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(String path, String def) {
        var val = getValue(path);
        return val instanceof String str ? str : def;
    }

    @Override
    public int getInteger(String path, int def) {
        var val = getValue(path);
        if (val instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return def;
        }
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        var val = getValue(path);
        if (val instanceof Boolean bool) {
            return bool;
        }

        if (val instanceof String str) {
            return Boolean.parseBoolean(str);
        }

        return def;
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        var val = getValue(path);
        if (val instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }

        return def;
    }

    @Override
    public Map<String, String> getStringMap(String path, Map<String, String> def) {
        var val = getValue(path);
        if (val instanceof Map<?, ?> map) {
            Map<String, String> result = new HashMap<>();
            for (var entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return result;
        }
        return def;
    }

    private Object getValue(String path) {
        if (this.config == null) {
            return null;
        }

        var keys = path.split("\\.");
        Object current = config;

        for (var key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }

            current = map.get(key);
        }

        return current;
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }
}
