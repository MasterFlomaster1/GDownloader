package net.brlns.gdownloader.ui.custom;

import java.awt.*;
import java.util.Dictionary;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
public class CustomSliderUI extends BasicSliderUI{

    private static final Color TRACK_COLOR = Color.LIGHT_GRAY;
    private static final Color THUMB_COLOR = Color.WHITE;
    private static final Color TICK_COLOR = Color.WHITE;
    private static final Color LABEL_COLOR = Color.WHITE;

    public CustomSliderUI(JSlider slider){
        super(slider);
    }

    @Override
    protected Dimension getThumbSize(){
        return new Dimension(20, 20);
    }

    @Override
    public void paintTrack(Graphics g){
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle trackBounds = trackRect;
        g2d.setColor(TRACK_COLOR);
        g2d.fillRect(trackBounds.x, trackBounds.y + (trackBounds.height / 2) - 2,
            trackBounds.width, 4);

        g2d.dispose();
    }

    @Override
    public void paintThumb(Graphics g){
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle thumbBounds = thumbRect;
        g2d.setColor(THUMB_COLOR);
        g2d.fillOval(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);

        g2d.dispose();
    }

    @Override
    public void paintTicks(Graphics g){
        Rectangle tickBounds = tickRect;
        g.setColor(TICK_COLOR);

        for(int i = slider.getMinimum(); i <= slider.getMaximum(); i += slider.getMajorTickSpacing()){
            int x = xPositionForValue(i);
            g.drawLine(x, tickBounds.y, x, tickBounds.y + tickBounds.height);
        }
    }

    @Override
    public void paintLabels(Graphics g){
        Font font = new Font("Arial", Font.PLAIN, 12);
        g.setFont(font);
        g.setColor(LABEL_COLOR);

        Dictionary<Integer, JLabel> labels = slider.getLabelTable();

        if(labels != null){
            Rectangle labelBounds = labelRect;

            for(int i = slider.getMinimum(); i <= slider.getMaximum(); i += slider.getMajorTickSpacing()){
                JLabel label = labels.get(i);
                if(label != null){
                    int x = xPositionForValue(i);
                    g.drawString(label.getText(), x - label.getBounds().width / 2,
                        labelBounds.y + labelBounds.height);
                }
            }
        }
    }

    @Override
    public void paint(Graphics g, JComponent c){
        c.setBackground(Color.DARK_GRAY);

        super.paint(g, c);
    }

    @Override
    protected void installDefaults(JSlider slider){
        super.installDefaults(slider);

        slider.setFocusable(false);
    }
}
