package org.demonsoft.spatialkappa.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
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

public class PlotViewer implements ActionListener, ObservationListener {


    private static final String WINDOW_TITLE = "Kappa Plot Viewer v" + Version.VERSION;

    private static enum ToolbarMode {
        START, DATAFILE_SELECTED, PROCESSING
    }
    
    private static final String STATUS_LOADING = "Loading Kappa output data file...";
    private static final String STATUS_STARTING_SIMULATION = "Starting file reading, please wait...";
    
    private static final String TOOLBAR_BUTTON_IMAGE_PATH = "/toolbarButtonGraphics/";

    private static final String ACTION_OPEN = "open";
    private static final String ACTION_RELOAD = "reload";

    Simulation simulation;
    private File replayFile;
    JFrame frame;
    private ChartPanel basicChartPanel;
    private JTextArea consoleTextArea;
    private PrintStream consoleStream;
    private JToolBar toolbar;

    private JTabbedPane tabbedPane;

    private JButton toolbarButtonOpen;
    private JButton toolbarButtonReload;

    JLabel textStatus;

    XYSeriesCollection chartData;
    
    Dimension minimumSize;

    public PlotViewer() throws Exception {

        frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        basicChartPanel = new ChartPanel(ChartFactory.createXYLineChart("", "Time", "Quantity", null, PlotOrientation.VERTICAL, true, false, false));
        tabbedPane.add(basicChartPanel, "Observation chart");

        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        JScrollPane textPane = new JScrollPane(consoleTextArea);
        tabbedPane.add(textPane, "Console Output");
        
        consoleStream = new PrintStream(new ConsoleOutputStream(consoleTextArea));
        System.setErr(consoleStream);
        System.setOut(consoleStream);
                
        frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        createToolbar();
        
        JPanel statusPanel = new JPanel();
        textStatus = new JLabel("Open a Kappa output data file.");
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

    private void createToolbar() {
        toolbar = new JToolBar();

        toolbarButtonOpen = makeToolbarButton("general/Open24", ACTION_OPEN, "Open Kappa file", "Open");
        toolbar.add(toolbarButtonOpen);
        toolbarButtonReload = makeToolbarButton("general/Refresh24", ACTION_RELOAD, "Reload Kappa file", "Reload");
        toolbar.add(toolbarButtonReload);

        frame.getContentPane().add(toolbar, BorderLayout.NORTH);

        setToolbarMode(ToolbarMode.START);
    }

    private JButton makeToolbarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        String imgLocation = TOOLBAR_BUTTON_IMAGE_PATH + imageName + ".gif";
        URL imageURL = PlotViewer.class.getResource(imgLocation);

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

    private void openDataFile(File inputFile) {
        textStatus.setText(STATUS_LOADING);
        consoleTextArea.setText("");

        if (simulation != null) {
            simulation.removeObservationListener(this);
            simulation = null;
        }
        replayFile = inputFile;
        
        try {
            simulation = new ReplaySimulation(new FileReader(replayFile), 0);
            simulation.addObservationListener(PlotViewer.this);
    
            String simulationName = replayFile.getName();
            Observation observation = simulation.getCurrentObservation();
    
            createBasicChart(simulationName, observation);
    
            setToolbarMode(ToolbarMode.PROCESSING);
            textStatus.setText(STATUS_STARTING_SIMULATION);
            
            new Thread() {
                @Override
                public void run() {
                    try {
                        simulation.runByEvent(0, 0);
                    }
                    catch (Exception e) {
                        handleException(e);
                    }
                }
            }.start();
        }
        catch (FileNotFoundException e) {
            handleException(e);
        }

    }
    
    private void createBasicChart(String simulationName, Observation observation) {
        chartData = new XYSeriesCollection();
        for (String observable : observation.orderedObservables) {
            ObservationElement element = observation.observables.get(observable);
            float value = element.value;
            if (Float.isInfinite(value) || Float.isNaN(value)) {
                value = 0;
            }
            XYSeries series = new XYSeries(observable);
            series.add(observation.time, value);
            chartData.addSeries(series);
        }
        
        JFreeChart chart = ChartFactory.createXYLineChart(simulationName, "Time", "Quantity", chartData, PlotOrientation.VERTICAL, true, false, false);
        XYPlot xyPlot = chart.getXYPlot();
        xyPlot.getRenderer().setSeriesPaint(0, Color.RED);
        xyPlot.getRenderer().setSeriesPaint(1, Color.GREEN);
        xyPlot.getRenderer().setSeriesPaint(2, Color.BLUE);
        
        basicChartPanel.setChart(chart);
    }

    void setToolbarMode(ToolbarMode mode) {
        toolbarButtonReload.setEnabled(mode == ToolbarMode.DATAFILE_SELECTED);
        toolbarButtonOpen.setEnabled(mode != ToolbarMode.PROCESSING);
    }
    
    public void actionPerformed(ActionEvent event) {
        if (ACTION_OPEN.equals(event.getActionCommand())) {
            actionOpenFile();
        }
        else if (ACTION_RELOAD.equals(event.getActionCommand())) {
            actionReloadFile();
        }
    }

    private void actionOpenFile() {
        JFileChooser fileChooser;
        if (replayFile != null) {
            fileChooser = new JFileChooser(replayFile.getParentFile());
        }
        else {
            fileChooser = new JFileChooser(new File(System.getProperty("user.dir")));
        }
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            openDataFile(file);
        }
    }


    private void actionReloadFile() {
        if (replayFile != null) {
            openDataFile(replayFile);
        }
    }

    void handleException(Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
    }

    public void observation(final Observation observation) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    for (Map.Entry<String, ObservationElement> entry : observation.observables.entrySet()) {
                        String key = entry.getKey();
                        float value = entry.getValue().value;
                        if (Float.isInfinite(value) || Float.isNaN(value)) {
                            value = 0;
                        }
                        XYSeries series = chartData.getSeries(key);
                        series.add(observation.time, value, false);
                    }
                }
            });
        }
        catch (Exception e) {
            handleException(e);
        }

        if (observation.finalObservation) { 
            for (Object series : chartData.getSeries()) {
                ((XYSeries) series).fireSeriesChanged();
            }
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setToolbarMode(ToolbarMode.DATAFILE_SELECTED);
                        textStatus.setText("Processing complete.");
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

    

    public static void main(String[] args) throws Exception {
        PlotViewer simulator = new PlotViewer();
        if (args.length == 1) {
            File replayFile = new File(args[0]);
            if (replayFile.exists()) {
                simulator.openDataFile(replayFile);
            }
        }
    }
}
