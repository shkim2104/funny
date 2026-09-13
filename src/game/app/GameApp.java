package game.app;

import game.ui.GameFrame;

import javax.swing.SwingUtilities;

public class GameApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}
