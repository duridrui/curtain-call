package focus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record PackManifest(List<Pack> packs) {

    record Pack(String tier, String file, long bytes, String sha256, int closed, int open, int files) {
    }

    private static final Pattern OBJECT = Pattern.compile("\\{[^{}]*\"tier\"[^{}]*\\}");

    static PackManifest parse(String json) {
        List<Pack> packs = new ArrayList<>();
        Matcher m = OBJECT.matcher(json);
        while (m.find()) {
            String o = m.group();
            packs.add(new Pack(text(o, "tier"), text(o, "file"), number(o, "bytes"), text(o, "sha256"),
                (int) number(o, "closed"), (int) number(o, "open"), (int) number(o, "files")));
        }
        return new PackManifest(List.copyOf(packs));
    }

    Pack pack(CurtainQuality quality) {
        for (Pack p : packs)
            if (p.tier().equals(quality.id))
                return p;
        return null;
    }

    private static String text(String object, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(object);
        return m.find() ? m.group(1) : "";
    }

    private static long number(String object, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(object);
        return m.find() ? Long.parseLong(m.group(1)) : -1;
    }
}
