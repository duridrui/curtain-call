package focus;

enum CurtainQuality {
    LOW("low", "저화질", true, true),
    MEDIUM("medium", "중화질", false, false),
    HIGH("high", "고화질", false, false);

    final String id;
    final String label;
    final boolean bundled;
    final boolean blend;

    CurtainQuality(String id, String label, boolean bundled, boolean blend) {
        this.id = id;
        this.label = label;
        this.bundled = bundled;
        this.blend = blend;
    }

    static CurtainQuality fromId(String id) {
        for (CurtainQuality q : values())
            if (q.id.equals(id == null ? null : id.trim()))
                return q;
        return LOW;
    }
}
