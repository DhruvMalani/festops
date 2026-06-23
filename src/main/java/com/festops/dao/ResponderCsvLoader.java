package com.festops.dao;

import com.festops.model.Responder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link Responder} rows from a CSV file using a {@link BufferedReader} in
 * a try-with-resources block.
 *
 * <p>Expected columns: {@code id,name,skill,lat,lng,available,currentLoad}.
 * A leading header row (first column {@code id}) is skipped.</p>
 */
public final class ResponderCsvLoader {

    private ResponderCsvLoader() {
    }

    public static List<Responder> load(String path) {
        List<Responder> responders = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (first) {
                    first = false;
                    if (line.toLowerCase().startsWith("id,")) {
                        continue; // skip header
                    }
                }
                responders.add(parse(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load responders CSV from: " + path, e);
        }
        return responders;
    }

    private static Responder parse(String line) {
        String[] f = line.split(",");
        if (f.length < 7) {
            throw new IllegalArgumentException("Malformed responder row: " + line);
        }
        return new Responder(
                f[0].trim(),
                f[1].trim(),
                f[2].trim(),
                Double.parseDouble(f[3].trim()),
                Double.parseDouble(f[4].trim()),
                Boolean.parseBoolean(f[5].trim()),
                Integer.parseInt(f[6].trim()));
    }
}
