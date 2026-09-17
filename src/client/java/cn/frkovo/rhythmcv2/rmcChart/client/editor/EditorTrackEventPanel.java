package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.*;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.*;
import static cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorUtils.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.util.*;

final class EditorTrackEventPanel {
    private final ChartEditorScreen screen;

    final List<TrackEventEditor> trackEventEditors = new ArrayList<>();
    final LinkedHashSet<TrackEventRow> selectedTrackEventRows = new LinkedHashSet<>();

    TrackEventRow easingPopupRow;
    int easingPopupX;
    int easingPopupY;
    TextFieldWidget easingPopupSearchField;

    TrackEventEditor draggingTrackEventEditor;
    int draggingTrackEventSourceIndex = -1;
    int draggingTrackEventTargetIndex = -1;
    double draggingTrackEventMouseY;
    SelectionBox trackEventSelectionBox;
    boolean trackEventSelectionAdditive;

    EditorTrackEventPanel(ChartEditorScreen screen) {
        this.screen = screen;
    }

    void clear() {
        trackEventEditors.clear();
        selectedTrackEventRows.clear();
        trackEventSelectionBox = null;
        easingPopupRow = null;
    }

    void onSelectionChanged() {
        selectedTrackEventRows.clear();
        easingPopupRow = null;
        if (easingPopupSearchField != null) {
            easingPopupSearchField.setText("");
            easingPopupSearchField.visible = false;
            easingPopupSearchField.active = false;
            easingPopupSearchField.setFocused(false);
        }
    }

