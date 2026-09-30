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
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class AllowListWindow {
    private final AllowList allowList;
    private Stage stage;
    private final ObservableList<String> items = FXCollections.observableArrayList();

    public AllowListWindow(AllowList allowList) {
        this.allowList = allowList;
    }

    public void show() {
        items.setAll(allowList.get());      // 최신 목록으로 다시 채움
        if (stage == null) {    // 처음 열 때만 창 생성
            stage = new Stage();
            stage.setTitle("허용 목록 설정");
            stage.setAlwaysOnTop(true);
            ListView<String> listView = new ListView<>(items);
            TextField inputField = new TextField();
            inputField.setPromptText("앱 이름");
            Button addButton = new Button("추가");
            Button deleteButton = new Button("선택 삭제");
            Button saveButton = new Button("저장");
            Label statusLabel = new Label();
            addButton.setOnAction(event -> {    // 빈칸과 중복을 제외하고 앱 추가
                String name = inputField.getText().trim();
                if (!name.isEmpty() && !items.contains(name)) {
                    items.add(name);
                    inputField.clear();
                }
            });
            deleteButton.setOnAction(event -> items.remove(listView.getSelectionModel().getSelectedItem()));    // 선택된 앱 삭제
            saveButton.setOnAction(event -> {       // 목록 저장과 실패 처리
                try {
                    boolean usingDefault = items.isEmpty();
                    allowList.replace(items);
                    items.setAll(allowList.get());
                    statusLabel.setText(usingDefault ? "기본 목록으로 저장했습니다." : "저장했습니다.");
                } catch (IOException exception) {
                    statusLabel.setText("저장에 실패했습니다");
                }
            });
            HBox inputRow = new HBox(8, inputField, addButton);     // 입력칸과 추가 버튼을 가로 배치
            HBox buttonRow = new HBox(8, deleteButton, saveButton);
            VBox root = new VBox(10, listView, inputRow, buttonRow, statusLabel);   // 화면 부품을 세로 배치
            root.setPadding(new Insets(16));
            Scene scene = new Scene(root, 420, 360);
            stage.setScene(scene);
        }
        stage.show();       // 창 표시
        stage.toFront();    // 기존 창을 앞으로 가져오기
    }
}
