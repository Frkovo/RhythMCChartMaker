package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;

import net.minecraft.client.gui.widget.ButtonWidget;
import java.util.List;

class TrackEventEditor {
    final EventLaneType eventType;
    final String section;
    final ButtonWidget addButton;
    final ButtonWidget duplicateButton;
    final ButtonWidget deleteButton;
    final List<TrackEventRow> rows;
    int baseX;
    int sectionY = -1;
    int titleY = -1;
    int columnsY = -1;

    TrackEventEditor(EventLaneType eventType, String section, ButtonWidget addButton, ButtonWidget duplicateButton, ButtonWidget deleteButton, List<TrackEventRow> rows) {
        this.eventType = eventType;
        this.section = section;
        this.addButton = addButton;
        this.duplicateButton = duplicateButton;
        this.deleteButton = deleteButton;
        this.rows = rows;
    }

    EventLaneType eventType() {
        return eventType;
    }

    String section() {
        return section;
    }
}
