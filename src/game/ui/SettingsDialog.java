package game.ui;

import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The settings page, as notebook index tabs (전투 / 증강 / 마을·아이템 / 화면·조작 / 저장·소리). Every row
 * saves the moment it changes; there is no "apply" button.
 */
public final class SettingsDialog {
    private SettingsDialog() {}

    public static void show(Window owner) {
        JDialog d = new JDialog(owner, "설정", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 10), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 24));
        d.setContentPane(page);
        d.add(Sketch.title("설정"), BorderLayout.NORTH);

        Sketch.Tabs tabs = new Sketch.Tabs(6100);
        tabs.addTab("전투", tab(
                choice("전투 속도", "공격과 스킬 연출을 빠르게 해요.", new String[]{"1배", "1.5배", "2배"}, new int[]{10, 15, 20},
                        Settings::getBattleSpeedTenths, Settings::setBattleSpeedTenths, 6110),
                toggle("자동 전투", "내 차례가 오면 기본 공격을 알아서 해요. 보스전에서는 쉬어요.",
                        Settings.isAutoBattle(), Settings::setAutoBattle, 6111),
                choice("자동 포션", "HP가 이만큼 아래로 내려가면 가진 포션을 먹어요 (그 턴을 써요).",
                        new String[]{"끔", "20%", "30%", "40%", "50%"}, new int[]{0, 20, 30, 40, 50},
                        Settings::getAutoPotionPercent, Settings::setAutoPotionPercent, 6112),
                toggle("연출 줄이기", "화면 흔들림, 하얀 번쩍임, 타격 순간 멈칫을 꺼요.",
                        Settings.isReducedEffects(), Settings::setReducedEffects, 6113),
                toggle("전투 기록 간단히", "한 대 한 대의 피해 대신 등장·처치·보상 같은 중요한 줄만 기록해요.",
                        Settings.isBriefLog(), Settings::setBriefLog, 6114)));
        tabs.addTab("증강", tab(
                toggle("증강 효과 미리 보기", "카드에 「치명타 22% → 30%」처럼 고르면 바뀌는 수치를 보여 줘요.",
                        Settings.isAugmentPreview(), Settings::setAugmentPreview, 6120),
                toggle("Tab으로 보유 증강 보기", "전투 중 Tab을 누르면 이번 던전의 증강과 장비 효과를 보여 줘요.",
                        Settings.isAugmentListKey(), Settings::setAugmentListKey, 6121)));
        tabs.addTab("마을·아이템", tab(
                toggle("새 장비 자동 장착", "보스·보물 상자·보물 뽑기에서 지금보다 좋은 장비가 나오면 바로 장착해요.",
                        Settings.isAutoEquip(), Settings::setAutoEquip, 6130),
                toggle("일반 장비 자동 판매", "마을에 돌아오면 장착하지 않은 [일반] 무기·방어구를 팔아요.",
                        Settings.isAutoSellCommon(), Settings::setAutoSellCommon, 6131),
                toggle("비싼 장비 판매 확인", "[영웅] 이상 장비를 팔 때 한 번 더 물어봐요.",
                        Settings.isSellConfirm(), Settings::setSellConfirm, 6132),
                toggle("돌림판 건너뛰기", "보물 뽑기 돌림판이 돌지 않고 바로 결과가 나와요.",
                        Settings.isSkipWheel(), Settings::setSkipWheel, 6133),
                toggle("의뢰 완료 알림", "마을에 돌아왔을 때 끝낸 의뢰가 있으면 알려 줘요.",
                        Settings.isQuestNotify(), Settings::setQuestNotify, 6134)));
        tabs.addTab("화면·조작", tab(
                toggle("전체 화면으로 시작", "끄면 다음부터 작은 창으로 시작해요. (F11로 언제든 바꿀 수 있어요)",
                        Settings.isStartFullscreen(), Settings::setStartFullscreen, 6140),
                toggle("페이지 넘김 효과", "화면이 바뀔 때 공책 넘기는 연출을 보여 줘요.",
                        Settings.isPageTurn(), Settings::setPageTurn, 6141),
                toggle("글자 크게", "연필 글씨를 조금 크게 써요. 게임을 다시 켜면 모든 화면에 적용돼요.",
                        Settings.isLargeText(), Settings::setLargeText, 6142),
                toggle("단축키", "1~4 스킬, I 장비, S 스탯 분배, Esc 설정 창.",
                        Settings.isHotkeys(), Settings::setHotkeys, 6143),
                toggle("건물 이름 표시", "마을과 지도에서 마우스를 올리면 이름표를 띄워요.",
                        Settings.isBuildingLabels(), Settings::setBuildingLabels, 6144)));
        tabs.addTab("저장·소리", tab(
                toggle("창을 닫을 때 자동 저장", "게임 창을 닫거나 「종료」·「타이틀로」를 누르면 지금 캐릭터를 저장해요.",
                        Settings.isAutoSaveOnExit(), Settings::setAutoSaveOnExit, 6150),
                toggle("세이브 백업", "저장할 때마다 이전 파일을 하나 남겨 두고, 세이브가 깨지면 그걸로 불러와요.",
                        Settings.isSaveBackup(), Settings::setSaveBackup, 6151),
                volume("배경음악 볼륨", "0으로 내리면 음악이 꺼져요.", Settings::getMusicVolume, Settings::setMusicVolume,
                        Music::volumeChanged, 6152),
                volume("효과음 볼륨", "타격, 치명타, 코인, 레벨업, 돌림판 소리예요. 0이면 꺼져요.", Settings::getSfxVolume,
                        Settings::setSfxVolume, () -> Sfx.play(Sfx.Sound.COIN), 6153)));
        d.add(tabs, BorderLayout.CENTER);

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        d.add(south, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(close);

        d.setSize(720, 720);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    /** One tab: its rows stacked from the top, scrolling if they don't fit. */
    private static JComponent tab(JComponent... rows) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (JComponent r : rows) {
            r.setAlignmentX(Component.LEFT_ALIGNMENT);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, r.getPreferredSize().height));
            list.add(r);
            list.add(Box.createVerticalStrut(8));
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(list, BorderLayout.NORTH);
        return Sketch.scroll(wrap);
    }

    /** A boxed row: title and explanation on the left, the control on the right. */
    private static JComponent row(String title, String desc, JComponent control, long seed) {
        return row(title, desc, control, seed, false);
    }

    /** below = the control goes on its own line under the text (wide button rows). */
    private static JComponent row(String title, String desc, JComponent control, long seed, boolean below) {
        Sketch.Box row = new Sketch.Box(new BorderLayout(14, 0), seed);
        row.setBorder(new EmptyBorder(12, 18, 12, 18));
        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(Sketch.label(title, Font.BOLD, 16f, Sketch.INK), BorderLayout.NORTH);
        text.add(Sketch.label("<html><body style='width:300px'>" + desc + "</body></html>", Font.PLAIN, 12f, Sketch.INK_SOFT),
                BorderLayout.CENTER);
        row.add(text, BorderLayout.CENTER);
        if (below) {
            JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
            line.setOpaque(false);
            line.add(control);
            row.add(line, BorderLayout.SOUTH);
            return row;
        }
        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.add(control);
        row.add(east, BorderLayout.EAST);
        return row;
    }

    private static JComponent toggle(String title, String desc, boolean value, Consumer<Boolean> onChange, long seed) {
        return row(title, desc, new CheckBox(value, onChange, seed + 1), seed);
    }

    /** A row of small pencil buttons; the chosen one is drawn in red pencil with a highlighter. */
    private static JComponent choice(String title, String desc, String[] labels, int[] values, IntSupplier get, IntConsumer set,
                                     long seed) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        List<Sketch.Button> all = new ArrayList<>();
        Runnable[] refresh = new Runnable[1];
        for (int i = 0; i < labels.length; i++) {
            final int v = values[i];
            Sketch.Button b = new Sketch.Button(labels[i], false) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (get.getAsInt() == v) {
                        Graphics2D g2 = Sketch.smooth(g);
                        Sketch.marker(g2, 8, getHeight() / 2.0 - 7, getWidth() - 16, 14, v * 7L + seed);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            b.setFont(Sketch.hand(Font.BOLD, 13f));
            b.setBorder(new EmptyBorder(5, 10, 5, 10));
            b.addActionListener(e -> {
                set.accept(v);
                refresh[0].run();
            });
            all.add(b);
            buttons.add(b);
        }
        refresh[0] = () -> {
            for (Sketch.Button b : all) b.repaint();
        };
        return row(title, desc, buttons, seed, true);
    }

    /** - / + in steps of 10 (0 = 끔); afterChange runs on every step (e.g. to hear the new level). */
    private static JComponent volume(String title, String desc, IntSupplier get, IntConsumer set, Runnable afterChange, long seed) {
        JLabel value = Sketch.label("", Font.BOLD, 16f, Sketch.BLUE_PENCIL);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        value.setPreferredSize(new Dimension(46, 30));
        JButton down = new Sketch.Button("-", false);
        JButton up = new Sketch.Button("+", false);
        for (JButton b : new JButton[]{down, up}) b.setBorder(new EmptyBorder(4, 12, 4, 12));
        Runnable show = () -> {
            int v = get.getAsInt();
            value.setText(v == 0 ? "끔" : String.valueOf(v));
            value.setForeground(v == 0 ? Sketch.INK_SOFT : Sketch.BLUE_PENCIL);
            down.setEnabled(v > 0);
            up.setEnabled(v < 100);
        };
        down.addActionListener(e -> {
            set.accept(get.getAsInt() - 10);
            afterChange.run();
            show.run();
        });
        up.addActionListener(e -> {
            set.accept(get.getAsInt() + 10);
            afterChange.run();
            show.run();
        });
        show.run();
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        controls.add(down);
        controls.add(value);
        controls.add(up);
        return row(title, desc, controls, seed);
    }

    /** A pencil-drawn checkbox with 켬/끔 under it; click or Space/Enter toggles it. */
    private static final class CheckBox extends JButton {
        private boolean on;
        private final long seed;

        CheckBox(boolean on, Consumer<Boolean> onChange, long seed) {
            this.on = on;
            this.seed = seed;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(64, 56));
            addActionListener(e -> {
                this.on = !this.on;
                onChange.accept(this.on);
                repaint();
            });
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int s = 30, x = (getWidth() - s) / 2, y = 2;
            if (isFocusOwner()) Sketch.marker(g2, x - 6, y + 8, s + 12, 16, seed + 3);
            Sketch.box(g2, x, y, s, s, seed, 2.4f, Sketch.INK);
            if (on) {
                Sketch.line(g2, x + 6, y + 15, x + 13, y + 24, seed + 1, 3.4f, Sketch.RED_PENCIL);
                Sketch.line(g2, x + 13, y + 24, x + s + 4, y - 4, seed + 2, 3.4f, Sketch.RED_PENCIL);
            }
            g2.setFont(Sketch.hand(Font.BOLD, 13f));
            String label = on ? "켬" : "끔";
            g2.setColor(on ? Sketch.RED_PENCIL : Sketch.INK_SOFT);
            g2.drawString(label, (getWidth() - g2.getFontMetrics().stringWidth(label)) / 2, y + s + 18);
            g2.dispose();
        }
    }
}