    void createFields(int rightX, int fieldWidth) {
        for (EventLaneType eventType : EventLaneType.values()) {
            ButtonWidget addButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Set"), b -> addTrackEventRow(eventType))
                    .dimensions(rightX + fieldWidth - 48, 0, 48, 18).build());
            ButtonWidget duplicateButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Copy"), b -> duplicateSelectedTrackEventRows(eventType))
                    .dimensions(rightX + fieldWidth - 80, 0, 28, 18).build());
            ButtonWidget deleteButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> deleteSelectedTrackEventRows(eventType))
                    .dimensions(rightX + fieldWidth - 116, 0, 36, 18).build());
            addButton.visible = false;
            addButton.active = false;
            duplicateButton.visible = false;
            duplicateButton.active = false;
            deleteButton.visible = false;
            deleteButton.active = false;
            trackEventEditors.add(new TrackEventEditor(eventType, trackEventSection(eventType), addButton, duplicateButton, deleteButton, new ArrayList<>()));
        }
        easingPopupSearchField = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), rightX, 0, 188, 18, Text.literal("Search easing")));
        easingPopupSearchField.setMaxLength(64);
        easingPopupSearchField.visible = false;
        easingPopupSearchField.active = false;
    }

    int layout(int panelX, int currentY) {
        EditorLayout layout = screen.editorLayout();
        int viewportTop = layout.topY() + 48;
        int viewportBottom = layout.topY() + layout.panelHeight() - 50;
        for (TrackEventEditor editor : trackEventEditors) {
            setTrackEventEditorVisible(editor, false);
        }
        if (screen.state.selection().kind() != EditorSelection.Kind.TRACK || screen.state.selection().track() == null) {
            return currentY;
        }
        String lastSection = null;
        for (TrackEventEditor editor : trackEventEditors) {
            if (!editor.section().equals(lastSection)) {
                currentY += 10;
                editor.sectionY = currentY;
                screen.propertyPanel.propertySectionHeaders.add(new PropertySectionHeader(screen.propertyPanel.propertySectionKey(EditorSelection.Kind.TRACK, editor.section()), editor.section(), panelX, currentY, screen.rightPanelInnerWidth(), 12));
                currentY += 12;
                lastSection = editor.section();
            } else {
                editor.sectionY = -1;
            }
            editor.baseX = panelX;
            editor.titleY = -1;
            editor.columnsY = -1;
            boolean collapsed = screen.propertyPanel.isSectionCollapsed(screen.propertyPanel.propertySectionKey(EditorSelection.Kind.TRACK, editor.section()));
            if (collapsed) {
                setTrackEventEditorVisible(editor, false);
                continue;
            }
            editor.titleY = currentY;
            List<NumEventData> laneEvents = screen.eventsForLane(screen.state.selection().track(), editor.eventType());
            boolean titleInsideViewport = currentY >= viewportTop && currentY + 18 <= viewportBottom;
            editor.addButton.visible = titleInsideViewport;
            editor.addButton.active = titleInsideViewport;
            editor.addButton.setPosition(panelX + screen.rightPanelInnerWidth() - 92, currentY - 3);
            editor.duplicateButton.visible = false;
            editor.duplicateButton.active = false;
            editor.deleteButton.visible = titleInsideViewport;
            editor.deleteButton.active = titleInsideViewport && !laneEvents.isEmpty();
            editor.deleteButton.setPosition(panelX + screen.rightPanelInnerWidth() - 42, currentY - 3);
            currentY += 14;
            editor.columnsY = currentY;
            currentY += 12;
            int rowY = currentY;
            if (!editor.rows.isEmpty()) {
                TrackEventRow row = editor.rows.getFirst();
                layoutTrackEventRow(panelX, rowY, row);
                boolean rowInsideViewport = rowY >= viewportTop && rowY + 18 <= viewportBottom;
                setTrackEventRowVisible(row, rowInsideViewport);
                rowY += 22;
            }
            currentY = rowY + 2;
        }
        return currentY;
    }

    void syncTrackEventEditors(TrackData track) {
        for (TrackEventEditor editor : trackEventEditors) {
            List<NumEventData> events = screen.eventsForLane(track, editor.eventType());
            ensureTrackEventRows(editor, 1);
            for (int index = 0; index < editor.rows.size(); index++) {
                TrackEventRow row = editor.rows.get(index);
                boolean active = index == 0;
                row.visible = active;
                row.startBeat.visible = active;
                row.endBeat.visible = active;
                row.startValue.visible = active;
                row.endValue.visible = active;
                row.easingButton.visible = active;
                row.copyButton.visible = false;
                row.removeButton.visible = false;
                row.startBeat.active = active;
                row.endBeat.active = active;
                row.startValue.active = active;
                row.endValue.active = active;
                row.easingButton.active = active;
                row.copyButton.active = false;
                row.removeButton.active = false;
                if (!active) continue;
                if (events.isEmpty()) {
                    row.startBeat.setText("");
                    row.endBeat.setText("");
                    row.startValue.setText("");
                    row.endValue.setText("");
                    row.easingType = EasingType.LINEAR;
                } else {
                    NumEventData event = events.getFirst();
                    row.startBeat.setText(format(event.startBeat()));
                    row.endBeat.setText(format(event.endBeat()));
                    row.startValue.setText(format(event.startValue()));
                    row.endValue.setText(format(event.endValue()));
                    row.easingType = event.easingType();
                }
                row.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(row.easingType)));
            }
        }
    }

    void hideTrackEventEditors() {
        for (TrackEventEditor editor : trackEventEditors) {
            editor.sectionY = -1;
            editor.titleY = -1;
            editor.columnsY = -1;
            setTrackEventEditorVisible(editor, false);
        }
    }

    void applyTrackEventEditors(TrackData track) {
        for (TrackEventEditor editor : trackEventEditors) {
            List<NumEventData> updated = new ArrayList<>();
            TrackEventRow row = editor.rows.isEmpty() ? null : editor.rows.getFirst();
            if (row != null && !row.startBeat.getText().isBlank()) {
                updated.add(new NumEventData(
                        parseDouble(row.startBeat.getText()),
                        parseDouble(row.endBeat.getText()),
                        parseDouble(row.startValue.getText()),
                        parseDouble(row.endValue.getText()),
                        row.easingType
                ));
            } else {
                updated.add(new NumEventData(0.0, 4096.0, defaultStartValueForEventLane(editor.eventType()), defaultStartValueForEventLane(editor.eventType()), EasingType.LINEAR));
            }
            replaceNumEvents(screen.eventsForLane(track, editor.eventType()), updated);
        }
    }

    boolean handleClick(Click click, double mouseX, double mouseY) {
        if (screen.state.selection().kind() == EditorSelection.Kind.TRACK) {
            for (TrackEventEditor editor : trackEventEditors) {
                for (int index = 0; index < editor.rows.size(); index++) {
                    TrackEventRow row = editor.rows.get(index);
                    if (!row.visible) {
                        continue;
                    }
                    if (EditorUtils.isInside(mouseX, mouseY, row.dragX, row.y, row.rowRight - row.dragX, 18)) {
                        selectTrackEventRow(editor, row, screen.isControlDown());
                        screen.propertyPanel.layoutPropertyFields();
                        return true;
                    }
                    if (EditorUtils.isInside(mouseX, mouseY, row.dragX, row.y, ChartEditorScreen.EVENT_DRAG_WIDTH, 18)) {
                        draggingTrackEventEditor = editor;
                        draggingTrackEventSourceIndex = index;
                        draggingTrackEventTargetIndex = index;
                        draggingTrackEventMouseY = mouseY;
                        screen.dragMode = DragMode.TRACK_EVENT_ROW;
                        return true;
                    }
                }
            }
            if (click.button() == InputUtil.GLFW_MOUSE_BUTTON_LEFT && isInsideTrackEventEditorArea(mouseX, mouseY)) {
                trackEventSelectionBox = new SelectionBox((int) mouseX, (int) mouseY, (int) mouseX, (int) mouseY);
                trackEventSelectionAdditive = screen.isControlDown();
                screen.dragMode = DragMode.TRACK_EVENT_BOX_SELECT;
                closeTrackEventEasingPopup();
                return true;
            }
        }
        return false;
    }

    boolean handleEasingPopupClick(double mouseX, double mouseY) {
        if (easingPopupRow == null) {
            return false;
        }
        EasingPopupLayout layout = buildEasingPopupLayout();
        if (EditorUtils.isInside(mouseX, mouseY, easingPopupSearchField.getX(), easingPopupSearchField.getY(), easingPopupSearchField.getWidth(), easingPopupSearchField.getHeight())) {
            return false;
        }
        if (!EditorUtils.isInside(mouseX, mouseY, easingPopupX, easingPopupY, layout.width(), layout.height())) {
            closeTrackEventEasingPopup();
            return false;
        }
        for (EasingPopupEntry entry : layout.entries()) {
            if (EditorUtils.isInside(mouseX, mouseY, entry.x(), entry.y(), entry.width(), entry.height())) {
                easingPopupRow.easingType = entry.easingType();
                easingPopupRow.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(entry.easingType())));
                closeTrackEventEasingPopup();
                return true;
            }
        }
        return true;
    }

    EasingPopupLayout buildEasingPopupLayout() {
        int popupWidth = screen.rightPanelInnerWidth();
        int padding = 6;
        int optionHeight = 12;
        int searchHeight = 18;
        easingPopupX = screen.rightPanelX() + 8;
        int searchY = easingPopupY + padding;
        easingPopupSearchField.setPosition(easingPopupX + padding, searchY);
        easingPopupSearchField.setWidth(popupWidth - padding * 2);
        easingPopupSearchField.visible = true;
        easingPopupSearchField.active = true;

        String filter = easingPopupSearchField.getText().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        Map<String, List<EasingType>> groups = filteredEasingGroups(filter);
        List<EasingPopupGroup> groupLayouts = new ArrayList<>();
        List<EasingPopupEntry> entries = new ArrayList<>();
        int y = searchY + searchHeight + 8;
        for (Map.Entry<String, List<EasingType>> group : groups.entrySet()) {
            if (group.getValue().isEmpty()) {
                continue;
            }
            groupLayouts.add(new EasingPopupGroup(group.getKey(), y));
            y += 12;
            for (int index = 0; index < group.getValue().size(); index++) {
                int column = index % 2;
                int row = index / 2;
                EasingType easingType = group.getValue().get(index);
                int columnWidth = (popupWidth - padding * 2) / 2;
                int optionX = easingPopupX + padding + column * columnWidth;
                int optionY = y + row * optionHeight;
                int optionWidth = columnWidth - 4;
                entries.add(new EasingPopupEntry(easingType, shortTrackEasePopupLabel(easingType), optionX, optionY, optionWidth, optionHeight));
            }
            y += Math.max(1, (int) Math.ceil(group.getValue().size() / 2.0)) * optionHeight + 6;
        }
        if (entries.isEmpty()) {
            y += 12;
        }
        int popupHeight = Math.max(searchHeight + 16, y - easingPopupY);
        int clampedY = EditorUtils.clamp(easingPopupY, 84, Math.max(84, screen.publicHeight() - popupHeight - 40));
        if (clampedY != easingPopupY) {
            easingPopupY = clampedY;
            return buildEasingPopupLayout();
        }
        return new EasingPopupLayout(popupWidth, popupHeight, groupLayouts, entries);
    }

    Map<String, List<EasingType>> filteredEasingGroups(String filter) {
        Map<String, List<EasingType>> groups = new LinkedHashMap<>();
        groups.put("Basic", new ArrayList<>());
        groups.put("Ease In", new ArrayList<>());
        groups.put("Ease Out", new ArrayList<>());
        groups.put("Ease In/Out", new ArrayList<>());
        for (EasingType easingType : EasingType.values()) {
            String label = trackEaseDisplayLabel(easingType).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
            if (!filter.isBlank() && !label.contains(filter)) {
                continue;
            }
            groups.get(trackEaseGroup(easingType)).add(easingType);
        }
        return groups;
    }

    boolean isInsideTrackEventEditorArea(double mouseX, double mouseY) {
        for (TrackEventEditor editor : trackEventEditors) {
            if (editor.titleY < 0) continue;
            int top = editor.columnsY;
            int bottom = editor.rows.stream().filter(r -> r.visible).mapToInt(r -> r.y + 18).max().orElse(editor.columnsY + 18);
            int left = editor.baseX;
            if (EditorUtils.isInside(mouseX, mouseY, left, top, screen.rightPanelWidth() - 24, Math.max(20, bottom - top + 4))) {
                return true;
            }
        }
        return false;
    }

    void applyTrackEventSelectionBox() {
        int left = Math.min(trackEventSelectionBox.startX(), trackEventSelectionBox.endX());
        int right = Math.max(trackEventSelectionBox.startX(), trackEventSelectionBox.endX());
        int top = Math.min(trackEventSelectionBox.startY(), trackEventSelectionBox.endY());
        int bottom = Math.max(trackEventSelectionBox.startY(), trackEventSelectionBox.endY());
        LinkedHashSet<TrackEventRow> next = trackEventSelectionAdditive ? new LinkedHashSet<>(selectedTrackEventRows) : new LinkedHashSet<>();
        for (TrackEventEditor editor : trackEventEditors) {
            for (TrackEventRow row : editor.rows) {
                if (!row.visible) continue;
                if (EditorUtils.rectanglesIntersect(left, top, right, bottom, row.dragX, row.y, row.rowRight, row.y + 18)) {
                    next.add(row);
                }
            }
        }
        selectedTrackEventRows.clear();
        selectedTrackEventRows.addAll(next);
        if (selectedTrackEventRows.isEmpty() && !trackEventSelectionAdditive) {
            closeTrackEventEasingPopup();
        }
        screen.propertyPanel.layoutPropertyFields();
    }

    void selectTrackEventRow(TrackEventEditor editor, TrackEventRow row, boolean additive) {
        if (!additive) {
            selectedTrackEventRows.clear();
            selectedTrackEventRows.add(row);
            return;
        }
        if (!selectedTrackEventRows.add(row)) {
            selectedTrackEventRows.remove(row);
        }
        if (selectedTrackEventRows.isEmpty()) {
            selectedTrackEventRows.add(row);
        }
    }

    boolean hasSelectedTrackEventRows(TrackEventEditor editor) {
        for (TrackEventRow row : editor.rows) {
            if (selectedTrackEventRows.contains(row)) return true;
        }
        return false;
    }

    void openTrackEventEasingPopup(TrackEventRow row) {
        if (row == null) return;
        easingPopupRow = row;
        easingPopupX = screen.rightPanelX() + 8;
        easingPopupY = row.easingButton.getY() + 20;
        easingPopupSearchField.setText("");
        easingPopupSearchField.visible = true;
        easingPopupSearchField.active = true;
        easingPopupSearchField.setFocused(true);
    }

    void closeTrackEventEasingPopup() {
        easingPopupRow = null;
        if (easingPopupSearchField != null) {
            easingPopupSearchField.visible = false;
            easingPopupSearchField.active = false;
            easingPopupSearchField.setFocused(false);
        }
    }

    boolean deleteSelectedTrackEventRows(EventLaneType eventType) {
        TrackData track = screen.state.selection().track();
        if (track == null) return false;
        boolean deletedAny = false;
        for (TrackEventEditor editor : trackEventEditors) {
            if (eventType != null && editor.eventType() != eventType) continue;
            boolean shouldClearLane = eventType != null || hasSelectedTrackEventRows(editor);
            if (!shouldClearLane) continue;
            List<NumEventData> events = screen.eventsForLane(track, editor.eventType());
            if (!events.isEmpty()) {
                events.clear();
                events.add(new NumEventData(0.0, 4096.0, defaultStartValueForEventLane(editor.eventType()), defaultStartValueForEventLane(editor.eventType()), EasingType.LINEAR));
                deletedAny = true;
            }
            selectedTrackEventRows.removeAll(editor.rows);
        }
        if (deletedAny) {
            screen.state.markDirty();
            screen.state.setStatus("Reset selected automation lane(s) to default");
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
        }
        return deletedAny;
    }

    void duplicateSelectedTrackEventRows(EventLaneType eventType) {
        TrackData track = screen.state.selection().track();
        if (track == null) return;
        for (TrackEventEditor editor : trackEventEditors) {
            if (editor.eventType() != eventType) continue;
            screen.state.setStatus(editor.eventType().label + " is a single-event lane; use Set or edit the card values");
            return;
        }
    }

    void layoutTrackEventRow(int x, int y, TrackEventRow row) {
        row.y = y;
        row.dragX = x;
        int gap = 4;
        int numericWidth = relativeTrackEventCellWidth();
        int easeWidth = relativeTrackEventEaseWidth(numericWidth);
        int cursorX = x + ChartEditorScreen.EVENT_DRAG_WIDTH + gap;
        row.startBeat.setPosition(cursorX, y);
        row.startBeat.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.endBeat.setPosition(cursorX, y);
        row.endBeat.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.startValue.setPosition(cursorX, y);
        row.startValue.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.endValue.setPosition(cursorX, y);
        row.endValue.setWidth(numericWidth);
        cursorX += numericWidth + gap;
        row.easingButton.setPosition(cursorX, y);
        row.easingButton.setWidth(easeWidth);
        row.rowRight = row.easingButton.getX() + easeWidth;
    }

    int relativeTrackEventCellWidth() {
        int gap = 4;
        int contentWidth = screen.rightPanelInnerWidth() - ChartEditorScreen.EVENT_DRAG_WIDTH - gap;
        int easeWidth = EditorUtils.clamp((int) Math.round(contentWidth * 0.30), 64, 100);
        return Math.max(24, (contentWidth - easeWidth - gap * 4) / 4);
    }

    int relativeTrackEventEaseWidth(int numericWidth) {
        int gap = 4;
        int contentWidth = screen.rightPanelInnerWidth() - ChartEditorScreen.EVENT_DRAG_WIDTH - gap;
        return Math.max(60, contentWidth - numericWidth * 4 - gap * 4);
    }

    void setTrackEventEditorVisible(TrackEventEditor editor, boolean visible) {
        editor.addButton.visible = visible;
        editor.addButton.active = visible;
        editor.duplicateButton.visible = visible;
        editor.duplicateButton.active = visible && hasSelectedTrackEventRows(editor);
        editor.deleteButton.visible = visible;
        editor.deleteButton.active = visible && hasSelectedTrackEventRows(editor);
        for (TrackEventRow row : editor.rows) {
            row.visible = visible;
            row.startBeat.visible = visible;
            row.startBeat.active = visible;
            row.endBeat.visible = visible;
            row.endBeat.active = visible;
            row.startValue.visible = visible;
            row.startValue.active = visible;
            row.endValue.visible = visible;
            row.endValue.active = visible;
            row.easingButton.visible = visible;
            row.easingButton.active = visible;
            row.copyButton.visible = false;
            row.copyButton.active = false;
            row.removeButton.visible = false;
            row.removeButton.active = false;
        }
    }

    void ensureTrackEventRows(TrackEventEditor editor, int count) {
        while (editor.rows.size() < count) {
            editor.rows.add(createTrackEventRow(editor));
        }
    }

    TrackEventRow createTrackEventRow(TrackEventEditor editor) {
        TextFieldWidget startBeat = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), 0, 0, ChartEditorScreen.EVENT_CELL_WIDTH, 18, Text.literal("Start")));
        TextFieldWidget endBeat = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), 0, 0, ChartEditorScreen.EVENT_CELL_WIDTH, 18, Text.literal("End")));
        TextFieldWidget startValue = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), 0, 0, ChartEditorScreen.EVENT_CELL_WIDTH, 18, Text.literal("From")));
        TextFieldWidget endValue = (TextFieldWidget) screen.publicAddDrawableChild(new TextFieldWidget(screen.getTextRenderer(), 0, 0, ChartEditorScreen.EVENT_CELL_WIDTH, 18, Text.literal("To")));
        ButtonWidget easingButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal(trackEaseDisplayLabel(EasingType.LINEAR)), b -> openTrackEventEasingPopup(findTrackEventRow(editor, b))).dimensions(0, 0, ChartEditorScreen.EVENT_EASE_WIDTH, 18).build());
        startBeat.setMaxLength(64);
        endBeat.setMaxLength(64);
        startValue.setMaxLength(64);
        endValue.setMaxLength(64);
        ButtonWidget copyButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Set"), b -> copyTrackEventRow(editor, findTrackEventRow(editor, b))).dimensions(0, 0, ChartEditorScreen.EVENT_COPY_WIDTH, 18).build());
        ButtonWidget removeButton = (ButtonWidget) screen.publicAddDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> removeTrackEventRow(editor, findTrackEventRow(editor, b))).dimensions(0, 0, ChartEditorScreen.EVENT_REMOVE_WIDTH, 18).build());
        TrackEventRow row = new TrackEventRow(startBeat, endBeat, startValue, endValue, easingButton, copyButton, removeButton);
        setTrackEventRowVisible(row, false);
        return row;
    }

    TrackEventRow findTrackEventRow(TrackEventEditor editor, ButtonWidget button) {
        for (TrackEventRow row : editor.rows) {
            if (row.removeButton == button || row.copyButton == button || row.easingButton == button) {
                return row;
            }
        }
        return null;
    }

    void setTrackEventRowVisible(TrackEventRow row, boolean visible) {
        row.visible = visible;
        row.startBeat.visible = visible;
        row.startBeat.active = visible;
        row.endBeat.visible = visible;
        row.endBeat.active = visible;
        row.startValue.visible = visible;
        row.startValue.active = visible;
        row.endValue.visible = visible;
        row.endValue.active = visible;
        row.easingButton.visible = visible;
        row.easingButton.active = visible;
        row.copyButton.visible = false;
        row.copyButton.active = false;
        row.removeButton.visible = false;
        row.removeButton.active = false;
    }

    void cycleTrackEventRowEasing(TrackEventRow row) {
        if (row == null) return;
        EasingType[] values = EasingType.values();
        int nextIndex = (row.easingType.ordinal() + 1) % values.length;
        row.easingType = values[nextIndex];
        row.easingButton.setMessage(Text.literal(trackEaseDisplayLabel(row.easingType)));
    }

    void copyTrackEventRow(TrackEventEditor editor, TrackEventRow row) {
        screen.state.setStatus(editor.eventType().label + " is a single-event lane; edit this automation card directly");
    }

    void addTrackEventRow(EventLaneType eventType) {
        TrackData track = screen.state.selection().track();
        if (track == null) return;
        List<NumEventData> events = screen.eventsForLane(track, eventType);
        double beat = screen.state.playheadBeat();
        double startValue = defaultStartValueForEventLane(eventType);
        double endValue = startValue;
        if (!events.isEmpty()) {
            NumEventData event = events.getFirst();
            startValue = event.startValue();
            endValue = event.endValue();
        }
        events.clear();
        events.add(new NumEventData(beat, beat + 1.0, startValue, endValue, EasingType.LINEAR));
        screen.state.markDirty();
        screen.state.setStatus("Set " + eventType.label + " automation lane");
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
    }

    void removeTrackEventRow(TrackEventEditor editor, TrackEventRow row) {
        TrackData track = screen.state.selection().track();
        if (track == null || row == null) return;
        List<NumEventData> events = screen.eventsForLane(track, editor.eventType());
        if (!events.isEmpty()) {
            events.clear();
            events.add(new NumEventData(0.0, 4096.0, defaultStartValueForEventLane(editor.eventType()), defaultStartValueForEventLane(editor.eventType()), EasingType.LINEAR));
            screen.state.markDirty();
            screen.state.setStatus("Reset " + editor.eventType().label + " to default");
            screen.propertyPanel.populateFieldsFromSelection();
            screen.propertyPanel.layoutPropertyFields();
        }
    }

    double defaultStartValueForEventLane(EventLaneType eventType) {
        return switch (eventType) {
            case SCALE_X, SCALE_Y, SCALE_Z -> 1.0;
            case SPEED -> 20.0;
            default -> 0.0;
        };
    }

    void updateDraggedTrackEventTarget(double mouseY) {
        if (draggingTrackEventEditor == null || draggingTrackEventSourceIndex < 0) return;
        int targetIndex = visibleTrackEventCount(draggingTrackEventEditor);
        for (int index = 0; index < draggingTrackEventEditor.rows.size(); index++) {
            TrackEventRow row = draggingTrackEventEditor.rows.get(index);
            if (!row.visible) continue;
            if (mouseY < row.y + 9) {
                targetIndex = index;
                break;
            }
        }
        draggingTrackEventTargetIndex = Math.max(0, targetIndex);
    }

    void applyDraggedTrackEventReorder() {
        TrackData track = screen.state.selection().track();
        if (track == null || draggingTrackEventEditor == null || draggingTrackEventSourceIndex < 0 || draggingTrackEventTargetIndex < 0) return;
        List<NumEventData> events = screen.eventsForLane(track, draggingTrackEventEditor.eventType());
        if (draggingTrackEventSourceIndex >= events.size()) return;
        int targetIndex = draggingTrackEventTargetIndex;
        if (targetIndex > draggingTrackEventSourceIndex) targetIndex--;
        targetIndex = EditorUtils.clamp(targetIndex, 0, Math.max(0, events.size() - 1));
        if (targetIndex == draggingTrackEventSourceIndex) return;
        NumEventData moved = events.remove(draggingTrackEventSourceIndex);
        events.add(targetIndex, moved);
        screen.state.markDirty();
        screen.state.setStatus("Reordered " + draggingTrackEventEditor.eventType().label + " events");
        screen.propertyPanel.populateFieldsFromSelection();
        screen.propertyPanel.layoutPropertyFields();
    }

    int visibleTrackEventCount(TrackEventEditor editor) {
        int count = 0;
        for (TrackEventRow row : editor.rows) {
            if (row.visible) count++;
        }
        return count;
    }

    int trackEventInsertLineY(TrackEventEditor editor, int targetIndex) {
        if (editor.rows.isEmpty() || visibleTrackEventCount(editor) == 0) return editor.columnsY + 14;
        int visibleCount = visibleTrackEventCount(editor);
        if (targetIndex >= visibleCount) {
            for (int index = editor.rows.size() - 1; index >= 0; index--) {
                TrackEventRow row = editor.rows.get(index);
                if (row.visible) return row.y + 20;
            }
        }
        return editor.rows.get(targetIndex).y - 2;
    }
}
