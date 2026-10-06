package focus;

import java.io.IOException;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class AllowListWindow {
    private static final String SAVED = "저장됨";
    private static final String UNSAVED = "저장 전";
    private final AllowList allowList;
    private final SiteAllowList siteAllowList;
    private Stage stage;
    private VBox root;
    private Label stateChip;
    private Label statusLabel;
    private final ObservableList<String> items = FXCollections.observableArrayList();
    private final ObservableList<String> siteItems = FXCollections.observableArrayList();
    private final CurtainQualityPane qualityPane;

    public AllowListWindow(AllowList allowList, SiteAllowList siteAllowList) {
        this(allowList, siteAllowList, new CurtainQualityPane(CurtainPacks.forApp()));
    }

    AllowListWindow(AllowList allowList, SiteAllowList siteAllowList, CurtainQualityPane qualityPane) {
        this.allowList = allowList;
        this.siteAllowList = siteAllowList;
        this.qualityPane = qualityPane;
    }

    public void show() {
        items.setAll(allowList.get()); // 최신 목록으로 다시 채움
        siteItems.setAll(siteAllowList.get());
        if (stage == null) { // 처음 열 때만 창 생성
            stage = new Stage();
            stage.setTitle("기본 허용 목록");
            stage.setAlwaysOnTop(true);
            statusLabel = Ui.label("", "t-caption");
            stateChip = Ui.label(SAVED, "chip");
            stateChip.setAlignment(Pos.CENTER);
            ListChangeListener<String> changed = change -> stateChip.setText(UNSAVED);
            items.addListener(changed);
            siteItems.addListener(changed);

            VBox appColumn = column("허용 앱", "앱 이름 (예: Code)", items, false);
            VBox siteColumn = column("허용 사이트", "사이트 주소 (예: daegu.ac.kr)", siteItems, true);
            Button closeButton = Ui.ghost("닫기", 120);
            Button saveButton = Ui.primary("저장", 200);
            closeButton.setOnAction(event -> stage.hide());
            saveButton.setOnAction(event -> { // 목록 저장과 실패 처리
                try {
                    boolean usingDefault = items.isEmpty() || siteItems.isEmpty();
                    allowList.replace(items);
                    siteAllowList.replace(siteItems);
                    items.setAll(allowList.get());
                    siteItems.setAll(siteAllowList.get());
                    statusLabel.setText(usingDefault ? "빈 목록은 기본 목록으로 저장했습니다." : "저장했습니다.");
                    stateChip.setText(SAVED);
                } catch (IOException exception) {
                    statusLabel.setText("저장에 실패했습니다.");
                    stateChip.setText("저장 실패");
                }
            });
            HBox header = new HBox(Ui.label("기본 허용 목록", "t-title"), Ui.grow(), stateChip);
            header.setAlignment(Pos.CENTER_LEFT);
            Label siteNote = Ui.label("예매할 때 미리 체크되는 목록입니다. Chrome·Safari는 탭 주소로, 그 밖의 앱은 앱 이름으로 판정합니다.", "t-body");
            HBox columns = new HBox(28, appColumn, siteColumn);
            HBox buttonRow = new HBox(12, statusLabel, Ui.grow(), closeButton, saveButton);
            buttonRow.setAlignment(Pos.CENTER_LEFT);
            VBox content = new VBox(0, header, siteNote, columns, qualityPane.view(), buttonRow);
            VBox.setMargin(siteNote, new Insets(4, 0, 20, 0));
            VBox.setMargin(qualityPane.view(), new Insets(20, 0, 0, 0));
            VBox.setMargin(buttonRow, new Insets(24, 0, 0, 0));
            content.setPadding(new Insets(24, 36, 28, 36));
            Rectangle valance = new Rectangle(0, 14);
            valance.setFill(Ui.velvet(0, 600));
            root = new VBox(valance, content);
            valance.widthProperty().bind(root.widthProperty());
            root.getStyleClass().add("settings");
            javafx.geometry.Rectangle2D area = javafx.stage.Screen.getPrimary().getVisualBounds();
            WindowSize size = WindowSize.settings(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
            Scene scene = new Scene(root, size.getWidth(), size.getHeight());
            scene.getStylesheets().add(AllowListWindow.class.getResource("/css/theater.css").toExternalForm());
            stage.setScene(scene);
            stage.setMinWidth(size.getMinWidth());
            stage.setMinHeight(size.getMinHeight());
            stage.setX(size.getX());
            stage.setY(size.getY());
        }
        stateChip.setText(SAVED);
        statusLabel.setText("");
        qualityPane.refresh();
        boolean opening = !stage.isShowing();
        stage.show();
        stage.toFront();
        if (opening)
            Motion.popIn(root);
    }

    private static VBox column(String title, String prompt,
            ObservableList<String> list, boolean lowerCase) {
        Label titleLabel = Ui.label(title, "t-caption");
        ListView<String> listView = new ListView<>(list);
        listView.getStyleClass().add("allow-list");
        listView.setPrefHeight(220);
        VBox.setVgrow(listView, Priority.ALWAYS);
        TextField inputField = new TextField();
        inputField.setPromptText(prompt);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button addButton = Ui.primary("추가", 0);
        addButton.getStyleClass().add("btn-small");
        Button deleteButton = Ui.ghost("선택 삭제", 0);
        deleteButton.getStyleClass().add("btn-small");
        Runnable add = () -> {
            String name = inputField.getText().trim();
            if (lowerCase)
                name = name.toLowerCase();
            if (!name.isEmpty() && !list.contains(name)) {
                list.add(name);
                inputField.clear();
            }
        };
        addButton.setOnAction(event -> add.run());
        inputField.setOnAction(event -> add.run());
        deleteButton.setOnAction(event -> list.remove(listView.getSelectionModel().getSelectedItem()));
        HBox inputRow = new HBox(10, inputField, addButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        VBox column = new VBox(10, titleLabel, listView, inputRow, deleteButton);
        HBox.setHgrow(column, Priority.ALWAYS);
        column.setPrefWidth(330);
        return column;
    }
}
