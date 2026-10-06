package focus;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

final class CurtainQualityPane {
    private final CurtainPacks packs;
    private final ToggleGroup group = new ToggleGroup();
    private final Label status = Ui.label("", "t-caption");
    private final ProgressBar progress = new ProgressBar(0);
    private final Button apply = Ui.primary("적용", 120);
    private final VBox view;

    CurtainQualityPane(CurtainPacks packs) {
        this.packs = packs;
        HBox choices = new HBox(18);
        choices.setAlignment(Pos.CENTER_LEFT);
        for (CurtainQuality q : CurtainQuality.values()) {
            RadioButton r = new RadioButton();
            r.getStyleClass().add("quality-choice");
            r.setUserData(q);
            r.setToggleGroup(group);
            choices.getChildren().add(r);
        }
        apply.getStyleClass().add("btn-small");
        apply.setOnAction(e -> applyChoice());
        progress.setPrefWidth(220);
        progress.setVisible(false);
        progress.managedProperty().bind(progress.visibleProperty());
        HBox row = new HBox(14, choices, Ui.grow(), apply);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox state = new HBox(12, progress, status);
        state.setAlignment(Pos.CENTER_LEFT);
        view = new VBox(8, Ui.label("커튼 화질", "t-caption"), row, state);
        refresh();
    }

    VBox view() {
        return view;
    }

    void refresh() {
        if (apply.isDisabled())
            return;
        CurtainQuality chosen = packs.chosen();
        for (var t : group.getToggles()) {
            CurtainQuality q = (CurtainQuality) t.getUserData();
            ((RadioButton) t).setText(q.label + " · " + sizeNote(q));
            if (q == chosen)
                group.selectToggle(t);
        }
        CurtainPacks.Active active = packs.active();
        status.setText(active.note().isEmpty() ? "지금 커튼: " + active.quality().label : active.note());
    }

    private String sizeNote(CurtainQuality q) {
        if (q.bundled)
            return "앱에 들어 있음";
        if (packs.installed(q))
            return "받아 둠";
        PackManifest.Pack p = packs.manifest().pack(q);
        return p == null ? "받을 수 없음" : "받기 " + megabytes(p.bytes());
    }

    private void applyChoice() {
        CurtainQuality q = (CurtainQuality) group.getSelectedToggle().getUserData();
        if (q.bundled || packs.installed(q)) {
            packs.choose(q);
            status.setText(q.label + "으로 바꿨습니다 — 다음 실행부터 적용");
            return;
        }
        PackManifest.Pack p = packs.manifest().pack(q);
        long total = p == null ? 0 : p.bytes();
        setBusy(true);
        status.setText(q.label + " 받는 중…");
        Thread t = new Thread(() -> {
            CurtainPacks.Result r = packs.download(q, got -> Platform.runLater(() -> {
                progress.setProgress(total > 0 ? (double) got / total : ProgressBar.INDETERMINATE_PROGRESS);
                status.setText(q.label + " 받는 중 " + megabytes(got) + " / " + megabytes(total));
            }));
            Platform.runLater(() -> {
                setBusy(false);
                refresh();
                status.setText(r.message());
            });
        }, "curtain-pack-download");
        t.setDaemon(true);
        t.start();
    }

    private void setBusy(boolean busy) {
        apply.setDisable(busy);
        group.getToggles().forEach(t -> ((RadioButton) t).setDisable(busy));
        progress.setVisible(busy);
        progress.setProgress(0);
    }

    static String megabytes(long bytes) {
        if (bytes >= 1_000_000_000L)
            return String.format("%.1fGB", bytes / 1e9);
        return Math.round(bytes / 1e6) + "MB";
    }
}
