package org.demonsoft.spatialkappa.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.demonsoft.spatialkappa.tools.RecordSimulation;
import org.demonsoft.spatialkappa.tools.ReplaySimulation;
import org.demonsoft.spatialkappa.tools.Simulation;
import org.demonsoft.spatialkappa.tools.Version;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public abstract class AbstractSimulatorGui implements ActionListener, ObservationListener {

    protected static final String WINDOW_TITLE = "Spatial Kappa Simulator v" + Version.VERSION;

    private static enum ToolbarMode {
        START, KAPPA_READY, REPLAY_READY, KAPPA_AND_REPLAY_READY, RUNNING_KAPPA, RUNNING_REPLAY, PROCESSING
    }
    
    protected static final String STATUS_ERROR_LOADING = "Error loading file";
    protected static final String STATUS_LOADING = "Loading Kappa file...";
    protected static final String STATUS_STARTING_SIMULATION = "Starting simulation, please wait...";
    
    protected static final String TOOLBAR_BUTTON_IMAGE_PATH = "/toolbarButtonGraphics/";
    private static final double MAX_STEP_SIZE = 1000000.0;
    private static final double MAX_STEPS = 1000000.0;

    private static final String ACTION_OPEN = "open";
    private static final String ACTION_RUN = "run";
    private static final String ACTION_REPLAY = "replay";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_EVENTS = "events";
    private static final String ACTION_TIME = "time";
    private static final String ACTION_RELOAD = "reload";
    private static final String KAPPA_FILE_SUFFIX = ".ka";
    private static final String REPLAY_FILE_SUFFIX = ".kareplay";

    protected Simulation simulation;
    protected File kappaFile;
    protected File replayFile;
    JFrame frame;
    private ChartPanel chartPanel;
    protected JPanel multiplePanel;
    private JTextArea consoleTextArea;
    private PrintStream consoleStream;
    private JToolBar toolbar;

    private JTabbedPane tabbedPane;

    JButton toolbarButtonOpen;
    JButton toolbarButtonRun;
    JButton toolbarButtonReplay;
    JButton toolbarButtonReload;
    JButton toolbarButtonStop;
    JSpinner toolbarSpinnerSteps;
    JSpinner toolbarSpinnerStepSize;
    JSpinner toolbarSpinnerReplayInterval;
    private SpinnerNumberModel toolbarSpinnerModelSteps;
    private SpinnerNumberModel toolbarSpinnerModelStepSize;
    private SpinnerNumberModel toolbarSpinnerModelReplayInterval;
    JToggleButton toolbarToggleUnitsTime;
    JToggleButton toolbarToggleUnitsEvents;

    protected JTextArea textAreaData;
    JLabel textStatus;
    JLabel labelSteps;
    JLabel labelStepSize;

    XYSeriesCollection chartData;
    
    Dimension minimumSize;
    boolean stopSimulation;
    boolean firstObservation;
    boolean replayRunning;

    public AbstractSimulatorGui() throws Exception {

        JFreeChart chart = ChartFactory.createXYLineChart("", "Time", "Quantity", null, PlotOrientation.VERTICAL, true, false, false);
        frame = new JFrame(getWindowTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chartPanel = new ChartPanel(chart);
        frame.getContentPane().setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.add(chartPanel, "Chart");

        chart = ChartFactory.createBarChart("Current quantities", "Observable", "Quantity", null, PlotOrientation.VERTICAL, true, false, false);

        multiplePanel = new JPanel();
        multiplePanel.setLayout(new BoxLayout(multiplePanel, BoxLayout.X_AXIS));
        tabbedPane.add(multiplePanel, "Compartment View");

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        JScrollPane textPane = new JScrollPane(consoleTextArea);
        tabbedPane.add(textPane, "Console Output");
        
        consoleStream = new PrintStream(new ConsoleOutputStream(consoleTextArea));
        System.setErr(consoleStream);
        System.setOut(consoleStream);
                
        textAreaData = new JTextArea();
        textAreaData.setEditable(false);
        textPane = new JScrollPane(textAreaData);
        tabbedPane.add(textPane, "Data");

        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        toolbar = new JToolBar();

        toolbarButtonOpen = makeToolbarButton("general/Open24", ACTION_OPEN, "Open Kappa file", "Open");
        toolbar.add(toolbarButtonOpen);
        toolbarButtonReload = makeToolbarButton("general/Refresh24", ACTION_RELOAD, "Reload Kappa file", "Reload");
        toolbarButtonReload.setEnabled(false);
        toolbar.add(toolbarButtonReload);
        toolbarButtonRun = makeToolbarButton("media/Play24", ACTION_RUN, "Run simulation", "Run");
        toolbarButtonRun.setEnabled(false);
        toolbar.add(toolbarButtonRun);
        toolbarButtonReplay = makeToolbarButton("media/FastForward24", ACTION_REPLAY, "Replay simulation", "Replay");
        toolbarButtonReplay.setEnabled(false);
        toolbar.add(toolbarButtonReplay);
        toolbarButtonStop = makeToolbarButton("media/Stop24", ACTION_STOP, "Stop simulation", "Stop");
        toolbarButtonStop.setEnabled(false);
        toolbar.add(toolbarButtonStop);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        ButtonGroup buttonGroup = new ButtonGroup();
        toolbarToggleUnitsEvents = new JToggleButton("Events");
        toolbarToggleUnitsEvents.setActionCommand(ACTION_EVENTS);
        toolbarToggleUnitsEvents.addActionListener(this);
        buttonGroup.add(toolbarToggleUnitsEvents);
        toolbar.add(toolbarToggleUnitsEvents);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarToggleUnitsTime = new JToggleButton("Time");
        toolbarToggleUnitsTime.setActionCommand(ACTION_TIME);
        toolbarToggleUnitsTime.addActionListener(this);
        buttonGroup.add(toolbarToggleUnitsTime);
        toolbar.add(toolbarToggleUnitsTime);
        toolbarToggleUnitsEvents.setSelected(true);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        labelSteps = new JLabel("Steps");
        toolbar.add(labelSteps);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarSpinnerModelSteps = new SpinnerNumberModel(1000.0, 1.0, MAX_STEPS, 1.0);
        toolbarSpinnerSteps = new JSpinner(toolbarSpinnerModelSteps);
        toolbar.add(toolbarSpinnerSteps);
        labelSteps.setLabelFor(toolbarSpinnerSteps);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        labelStepSize = new JLabel("Step size");
        toolbar.add(labelStepSize);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarSpinnerModelStepSize = new SpinnerNumberModel(100.0, 1.0, MAX_STEP_SIZE, 1.0);
        toolbarSpinnerStepSize = new JSpinner(toolbarSpinnerModelStepSize);
        toolbar.add(toolbarSpinnerStepSize);
        labelSteps.setLabelFor(toolbarSpinnerStepSize);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        labelSteps = new JLabel("Replay Interval");
        toolbar.add(labelSteps);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarSpinnerModelReplayInterval = new SpinnerNumberModel(0, 0, 1000, 1);
        toolbarSpinnerReplayInterval = new JSpinner(toolbarSpinnerModelReplayInterval);
        toolbar.add(toolbarSpinnerReplayInterval);
        labelSteps.setLabelFor(toolbarSpinnerReplayInterval);

        frame.getContentPane().add(toolbar, BorderLayout.NORTH);

        setToolbarMode(ToolbarMode.START);
        
        JPanel statusPanel = new JPanel();
        textStatus = new JLabel("Open a Kappa file.");
        statusPanel.add(textStatus);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        frame.pack();
        minimumSize = new Dimension(frame.getWidth() + 70, frame.getHeight());
        frame.setSize(minimumSize);
        frame.setMinimumSize(minimumSize);

        frame.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                if (frame.getWidth() < minimumSize.width) {
                    frame.setSize(minimumSize.width, frame.getHeight());
                }
                if (frame.getHeight() < minimumSize.height) {
                    frame.setSize(frame.getWidth(), minimumSize.height);
                }
            }

        });
        frame.setVisible(true);
    }

    protected abstract String getWindowTitle();
    
    protected JButton makeToolbarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        String imgLocation = TOOLBAR_BUTTON_IMAGE_PATH + imageName + ".gif";
        URL imageURL = AbstractSimulatorGui.class.getResource(imgLocation);

        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);

        if (imageURL != null) {
            button.setIcon(new ImageIcon(imageURL, altText));
        }
        else {
            button.setText(altText);
            System.err.println("Resource not found: " + imgLocation);
        }

        return button;
    }

    protected void openKappaFile(File inputFile) {
        textStatus.setText(STATUS_LOADING);
        setToolbarMode(ToolbarMode.PROCESSING);
        consoleTextArea.setText("");

        try {
            simulation = createRecordSimulation(inputFile);
            simulation.addObservationListener(this);

            textAreaData.setText(simulation.toString() + "\n");

            textStatus.setText("File loaded");
            setToolbarMode(ToolbarMode.KAPPA_READY);
        }
        catch (Exception e) {
            textStatus.setText(STATUS_ERROR_LOADING);
            handleException(e);
            setToolbarMode(ToolbarMode.START);
        }
    }

    private Simulation createRecordSimulation(File inputFile) throws Exception {
        replayFile = createRecordFile(inputFile);
        kappaFile = inputFile;
        return new RecordSimulation(createSimulation(inputFile), replayFile);
    }

    private File createRecordFile(File inputFile) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HH-mm-ss");
        String filePrefix = inputFile.getName();
        filePrefix = filePrefix.substring(0, filePrefix.length() - KAPPA_FILE_SUFFIX.length());
        for (int attempt = 0; attempt < 5; attempt++) {
            String dateString = dateFormat.format(Calendar.getInstance().getTime());
            File file = new File(inputFile.getParentFile(), filePrefix + "-" + dateString + REPLAY_FILE_SUFFIX);
            if (!file.exists()) {
                return file;
            }
            // File exists - wait & try again
            Thread.sleep(2000);
        }
        throw new Exception("Cannot create record file");
    }

    protected void openReplayFile(File inputFile) {
        textStatus.setText(STATUS_LOADING);
        setToolbarMode(ToolbarMode.PROCESSING);
        consoleTextArea.setText("");

        try {
            simulation = createReplaySimulation(inputFile);
            simulation.addObservationListener(this);

            textAreaData.setText(simulation.toString() + "\n");
            replayFile = inputFile;

            textStatus.setText("Replay file ready");
            setToolbarMode(ToolbarMode.REPLAY_READY);
        }
        catch (Exception e) {
            textStatus.setText(STATUS_ERROR_LOADING);
            handleException(e);
        }
    }
    
    

    private Simulation createReplaySimulation(File inputFile) {
        return new ReplaySimulation(inputFile, (Integer) toolbarSpinnerModelReplayInterval.getValue());
    }

    protected abstract Simulation createSimulation(File inputFile) throws Exception;
    
    void runSimulation() {
        runSimulation(kappaFile == null);
    }
    
    
    void runSimulation(final boolean replay) {
        if (!replay && kappaFile == null || replay && replayFile == null) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setToolbarMode(ToolbarMode.PROCESSING);
                    textStatus.setText(STATUS_STARTING_SIMULATION);
                }
            });
            stopSimulation = false;
            firstObservation = true;
            chartData = new XYSeriesCollection();
            String simulationName = replay ? replayFile.getName() : kappaFile.getName();

            simulation.reset();
            
            Observation observation = simulation.getCurrentObservation();
            for (Component component : multiplePanel.getComponents()) {
                simulation.removeObservationListener((ObservationListener) component);
            }
            multiplePanel.removeAll();
            for (String observable : observation.orderedObservables) {
                ObservationElement element = observation.observables.get(observable);
                int value = (int) element.value;
                XYSeries series = new XYSeries(observable);
                series.add(observation.time, value);
                chartData.addSeries(series);
            }

            String redObservable = null;
            String greenObservable = null;
            String blueObservable = null;
            for (String observable : observation.orderedObservables) {
                if (observable.equals("Red cytosol")) {
                    redObservable = observable;
                }
                else if (observable.equals("Green cytosol")) {
                    greenObservable = observable;
                }
                else if (observable.equals("Blue cytosol")) {
                    blueObservable = observable;
                }
            }
            
            if (redObservable != null && greenObservable != null && blueObservable != null) {
                ThreeChannelCompartmentViewPanel compartmentPanel = new ThreeChannelCompartmentViewPanel();
                simulation.addObservationListener(compartmentPanel);
                multiplePanel.add(compartmentPanel);
                compartmentPanel.setCompartment(redObservable, greenObservable, blueObservable, observation);
            }
            else {
                for (String observable : observation.orderedObservables) {
                    ObservationElement element = observation.observables.get(observable);
                    if (element.isCompartment) {
                        CompartmentViewPanel compartmentPanel = new CompartmentViewPanel();
                        simulation.addObservationListener(compartmentPanel);
                        multiplePanel.add(compartmentPanel);
                        compartmentPanel.setCompartment(observable, observation);
                        break;
                    }
                }
            }
            
            JFreeChart chart = ChartFactory.createXYLineChart(simulationName, "Time", "Quantity", chartData, PlotOrientation.VERTICAL, true, false, false);
            XYPlot xyPlot = chart.getXYPlot();
            xyPlot.getRenderer().setSeriesPaint(0, Color.RED);
            xyPlot.getRenderer().setSeriesPaint(1, Color.GREEN);
            xyPlot.getRenderer().setSeriesPaint(2, Color.BLUE);
            
            
            chartPanel.setChart(chart);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setToolbarMode(replay ? ToolbarMode.RUNNING_REPLAY : ToolbarMode.RUNNING_KAPPA);
                }
            });

            boolean eventModelling = toolbarToggleUnitsEvents.isSelected();

                simulation.reset();
    
                for (Object series : chartData.getSeries()) {
                    ((XYSeries) series).clear();
                }
                
                replayRunning = replay;
                
                if (eventModelling) {
                    int steps = ((Double) toolbarSpinnerModelSteps.getValue()).intValue();
                    int stepSize = ((Double) toolbarSpinnerModelStepSize.getValue()).intValue();
                    simulation.runByEvent(steps, stepSize);
                }
                else {
                    float steps = ((Double) toolbarSpinnerModelSteps.getValue()).floatValue();
                    float stepSize = ((Double) toolbarSpinnerModelStepSize.getValue()).floatValue();
                    simulation.runByTime(steps, stepSize);
                }
        }
        catch (Exception e) {
            handleException(e);
        }
    }

    protected void setToolbarMode(ToolbarMode mode) {
        boolean simulationActive = mode == ToolbarMode.RUNNING_KAPPA || mode == ToolbarMode.RUNNING_REPLAY || mode == ToolbarMode.PROCESSING;
        boolean simulationParametersEditable = mode != ToolbarMode.RUNNING_KAPPA && mode != ToolbarMode.RUNNING_REPLAY && mode != ToolbarMode.REPLAY_READY && mode != ToolbarMode.PROCESSING;
        
        toolbarButtonRun.setEnabled(mode == ToolbarMode.KAPPA_READY || mode == ToolbarMode.KAPPA_AND_REPLAY_READY);
        toolbarButtonReplay.setEnabled(mode == ToolbarMode.REPLAY_READY || mode == ToolbarMode.KAPPA_AND_REPLAY_READY);
        toolbarButtonStop.setEnabled(mode == ToolbarMode.RUNNING_KAPPA || mode == ToolbarMode.RUNNING_REPLAY);
        toolbarButtonReload.setEnabled(mode == ToolbarMode.KAPPA_READY || mode == ToolbarMode.KAPPA_AND_REPLAY_READY);
        toolbarButtonOpen.setEnabled(!simulationActive);
        toolbarSpinnerSteps.setEnabled(simulationParametersEditable);
        toolbarSpinnerStepSize.setEnabled(simulationParametersEditable);
        toolbarSpinnerReplayInterval.setEnabled(mode == ToolbarMode.REPLAY_READY || mode == ToolbarMode.KAPPA_AND_REPLAY_READY);
        toolbarToggleUnitsTime.setEnabled(simulationParametersEditable);
        toolbarToggleUnitsEvents.setEnabled(simulationParametersEditable);
        labelSteps.setEnabled(simulationParametersEditable);
        labelStepSize.setEnabled(simulationParametersEditable);
    }
    
    public void actionPerformed(ActionEvent event) {
        if (ACTION_OPEN.equals(event.getActionCommand())) {
            actionOpenFile();
        }
        else if (ACTION_RELOAD.equals(event.getActionCommand())) {
            actionReloadFile();
        }
        else if (ACTION_RUN.equals(event.getActionCommand())) {
            actionRunSimulation();
        }
        else if (ACTION_REPLAY.equals(event.getActionCommand())) {
            actionReplaySimulation();
        }
        else if (ACTION_STOP.equals(event.getActionCommand())) {
            actionStopSimulation();
        }
        else if (ACTION_EVENTS.equals(event.getActionCommand())) {
            actionEventModelling();
        }
        else if (ACTION_TIME.equals(event.getActionCommand())) {
            actionTimeModelling();
        }
    }

    protected void actionStopSimulation() {
        stopSimulation = true;
        simulation.stop();
    }

    private void actionEventModelling() {
        labelSteps.setText("Steps");
        labelStepSize.setText("Step size");
        toolbarSpinnerModelStepSize.setMinimum(1.0);
        if ((Double) toolbarSpinnerModelStepSize.getValue() < (Double) toolbarSpinnerModelStepSize.getMinimum()) {
            toolbarSpinnerModelStepSize.setValue(toolbarSpinnerModelStepSize.getMinimum());
        }
        toolbar.repaint();
    }

    private void actionTimeModelling() {
        labelSteps.setText("Total time");
        labelStepSize.setText("Step time");
        toolbarSpinnerModelStepSize.setMinimum(0.0001);
        toolbar.repaint();
    }

    private void actionOpenFile() {
        JFileChooser fileChooser;
        if (kappaFile != null) {
            fileChooser = new JFileChooser(kappaFile.getParentFile());
        }
        else if (replayFile != null) {
            fileChooser = new JFileChooser(replayFile.getParentFile());
        }
        else {
            fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        }
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            openFile(file);
        }
    }

    protected void openFile(File file) {
        if (file.getName().endsWith(KAPPA_FILE_SUFFIX)) {
            openKappaFile(file);
        }
        else if (file.getName().endsWith(REPLAY_FILE_SUFFIX)) {
            openReplayFile(file);
            kappaFile = null;
        }
        else {
            textStatus.setText("Unknown file type: " + file.getName());
        }
    }

    protected void actionReloadFile() {
        if (kappaFile != null) {
            openKappaFile(kappaFile);
        }
    }

    void handleException(Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
    }

    private void actionRunSimulation() {
        openKappaFile(kappaFile);
        new Thread() {
            @Override
            public void run() {
                runSimulation(false);
            }
        }.start();
    }

    private void actionReplaySimulation() {
        openReplayFile(replayFile);
        new Thread() {
            @Override
            public void run() {
                runSimulation(true);
            }
        }.start();
    }

    public void observation(final Observation observation) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    for (Map.Entry<String, ObservationElement> entry : observation.observables.entrySet()) {
                        String key = entry.getKey();
                        int value = (int) entry.getValue().value;
                        XYSeries series = chartData.getSeries(key);
                        series.add(observation.time, value, !replayRunning);
                    }
                    StringBuilder status = new StringBuilder();
                    status.append("Time elapsed (s): ").append(observation.elapsedTime / 1000);
                    status.append(", Estimated time remaining (s): ").append(observation.estimatedRemainingTime / 1000);
                    textStatus.setText(status.toString());
                }
            });
        }
        catch (Exception e) {
            handleException(e);
        }

        if (observation.finalObservation) { 
            if (replayRunning) {
                for (Object series : chartData.getSeries()) {
                    ((XYSeries) series).fireSeriesChanged();
                }
            }
            textAreaData.append(simulation.getDebugOutput());
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setToolbarMode(kappaFile != null ? ToolbarMode.KAPPA_AND_REPLAY_READY : ToolbarMode.REPLAY_READY);
                        textStatus.setText("Simulation complete. Time elapsed (s): " + (observation.elapsedTime / 1000));
                    }
                });
            }
            catch (Exception e) {
                handleException(e);
            }
        }
    }
    
    static class ConsoleOutputStream extends FilterOutputStream {

        private JTextArea textArea;
        
        public ConsoleOutputStream(JTextArea textArea) {
            super(new ByteArrayOutputStream());
            this.textArea = textArea;
        }

        @Override
        public void write(byte[] b) throws IOException {
            textArea.append(new String(b));
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            textArea.append(new String(b, off, len));
        }
        
        @Override
        public void write(int b) throws IOException {
            textArea.append(Character.toString((char) b));
        }
    }

}
