package org.demonsoft.spatialkappa.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.demonsoft.spatialkappa.model.LocatedObservable;
import org.demonsoft.spatialkappa.model.Observation;
import org.demonsoft.spatialkappa.model.ObservationElement;
import org.demonsoft.spatialkappa.model.ObservationListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.data.Values2D;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;

public class ThreeChannelCompartmentViewPanel extends JPanel implements ObservationListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String ACTION_MAXIMUM_FIXED = "maxFixed";
    private static final String ACTION_MAXIMUM_DYNAMIC = "maxDynamic";
    private static final String ACTION_COLOUR_LINEAR = "colourLinear";
    private static final String ACTION_COLOUR_LOG = "colourLog";
    private static final String ACTION_CHANNELS_SEPARATE = "channelsSeparate";
    private static final String ACTION_CHANNELS_BLEND = "channelsBlend";

    protected static final boolean SAVE_IMAGE_FILES = false;

    private static ChartTheme CHART_THEME = new StandardChartTheme("JFree");

    DefaultCellDataset datasetRed;
    DefaultCellDataset datasetGreen;
    DefaultCellDataset datasetBlue;
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private boolean initialised = false;

    LocatedObservable observableRed;
    LocatedObservable observableGreen;
    LocatedObservable observableBlue;
    int rows;
    int columns;

    private JToggleButton toolbarToggleMaximumFixed;
    private JToggleButton toolbarToggleMaximumDynamic;
    private JToggleButton toolbarToggleColourLinear;
    private JToggleButton toolbarToggleColourLog;
    private JToggleButton toolbarToggleChannelsSeparate;
    private JToggleButton toolbarToggleChannelsBlend;

    public ThreeChannelCompartmentViewPanel() {
        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();

        JLabel label = new JLabel("Maximum value");
        toolbar.add(label);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        ButtonGroup buttonGroup = new ButtonGroup();
        toolbarToggleMaximumFixed = new JToggleButton("Fixed");
        toolbarToggleMaximumFixed.setActionCommand(ACTION_MAXIMUM_FIXED);
        toolbarToggleMaximumFixed.addActionListener(this);
        buttonGroup.add(toolbarToggleMaximumFixed);
        toolbar.add(toolbarToggleMaximumFixed);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarToggleMaximumDynamic = new JToggleButton("Dynamic");
        toolbarToggleMaximumDynamic.setActionCommand(ACTION_MAXIMUM_DYNAMIC);
        toolbarToggleMaximumDynamic.addActionListener(this);
        buttonGroup.add(toolbarToggleMaximumDynamic);
        toolbar.add(toolbarToggleMaximumDynamic);
        toolbarToggleMaximumDynamic.setSelected(true);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        label = new JLabel("Colour");
        toolbar.add(label);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        buttonGroup = new ButtonGroup();
        toolbarToggleColourLinear = new JToggleButton("Linear");
        toolbarToggleColourLinear.setActionCommand(ACTION_COLOUR_LINEAR);
        toolbarToggleColourLinear.addActionListener(this);
        buttonGroup.add(toolbarToggleColourLinear);
        toolbar.add(toolbarToggleColourLinear);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarToggleColourLog = new JToggleButton("Log");
        toolbarToggleColourLog.setActionCommand(ACTION_COLOUR_LOG);
        toolbarToggleColourLog.addActionListener(this);
        buttonGroup.add(toolbarToggleColourLog);
        toolbar.add(toolbarToggleColourLog);
        toolbarToggleColourLinear.setSelected(true);

        toolbar.add(new JToolBar.Separator(new Dimension(20, 0)));

        label = new JLabel("Channels");
        toolbar.add(label);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        buttonGroup = new ButtonGroup();
        toolbarToggleChannelsSeparate = new JToggleButton("Separate");
        toolbarToggleChannelsSeparate.setActionCommand(ACTION_CHANNELS_SEPARATE);
        toolbarToggleChannelsSeparate.addActionListener(this);
        buttonGroup.add(toolbarToggleChannelsSeparate);
        toolbar.add(toolbarToggleChannelsSeparate);

        toolbar.add(new JToolBar.Separator(new Dimension(5, 0)));

        toolbarToggleChannelsBlend = new JToggleButton("Blend");
        toolbarToggleChannelsBlend.setActionCommand(ACTION_CHANNELS_BLEND);
        toolbarToggleChannelsBlend.addActionListener(this);
        buttonGroup.add(toolbarToggleChannelsBlend);
        toolbar.add(toolbarToggleChannelsBlend);
        toolbarToggleChannelsSeparate.setSelected(true);

        add(toolbar, BorderLayout.NORTH);
    }

    public void actionPerformed(ActionEvent e) {
        if (!initialised) {
            return;
        }
        ((HexagonalCellPlot) chart.getPlot()).setLogColour(toolbarToggleColourLog.isSelected());
        ((HexagonalCellPlot) chart.getPlot()).setChannelsSeparate(toolbarToggleChannelsSeparate.isSelected());
        datasetRed.setUseDynamicMaximum(toolbarToggleMaximumDynamic.isSelected());
        datasetGreen.setUseDynamicMaximum(toolbarToggleMaximumDynamic.isSelected());
        datasetBlue.setUseDynamicMaximum(toolbarToggleMaximumDynamic.isSelected());
        chartPanel.chartChanged(null);
    }

    public void setCompartment(LocatedObservable observableRed, LocatedObservable observableGreen, LocatedObservable observableBlue, Observation initialObservation) {
        this.observableRed = observableRed;
        this.observableGreen = observableGreen;
        this.observableBlue = observableBlue;

        if (chartPanel != null) {
            remove(chartPanel);
        }
        initialise(initialObservation);
    }

    public void observation(final Observation observation) {
        if (observableRed == null) {
            return;
        }
        if (!initialised) {
            if (!initialise(observation)) {
                return;
            }
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    populateDataset(observation.observables.get(observableRed), datasetRed);
                    populateDataset(observation.observables.get(observableGreen), datasetGreen);
                    populateDataset(observation.observables.get(observableBlue), datasetBlue);
                }
                
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean initialise(Observation observation) {
        ObservationElement element = observation.observables.get(observableRed);

        if (element == null || !element.isCompartment) {
            return false;
        }

        columns = element.dimensions[0];
        rows = 1;
        if (element.dimensions.length > 1) {
            rows = element.dimensions[1];
        }

        datasetRed = new DefaultCellDataset(rows, columns);
        datasetGreen = new DefaultCellDataset(rows, columns);
        datasetBlue = new DefaultCellDataset(rows, columns);

        HexagonalCellPlot plot = new HexagonalCellPlot(datasetRed, datasetGreen, datasetBlue);
        chart = new JFreeChart("Channels", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        CHART_THEME.apply(chart);

        chartPanel = new ChartPanel(chart);
        add(chartPanel);

        populateDataset(observation.observables.get(observableRed), datasetRed);
        populateDataset(observation.observables.get(observableGreen), datasetGreen);
        populateDataset(observation.observables.get(observableBlue), datasetBlue);

        initialised = true;
        
        return true;
    }

    void populateDataset(ObservationElement element, DefaultCellDataset dataset) {
        if (element != null) {
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                    dataset.setValue(rowIndex, columnIndex, element.getCellValue(columnIndex, rowIndex));
                }
            }
        }
    }

    
    static class HexagonalCellPlot extends Plot {

        private static final long serialVersionUID = 1L;

        private static final double[] X_POINTS = {0.333, 1.0, 1.333, 1.0, 0.333, 0.0, 0.667};
        private static final double[] Y_POINTS = {0.0, 0.0, 0.5, 1.0, 1.0, 0.5, 0.5};
        
        private static final double[] X_POINTS_RED = {X_POINTS[0], X_POINTS[1], X_POINTS[2], X_POINTS[6]};
        private static final double[] Y_POINTS_RED = {Y_POINTS[0], Y_POINTS[1], Y_POINTS[2], Y_POINTS[6]};
        
        private static final double[] X_POINTS_GREEN = {X_POINTS[2], X_POINTS[3], X_POINTS[4], X_POINTS[6]};
        private static final double[] Y_POINTS_GREEN = {Y_POINTS[2], Y_POINTS[3], Y_POINTS[4], Y_POINTS[6]};
        
        private static final double[] X_POINTS_BLUE = {X_POINTS[4], X_POINTS[5], X_POINTS[0], X_POINTS[6]};
        private static final double[] Y_POINTS_BLUE = {Y_POINTS[4], Y_POINTS[5], Y_POINTS[0], Y_POINTS[6]};
        
        private static final double[] X_POINTS_BLEND = {X_POINTS[0], X_POINTS[1], X_POINTS[2], X_POINTS[3], X_POINTS[4], X_POINTS[5]};
        private static final double[] Y_POINTS_BLEND = {Y_POINTS[0], Y_POINTS[1], Y_POINTS[2], Y_POINTS[3], Y_POINTS[4], Y_POINTS[5]};
        
        protected final CellDataset redDataset;
        protected final CellDataset greenDataset;
        protected final CellDataset blueDataset;
        protected static Color baseColour = Color.BLACK;
        protected float[] differenceComponentsRed;
        protected float[] differenceComponentsGreen;
        protected float[] differenceComponentsBlue;
        protected float[] differenceComponentsBlend;
        protected float[] baseComponents;
        boolean useLogColour = false;
        boolean channelsSeparate = true;
        
        public HexagonalCellPlot(CellDataset redDataset, CellDataset greenDataset, CellDataset blueDataset) {
            if (redDataset == null || greenDataset == null || blueDataset == null) {
                throw new NullPointerException();
            }
            this.redDataset = redDataset;
            this.greenDataset = greenDataset;
            this.blueDataset = blueDataset;
            blueDataset.addChangeListener(this);

            baseComponents = baseColour.getRGBColorComponents(new float[3]);
            differenceComponentsRed = Color.RED.getRGBColorComponents(new float[3]);
            differenceComponentsGreen = Color.GREEN.getRGBColorComponents(new float[3]);
            differenceComponentsBlue = Color.BLUE.getRGBColorComponents(new float[3]);
            differenceComponentsBlend = Color.WHITE.getRGBColorComponents(new float[3]);
            for (int index = 0; index < baseComponents.length; index++) {
                differenceComponentsRed[index] -= baseComponents[index];
                differenceComponentsGreen[index] -= baseComponents[index];
                differenceComponentsBlue[index] -= baseComponents[index];
                differenceComponentsBlend[index] -= baseComponents[index];
            }
        }
        public void setLogColour(boolean value) {
            useLogColour = value;
        }
        public void setChannelsSeparate(boolean value) {
            channelsSeparate = value;
        }

        

        @Override
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
            int areaX = (int) area.getX();
            int areaY = (int) area.getY();
            int areaWidth = (int) area.getWidth();
            int areaHeight = (int) area.getHeight();

            int cellSize = (int) Math.ceil(Math.min(areaWidth / (redDataset.getColumnCount() + 0.333),
                    (areaHeight) / (redDataset.getRowCount() + 0.5)));

            int dataWidth = Math.min(cellSize * redDataset.getColumnCount(), areaWidth);
            int dataHeight = Math.min((int) ((double) cellSize * (((double) redDataset.getRowCount()) + 0.5)), areaHeight);

            int xOffset = (areaWidth - dataWidth) / 2;
            int yOffset = (areaHeight - dataHeight) / 2;
            if (xOffset < 0) {
                xOffset = 0;
            }
            if (yOffset < 0) {
                yOffset = 0;
            }

            // Paint boundaries
            g2.setColor(baseColour);
            g2.fillRect(areaX, areaY, areaX + areaWidth, areaY + areaHeight);
            
            float redMaxValue = redDataset.getMaxValue().floatValue();
            float greenMaxValue = greenDataset.getMaxValue().floatValue();
            float blueMaxValue = blueDataset.getMaxValue().floatValue();
            if (useLogColour) {
                redMaxValue = (float) Math.log(redMaxValue);
                greenMaxValue = (float) Math.log(greenMaxValue);
                blueMaxValue = (float) Math.log(blueMaxValue);
            }

            int xDataStart = areaX + xOffset;
            int yDataStart = areaY + yOffset;

            Polygon polygonRed = null;
            Polygon polygonGreen = null;
            Polygon polygonBlue = null;
            Polygon hexagon = null;
            
            if (channelsSeparate) {
                polygonRed = createScaledPolygon(X_POINTS_RED, Y_POINTS_RED, cellSize);
                polygonGreen = createScaledPolygon(X_POINTS_GREEN, Y_POINTS_GREEN, cellSize);
                polygonBlue = createScaledPolygon(X_POINTS_BLUE, Y_POINTS_BLUE, cellSize);
            }
            else {
                hexagon = createScaledPolygon(X_POINTS_BLEND, Y_POINTS_BLEND, cellSize);
            }
           
            AffineTransform savedTransform = g2.getTransform();
            
            g2.translate(xDataStart, yDataStart - cellSize / 2);
            for (int rowIndex = 0; rowIndex < redDataset.getRowCount(); rowIndex++) {
                g2.translate(0, cellSize);
                for (int columnIndex = 0; columnIndex < redDataset.getColumnCount(); columnIndex++) {
                    if (channelsSeparate) {
                        drawChannelPolygon(g2, redDataset.getValue(rowIndex, columnIndex).floatValue(), redMaxValue, polygonRed, differenceComponentsRed);
                        drawChannelPolygon(g2, greenDataset.getValue(rowIndex, columnIndex).floatValue(), greenMaxValue, polygonGreen, differenceComponentsGreen);
                        drawChannelPolygon(g2, blueDataset.getValue(rowIndex, columnIndex).floatValue(), blueMaxValue, polygonBlue, differenceComponentsBlue);
                    }
                    else {
                        drawBlendPolygon(g2, hexagon, differenceComponentsBlend, redDataset.getValue(rowIndex, columnIndex).floatValue(), redMaxValue, greenDataset.getValue(rowIndex, columnIndex)
                                .floatValue(), greenMaxValue, blueDataset.getValue(rowIndex, columnIndex).floatValue(), blueMaxValue);
                    }
                    g2.translate(cellSize, cellSize / 2 * ((columnIndex % 2 == 1) ? 1 : -1));
                }
                g2.translate(-cellSize * redDataset.getColumnCount(), ((redDataset.getColumnCount() % 2 == 0) ? 0 : cellSize / 2));
            }
            
            g2.setTransform(savedTransform);
        }
        protected Polygon createScaledPolygon(double[] xPoints, double[] yPoints, double cellSize) {
            int[] xPointsScaled  = new int[xPoints.length];
            int[] yPointsScaled  = new int[yPoints.length];
            for (int index = 0; index < xPoints.length; index++) {
                xPointsScaled[index] = (int) (xPoints[index] * cellSize);
                yPointsScaled[index] = (int) (yPoints[index] * cellSize);
            }
            return new Polygon(xPointsScaled, yPointsScaled, xPoints.length);
        }

        private void drawChannelPolygon(Graphics2D g2, float value, float maxValue, Polygon polygon, float[] differenceComponents) {
            if (useLogColour) {
                value = (value == 0) ? 0 : ((float) Math.log(value));
            }
            float ratio = (maxValue == 0) ? 0 : value / maxValue;

            Color cellColor = new Color(baseComponents[0] + (differenceComponents[0] * ratio), baseComponents[1] + (differenceComponents[1] * ratio), baseComponents[2]
                    + (differenceComponents[2] * ratio));

            g2.setColor(cellColor);

            g2.fillPolygon(polygon);
        }
        
        private void drawBlendPolygon(Graphics2D g2, Polygon polygon, float[] differenceComponents, 
                float redValue, float redMaxValue, float greenValue, float greenMaxValue, float blueValue, float blueMaxValue) {
            if (useLogColour) {
                redValue = (redValue == 0) ? 0 : ((float) Math.log(redValue));
                greenValue = (greenValue == 0) ? 0 : ((float) Math.log(greenValue));
                blueValue = (blueValue == 0) ? 0 : ((float) Math.log(blueValue));
            }
            float redRatio = (redMaxValue == 0) ? 0 : redValue / redMaxValue;
            float greenRatio = (greenMaxValue == 0) ? 0 : greenValue / greenMaxValue;
            float blueRatio = (blueMaxValue == 0) ? 0 : blueValue / blueMaxValue;

            Color cellColor = new Color(baseComponents[0] + (differenceComponents[0] * redRatio), baseComponents[1] + (differenceComponents[1] * greenRatio), baseComponents[2]
                    + (differenceComponents[2] * blueRatio));

            g2.setColor(cellColor);

            g2.fillPolygon(polygon);
        }
        
        
        @Override
        public String getPlotType() {
            return "HexagonalCellPlot";
        }

    }

    static interface CellDataset extends Values2D, Dataset {
        public Number getMaxValue();

    }

    static class DefaultCellDataset implements CellDataset {

        private final int rows;
        private final int columns;
        private final int[][] data;
        private int dynamicMaxValue = 0;
        private int maxValue = 0;
        private final List<DatasetChangeListener> listeners = new ArrayList<DatasetChangeListener>();
        private DatasetGroup group;
        private boolean useDynamicMaximum = true;

        public DefaultCellDataset(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
            this.data = new int[columns][rows];
        }

        public void setUseDynamicMaximum(boolean value) {
            useDynamicMaximum = value;
        }

        public void setValue(int row, int column, int value) {
            data[column][row] = value;
            dynamicMaxValue = 0;
            notifyListeners();
        }

        private void notifyListeners() {
            DatasetChangeEvent event = new DatasetChangeEvent(this, this);
            for (DatasetChangeListener listener : listeners) {
                listener.datasetChanged(event);
            }
        }

        public int getColumnCount() {
            return columns;
        }

        public int getRowCount() {
            return rows;
        }

        public Number getValue(int row, int column) {
            return data[column][row];
        }

        public Number getMaxValue() {
            if (dynamicMaxValue > 0) {
                return useDynamicMaximum ? dynamicMaxValue : maxValue;
            }

            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    if (data[column][row] > maxValue) {
                        maxValue = data[column][row];
                    }
                    if (data[column][row] > dynamicMaxValue) {
                        dynamicMaxValue = data[column][row];
                    }
                }
            }
            return useDynamicMaximum ? dynamicMaxValue : maxValue;
        }

        public void addChangeListener(DatasetChangeListener listener) {
            listeners.add(listener);
        }

        public void removeChangeListener(DatasetChangeListener listener) {
            listeners.remove(listener);
        }

        public DatasetGroup getGroup() {
            return group;
        }

        public void setGroup(DatasetGroup group) {
            this.group = group;
        }

    }
}
