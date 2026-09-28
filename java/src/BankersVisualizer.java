import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class BankersVisualizer extends JFrame {
    // 系统配置
    private static final int PROCESS_COUNT = 5;
    private static final int RESOURCE_TYPES = 3;
    private static final int MAX_RESOURCE = 15;

    // 系统状态
    private int[] availableResources;
    private int[][] maxDemand;
    private int[][] allocated;
    private int[][] remainingNeed;
    private boolean[] completedProcesses;

    // 可视化组件
    private GraphPanel graphPanel;
    private JTextArea statusArea;
    private JButton requestBtn;
    private JButton releaseBtn;
    private JButton detectBtn;
    private JButton resetBtn;

    // 随机数生成器
    private Random rand = new Random();

    public BankersVisualizer() {
        setTitle("银行家算法交互演示");
        setSize(1400, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 设置外观风格
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initSystemState();
        setupUI();

        setVisible(true);
    }

    private void initSystemState() {
        // 初始化可用资源
        availableResources = new int[RESOURCE_TYPES];
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            availableResources[i] = rand.nextInt(MAX_RESOURCE/2) + MAX_RESOURCE/2;
        }

        // 初始化进程数据
        maxDemand = new int[PROCESS_COUNT][RESOURCE_TYPES];
        allocated = new int[PROCESS_COUNT][RESOURCE_TYPES];
        remainingNeed = new int[PROCESS_COUNT][RESOURCE_TYPES];
        completedProcesses = new boolean[PROCESS_COUNT];

        // 随机生成初始分配
        int[] tempAvail = availableResources.clone();
        for (int i = 0; i < PROCESS_COUNT; i++) {
            for (int j = 0; j < RESOURCE_TYPES; j++) {
                maxDemand[i][j] = rand.nextInt(tempAvail[j]/2 + 1) + 1;
                allocated[i][j] = rand.nextInt(Math.min(maxDemand[i][j], tempAvail[j]) + 1);
                remainingNeed[i][j] = maxDemand[i][j] - allocated[i][j];
                tempAvail[j] -= allocated[i][j];
            }
        }
        availableResources = tempAvail;

        // 确保初始状态安全
        while (!checkSystemSafety()) {
            initSystemState();
        }
    }

    private void setupUI() {
        // 主面板设置
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 图形展示面板
        graphPanel = new GraphPanel();
        graphPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.add(graphPanel, BorderLayout.CENTER);

        // 状态信息面板
        statusArea = new JTextArea(10, 60);
        statusArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusArea.setBackground(new Color(250, 250, 250));
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("系统状态信息"));
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        // 控制按钮面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("操作控制"));
        controlPanel.setBackground(new Color(230, 230, 230));

        requestBtn = createStyledButton("生成随机请求", new Color(70, 130, 180));
        releaseBtn = createStyledButton("释放资源", new Color(60, 179, 113));
        detectBtn = createStyledButton("检测死锁", new Color(205, 92, 92));
        resetBtn = createStyledButton("重置系统", new Color(169, 169, 169));

        requestBtn.addActionListener(e -> handleRandomRequest());
        releaseBtn.addActionListener(e -> handleResourceRelease());
        detectBtn.addActionListener(e -> detectDeadlocks());
        resetBtn.addActionListener(e -> resetSystem());

        controlPanel.add(requestBtn);
        controlPanel.add(releaseBtn);
        controlPanel.add(detectBtn);
        controlPanel.add(resetBtn);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        add(mainPanel);

        updateStatus("系统初始化完成，当前处于安全状态");
        graphPanel.repaint();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }

    private void handleRandomRequest() {
        int processId = rand.nextInt(PROCESS_COUNT);
        if (completedProcesses[processId]) {
            updateStatus("进程 P" + processId + " 已完成，无法请求资源");
            return;
        }

        int[] request = new int[RESOURCE_TYPES];
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            request[i] = remainingNeed[processId][i] > 0 ? rand.nextInt(remainingNeed[processId][i] + 1) : 0;
        }

        processRequest(processId, request);
    }

    private void handleResourceRelease() {
        int processId = rand.nextInt(PROCESS_COUNT);
        if (completedProcesses[processId]) {
            updateStatus("进程 P" + processId + " 已完成，无法释放资源");
            return;
        }

        int[] release = new int[RESOURCE_TYPES];
        boolean hasResources = false;

        for (int i = 0; i < RESOURCE_TYPES; i++) {
            if (allocated[processId][i] > 0) {
                release[i] = rand.nextInt(allocated[processId][i] + 1);
                hasResources = true;
            }
        }

        if (!hasResources) {
            updateStatus("进程 P" + processId + " 没有持有任何资源");
            return;
        }

        processRelease(processId, release);
    }

    private void processRequest(int processId, int[] request) {
        // 验证请求有效性
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            if (request[i] > remainingNeed[processId][i]) {
                updateStatus("无效请求: P" + processId + " 请求超过需求 (R" + i + ": " + request[i] + " > " + remainingNeed[processId][i] + ")");
                return;
            }
            if (request[i] > availableResources[i]) {
                updateStatus("无效请求: 资源不足 (R" + i + ": " + request[i] + " > " + availableResources[i] + ")");
                return;
            }
        }

        // 模拟分配
        int[] tempAvail = availableResources.clone();
        int[][] tempAlloc = deepCopy(allocated);
        int[][] tempNeed = deepCopy(remainingNeed);

        for (int i = 0; i < RESOURCE_TYPES; i++) {
            tempAvail[i] -= request[i];
            tempAlloc[processId][i] += request[i];
            tempNeed[processId][i] -= request[i];
        }

        // 安全性检查
        if (simulateSafetyCheck(tempAvail, tempAlloc, tempNeed)) {
            // 安全则实际分配
            availableResources = tempAvail;
            allocated = tempAlloc;
            remainingNeed = tempNeed;

            // 检查进程是否完成
            checkProcessCompletion(processId);

            updateStatus("请求批准: P" + processId + " 获得资源 " + Arrays.toString(request));
            graphPanel.repaint();
        } else {
            updateStatus("请求拒绝: P" + processId + " 的请求会导致不安全状态");
        }
    }

    private void processRelease(int processId, int[] release) {
        // 验证释放有效性
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            if (release[i] > allocated[processId][i]) {
                updateStatus("无效释放: P" + processId + " 尝试释放超过已分配量 (R" + i + ": " + release[i] + " > " + allocated[processId][i] + ")");
                return;
            }
        }

        // 执行释放
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            availableResources[i] += release[i];
            allocated[processId][i] -= release[i];
            remainingNeed[processId][i] += release[i];
        }

        updateStatus("资源释放: P" + processId + " 释放资源 " + Arrays.toString(release));
        graphPanel.repaint();
    }

    private boolean checkSystemSafety() {
        return simulateSafetyCheck(availableResources.clone(), deepCopy(allocated), deepCopy(remainingNeed));
    }

    private boolean simulateSafetyCheck(int[] avail, int[][] alloc, int[][] need) {
        boolean[] finish = new boolean[PROCESS_COUNT];
        int[] work = avail.clone();
        int count = 0;

        while (count < PROCESS_COUNT) {
            boolean found = false;

            for (int i = 0; i < PROCESS_COUNT; i++) {
                if (!finish[i] && canProcessRun(i, work, need)) {
                    for (int j = 0; j < RESOURCE_TYPES; j++) {
                        work[j] += alloc[i][j];
                    }
                    finish[i] = true;
                    found = true;
                    count++;
                }
            }

            if (!found) break;
        }

        return count == PROCESS_COUNT;
    }

    private boolean canProcessRun(int process, int[] work, int[][] need) {
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            if (need[process][i] > work[i]) {
                return false;
            }
        }
        return true;
    }

    private void checkProcessCompletion(int processId) {
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            if (remainingNeed[processId][i] != 0) {
                return;
            }
        }

        // 进程完成，释放资源
        for (int i = 0; i < RESOURCE_TYPES; i++) {
            availableResources[i] += allocated[processId][i];
            allocated[processId][i] = 0;
        }
        completedProcesses[processId] = true;
        updateStatus("进程 P" + processId + " 完成执行并释放所有资源");
    }

    private void detectDeadlocks() {
        List<Integer> cycle = findDeadlockCycle();
        if (cycle.isEmpty()) {
            updateStatus("未检测到死锁");
        } else {
            updateStatus("检测到死锁环路: " + cycle);
            graphPanel.highlightProcesses(cycle);
            graphPanel.repaint();
        }
    }

    private List<Integer> findDeadlockCycle() {
        // 构建等待图
        Map<Integer, List<Integer>> waitGraph = new HashMap<>();
        for (int i = 0; i < PROCESS_COUNT; i++) {
            if (completedProcesses[i]) continue;
            waitGraph.put(i, new ArrayList<>());

            for (int j = 0; j < RESOURCE_TYPES; j++) {
                if (remainingNeed[i][j] > 0 && remainingNeed[i][j] > availableResources[j]) {
                    for (int k = 0; k < PROCESS_COUNT; k++) {
                        if (k != i && allocated[k][j] > 0) {
                            waitGraph.get(i).add(k);
                        }
                    }
                }
            }
        }

        // 深度优先搜索检测环路
        boolean[] visited = new boolean[PROCESS_COUNT];
        boolean[] recursionStack = new boolean[PROCESS_COUNT];
        List<Integer> cycle = new ArrayList<>();

        for (int i = 0; i < PROCESS_COUNT; i++) {
            if (!visited[i] && !completedProcesses[i]) {
                if (dfsCycleDetection(i, waitGraph, visited, recursionStack, cycle)) {
                    return cycle;
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean dfsCycleDetection(int process, Map<Integer, List<Integer>> graph,
                                      boolean[] visited, boolean[] recursionStack,
                                      List<Integer> cycle) {
        visited[process] = true;
        recursionStack[process] = true;

        for (int neighbor : graph.getOrDefault(process, Collections.emptyList())) {
            if (!visited[neighbor]) {
                if (dfsCycleDetection(neighbor, graph, visited, recursionStack, cycle)) {
                    cycle.add(process);
                    return true;
                }
            } else if (recursionStack[neighbor]) {
                cycle.add(process);
                return true;
            }
        }

        recursionStack[process] = false;
        return false;
    }

    private void resetSystem() {
        initSystemState();
        graphPanel.clearHighlights();
        updateStatus("系统已重置");
        graphPanel.repaint();
    }

    private void updateStatus(String message) {
        statusArea.append("[" + new Date().toString() + "] " + message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    private int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

    class GraphPanel extends JPanel {
        private Set<Integer> highlighted = new HashSet<>();

        public GraphPanel() {
            setPreferredSize(new Dimension(1000, 550));
            setBackground(new Color(255, 255, 255));
        }

        public void highlightProcesses(List<Integer> processes) {
            highlighted.clear();
            highlighted.addAll(processes);
        }

        public void clearHighlights() {
            highlighted.clear();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 绘制标题
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 16));
            g2d.setColor(new Color(70, 70, 70));
            g2d.drawString("资源分配图 (RAG)", 20, 30);

            // 绘制资源节点
            for (int i = 0; i < RESOURCE_TYPES; i++) {
                drawResourceNode(g2d, 150 + i * 250, 80, "R" + i, availableResources[i]);
            }

            // 绘制进程节点
            for (int i = 0; i < PROCESS_COUNT; i++) {
                drawProcessNode(g2d, 150 + i * 250, 350, "P" + i, highlighted.contains(i));
            }

            // 绘制分配边和请求边
            for (int i = 0; i < PROCESS_COUNT; i++) {
                for (int j = 0; j < RESOURCE_TYPES; j++) {
                    if (allocated[i][j] > 0) {
                        drawAllocationEdge(g2d, 150 + j * 250, 80 + 60, 150 + i * 250, 350, allocated[i][j]);
                    }
                    if (remainingNeed[i][j] > 0) {
                        drawRequestEdge(g2d, 150 + i * 250, 350, 150 + j * 250, 80 + 60, remainingNeed[i][j]);
                    }
                }
            }

            // 绘制图例
            drawLegend(g2d, 750, 30);
        }

        private void drawResourceNode(Graphics2D g2d, int x, int y, String label, int count) {
            GradientPaint gradient = new GradientPaint(x, y, new Color(173, 216, 230), x+60, y+40, new Color(135, 206, 250));
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x, y, 80, 60, 15, 15);
            g2d.setColor(new Color(70, 130, 180));
            g2d.drawRoundRect(x, y, 80, 60, 15, 15);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 14));
            g2d.drawString(label, x + 30, y + 25);

            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            g2d.drawString("可用:" + count, x + 20, y + 45);
        }

        private void drawProcessNode(Graphics2D g2d, int x, int y, String label, boolean highlight) {
            Color fillColor;
            if (highlight) {
                fillColor = new Color(255, 165, 0); // 橙色高亮
            } else if (completedProcesses[label.charAt(1) - '0']) {
                fillColor = new Color(200, 200, 200); // 灰色表示已完成
            } else {
                fillColor = new Color(144, 238, 144); // 浅绿色表示活跃
            }

            GradientPaint gradient = new GradientPaint(x, y, fillColor.brighter(), x+60, y+40, fillColor.darker());
            g2d.setPaint(gradient);
            g2d.fillOval(x, y, 80, 60);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, 80, 60);

            g2d.setFont(new Font("微软雅黑", Font.BOLD, 14));
            g2d.drawString(label, x + 30, y + 35);
        }

        private void drawAllocationEdge(Graphics2D g2d, int x1, int y1, int x2, int y2, int amount) {
            g2d.setColor(new Color(0, 100, 200));
            drawArrow(g2d, x1 + 40, y1, x2 + 40, y2, "分配:" + amount);
        }

        private void drawRequestEdge(Graphics2D g2d, int x1, int y1, int x2, int y2, int amount) {
            g2d.setColor(new Color(200, 50, 50));
            drawArrow(g2d, x1 + 40, y1 + 60, x2 + 40, y2, "请求:" + amount);
        }

        private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2, String label) {
            // 绘制线条
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x1, y1, x2, y2);
            g2d.setStroke(oldStroke);

            // 绘制箭头
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 12;
            int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
            int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
            int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
            int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

            g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);

            // 绘制标签
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 10));
            g2d.drawString(label, (x1 + x2) / 2, (y1 + y2) / 2);
        }

        private void drawLegend(Graphics2D g2d, int x, int y) {
            g2d.setFont(new Font("微软雅黑", Font.BOLD, 12));
            g2d.setColor(Color.BLACK);
            g2d.drawString("图例说明:", x, y);

            // 进程状态
            g2d.setColor(new Color(144, 238, 144));
            g2d.fillOval(x, y + 20, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("- 活跃进程", x + 20, y + 32);

            g2d.setColor(new Color(200, 200, 200));
            g2d.fillOval(x, y + 40, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("- 已完成进程", x + 20, y + 52);

            g2d.setColor(new Color(255, 165, 0));
            g2d.fillOval(x, y + 60, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString("- 死锁进程", x + 20, y + 72);

            // 边说明
            g2d.setColor(new Color(0, 100, 200));
            g2d.drawLine(x, y + 90, x + 30, y + 90);
            g2d.drawString("- 资源分配", x + 40, y + 95);

            g2d.setColor(new Color(200, 50, 50));
            g2d.drawLine(x, y + 110, x + 30, y + 110);
            g2d.drawString("- 资源请求", x + 40, y + 115);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankersVisualizer());
    }
}
