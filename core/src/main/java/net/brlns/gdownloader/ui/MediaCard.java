/*
 * Copyright (C) 2024 hstr0100
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.brlns.gdownloader.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.brlns.gdownloader.ui.custom.CustomProgressBar;
import net.brlns.gdownloader.ui.custom.CustomThumbnailPanel;

import static net.brlns.gdownloader.ui.GUIManager.runOnEDT;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
@Data
@Slf4j
public class MediaCard {

    private final int id;

    private final JPanel panel;
    private final JLabel mediaLabel;
    private final CustomThumbnailPanel thumbnailPanel;
    private final CustomProgressBar progressBar;

    private double percentage = 0;

    private Runnable onLeftClick;
    private Map<String, Runnable> rightClickMenu = new LinkedHashMap<>();
    private Runnable onClose;
    private Consumer<Integer> onDrag;
    private boolean closed;

    private Supplier<Boolean> validateDropTarget;

    protected static final int THUMBNAIL_WIDTH = 170;
    protected static final int THUMBNAIL_HEIGHT = (int)(THUMBNAIL_WIDTH / 16.0 * 9.0);

    public void close() {
        closed = true;

        if (onClose != null) {
            onClose.run();
        }
    }

    protected void scaleThumbnail(double factor) {
        Dimension dimension = new Dimension(
            (int)(MediaCard.THUMBNAIL_WIDTH * factor),
            (int)(MediaCard.THUMBNAIL_HEIGHT * factor));

        runOnEDT(() -> {
            thumbnailPanel.setPreferredSize(dimension);
            thumbnailPanel.setMinimumSize(dimension);
        });
    }

    public void setTooltip(String tooltipText) {
        runOnEDT(() -> {
            mediaLabel.setToolTipText(tooltipText);
        });
    }

    public void setThumbnailTooltip(String tooltipText) {
        runOnEDT(() -> {
            thumbnailPanel.setToolTipText(tooltipText);
        });
    }

    public void setLabel(String... label) {
        runOnEDT(() -> {
            mediaLabel.setText(GUIManager.wrapText(51, label));
        });
    }

    public void setPercentage(double percentageIn) {
        percentage = percentageIn;

        runOnEDT(() -> {
            progressBar.setValue((int)percentageIn);
        });
    }

    public void setProgressBarText(String text) {
        runOnEDT(() -> {
            progressBar.setString(text);
        });
    }

    public void setProgressBarTextAndColors(String text, Color backgroundColor) {
        setProgressBarTextAndColors(text, backgroundColor, Color.WHITE);
    }

    public void setProgressBarTextAndColors(String text, Color backgroundColor, Color textColor) {
        runOnEDT(() -> {
            progressBar.setString(text);
            progressBar.setForeground(backgroundColor);
            progressBar.setTextColor(textColor);
        });
    }

    public void setThumbnailAndDuration(BufferedImage img, long duration) {
        runOnEDT(() -> {
            thumbnailPanel.setImageAndDuration(img, duration);
        });
    }
}
