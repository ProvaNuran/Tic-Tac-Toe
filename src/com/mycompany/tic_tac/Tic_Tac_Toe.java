package com.mycompany.tic_tac;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Tic Tac Toe - "Neon Pro Dashboard" edition.
 *
 * Full control-center pass on top of the previous "Neon Pro" refactor:
 *  - Top bar: settings + sound icon buttons, gradient title, "How to Play"
 *  - Right-hand sidebar: Difficulty selector, live Game Status card, running
 *    Stats (games / wins / draws), a Win Conditions legend, and a Last Moves log
 *  - Bottom toolbar: New Game, Undo, Hint (limited uses), AI Suggest (limited
 *    uses), Analyze (perfect-play outcome), and a Theme cycler
 *  - Five selectable color themes plus a synced sound on/off switch
 *  - Difficulty now actually changes how well the computer plays
 *
 * Visual language (gradient backdrop, glowing cells, animated marks, confetti,
 * glassy buttons) carries over from the previous pass.
 */
public class Tic_Tac_Toe extends JFrame {

    // ---- constants -------------------------------------------------------
    private static final int SIZE = 9;
    private static final int[][] WIN_LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
        {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    // ---- theme-able palette (mutable so the theme switcher can repaint everything) ----
    private static Color BG_TOP = new Color(5, 6, 26);
    private static Color BG_MID = new Color(36, 12, 74);
    private static Color BG_MID2 = new Color(78, 16, 92);
    private static Color BG_BOTTOM = new Color(20, 6, 34);
    private static Color CELL_BG = new Color(255, 255, 255, 14);
    private static Color CELL_BG_HOVER = new Color(255, 255, 255, 30);
    private static Color CELL_BORDER = new Color(255, 255, 255, 46);
    private static final Color WIN_GLOW = new Color(255, 215, 90);
    private static Color O_COLOR = new Color(64, 220, 255);   // neon cyan
    private static Color X_COLOR = new Color(255, 72, 176);   // neon magenta
    private static Color PANEL_LINE = new Color(170, 120, 255);
    private static final Color STATUS_COLOR = new Color(235, 235, 245);

    private enum Player { O, X;
        Player other() { return this == O ? X : O; }
    }

    private enum Difficulty {
        EASY("Easy", 0.85), MEDIUM("Medium", 0.5), HARD("Hard", 0.15), IMPOSSIBLE("Impossible (Pro)", 0.0);
        final String label;
        final double randomWeight;
        Difficulty(String label, double randomWeight) { this.label = label; this.randomWeight = randomWeight; }
    }

    /** A full color theme: background gradient stops + O/X accents + panel line. */
    private static final class ThemeDef {
        final String name;
        final Color swatch, bgTop, bgMid, bgMid2, bgBottom, o, x, panel;
        ThemeDef(String name, Color swatch, Color bgTop, Color bgMid, Color bgMid2, Color bgBottom,
                 Color o, Color x, Color panel) {
            this.name = name; this.swatch = swatch;
            this.bgTop = bgTop; this.bgMid = bgMid; this.bgMid2 = bgMid2; this.bgBottom = bgBottom;
            this.o = o; this.x = x; this.panel = panel;
        }
    }

    private static final ThemeDef[] THEMES = {
        new ThemeDef("Aurora", new Color(64, 130, 255),
                new Color(5, 6, 26), new Color(36, 12, 74), new Color(78, 16, 92), new Color(20, 6, 34),
                new Color(64, 220, 255), new Color(255, 72, 176), new Color(170, 120, 255)),
        new ThemeDef("Blossom", new Color(220, 90, 210),
                new Color(18, 4, 30), new Color(60, 10, 70), new Color(90, 14, 78), new Color(28, 4, 40),
                new Color(180, 130, 255), new Color(255, 105, 180), new Color(220, 130, 255)),
        new ThemeDef("Ember", new Color(255, 140, 60),
                new Color(24, 8, 6), new Color(70, 24, 10), new Color(96, 30, 8), new Color(30, 8, 4),
                new Color(255, 190, 90), new Color(255, 100, 70), new Color(255, 170, 90)),
        new ThemeDef("Lagoon", new Color(40, 210, 180),
                new Color(3, 18, 22), new Color(8, 55, 60), new Color(10, 70, 66), new Color(4, 24, 26),
                new Color(80, 230, 200), new Color(110, 220, 120), new Color(80, 220, 190)),
        new ThemeDef("Prism", new Color(160, 90, 255),
                new Color(10, 6, 30), new Color(60, 14, 90), new Color(20, 60, 120), new Color(30, 8, 40),
                new Color(90, 190, 255), new Color(255, 90, 190), new Color(200, 140, 255)),
    };

    private static final class MoveRecord {
        final int pos;
        final Player player;
        boolean endedWin = false;
        boolean endedDraw = false;
        MoveRecord(int pos, Player player) { this.pos = pos; this.player = player; }
    }

    // ---- state -------------------------------------------------------
    private final Player[] board = new Player[SIZE];
    private final GlowButton[] cells = new GlowButton[SIZE];
    private Player current = Player.O;
    private boolean gameOver = false;
    private boolean vsComputer = false;
    private int oWins = 0;
    private int xWins = 0;
    private int draws = 0;
    private int gamesPlayed = 0;
    private Timer glowTimer;
    private Difficulty difficulty = Difficulty.IMPOSSIBLE;
    private final Random rnd = new Random();
    private final List<MoveRecord> moveHistory = new ArrayList<>();
    private int hintsLeft = 3;
    private int aiSuggestLeft = 2;
    private boolean soundOn = true;
    private int themeIndex = 0;

    // ---- UI components -------------------------------------------------------
    private GradientPanel root;
    private GradientTitleLabel titleLabel;
    private JLabel statusLabel;
    private ScoreCard oCard;
    private ScoreCard xCard;
    private JLabel oScoreValue;
    private JLabel xScoreValue;
    private PillToggle twoPlayerBtn;
    private PillToggle vsComputerBtn;
    private ConfettiOverlay confetti;
    private JPanel boardPanel;

    private JLabel statusHeadline;
    private JLabel statusSub;
    private JLabel gamesPlayedValue, oWinsStatValue, xWinsStatValue, drawsStatValue;
    private JLabel[] lastMoveLabels = new JLabel[3];
    private final List<DifficultyRow> difficultyRows = new ArrayList<>();
    private final List<SwatchButton> swatchButtons = new ArrayList<>();
    private ToolbarIconButton hintBtn, aiSuggestBtn;
    private IconButton topSoundBtn;
    private SoundSwitch soundSwitch;

    public Tic_Tac_Toe() {
        super("Tic Tac Toe");
        buildUI();
        newGame();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 960);
        setMinimumSize(new Dimension(1040, 860));
        setLocationRelativeTo(null);
    }

