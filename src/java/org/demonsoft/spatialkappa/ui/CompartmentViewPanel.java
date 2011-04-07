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

public class CompartmentViewPanel extends JPanel implements ObservationListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String ACTION_MAXIMUM_FIXED = "maxFixed";
    private static final String ACTION_MAXIMUM_DYNAMIC = "maxDynamic";
    private static final String ACTION_COLOUR_LINEAR = "colourLinear";
    private static final String ACTION_COLOUR_LOG = "colourLog";

    private static ChartTheme CHART_THEME = new StandardChartTheme("JFree");

    DefaultCellDataset dataset;
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private boolean initialised = false;

    String observable;
    int rows;
    int columns;

    private JToggleButton toolbarToggleMaximumFixed;
    private JToggleButton toolbarToggleMaximumDynamic;
    private JToggleButton toolbarToggleColourLinear;
    private JToggleButton toolbarToggleColourLog;

    public CompartmentViewPanel() {
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

        add(toolbar, BorderLayout.NORTH);
    }

    public void actionPerformed(ActionEvent e) {
        if (!initialised) {
            return;
        }
        ((CellPlot) chart.getPlot()).setLogColour(toolbarToggleColourLog.isSelected());
        dataset.setUseDynamicMaximum(toolbarToggleMaximumDynamic.isSelected());
        repaint();
    }

    public void setCompartment(String observable, Observation initialObservation) {
        this.observable = observable;

        if (chartPanel != null) {
            remove(chartPanel);
        }
        initialise(initialObservation);
    }

    public void observation(final Observation observation) {
        if (observable == null) {
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

                    ObservationElement element = observation.observables.get(observable);
                    if (element != null) {
                        for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                            for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
                                dataset.setValue(rowIndex, columnIndex, element.getCellValue(columnIndex, rowIndex));
                            }
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean initialise(Observation observation) {
        ObservationElement element = observation.observables.get(observable);

        if (element == null || !element.isCompartment) {
            return false;
        }

        columns = element.dimensions[0];
        rows = 1;
        if (element.dimensions.length > 1) {
            rows = element.dimensions[1];
        }

        dataset = new DefaultCellDataset(rows, columns);

        CellPlot plot = new HexagonalCellPlot(dataset);
        chart = new JFreeChart(observable, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        CHART_THEME.apply(chart);

        chartPanel = new ChartPanel(chart);
        add(chartPanel);

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                float value = element.getCellValue(columnIndex, rowIndex);
                dataset.setValue(rowIndex, columnIndex, value);
            }
        }

        initialised = true;
        return true;
    }

    static class CellPlot extends Plot {

        private static final long serialVersionUID = 1L;

        protected final CellDataset dataset;
        protected Color baseColour = Color.WHITE;
        protected Color activeColour = Color.BLUE;
        protected float[] differenceComponents;
        protected float[] baseComponents;
        boolean useLogColour = false;

        public CellPlot(CellDataset dataset) {
            if (dataset == null) {
                throw new NullPointerException();
            }
            this.dataset = dataset;
            dataset.addChangeListener(this);

            baseComponents = baseColour.getRGBColorComponents(new float[3]);
            differenceComponents = activeColour.getRGBColorComponents(new float[3]);
            for (int index = 0; index < baseComponents.length; index++) {
                differenceComponents[index] -= baseComponents[index];
            }
        }

        public void setLogColour(boolean value) {
            useLogColour = value;
        }

        @Override
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
            int areaX = (int) area.getX();
            int areaY = (int) area.getY();
            int areaWidth = (int) area.getWidth();
            int areaHeight = (int) area.getHeight();

            int cellSize = (int) Math.ceil(Math.min(areaWidth / dataset.getColumnCount(), areaHeight / dataset.getRowCount()));

            int dataWidth = Math.min(cellSize * dataset.getColumnCount(), areaWidth);
            int dataHeight = Math.min(cellSize * dataset.getRowCount(), areaHeight);

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
            if (xOffset > 0) {
                // Left/right borders
                g2.fillRect(areaX, areaY, areaX + xOffset, areaY + areaHeight);
                g2.fillRect(areaX + areaWidth - xOffset, areaY, areaX + xOffset, areaY + areaHeight);
            }
            else if (yOffset > 0) {
                // Top/bottom borders
                g2.fillRect(areaX, areaY, areaX + areaWidth, areaY + yOffset);
                g2.fillRect(areaX, areaY + areaHeight - yOffset, areaX + areaWidth, areaY + yOffset);
            }

            float maxValue = dataset.getMaxValue().floatValue();
            if (useLogColour) {
                maxValue = (float) Math.log(maxValue);
            }

            int xDataStart = areaX + xOffset;
            int yDataStart = areaY + yOffset;

            for (int rowIndex = 0; rowIndex < dataset.getRowCount(); rowIndex++) {
                for (int columnIndex = 0; columnIndex < dataset.getColumnCount(); columnIndex++) {

                    float value = dataset.getValue(rowIndex, columnIndex).floatValue();
                    if (useLogColour) {
                        value = (value == 0) ? 0 : ((float) Math.log(value));
                    }
                    float ratio = (maxValue == 0) ? 0 : value / maxValue;

                    Color cellColor = new Color(baseComponents[0] + (differenceComponents[0] * ratio), baseComponents[1] + (differenceComponents[1] * ratio), baseComponents[2]
                            + (differenceComponents[2] * ratio));

                    g2.setColor(cellColor);

                    g2.fillRect(xDataStart + dataWidth * columnIndex / dataset.getColumnCount(), yDataStart + dataHeight * rowIndex / dataset.getRowCount(), cellSize, cellSize);
                }

            }
        }

        @Override
        public String getPlotType() {
            return "CellPlot";
        }

    }

    static class HexagonalCellPlot extends CellPlot {

        private static final long serialVersionUID = 1L;

        private static final double[] X_POINTS = {0.333, 1.0, 1.333, 1.0, 0.333, 0.0};
        private static final double[] Y_POINTS = {0.0, 0.0, 0.5, 1.0, 1.0, 0.5};
        
        public HexagonalCellPlot(CellDataset dataset) {
            super(dataset);
        }
        

        @Override
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, PlotState parentState, PlotRenderingInfo info) {
            int areaX = (int) area.getX();
            int areaY = (int) area.getY();
            int areaWidth = (int) area.getWidth();
            int areaHeight = (int) area.getHeight();

            int cellSize = (int) Math.ceil(Math.min(areaWidth / (dataset.getColumnCount() + 0.333),
                    (areaHeight) / (dataset.getRowCount() + 0.5)));

            int dataWidth = Math.min(cellSize * dataset.getColumnCount(), areaWidth);
            int dataHeight = Math.min(cellSize * dataset.getRowCount(), areaHeight);

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
            if (xOffset > 0) {
                // Left/right borders
                g2.fillRect(areaX, areaY, areaX + xOffset, areaY + areaHeight);
                g2.fillRect(areaX + areaWidth - xOffset, areaY, areaX + xOffset, areaY + areaHeight);
            }
            else if (yOffset > 0) {
                // Top/bottom borders
                g2.fillRect(areaX, areaY, areaX + areaWidth, areaY + yOffset);
                g2.fillRect(areaX, areaY + areaHeight - yOffset, areaX + areaWidth, areaY + yOffset);
            }

            float maxValue = dataset.getMaxValue().floatValue();
            if (useLogColour) {
                maxValue = (float) Math.log(maxValue);
            }

            int xDataStart = areaX + xOffset;
            int yDataStart = areaY + yOffset;

            int[] xPoints  = new int[6];
            int[] yPoints  = new int[6];
            for (int index = 0; index < 6; index++) {
                xPoints[index] = (int) (X_POINTS[index] * cellSize);
                yPoints[index] = (int) (Y_POINTS[index] * cellSize);
            }
            
            Polygon hexagon = new Polygon(xPoints, yPoints, 6);
            
            AffineTransform savedTransform = g2.getTransform();
            
            g2.translate(xDataStart, yDataStart);
            for (int rowIndex = 0; rowIndex < dataset.getRowCount(); rowIndex++) {
                g2.translate(0, cellSize);
                for (int columnIndex = 0; columnIndex < dataset.getColumnCount(); columnIndex++) {
                    float value = dataset.getValue(rowIndex, columnIndex).floatValue();
                    if (useLogColour) {
                        value = (value == 0) ? 0 : ((float) Math.log(value));
                    }
                    float ratio = (maxValue == 0) ? 0 : value / maxValue;

                    Color cellColor = new Color(baseComponents[0] + (differenceComponents[0] * ratio), baseComponents[1] + (differenceComponents[1] * ratio), baseComponents[2]
                            + (differenceComponents[2] * ratio));

                    g2.setColor(cellColor);

                    g2.fillPolygon(hexagon);

                    g2.translate(cellSize, cellSize / 2 * ((columnIndex % 2 == 0) ? 1 : -1));
                }
                g2.translate(-cellSize * dataset.getColumnCount(), ((dataset.getColumnCount() % 2 == 0) ? 0 : - cellSize / 2));
            }
            
            g2.setTransform(savedTransform);
        }

        @Override
        public String getPlotType() {
            return "CellPlot";
        }

    }

    static interface CellDataset extends Values2D, Dataset {
        public Number getMaxValue();

    }

    static class DefaultCellDataset implements CellDataset {

        private final int rows;
        private final int columns;
        private final float[][] data;
        private float dynamicMaxValue = 0;
        private float maxValue = 0;
        private final List<DatasetChangeListener> listeners = new ArrayList<DatasetChangeListener>();
        private DatasetGroup group;
        private boolean useDynamicMaximum = true;

        public DefaultCellDataset(int rows, int columns) {
            this.rows = rows;
            this.columns = columns;
            this.data = new float[columns][rows];
        }

        public void setUseDynamicMaximum(boolean value) {
            useDynamicMaximum = value;
        }

        public void setValue(int row, int column, float value) {
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
