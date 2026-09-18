package focus;
// 감지 결과(앱 이름·딴짓 여부·경과 시간)를 담는 곳
public class DetectionState {
    private String currentAppName;
    private boolean isDistracting;
    private long distractionElapsedSeconds;

    public String getCurrentAppName() {
        return currentAppName;
    }

    public void setCurrentAppName(String currentAppName) {
        this.currentAppName = currentAppName;
    }

    public boolean isDistracting() {
        return isDistracting;
    }

    public void setDistracting(boolean distracting) {
        this.isDistracting = distracting;
    }

    public long getDistractionElapsedSeconds() {
        return distractionElapsedSeconds;
    }

    public void setDistractionElapsedSeconds(long distractionElapsedSeconds) {
        this.distractionElapsedSeconds = distractionElapsedSeconds;
    }
}