    // =====================================================================
    // UI construction
    // =====================================================================
    private void buildUI() {
        root = new GradientPanel(BG_TOP, BG_MID, BG_MID2, BG_BOTTOM);
        root.setLayout(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(22, 30, 20, 30));
        setContentPane(root);

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(buildTopBar());
        north.add(Box.createVerticalStrut(10));
        north.add(buildHeader());
        root.add(north, BorderLayout.NORTH);

        JPanel mainArea = new JPanel(new BorderLayout(24, 0));
        mainArea.setOpaque(false);
        mainArea.add(buildBoard(), BorderLayout.CENTER);
        mainArea.add(buildSidebar(), BorderLayout.EAST);
        root.add(mainArea, BorderLayout.CENTER);

        root.add(buildFooter(), BorderLayout.SOUTH);

        confetti = new ConfettiOverlay();
        setGlassPane(confetti);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        IconButton settingsBtn = new IconButton("\u2699");
        settingsBtn.setToolTipText("Settings");
        settingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Pick a difficulty and theme from the sidebar / toolbar.\nSound can be toggled up here or at the bottom.",
                "Settings", JOptionPane.INFORMATION_MESSAGE));
        topSoundBtn = new IconButton(soundOn ? "\uD83D\uDD0A" : "\uD83D\uDD07");
        topSoundBtn.setToolTipText("Toggle sound");
        topSoundBtn.addActionListener(e -> toggleSound());
        left.add(settingsBtn);
        left.add(topSoundBtn);
        bar.add(left, BorderLayout.WEST);

        titleLabel = new GradientTitleLabel("TIC \u00B7 TAC \u00B7 TOE", O_COLOR, X_COLOR);
        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrap.setOpaque(false);
        centerWrap.add(titleLabel);
        bar.add(centerWrap, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        FlatButton howToPlay = new FlatButton("\u2753 How to Play", new Color(120, 120, 155));
        howToPlay.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Take turns placing your mark on the grid.\n"
                        + "Get three in a row - across, down, or diagonally - to win.\n"
                        + "Use Hint or AI Suggest if you're stuck, and Analyze to see the\n"
                        + "perfect-play outcome from the current position.",
                "How to Play", JOptionPane.INFORMATION_MESSAGE));
        right.add(howToPlay);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(Box.createVerticalStrut(6));

        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        scoreRow.setOpaque(false);
        scoreRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        oCard = new ScoreCard("PLAYER O", O_COLOR);
        oScoreValue = oCard.valueLabel;
        xCard = new ScoreCard("PLAYER X", X_COLOR);
        xScoreValue = xCard.valueLabel;

        JLabel vs = new JLabel("VS");
        vs.setForeground(new Color(255, 255, 255, 130));
        vs.setFont(new Font("SansSerif", Font.BOLD, 15));

        scoreRow.add(oCard);
        scoreRow.add(vs);
        scoreRow.add(xCard);
        header.add(scoreRow);
        header.add(Box.createVerticalStrut(16));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        modeRow.setOpaque(false);
        modeRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        twoPlayerBtn = new PillToggle("\uD83D\uDC65 2 Player", true);
        vsComputerBtn = new PillToggle("\uD83E\uDD16 vs Computer", false);
        twoPlayerBtn.linkedGroup(vsComputerBtn);
        vsComputerBtn.linkedGroup(twoPlayerBtn);
        twoPlayerBtn.addActionListener(e -> { vsComputer = false; newGame(); });
        vsComputerBtn.addActionListener(e -> { vsComputer = true; newGame(); });
        modeRow.add(twoPlayerBtn);
        modeRow.add(vsComputerBtn);
        header.add(modeRow);

        return header;
    }

    private JPanel buildBoard() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        boardPanel = new JPanel(new GridLayout(3, 3, 14, 14));
        boardPanel.setOpaque(false);
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(PANEL_LINE, 2, 26),
                new EmptyBorder(18, 18, 18, 18)
        ));
        Dimension boardSize = new Dimension(530, 530);
        boardPanel.setPreferredSize(boardSize);
        boardPanel.setMinimumSize(boardSize);

        for (int i = 0; i < SIZE; i++) {
            final int pos = i;
            GlowButton cell = new GlowButton(CELL_BG, CELL_BG_HOVER, CELL_BORDER, 20);
            cell.setPreferredSize(new Dimension(150, 150));
            cell.addActionListener(e -> onCellClicked(pos));
            cells[i] = cell;
            boardPanel.add(cell);
        }
        wrapper.add(boardPanel);
        return wrapper;
    }

    // ---- sidebar -------------------------------------------------------
    private JScrollPane buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        sidebar.add(buildDifficultyCard());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(buildGameStatusCard());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(buildStatsCard());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(buildWinConditionsCard());
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(buildLastMovesCard());
        sidebar.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(sidebar,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setPreferredSize(new Dimension(310, 10));
        return scroll;
    }

    private JLabel cardHeaderLabel(String icon, String text) {
        JLabel lbl = new JLabel(icon + "  " + text.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(new Color(225, 225, 240));
        return lbl;
    }

    private CardPanel buildDifficultyCard() {
        CardPanel card = new CardPanel(PANEL_LINE);
        card.add(cardHeaderLabel("\uD83C\uDFAF", "Difficulty"), BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (Difficulty d : Difficulty.values()) {
            DifficultyRow row = new DifficultyRow(d, d == difficulty, this::selectDifficulty);
            difficultyRows.add(row);
            rows.add(row);
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildGameStatusCard() {
        CardPanel card = new CardPanel(O_COLOR);
        card.add(cardHeaderLabel("\uD83C\uDFC6", "Game Status"), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 2, 0, 0));

        statusHeadline = new JLabel("Player O's Turn");
        statusHeadline.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusHeadline.setForeground(O_COLOR);
        statusSub = new JLabel("Make your move!");
        statusSub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusSub.setForeground(new Color(210, 210, 225));

        content.add(statusHeadline);
        content.add(Box.createVerticalStrut(4));
        content.add(statusSub);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private CardPanel buildStatsCard() {
        CardPanel card = new CardPanel(new Color(120, 200, 255));
        card.add(cardHeaderLabel("\uD83D\uDCCA", "Stats"), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 2, 0, 0));

        gamesPlayedValue = new JLabel("0");
        oWinsStatValue = new JLabel("0");
        xWinsStatValue = new JLabel("0");
        drawsStatValue = new JLabel("0");

        content.add(statRow("Games Played", gamesPlayedValue));
        content.add(statRow("Player O Wins", oWinsStatValue));
        content.add(statRow("Player X Wins", xWinsStatValue));
        content.add(statRow("Draws", drawsStatValue));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel statRow(String label, JLabel value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(210, 210, 225));
        value.setFont(new Font("SansSerif", Font.BOLD, 14));
        value.setForeground(Color.WHITE);
        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private CardPanel buildWinConditionsCard() {
        CardPanel card = new CardPanel(new Color(150, 220, 150));
        card.add(cardHeaderLabel("\uD83D\uDEE1", "Win Conditions"), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 2, 0, 0));
        content.add(winConditionRow("Rows", new Color(120, 200, 255)));
        content.add(winConditionRow("Columns", new Color(190, 140, 255)));
        content.add(winConditionRow("Diagonals", new Color(255, 130, 190)));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel winConditionRow(String label, Color lineColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        row.setOpaque(false);
        JComponent line = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(lineColor);
                g2.drawLine(2, getHeight() / 2, getWidth() - 2, getHeight() / 2);
                g2.dispose();
            }
        };
        line.setPreferredSize(new Dimension(26, 12));
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(new Color(220, 220, 232));
        row.add(line);
        row.add(l);
        return row;
    }

    private CardPanel buildLastMovesCard() {
        CardPanel card = new CardPanel(new Color(255, 200, 120));
        card.add(cardHeaderLabel("\u23F1", "Last Moves"), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(8, 2, 0, 0));
        for (int i = 0; i < 3; i++) {
            JLabel lbl = new JLabel((i + 1) + ".   \u2013");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setForeground(new Color(210, 210, 225));
            lbl.setBorder(new EmptyBorder(3, 0, 3, 0));
            lastMoveLabels[i] = lbl;
            content.add(lbl);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ---- footer: toolbar + status + themes/reset/sound -------------------------------------------------------
    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.CENTER_ALIGNMENT);

        ToolbarIconButton newGameBtn = new ToolbarIconButton("\u2795", "New Game", new Color(70, 140, 230));
        newGameBtn.addActionListener(e -> newGame());

        ToolbarIconButton undoBtn = new ToolbarIconButton("\u21A9", "Undo", new Color(140, 110, 220));
        undoBtn.addActionListener(e -> onUndoClicked());

        hintBtn = new ToolbarIconButton("\uD83D\uDCA1", "Hint", new Color(220, 150, 60));
        hintBtn.setBadge(hintsLeft);
        hintBtn.addActionListener(e -> onHintClicked());

        aiSuggestBtn = new ToolbarIconButton("\uD83E\uDDE0", "AI Suggest", new Color(60, 190, 140));
        aiSuggestBtn.setBadge(aiSuggestLeft);
        aiSuggestBtn.addActionListener(e -> onAiSuggestClicked());

        ToolbarIconButton analyzeBtn = new ToolbarIconButton("\uD83D\uDCCA", "Analyze", new Color(70, 150, 220));
        analyzeBtn.addActionListener(e -> onAnalyzeClicked());

        ToolbarIconButton themeBtn = new ToolbarIconButton("\uD83C\uDFA8", "Theme", new Color(160, 110, 220));
        themeBtn.addActionListener(e -> applyTheme((themeIndex + 1) % THEMES.length));

        toolbar.add(newGameBtn);
        toolbar.add(undoBtn);
        toolbar.add(hintBtn);
        toolbar.add(aiSuggestBtn);
        toolbar.add(analyzeBtn);
        toolbar.add(themeBtn);
        footer.add(toolbar);
        footer.add(Box.createVerticalStrut(14));

        statusLabel = new JLabel("Player O's turn", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));
        statusLabel.setForeground(STATUS_COLOR);
        JPanel statusRow = dividerStatusRow(statusLabel);
        statusRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(statusRow);
        footer.add(Box.createVerticalStrut(14));

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel themesPanel = new JPanel();
        themesPanel.setOpaque(false);
        themesPanel.setLayout(new BoxLayout(themesPanel, BoxLayout.Y_AXIS));
        JLabel themesLabel = new JLabel("THEMES");
        themesLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        themesLabel.setForeground(new Color(200, 200, 215));
        JPanel swatchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        swatchRow.setOpaque(false);
        for (int i = 0; i < THEMES.length; i++) {
            SwatchButton sw = new SwatchButton(THEMES[i].swatch, i, this::applyTheme);
            sw.setSelected(i == themeIndex);
            swatchButtons.add(sw);
            swatchRow.add(sw);
        }
        themesPanel.add(themesLabel);
        themesPanel.add(swatchRow);
        bottomRow.add(themesPanel, BorderLayout.WEST);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        actionsPanel.setOpaque(false);
        FlatButton resetBtn = new FlatButton("\u21BB Reset", new Color(90, 200, 160));
        resetBtn.addActionListener(this::onResetClicked);
        FlatButton exitBtn = new FlatButton("\u2192] Exit", new Color(230, 90, 110));
        exitBtn.addActionListener(this::onExitClicked);
        actionsPanel.add(resetBtn);
        actionsPanel.add(exitBtn);
        bottomRow.add(actionsPanel, BorderLayout.CENTER);

        JPanel soundPanel = new JPanel();
        soundPanel.setOpaque(false);
        soundPanel.setLayout(new BoxLayout(soundPanel, BoxLayout.Y_AXIS));
        JLabel soundLabel = new JLabel("SOUND");
        soundLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        soundLabel.setForeground(new Color(200, 200, 215));
        soundSwitch = new SoundSwitch(soundOn);
        soundSwitch.addActionListener(e -> setSound(soundSwitch.isSelected()));
        JPanel soundRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        soundRow.setOpaque(false);
        soundRow.add(soundSwitch);
        soundPanel.add(soundLabel);
        soundPanel.add(soundRow);
        bottomRow.add(soundPanel, BorderLayout.EAST);

        footer.add(bottomRow);
        return footer;
    }

    private JPanel dividerStatusRow(JLabel label) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 0, 16);
        row.add(linePanel(), gc);
        gc.gridx = 1;
        gc.weightx = 0;
        gc.insets = new Insets(0, 0, 0, 0);
        row.add(label, gc);
        gc.gridx = 2;
        gc.weightx = 1;
        gc.insets = new Insets(0, 16, 0, 0);
        row.add(linePanel(), gc);
        return row;
    }

    private JPanel linePanel() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(new Color(140, 120, 180, 140));
        p.setPreferredSize(new Dimension(10, 1));
        return p;
    }

    // =====================================================================
    // Game flow
    // =====================================================================
    private void newGame() {
        if (glowTimer != null) {
            glowTimer.stop();
        }
        confetti.stop();
        Arrays.fill(board, null);
        for (GlowButton cell : cells) {
            cell.clearMark();
            cell.setEnabled(true);
            cell.resetGlow();
        }
        current = Player.O;
        gameOver = false;
        moveHistory.clear();
        hintsLeft = 3;
        aiSuggestLeft = 2;
        if (hintBtn != null) hintBtn.setBadge(hintsLeft);
        if (aiSuggestBtn != null) aiSuggestBtn.setBadge(aiSuggestLeft);
        refreshLastMoves();
        updatePreviewMarks();
        updateStatus("Player O's Turn", "Make your move!", "Player O's turn");
    }

    private void onCellClicked(int pos) {
        if (gameOver || board[pos] != null) {
            return;
        }
        if (vsComputer && current == Player.X) {
            return; // computer's turn, ignore human clicks on X
        }
        placeMark(pos, current);

        if (!gameOver && vsComputer && current == Player.X) {
            int aiMove = computerMove();
            if (aiMove >= 0) {
                placeMark(aiMove, current);
            }
        }
    }

    private void placeMark(int pos, Player player) {
        board[pos] = player;
        GlowButton cell = cells[pos];
        Color markColor = player == Player.O ? O_COLOR : X_COLOR;
        cell.setMark(player == Player.O ? GlowButton.Mark.O : GlowButton.Mark.X, markColor);
        cell.setEnabled(false);
        playBeep();

        MoveRecord rec = new MoveRecord(pos, player);
        moveHistory.add(rec);
        refreshLastMoves();

        int[] winLine = winningLine(player);
        if (winLine != null) {
            gameOver = true;
            rec.endedWin = true;
            animateWin(winLine);
            recordWin(player);
            updateStatus("Player " + player + " Wins!", "\uD83C\uDF89 Congratulations!",
                    "Player " + player + " wins! \uD83C\uDF89");
            disableAll();
            updatePreviewMarks();
            return;
        }

        if (isBoardFull()) {
            gameOver = true;
            rec.endedDraw = true;
            draws++;
            gamesPlayed++;
            refreshStats();
            updateStatus("Match Tied!", "No winner this round", "Match tied!");
            updatePreviewMarks();
            return;
        }

        current = current.other();
        updatePreviewMarks();
        updateStatus("Player " + current + "'s Turn", "Make your move!", "Player " + current + "'s turn");
    }

    /** Tells every empty, enabled cell what ghost-preview mark to show on hover. */
    private void updatePreviewMarks() {
        GlowButton.Mark previewMark = current == Player.O ? GlowButton.Mark.O : GlowButton.Mark.X;
        Color previewColor = current == Player.O ? O_COLOR : X_COLOR;
        boolean humanTurnActive = !gameOver && !(vsComputer && current == Player.X);
        for (GlowButton cell : cells) {
            cell.setPreview(humanTurnActive ? previewMark : null, previewColor);
        }
    }

    private void recordWin(Player player) {
        gamesPlayed++;
        if (player == Player.O) {
            oWins++;
            oScoreValue.setText(String.valueOf(oWins));
            oCard.pulse();
        } else {
            xWins++;
            xScoreValue.setText(String.valueOf(xWins));
            xCard.pulse();
        }
        refreshStats();
    }

    private void refreshStats() {
        gamesPlayedValue.setText(String.valueOf(gamesPlayed));
        oWinsStatValue.setText(String.valueOf(oWins));
        xWinsStatValue.setText(String.valueOf(xWins));
        drawsStatValue.setText(String.valueOf(draws));
    }

    private void refreshLastMoves() {
        int total = moveHistory.size();
        for (int i = 0; i < 3; i++) {
            int idx = total - 1 - i;
            if (idx >= 0) {
                MoveRecord rec = moveHistory.get(idx);
                int row = rec.pos / 3 + 1;
                int col = rec.pos % 3 + 1;
                lastMoveLabels[i].setText((i + 1) + ".   " + rec.player + " \u2192 (" + row + "," + col + ")");
            } else {
                lastMoveLabels[i].setText((i + 1) + ".   \u2013");
            }
        }
    }

    private void animateWin(int[] line) {
        for (int idx : line) {
            cells[idx].setGlowing(true);
        }
        final float[] phase = {0f};
        glowTimer = new Timer(40, e -> {
            phase[0] += 0.12f;
            float pulse = (float) (0.55 + 0.45 * Math.sin(phase[0]));
            for (int idx : line) {
                cells[idx].setGlowIntensity(pulse);
            }
        });
        glowTimer.start();

        Point boardCenter = SwingUtilities.convertPoint(
                cells[line[1]].getParent(), cells[line[1]].getBounds().getLocation(), getRootPane());
        int cx = boardCenter.x + cells[line[1]].getWidth() / 2;
        int cy = boardCenter.y + cells[line[1]].getHeight() / 2;
        confetti.burst(cx, cy);
    }

    private void disableAll() {
        for (GlowButton cell : cells) {
            cell.setEnabled(false);
        }
    }

    private boolean isBoardFull() {
        for (Player p : board) {
            if (p == null) return false;
        }
        return true;
    }

    private int[] winningLine(Player player) {
        for (int[] line : WIN_LINES) {
            if (board[line[0]] == player && board[line[1]] == player && board[line[2]] == player) {
                return line;
            }
        }
        return null;
    }

    private void updateStatus(String headline, String sub, String footerText) {
        statusLabel.setText(footerText);
        statusHeadline.setText(headline);
        statusSub.setText(sub);
        Color c = current == Player.O ? O_COLOR : X_COLOR;
        statusHeadline.setForeground(gameOver ? WIN_GLOW : c);
    }

    // =====================================================================
    // Minimax AI (generalized for either player) + difficulty-weighted computer moves
    // =====================================================================
    private int bestMoveFor(Player p) {
        boolean maximizing = p == Player.X;
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int move = -1;
        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null) {
                board[i] = p;
                int score = minimax(!maximizing);
                board[i] = null;
                boolean better = maximizing ? score > bestScore : score < bestScore;
                if (better) {
                    bestScore = score;
                    move = i;
                }
            }
        }
        return move;
    }

    private int computerMove() {
        if (difficulty.randomWeight > 0 && rnd.nextDouble() < difficulty.randomWeight) {
            List<Integer> empties = new ArrayList<>();
            for (int i = 0; i < SIZE; i++) {
                if (board[i] == null) empties.add(i);
            }
            if (!empties.isEmpty()) {
                return empties.get(rnd.nextInt(empties.size()));
            }
        }
        return bestMoveFor(Player.X);
    }

    private int minimax(boolean isMaximizing) {
        if (winningLine(Player.X) != null) return 1;
        if (winningLine(Player.O) != null) return -1;
        if (isBoardFull()) return 0;

        int best = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Player turn = isMaximizing ? Player.X : Player.O;

        for (int i = 0; i < SIZE; i++) {
            if (board[i] == null) {
                board[i] = turn;
                int score = minimax(!isMaximizing);
                board[i] = null;
                best = isMaximizing ? Math.max(best, score) : Math.min(best, score);
            }
        }
        return best;
    }

    // =====================================================================
    // Button handlers
    // =====================================================================
    private void onResetClicked(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Start a new game? Current board will be cleared.",
                "Reset", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            newGame();
        }
    }

    private void onExitClicked(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Exit", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void onUndoClicked() {
        if (moveHistory.isEmpty() || gameOver && moveHistory.isEmpty()) {
            // nothing to undo
        }
        if (moveHistory.isEmpty()) return;

        int toPop = 1;
        if (vsComputer && moveHistory.size() >= 2) {
            MoveRecord last = moveHistory.get(moveHistory.size() - 1);
            MoveRecord prev = moveHistory.get(moveHistory.size() - 2);
            if (last.player == Player.X && prev.player == Player.O && !last.endedWin && !last.endedDraw) {
                toPop = 2;
            }
        }

        for (int k = 0; k < toPop && !moveHistory.isEmpty(); k++) {
            MoveRecord rec = moveHistory.remove(moveHistory.size() - 1);
            board[rec.pos] = null;
            cells[rec.pos].clearMark();
            cells[rec.pos].setEnabled(true);
            if (rec.endedWin) {
                gameOver = false;
                if (rec.player == Player.O) { oWins--; oScoreValue.setText(String.valueOf(oWins)); }
                else { xWins--; xScoreValue.setText(String.valueOf(xWins)); }
                gamesPlayed--;
                if (glowTimer != null) glowTimer.stop();
                confetti.stop();
                for (GlowButton c : cells) c.resetGlow();
            } else if (rec.endedDraw) {
                gameOver = false;
                draws--;
                gamesPlayed--;
            }
        }
        refreshStats();
        current = moveHistory.isEmpty() ? Player.O : moveHistory.get(moveHistory.size() - 1).player.other();
        refreshLastMoves();
        updatePreviewMarks();
        updateStatus("Player " + current + "'s Turn", "Make your move!", "Player " + current + "'s turn");
    }

    private void onHintClicked() {
        if (gameOver || hintsLeft <= 0) return;
        int move = bestMoveFor(current);
        if (move >= 0) {
            hintsLeft--;
            hintBtn.setBadge(hintsLeft);
            cells[move].hintFlash(current == Player.O ? O_COLOR : X_COLOR);
        }
    }

    private void onAiSuggestClicked() {
        if (gameOver || aiSuggestLeft <= 0) return;
        int move = bestMoveFor(current);
        if (move >= 0) {
            aiSuggestLeft--;
            aiSuggestBtn.setBadge(aiSuggestLeft);
            cells[move].hintFlash(WIN_GLOW);
        }
    }

    private void onAnalyzeClicked() {
        if (isBoardFull() || gameOver) {
            JOptionPane.showMessageDialog(this, "The game has already finished.", "Analyze", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean maximizing = current == Player.X;
        int score = minimax(maximizing);
        String message;
        if (score > 0) message = "With perfect play from here, Player X wins.";
        else if (score < 0) message = "With perfect play from here, Player O wins.";
        else message = "With perfect play from here, the game ends in a draw.";
        JOptionPane.showMessageDialog(this, message, "Analyze", JOptionPane.INFORMATION_MESSAGE);
    }

    private void selectDifficulty(Difficulty d) {
        difficulty = d;
        for (DifficultyRow row : difficultyRows) {
            row.setSelected(row.value == d);
        }
    }

    private void toggleSound() {
        setSound(!soundOn);
    }

    private void setSound(boolean on) {
        soundOn = on;
        topSoundBtn.setText(soundOn ? "\uD83D\uDD0A" : "\uD83D\uDD07");
        soundSwitch.setSelected(soundOn);
        soundSwitch.repaint();
    }

    private void playBeep() {
        if (soundOn) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void applyTheme(int idx) {
        themeIndex = idx;
        ThemeDef t = THEMES[idx];
        BG_TOP = t.bgTop; BG_MID = t.bgMid; BG_MID2 = t.bgMid2; BG_BOTTOM = t.bgBottom;
        O_COLOR = t.o; X_COLOR = t.x; PANEL_LINE = t.panel;

        root.setColors(BG_TOP, BG_MID, BG_MID2, BG_BOTTOM);
        boardPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(PANEL_LINE, 2, 26),
                new EmptyBorder(18, 18, 18, 18)
        ));
        titleLabel.setColors(O_COLOR, X_COLOR);
        oCard.setAccent(O_COLOR);
        xCard.setAccent(X_COLOR);
        for (int i = 0; i < SIZE; i++) {
            if (board[i] != null) {
                cells[i].recolor(board[i] == Player.O ? O_COLOR : X_COLOR);
            }
        }
        updatePreviewMarks();
        twoPlayerBtn.repaint();
        vsComputerBtn.repaint();
        statusHeadline.setForeground(gameOver ? WIN_GLOW : (current == Player.O ? O_COLOR : X_COLOR));
        for (SwatchButton sw : swatchButtons) {
            sw.setSelected(sw.themeIdx == idx);
        }
        root.repaint();
    }

    // =====================================================================
    // Easing helpers
    // =====================================================================
    private static float easeOutCubic(float t) {
        float f = t - 1f;
        return f * f * f + 1f;
    }

    // =====================================================================
    // Custom visual components
    // =====================================================================

    /**
     * Panel painted with a rich, diagonal multi-stop gradient background, slow drifting
     * "aurora" glow blobs of varied color and size, and a soft vignette for cinematic depth.
     */
    private static class GradientPanel extends JPanel {
        private Color top;
        private Color mid;
        private Color mid2;
        private Color bottom;
        private float phase = 0f;
        private final Timer ambientTimer;

        GradientPanel(Color top, Color mid, Color mid2, Color bottom) {
            this.top = top;
            this.mid = mid;
            this.mid2 = mid2;
            this.bottom = bottom;
            setOpaque(false);
            ambientTimer = new Timer(45, e -> {
                phase += 0.01f;
                repaint();
            });
            ambientTimer.start();
        }

        void setColors(Color top, Color mid, Color mid2, Color bottom) {
            this.top = top; this.mid = mid; this.mid2 = mid2; this.bottom = bottom;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            LinearGradientPaint gp = new LinearGradientPaint(
                    new java.awt.geom.Point2D.Float(0, 0), new java.awt.geom.Point2D.Float(w * 0.35f, h),
                    new float[]{0f, 0.4f, 0.72f, 1f},
                    new Color[]{top, mid, mid2, bottom});
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            drawBlob(g2, w, h,
                    0.22f + 0.08f * (float) Math.sin(phase), 0.16f + 0.05f * (float) Math.cos(phase * 0.8),
                    Math.min(w, h) * 0.72f, withAlpha(O_COLOR, 58));
            drawBlob(g2, w, h,
                    0.82f + 0.05f * (float) Math.cos(phase * 0.65), 0.24f + 0.06f * (float) Math.sin(phase * 1.3),
                    Math.min(w, h) * 0.62f, withAlpha(X_COLOR, 55));
            drawBlob(g2, w, h,
                    0.48f + 0.06f * (float) Math.sin(phase * 0.5), 0.5f + 0.05f * (float) Math.cos(phase * 0.9),
                    Math.min(w, h) * 0.5f, withAlpha(PANEL_LINE, 30));
            drawBlob(g2, w, h,
                    0.5f + 0.05f * (float) Math.sin(phase * 0.45), 0.86f + 0.04f * (float) Math.cos(phase * 0.9),
                    Math.min(w, h) * 0.75f, withAlpha(PANEL_LINE, 42));
            drawBlob(g2, w, h,
                    0.13f + 0.04f * (float) Math.cos(phase * 0.6), 0.8f + 0.03f * (float) Math.sin(phase * 0.4),
                    Math.min(w, h) * 0.42f, new Color(255, 215, 90, 20));
            drawBlob(g2, w, h,
                    0.9f + 0.03f * (float) Math.sin(phase * 0.35), 0.9f + 0.03f * (float) Math.cos(phase * 0.5),
                    Math.min(w, h) * 0.38f, withAlpha(X_COLOR, 18));

            RadialGradientPaint vignette = new RadialGradientPaint(
                    new Point2D(w * 0.5f, h * 0.46f), Math.max(w, h) * 0.75f,
                    new float[]{0f, 0.72f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, 95)});
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);

            GradientPaint sheen = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 16), w * 0.6f, h * 0.35f, new Color(255, 255, 255, 0));
            g2.setPaint(sheen);
            g2.fillRect(0, 0, w, (int) (h * 0.45f));

            g2.dispose();
            super.paintComponent(g);
        }

        private Color withAlpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        private void drawBlob(Graphics2D g2, int w, int h, float relX, float relY, float radius, Color color) {
            float cx = w * relX;
            float cy = h * relY;
            if (radius <= 0) return;
            RadialGradientPaint paint = new RadialGradientPaint(
                    new Point2D(cx, cy), radius,
                    new float[]{0f, 1f},
                    new Color[]{color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)});
            g2.setPaint(paint);
            g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
        }
    }

    /** Small helper so RadialGradientPaint can take float coordinates directly. */
    private static class Point2D extends java.awt.geom.Point2D.Float {
        Point2D(float x, float y) { super(x, y); }
    }

    /** Title label rendered with a horizontal gradient fill and a soft breathing glow/shadow. */
    private static class GradientTitleLabel extends JComponent {
        private final String text;
        private Color left;
        private Color right;
        private final Font font = new Font("SansSerif", Font.BOLD, 32);
        private float glowPhase = 0f;
        private final Timer glowTimer;

        GradientTitleLabel(String text, Color left, Color right) {
            this.text = text;
            this.left = left;
            this.right = right;
            setOpaque(false);
            glowTimer = new Timer(45, e -> {
                glowPhase += 0.06f;
                repaint();
            });
            glowTimer.start();
        }

        void setColors(Color left, Color right) {
            this.left = left; this.right = right;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(font);
            return new Dimension(fm.stringWidth(text) + 30, fm.getHeight() + 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 6;

            float breathe = (float) (0.5 + 0.5 * Math.sin(glowPhase));
            int glowAlpha = (int) (40 + 55 * breathe);
            for (int r = 6; r >= 1; r--) {
                g2.setColor(new Color(left.getRed(), left.getGreen(), left.getBlue(),
                        Math.max(0, glowAlpha - r * 6)));
                g2.drawString(text, x, y - r / 3);
            }

            g2.setColor(new Color(0, 0, 0, 90));
            g2.drawString(text, x + 2, y + 3);

            g2.setPaint(new GradientPaint(x, 0, left, x + fm.stringWidth(text), 0, right));
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }

    /** Small rounded "card" showing a player's label and running win count, with a flash pulse on score. */
    private static class ScoreCard extends JPanel {
        final JLabel valueLabel;
        private float flash = 0f;
        private Timer flashTimer;
        private Color accent;
        private JLabel nameLabel;
        private JLabel winsLabel;

        ScoreCard(String label, Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BorderLayout(2, 2));

            nameLabel = new JLabel(label, SwingConstants.CENTER);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            nameLabel.setForeground(accent);

            valueLabel = new JLabel("0", SwingConstants.CENTER);
            valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
            valueLabel.setForeground(Color.WHITE);

            winsLabel = new JLabel("WINS: 0", SwingConstants.CENTER);
            winsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            winsLabel.setForeground(new Color(200, 200, 215));

            JPanel centerWrap = new JPanel();
            centerWrap.setOpaque(false);
            centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.Y_AXIS));
            valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            winsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerWrap.add(valueLabel);
            centerWrap.add(winsLabel);

            add(nameLabel, BorderLayout.NORTH);
            add(centerWrap, BorderLayout.CENTER);

            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 130), 2, 18),
                    new EmptyBorder(8, 20, 8, 20)
            ));

            valueLabel.addPropertyChangeListener("text", e -> winsLabel.setText("WINS: " + valueLabel.getText()));
        }

        void setAccent(Color newAccent) {
            this.accent = newAccent;
            nameLabel.setForeground(newAccent);
            setBorder(BorderFactory.createCompoundBorder(
                    new RoundedLineBorder(new Color(newAccent.getRed(), newAccent.getGreen(), newAccent.getBlue(), 130), 2, 18),
                    new EmptyBorder(8, 20, 8, 20)
            ));
            repaint();
        }

        void pulse() {
            flash = 1f;
            if (flashTimer != null) flashTimer.stop();
            flashTimer = new Timer(25, e -> {
                flash -= 0.05f;
                if (flash <= 0f) {
                    flash = 0f;
                    flashTimer.stop();
                }
                repaint();
            });
            flashTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int baseAlpha = 20 + (int) (60 * flash);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), baseAlpha));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded border, used by the board frame and the score cards. */
    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int arc;

        RoundedLineBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Float(x + thickness / 2f, y + thickness / 2f,
                    width - thickness, height - thickness, arc, arc));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }
    }

    /**
     * A game-board cell: rounded glass button with hover lift + ghost preview,
     * a hand-drawn O/X mark that animates in with an ease-out reveal + bounce,
     * a pulsing glow used to celebrate the winning line, and a separate short
     * pulsing "hint" ring used by the Hint / AI Suggest toolbar actions.
     */
    private static class GlowButton extends JButton {

        enum Mark { O, X }

        private final Color baseBg;
        private final Color hoverBg;
        private final Color borderColor;
        private final int arc;
        private boolean hovering = false;
        private boolean glowing = false;
        private float glowIntensity = 0f;

        private Mark mark = null;
        private Color markColor = Color.WHITE;
        private float revealT = 1f;
        private Timer revealTimer;

        private Mark previewMark = null;
        private Color previewColor = Color.WHITE;

        private boolean hintActive = false;
        private float hintIntensity = 0f;
        private Color hintColor = Color.WHITE;
        private Timer hintTimer;

        GlowButton(Color baseBg, Color hoverBg, Color borderColor, int arc) {
            super("");
            this.baseBg = baseBg;
            this.hoverBg = hoverBg;
            this.borderColor = borderColor;
            this.arc = arc;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override
                public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
            });
        }

        void resetGlow() {
            glowing = false;
            glowIntensity = 0f;
            repaint();
        }

        void setGlowing(boolean value) {
            this.glowing = value;
            repaint();
        }

        void setGlowIntensity(float value) {
            this.glowIntensity = value;
            repaint();
        }

        void clearMark() {
            mark = null;
            revealT = 1f;
            if (revealTimer != null) revealTimer.stop();
            repaint();
        }

        void setPreview(Mark m, Color color) {
            this.previewMark = m;
            this.previewColor = color;
            if (mark == null) repaint();
        }

        void setMark(Mark m, Color color) {
            this.mark = m;
            this.markColor = color;
            this.revealT = 0f;
            if (revealTimer != null) revealTimer.stop();
            revealTimer = new Timer(15, null);
            revealTimer.addActionListener(e -> {
                revealT += 0.07f;
                if (revealT >= 1f) {
                    revealT = 1f;
                    revealTimer.stop();
                }
                repaint();
            });
            revealTimer.start();
        }

        /** Recolor an already-placed mark (used when switching themes) without replaying the reveal animation. */
        void recolor(Color newColor) {
            if (mark != null) {
                markColor = newColor;
                repaint();
            }
        }

        /** Brief pulsing highlight ring used by the Hint / AI Suggest toolbar actions. */
        void hintFlash(Color color) {
            this.hintColor = color;
            this.hintActive = true;
            if (hintTimer != null) hintTimer.stop();
            final float[] t = {0f};
            hintTimer = new Timer(30, null);
            hintTimer.addActionListener(e -> {
                t[0] += 0.05f;
                hintIntensity = (float) (0.5 + 0.5 * Math.sin(t[0] * 5));
                repaint();
                if (t[0] >= 1.5f) {
                    hintActive = false;
                    hintIntensity = 0f;
                    hintTimer.stop();
                    repaint();
                }
            });
            hintTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();

            Color fill = (hovering && isEnabled()) ? hoverBg : baseBg;
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc);
            Color fillTop = new Color(255, 255, 255, fill.getAlpha() + 6);
            Color fillBottom = new Color(255, 255, 255, Math.max(0, fill.getAlpha() - 6));
            g2.setPaint(new GradientPaint(0, 1, fillTop, 0, h - 1, fillBottom));
            g2.fill(body);

            Graphics2D sheenG = (Graphics2D) g2.create();
            sheenG.clip(body);
            sheenG.setColor(new Color(255, 255, 255, 10));
            sheenG.fill(new RoundRectangle2D.Float(1, 1, w - 2, h * 0.4f, arc, arc));
            sheenG.dispose();

            if (glowing) {
                Color glow = new Color(WIN_GLOW.getRed(), WIN_GLOW.getGreen(), WIN_GLOW.getBlue(),
                        (int) (90 + 120 * glowIntensity));
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(3 + 2 * glowIntensity));
                g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            } else if (hintActive) {
                Color glow = new Color(hintColor.getRed(), hintColor.getGreen(), hintColor.getBlue(),
                        (int) (90 + 120 * hintIntensity));
                g2.setColor(glow);
                g2.setStroke(new BasicStroke(3 + 2 * hintIntensity));
                g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            } else {
                Color b = (hovering && isEnabled()) ? brighten(borderColor, 40) : borderColor;
                g2.setColor(b);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, arc, arc));
            }

            if (mark != null) {
                drawMark(g2, w, h, mark, markColor, easeOutCubic(revealT), bounceScale(revealT));
            } else if (hovering && isEnabled() && previewMark != null) {
                Color faint = new Color(previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue(), 55);
                drawMark(g2, w, h, previewMark, faint, 1f, 1f);
            }

            g2.dispose();
        }

        private float bounceScale(float t) {
            if (t >= 1f) return 1f;
            return 1f + 0.16f * (float) Math.sin(Math.min(t, 1f) * Math.PI) * (1f - t);
        }

        private void drawMark(Graphics2D g2, int w, int h, Mark m, Color color, float progress, float scale) {
            float cx = w / 2f;
            float cy = h / 2f;
            float size = Math.min(w, h) * 0.34f * scale;

            Graphics2D gm = (Graphics2D) g2.create();
            gm.setColor(color);

            if (m == Mark.O) {
                float stroke = Math.max(4f, size * 0.22f);
                gm.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float extent = 360f * progress;
                Arc2D.Float arc2d = new Arc2D.Float(cx - size, cy - size, size * 2, size * 2,
                        90f, -extent, Arc2D.OPEN);
                gm.draw(arc2d);
            } else {
                float stroke = Math.max(4f, size * 0.22f);
                gm.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float half = progress;
                float p1 = Math.min(1f, half * 2f);
                float p2 = Math.max(0f, half * 2f - 1f);

                float x1 = cx - size, y1 = cy - size, x2 = cx + size, y2 = cy + size;
                gm.draw(new Line2D.Float(x1, y1, x1 + (x2 - x1) * p1, y1 + (y2 - y1) * p1));

                float x3 = cx + size, y3 = cy - size, x4 = cx - size, y4 = cy + size;
                gm.draw(new Line2D.Float(x3, y3, x3 + (x4 - x3) * p2, y3 + (y4 - y3) * p2));
            }
            gm.dispose();
        }

        private Color brighten(Color c, int amount) {
            return new Color(
                    Math.min(255, c.getRed() + amount),
                    Math.min(255, c.getGreen() + amount),
                    Math.min(255, c.getBlue() + amount),
                    c.getAlpha());
        }
    }

    /** Rounded pill-shaped mode selector (2 Player / vs Computer) with a smooth fill transition. */
    private static class PillToggle extends JToggleButton {
        private PillToggle linked;
        private float selectProgress;
        private Timer animTimer;

        PillToggle(String text, boolean selected) {
            super(text, selected);
            this.selectProgress = selected ? 1f : 0f;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 20, 9, 20));
            addActionListener(e -> {
                setSelected(true);
                if (linked != null) linked.setSelected(false);
                animateTo(1f);
                if (linked != null) linked.animateTo(0f);
            });
        }

        void linkedGroup(PillToggle other) {
            this.linked = other;
        }

        private void animateTo(float target) {
            if (animTimer != null) animTimer.stop();
            animTimer = new Timer(15, null);
            animTimer.addActionListener(e -> {
                float delta = target - selectProgress;
                selectProgress += delta * 0.35f;
                if (Math.abs(target - selectProgress) < 0.01f) {
                    selectProgress = target;
                    animTimer.stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(0, 0, 0, (int) (55 * selectProgress) + 18));
            g2.fill(new RoundRectangle2D.Float(0, 2.5f, w, h, h, h));

            RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, 0, w, h, h, h);
            g2.setColor(new Color(255, 255, 255, (int) (20 * (1f - selectProgress)) + 6));
            g2.fill(body);

            if (selectProgress > 0.01f) {
                Graphics2D gGrad = (Graphics2D) g2.create();
                gGrad.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectProgress));
                gGrad.setPaint(new GradientPaint(0, 0, O_COLOR, w, 0, X_COLOR));
                gGrad.fill(body);
                gGrad.dispose();
            }

            Graphics2D clipG = (Graphics2D) g2.create();
            clipG.clip(body);
            clipG.setColor(new Color(255, 255, 255, 28));
            clipG.fill(new RoundRectangle2D.Float(1, 1, w - 2, h * 0.45f, h, h));
            clipG.dispose();

            g2.setStroke(new BasicStroke(1.1f));
            g2.setColor(new Color(255, 255, 255, (int) (30 + 40 * selectProgress)));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, h, h));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Pro-style rounded action button (Reset / Exit): gradient fill that deepens on hover,
     * a glassy top highlight, a soft multi-layer drop shadow, a subtle bright rim, and a
     * smoothly animated hover/press lerp instead of an instant color snap.
     */
    private static class FlatButton extends JButton {
        private final Color accent;
        private boolean hovering = false;
        private boolean pressed = false;
        private float hoverT = 0f;
        private float pressT = 0f;
        private Timer animTimer;

        FlatButton(String text, Color accent) {
            super(text);
            this.accent = accent;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(12, 30, 12, 30));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { hovering = true; startAnim(); }
                @Override
                public void mouseExited(MouseEvent e) { hovering = false; pressed = false; startAnim(); }
                @Override
                public void mousePressed(MouseEvent e) { pressed = true; startAnim(); }
                @Override
                public void mouseReleased(MouseEvent e) { pressed = false; startAnim(); }
            });
        }

        private void startAnim() {
            if (animTimer != null && animTimer.isRunning()) return;
            animTimer = new Timer(15, e -> {
                float hTarget = hovering ? 1f : 0f;
                float pTarget = pressed ? 1f : 0f;
                hoverT += (hTarget - hoverT) * 0.3f;
                pressT += (pTarget - pressT) * 0.4f;
                if (Math.abs(hTarget - hoverT) < 0.01f && Math.abs(pTarget - pressT) < 0.01f) {
                    hoverT = hTarget;
                    pressT = pTarget;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float yOffset = pressT * 2f;

            float shadowFade = 1f - pressT;
            for (int i = 3; i >= 1; i--) {
                g2.setColor(new Color(0, 0, 0, (int) ((22 * i) * shadowFade)));
                g2.fill(new RoundRectangle2D.Float(0, 4 + i, w, h, h, h));
            }

            Color base = darken(accent, 18);
            Color light = brighten(accent, hoverT > 0 ? 34 : 14);
            int fillAlpha = (int) (150 + 70 * hoverT - 20 * pressT);
            Color topStop = new Color(light.getRed(), light.getGreen(), light.getBlue(), fillAlpha);
            Color botStop = new Color(base.getRed(), base.getGreen(), base.getBlue(), fillAlpha);

            RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, yOffset, w, h - yOffset, h, h);
            GradientPaint fill = new GradientPaint(0, yOffset, topStop, 0, h - yOffset, botStop);
            g2.setPaint(fill);
            g2.fill(body);

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, (int) (60 + 50 * hoverT)));
            g2.draw(new RoundRectangle2D.Float(0.6f, yOffset + 0.6f, w - 1.2f, h - yOffset - 1.2f, h, h));

            Graphics2D clipG = (Graphics2D) g2.create();
            clipG.clip(body);
            clipG.setColor(new Color(255, 255, 255, (int) (34 * (1f - pressT * 0.6f))));
            clipG.fill(new RoundRectangle2D.Float(2, yOffset + 2, w - 4, (h - yOffset) * 0.42f, h, h));
            clipG.dispose();

            g2.dispose();

            Graphics2D textG = (Graphics2D) g.create();
            textG.translate(0, (int) yOffset);
            super.paintComponent(textG);
            textG.dispose();
        }

        private Color brighten(Color c, int amount) {
            return new Color(
                    Math.min(255, c.getRed() + amount),
                    Math.min(255, c.getGreen() + amount),
                    Math.min(255, c.getBlue() + amount));
        }

        private Color darken(Color c, int amount) {
            return new Color(
                    Math.max(0, c.getRed() - amount),
                    Math.max(0, c.getGreen() - amount),
                    Math.max(0, c.getBlue() - amount));
        }
    }

    /** Small circular icon button used in the top bar (settings / sound). */
    private static class IconButton extends JButton {
        private boolean hovering = false;

        IconButton(String glyph) {
            super(glyph);
            setFont(new Font("SansSerif", Font.PLAIN, 16));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 12, 8, 12));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(255, 255, 255, hovering ? 30 : 16));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
            g2.setColor(new Color(255, 255, 255, hovering ? 60 : 34));
            g2.setStroke(new BasicStroke(1.1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Toolbar button used for New Game / Undo / Hint / AI Suggest / Analyze / Theme:
     * an icon glyph above a small label, with an optional numeric usage badge in the corner.
     */
    private static class ToolbarIconButton extends JButton {
        private final String glyph;
        private final String label;
        private final Color accent;
        private boolean hovering = false;
        private int badge = 0;

        ToolbarIconButton(String glyph, String label, Color accent) {
            super();
            this.glyph = glyph;
            this.label = label;
            this.accent = accent;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(92, 62));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovering = false; repaint(); }
            });
        }

        void setBadge(int n) {
            this.badge = n;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, 0, w, h, 16, 16);
            int alpha = hovering ? 55 : 30;
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            g2.fill(body);
            g2.setStroke(new BasicStroke(1.1f));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hovering ? 160 : 100));
            g2.draw(body);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            FontMetrics gfm = g2.getFontMetrics();
            int gx = (w - gfm.stringWidth(glyph)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(glyph, gx, h / 2 - 2);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics lfm = g2.getFontMetrics();
            int lx = (w - lfm.stringWidth(label)) / 2;
            g2.setColor(new Color(235, 235, 245));
            g2.drawString(label, lx, h - 10);

            if (badge > 0) {
                int r = 16;
                int bx = w - r - 2;
                int by = 2;
                g2.setColor(new Color(230, 90, 110));
                g2.fill(new Ellipse2D.Float(bx, by, r, r));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics bfm = g2.getFontMetrics();
                String s = String.valueOf(badge);
                g2.drawString(s, bx + (r - bfm.stringWidth(s)) / 2, by + (r + bfm.getAscent() - 3) / 2);
            }
            g2.dispose();
        }
    }

    /** A clickable difficulty row with a hand-painted radio dot and a highlighted selected state. */
    private static class DifficultyRow extends JPanel {
        final Difficulty value;
        private boolean selected;

        DifficultyRow(Difficulty value, boolean selected, Consumer<Difficulty> onSelect) {
            this.value = value;
            this.selected = selected;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(10, 34));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onSelect.accept(value); }
            });
        }

        void setSelected(boolean s) {
            this.selected = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            Color accent = value == Difficulty.IMPOSSIBLE ? X_COLOR : O_COLOR;
            if (selected) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 34));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, 12, 12));
            }

            int cy = h / 2;
            int cx = 16;
            g2.setStroke(new BasicStroke(1.6f));
            g2.setColor(new Color(255, 255, 255, 150));
            g2.draw(new Ellipse2D.Float(cx - 7, cy - 7, 14, 14));
            if (selected) {
                g2.setColor(accent);
                g2.fill(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));
            }

            String text = value.label + (value == Difficulty.IMPOSSIBLE ? "  \uD83D\uDC51" : "");
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.setColor(selected ? Color.WHITE : new Color(210, 210, 225));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, cx + 16, cy + fm.getAscent() / 2 - 2);

            g2.dispose();
        }
    }

    /** Small circular swatch used to pick one of the color themes. */
    private static class SwatchButton extends JButton {
        final int themeIdx;
        private final Color swatch;
        private boolean selected;

        SwatchButton(Color swatch, int themeIdx, Consumer<Integer> onSelect) {
            this.swatch = swatch;
            this.themeIdx = themeIdx;
            setPreferredSize(new Dimension(26, 26));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> onSelect.accept(themeIdx));
        }

        public void setSelected(boolean s) {
            this.selected = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(swatch);
            g2.fill(new Ellipse2D.Float(2, 2, w - 4, h - 4));
            if (selected) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(Color.WHITE);
                g2.draw(new Ellipse2D.Float(1, 1, w - 2, h - 2));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String check = "\u2713";
                g2.drawString(check, (w - fm.stringWidth(check)) / 2, (h + fm.getAscent()) / 2 - 3);
            } else {
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(255, 255, 255, 70));
                g2.draw(new Ellipse2D.Float(1, 1, w - 2, h - 2));
            }
            g2.dispose();
        }
    }

    /** ON/OFF pill switch used for the sound toggle. */
    private static class SoundSwitch extends JToggleButton {
        SoundSwitch(boolean initiallyOn) {
            super("", initiallyOn);
            setPreferredSize(new Dimension(54, 26));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean on = isSelected();
            RoundRectangle2D.Float track = new RoundRectangle2D.Float(0, 0, w, h, h, h);
            g2.setColor(on ? new Color(60, 200, 140) : new Color(255, 255, 255, 40));
            g2.fill(track);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, 60));
            g2.draw(track);

            int knobSize = h - 6;
            int knobX = on ? w - knobSize - 3 : 3;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Float(knobX, 3, knobSize, knobSize));
            g2.dispose();
        }
    }

    /** Rounded translucent "glass" card used throughout the sidebar. */
    private static class CardPanel extends JPanel {
        private final Color accent;

        CardPanel(Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BorderLayout(0, 8));
            setBorder(new EmptyBorder(14, 16, 14, 16));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, pref.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
            g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, 18, 18));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Lightweight confetti burst used to celebrate a win. Installed as the frame's glass pane. */
    private static class ConfettiOverlay extends JComponent {
        private static final Color[] PALETTE = {
                new Color(64, 220, 255), new Color(255, 72, 176),
                new Color(255, 215, 90), new Color(180, 140, 255), Color.WHITE
        };
        private final List<Particle> particles = new ArrayList<>();
        private final Random rnd = new Random();
        private Timer timer;
        private int ticksLeft;

        ConfettiOverlay() {
            setOpaque(false);
        }

        void burst(int cx, int cy) {
            particles.clear();
            for (int i = 0; i < 90; i++) {
                double angle = rnd.nextDouble() * Math.PI * 2;
                double speed = 2 + rnd.nextDouble() * 6;
                Particle p = new Particle();
                p.x = cx;
                p.y = cy;
                p.vx = (float) (Math.cos(angle) * speed);
                p.vy = (float) (Math.sin(angle) * speed - 4);
                p.size = 5 + rnd.nextFloat() * 6;
                p.color = PALETTE[rnd.nextInt(PALETTE.length)];
                p.rotation = rnd.nextFloat() * 360f;
                p.rotSpeed = -10 + rnd.nextFloat() * 20;
                particles.add(p);
            }
            ticksLeft = 130;
            setVisible(true);
            if (timer != null) timer.stop();
            timer = new Timer(16, e -> tick());
            timer.start();
        }

        void stop() {
            if (timer != null) timer.stop();
            particles.clear();
            setVisible(false);
        }

        private void tick() {
            ticksLeft--;
            for (Particle p : particles) {
                p.vy += 0.18f;
                p.x += p.vx;
                p.y += p.vy;
                p.rotation += p.rotSpeed;
            }
            repaint();
            if (ticksLeft <= 0) {
                stop();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (particles.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Particle p : particles) {
                Graphics2D pg = (Graphics2D) g2.create();
                pg.translate(p.x, p.y);
                pg.rotate(Math.toRadians(p.rotation));
                pg.setColor(p.color);
                pg.fillRect((int) (-p.size / 2), (int) (-p.size / 2), (int) p.size, (int) (p.size * 0.6f));
                pg.dispose();
            }
            g2.dispose();
        }

        private static class Particle {
            float x, y, vx, vy, size, rotation, rotSpeed;
            Color color;
        }
    }

    // =====================================================================
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to default look and feel
        }
        SwingUtilities.invokeLater(() -> new Tic_Tac_Toe().setVisible(true));
    }
}