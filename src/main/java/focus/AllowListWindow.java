package focus;

import java.io.IOException;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class AllowListWindow {
    private final AllowList allowList;
    private Stage stage;
    private final SiteAllowList siteAllowList;
    private final ObservableList<String> items = FXCollections.observableArrayList();
    private final ObservableList<String> siteItems = FXCollections.observableArrayList();

    public AllowListWindow(AllowList allowList, SiteAllowList siteAllowList) {
        this.allowList = allowList;
        this.siteAllowList = siteAllowList;
    }

    public void show() {
        items.setAll(allowList.get()); // 최신 목록으로 다시 채움
        siteItems.setAll(siteAllowList.get());
        if (stage == null) { // 처음 열 때만 창 생성
            stage = new Stage();
            stage.setTitle("기본 허용 목록");
            stage.setAlwaysOnTop(true);
            VBox appColumn = column("허용 앱", "앱 이름 (예: Code)", items, false);
            VBox siteColumn = column("허용 사이트", "사이트 주소 (예: daegu.ac.kr)", siteItems, true);
            HBox columns = new HBox(20, appColumn, siteColumn);
            Button saveButton = new Button("저장");
            Label statusLabel = new Label();
            Button closeButton = new Button("닫기");
            closeButton.setOnAction(event -> stage.hide());
            saveButton.setOnAction(event -> { // 목록 저장과 실패 처리
                try {
                    boolean usingDefault = items.isEmpty() || siteItems.isEmpty();
                    allowList.replace(items);
                    siteAllowList.replace(siteItems);
                    items.setAll(allowList.get());
                    siteItems.setAll(siteAllowList.get());
                    statusLabel.setText(usingDefault ? "빈 목록은 기본 목록으로 저장했습니다." : "저장했습니다.");
                } catch (IOException exception) {
                    statusLabel.setText("저장에 실패했습니다.");
                }
            });
            HBox buttonRow = new HBox(8, statusLabel, closeButton, saveButton);
            VBox root = new VBox(16, columns, buttonRow);
            root.setPadding(new Insets(16));
            Scene scene = new Scene(root, 760, 420);
            stage.setScene(scene);
        }
        stage.show(); // 창 표시
        stage.toFront(); // 기존 창을 앞으로 가져오기
    }

    private static VBox column(String title, String prompt,
            ObservableList<String> list, boolean lowerCase) {
        Label titleLabel = new Label(title);
        ListView<String> listView = new ListView<>(list);
        listView.setPrefHeight(220);
        VBox.setVgrow(listView, Priority.ALWAYS);
        TextField inputField = new TextField();
        inputField.setPromptText(prompt);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button addButton = new Button("추가");
        Button deleteButton = new Button("선택 삭제");
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
        VBox column = new VBox(10, titleLabel, listView, inputRow, deleteButton);
        HBox.setHgrow(column, Priority.ALWAYS);
        column.setPrefWidth(330);
        return column;
    }
}
