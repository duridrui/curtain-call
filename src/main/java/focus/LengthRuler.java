package focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

final class LengthRuler extends Region {
    private static final double HOUR_AT = 0.40;
    private static final double THREE_HOURS_AT = 0.70;

    private static final double TICK_TOP = 22;
    private static final int[] LABELS = {1, 30, 60, 120, 180, 720, 1440};
    private final List<Line> ticks = new ArrayList<>();
    private final List<Integer> tickMinutes = new ArrayList<>();
    private final Map<Integer, Label> numbers = new HashMap<>();
    private final Polygon pointer = new Polygon(0, 0, 14, 0, 7, 11);
    private final DoubleProperty at = new SimpleDoubleProperty(-1);
    private java.util.function.IntConsumer onPick = m -> { };

    LengthRuler() {
        for (int m = 5; m < 60; m += 5)
            tickMinutes.add(m);
        for (int m = 60; m < 180; m += 15)
            tickMinutes.add(m);
        for (int m = 180; m <= 1440; m += 60)
            tickMinutes.add(m);
        for (int m : tickMinutes) {
            Line line = new Line();
            line.getStyleClass().add("ruler-tick");
            if (m == 60 || m == 180 || m % 360 == 0 || m == 30)
                line.getStyleClass().add("major");
            ticks.add(line);
        }
        getChildren().addAll(ticks);
        for (int m : LABELS)
            numbers.put(m, Ui.label(lengthText(m), "ruler-num"));
        getChildren().addAll(numbers.values());
        pointer.setFill(Color.web("#D4202A"));
        pointer.setVisible(false);
        getChildren().add(pointer);
        at.addListener((obs, was, now) -> requestLayout());
        setPrefHeight(84);
        setMinHeight(84);
        setCursor(Cursor.HAND);
        setOnMousePressed(e -> pick(e.getX()));
        setOnMouseDragged(e -> pick(e.getX()));
    }

    void setOnPick(java.util.function.IntConsumer onPick) {
        this.onPick = onPick;
    }

    private void pick(double x) {
        onPick.accept(minutesAt(x / getWidth()));
    }

    void show(ShowLength length) {
        boolean visible = length != null && !length.isUnlimited();
        pointer.setVisible(visible);
        if (visible)
            at.set(fractionOf(length.getMinutes()));
    }
    @Override
    protected void layoutChildren() {
        double w = getWidth();
        for (int i = 0; i < ticks.size(); i++) {
            int m = tickMinutes.get(i);
            double x = snapPositionX(w * fractionOf(m)) + 0.75;
            Line line = ticks.get(i);
            line.setStartX(x);
            line.setEndX(x);
            line.setStartY(TICK_TOP + (line.getStyleClass().contains("major") ? 0 : 10));
            line.setEndY(TICK_TOP + 22);
        }
        numbers.forEach((m, label) -> {
            double lw = label.prefWidth(-1);
            double x = w * fractionOf(m) - lw / 2;
            label.resizeRelocate(Math.max(0, Math.min(w - lw, x)), TICK_TOP + 30, lw, label.prefHeight(-1));
        });
        pointer.setLayoutX(w * Math.max(0, Math.min(1, at.get())) -7);
        pointer.setLayoutY(0);
    }
    // 선택 시간을 1시간까지 1분, 3시간까지 5분, 그 이상은 15분으로 맞춤
    static int snap(int minutes) {
        int m = Math.max(ShowLength.MIN_MINUTES, Math.min(ShowLength.MAX_MINUTES, minutes));
        int step = m <= 60 ? 1 : m <= 180 ? 5 : 15;
        return Math.max(ShowLength.MIN_MINUTES, Math.min(ShowLength.MAX_MINUTES, Math.round((float) m / step) * step));
    }
    // 눈금자에서 누른 위치를 1분부터 24시간 사이의 시간으로 바꿈
    static int minutesAt(double fraction) {
        double f = Math.max(0, Math.min(1, fraction));
        double m;
        if (f <= HOUR_AT)
            m = 1 + (60 - 1) * (f / HOUR_AT);
        else if (f <= THREE_HOURS_AT)
            m = 60 + (180 - 60) * ((f - HOUR_AT) / (THREE_HOURS_AT - HOUR_AT));
        else
            m = 180 + (ShowLength.MAX_MINUTES - 180) * ((f - THREE_HOURS_AT) / (1 - THREE_HOURS_AT));
        return snap((int) Math.round(m));
    }
    // 선택 시간을 눈금자 위의 위치 비율로 바꿈
    static double fractionOf(int minutes) {
        int m = Math.max(ShowLength.MIN_MINUTES, Math.min(ShowLength.MAX_MINUTES, minutes));
        if (m <= 60)
            return HOUR_AT * (m - 1) / (60.0 - 1);
        if (m <= 180)
            return HOUR_AT + (THREE_HOURS_AT - HOUR_AT) * (m - 60) / 120.0;
        return THREE_HOURS_AT + (1 - THREE_HOURS_AT) * (m - 180) / (ShowLength.MAX_MINUTES - 180.0);
    }
    static String lengthText(int minutes) {
        if (minutes <= 0)
            return "무제한";
        int h = minutes / 60, m = minutes % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }
}