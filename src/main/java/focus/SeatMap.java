package focus;

import java.util.Random;

// 좌석표가 기억하는 두 상태
public class SeatMap {
    private final int rows;
    private final int cols;
    private final boolean[][] taken;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // 같은 seed의 팔린 자리 배치와 seed == 0의 의미
    public SeatMap(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.taken = new boolean[rows][cols];
        if (seed != 0) {
            Random random = new Random(seed);
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    taken[r][c] = random.nextInt(100) < 30;
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isTaken(int row, int col) {
        return inside(row, col) && taken[row][col];
    }

    public boolean isSelected(int row, int col) {
        return row == selectedRow && col == selectedCol;
    }

    // 팔렸거나 범위 밖이면 선택하지 않는 동작
    public boolean select(int row, int col) {
        if (!inside(row, col) || taken[row][col])
            return false;
        selectedRow = row;
        selectedCol = col;
        return true;
    }

    public boolean hasSelection() {
        return selectedRow >= 0;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public String getSelectedName() {
        return hasSelection() ? seatName(selectedRow, selectedCol) : null;
    }

    // 행, 열 번호를 A열 1번 형식으로 바꾸는 동작
    static String seatName(int row, int col) {
        return (char) ('A' + row) + "열 " + (col + 1) + "번";
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}