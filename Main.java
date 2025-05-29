import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import classes.RadarLegendPanel;
import helperfn.map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.geotools.swing.JMapPane;

import classes.ElevationLegendPanel;

public class Main extends JFrame implements ActionListener{
    private JFrame f = new JFrame("Radar LOS Map");
    private JTextField latField;
    private JTextField lonField;
    private JTextField heigthField;
    private JButton b1;
    private JLabel reportLabel;
    private map mapManager = new map();
    private Container c;

    Color[] colors = new Color[] {
        new Color(255, 255, 255),  // White (very low)
        new Color(192, 192, 192),  // Light Gray
        new Color(128, 128, 128),  // Medium Gray
        new Color(255, 165, 0),    // Orange
        new Color(160, 82, 45),    // SaddleBrown
        new Color(101, 67, 33)     // Dark Brown
    };

    

    String[] labels = new String[] {
            "Very Low", "Low", "Mid", "High", "Very High", "Peak"
    };


    Main(){
        c = this.getContentPane();
        c.setLayout(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel latLabel = new JLabel("Latitude [10, 11, 13, 14]");
        JLabel lonLabel = new JLabel("Longitude [75, 79]");
        JLabel HeightLabel = new JLabel("Height");
        latField = new JTextField(10);
        lonField = new JTextField(10);
        heigthField = new JTextField(10);
        reportLabel = new JLabel();
        b1 = new JButton("Show LOS Map");
        b1.addActionListener(this);

        inputPanel.add(latLabel);
        inputPanel.add(latField);
        inputPanel.add(lonLabel);
        inputPanel.add(lonField);
        inputPanel.add(HeightLabel);
        inputPanel.add(heigthField);
        inputPanel.add(b1);
        inputPanel.add(reportLabel);
        inputPanel.setVisible(true);

        c.add(inputPanel, BorderLayout.NORTH);

    }
    public void actionPerformed(ActionEvent e){
        reportLabel.setText("");
        double lat, lon, height;
        try{
            lat = Double.parseDouble(latField.getText());
            lon = Double.parseDouble(lonField.getText());
            height = Double.parseDouble(heigthField.getText());
            JMapPane map = mapManager.giveMap(lat, lon, height);
            if(map!=null){
                for(Component comp : c.getComponents()){
                    if(comp instanceof JMapPane || comp instanceof ElevationLegendPanel || comp instanceof RadarLegendPanel){
                        c.remove(comp);
                        
                    }
                    
                }

            }
            c.add(map, BorderLayout.CENTER);
            int[] elevation = mapManager.giveElevationData(lat, lon, height);
            int min = elevation[0];
            int max = elevation[1];
            double[] values = new double[] {
                min,
                min + (max - min) * 0.2,
                min + (max - min) * 0.4,
                min + (max - min) * 0.6,
                min + (max - min) * 0.8,
                max
            };
            reportLabel.setText("Minimum Elevation = " + min + ", Maximum Elevation = " + max);
            ElevationLegendPanel legend = new ElevationLegendPanel(values, colors, labels);
            legend.setBorder(new EmptyBorder(0, 100, 0, 10));
            c.add(legend, BorderLayout.EAST);
            c.add(new RadarLegendPanel(), BorderLayout.SOUTH);
            c.revalidate();
            c.repaint();
            
        }
        catch (Exception err){
            reportLabel.setText(err.getMessage());
        }
        finally{
            
        }
        
    }
    public static void main(String[] args) {
        Main curr = new Main();
        curr.setSize(800,600);
        
        curr.setVisible(true);
    }
}
