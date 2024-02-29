package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.version.VERSION;

import javax.swing.*;
import java.awt.*;

public class SplashScreenView extends JFrame {
    private static final Dimension SPLASH_SCREEN_DIMENSION = new Dimension(300, 200);

    public SplashScreenView(boolean isCuda, String cudaMessage) {
        super("Loading Imaging FCS " + VERSION.IMFCS_VERSION);
        setUIFont();
        InitializeUI(isCuda, cudaMessage);
        setVisible(true);
    }

    public void setUIFont() {
        UIManager.getLookAndFeelDefaults().put("defaultFont",
                new Font(Constants.PANEL_FONT, Font.PLAIN, Constants.PANEL_FONT_SIZE));
        UIManager.put("Button.font", new Font(Constants.PANEL_FONT, Font.BOLD, Constants.PANEL_FONT_SIZE));
        UIManager.put("ToggleButton.font", new Font(Constants.PANEL_FONT, Font.BOLD, Constants.PANEL_FONT_SIZE));
        UIManager.put("RadioButton.font", new Font(Constants.PANEL_FONT, Font.BOLD, Constants.PANEL_FONT_SIZE));
        UIManager.put("Label.font", new Font(Constants.PANEL_FONT, Font.ITALIC, Constants.PANEL_FONT_SIZE));
        UIManager.put("ComboBox.font", new Font(Constants.PANEL_FONT, Font.PLAIN, Constants.PANEL_FONT_SIZE));
        UIManager.put("TextField.font", new Font(Constants.PANEL_FONT, Font.PLAIN, Constants.PANEL_FONT_SIZE));
        UIManager.put("ToolTip.font", new Font(Constants.PANEL_FONT, Font.PLAIN, Constants.PANEL_FONT_SIZE));
    }

    public void InitializeUI(boolean isCuda, String cudaMessage) {
        setFocusable(true);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new GridLayout(2, 1));
        String mode = isCuda ? "GPU mode" : "CPU mode";

        JLabel l1 = new JLabel(mode);
        // Use html tags to wrap long sentences if necessary.
        JLabel l2 = new JLabel("<html><p>" + cudaMessage + "</p></html>");

        l1.setVerticalAlignment(JLabel.CENTER);
        l1.setHorizontalAlignment(JLabel.CENTER);
        l2.setVerticalAlignment(JLabel.CENTER);
        l2.setHorizontalAlignment(JLabel.CENTER);

        add(l1);
        add(l2);

        setSize(SPLASH_SCREEN_DIMENSION);
        setLocation(Constants.MAIN_PANEL_POS.x,
                (Constants.MAIN_PANEL_POS.y + Constants.MAIN_PANEL_DIM.height - SPLASH_SCREEN_DIMENSION.height) / 2);
    }
}
