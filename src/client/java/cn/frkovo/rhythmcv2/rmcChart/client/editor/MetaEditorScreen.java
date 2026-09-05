package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import cn.frkovo.rhythmcv2.rmcChart.chart.model.BpmPoint;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.ChartDifficulty;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.MetaData;
import cn.frkovo.rhythmcv2.rmcChart.chart.model.SongManifestData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaEditorScreen extends Screen {
    private static final int PADDING = 12;
    private static final int GAP = 8;
    private static final int FIELD_HEIGHT = 38;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LORE_LINE_HEIGHT = 11;

    private static final int BG = 0xF0101318;
    private static final int PANEL_BG = 0xD71B222B;
    private static final int BORDER = 0xFF48596A;
    private static final int TEXT = 0xFFE9F2FA;
    private static final int MUTED = 0xFF9CB1C2;
    private static final int ACCENT = 0xFF7BC8FF;
    private static final int GREEN = 0xFF8FE1B2;
    private static final int WARN = 0xFFFFD37A;

    private static final Pattern BRACED_PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>");

    private final ChartEditorState state;
    private final Screen parent;
    private final List<FieldRow> rows = new ArrayList<>();
    private int scrollOffset = 0;
    private int leftPanelWidth;
    private int leftPanelHeight;
    private int leftPanelX;
    private int leftPanelY;
    private int rightPanelX;
    private int rightPanelWidth;
    private int rightPanelY;
    private int rightPanelHeight;
    private String status = "";

    public MetaEditorScreen(ChartEditorState state, Screen parent) {
        super(Text.literal("Meta Editor"));
        this.state = state;
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        rows.clear();

        leftPanelWidth = Math.max(260, (int) Math.round(width * 0.52));
        leftPanelX = PADDING;
        leftPanelY = PADDING + 22;
        leftPanelHeight = height - leftPanelY - PADDING - BUTTON_HEIGHT - GAP;

        rightPanelX = leftPanelX + leftPanelWidth + GAP;
        rightPanelWidth = width - rightPanelX - PADDING;
        rightPanelY = leftPanelY;
        rightPanelHeight = leftPanelHeight;

        int fieldX = leftPanelX + GAP;
        int fieldWidth = leftPanelWidth - GAP * 2;

        addSection("Song Manifest");
        addField(fieldX, fieldWidth, "Name", "Song list title");
        addField(fieldX, fieldWidth, "Composer", "Artist / composer name");
        addField(fieldX, fieldWidth, "Icon", "Material icon key");
        addField(fieldX, fieldWidth, "Alias", "Short song code");
        addField(fieldX, fieldWidth, "Length(ms)", "Audio length ms");
        addField(fieldX, fieldWidth, "SHA1", "Respack SHA1");
        addField(fieldX, fieldWidth, "Key", "Optional unique key");
        addField(fieldX, fieldWidth, "Description", "Song blurb");
        addField(fieldX, fieldWidth, "Song ID", "Unique song id");
        addField(fieldX, fieldWidth, "Version", "Manifest version");
        addField(fieldX, fieldWidth, "Beats Per Bar", "Editor shell beats per bar 1-32");
        addField(fieldX, fieldWidth, "Comments(csv)", "CSV list");
        addField(fieldX, fieldWidth, "PlayerAlias(csv)", "CSV list");
        addField(fieldX, fieldWidth, "Tags(csv)", "CSV list");
        addField(fieldX, fieldWidth, "UnlockSong Entries", "Rows: key=value,...");
        addField(fieldX, fieldWidth, "UnlockWorld Entries", "Rows: key=value,...");
        addField(fieldX, fieldWidth, "UnlockNether Entries", "Rows: key=value,...");
        addField(fieldX, fieldWidth, "UnlockVoid Entries", "Rows: key=value,...");

        addSection("Level Meta");
        addField(fieldX, fieldWidth, "UID", "Difficulty uid");
        addField(fieldX, fieldWidth, "Initial Arena", "Starting arena id");
        addField(fieldX, fieldWidth, "Offset(ms)", "Audio offset ms");
        addField(fieldX, fieldWidth, "Level", "Displayed level");
        addField(fieldX, fieldWidth, "Charters(csv)", "CSV list");
        addField(fieldX, fieldWidth, "Meta Comments(csv)", "CSV list");
        addField(fieldX, fieldWidth, "BPM List", "beat:bpm,beat:bpm");

        populateFields();

        int buttonY = height - PADDING - BUTTON_HEIGHT;
        int buttonX = leftPanelX;
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> apply())
                .dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        buttonX += BUTTON_WIDTH + GAP;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
            populateFields();
            status = "Reset to current values";
        }).dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        buttonX += BUTTON_WIDTH + GAP;
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void addSection(String title) {
        rows.add(new FieldRow(title, null, null, true));
    }

    private void addField(int x, int width, String label, String hint) {
        TextFieldWidget widget = addDrawableChild(new TextFieldWidget(textRenderer, x, 0, width, 18, Text.literal(label)));
        widget.setMaxLength(4096);
        rows.add(new FieldRow(label, hint, widget, false));
    }

    private FieldRow field(String label) {
        for (FieldRow row : rows) {
            if (!row.section && row.label.equals(label)) {
                return row;
            }
        }
        return null;
    }

    private void set(String label, String value) {
        FieldRow row = field(label);
        if (row != null) {
            row.widget.setText(value);
        }
    }

    private String get(String label) {
        FieldRow row = field(label);
        return row == null ? "" : row.widget.getText();
    }

    private void populateFields() {
        SongManifestData manifest = state.project().manifest();
        set("Name", manifest.name());
        set("Composer", manifest.composer());
        set("Icon", manifest.icon());
        set("Alias", manifest.alias());
        set("Length(ms)", Integer.toString(manifest.length()));
        set("SHA1", manifest.respackSha1());
        set("Key", manifest.key());
        set("Description", manifest.description());
        set("Song ID", Integer.toString(manifest.songId()));
        set("Version", manifest.version());
        set("Beats Per Bar", Integer.toString(manifest.beatsPerBar()));
        set("Comments(csv)", joinCsv(manifest.comments()));
        set("PlayerAlias(csv)", joinCsv(manifest.playerAlias()));
        set("Tags(csv)", joinCsv(manifest.tags()));
        set("UnlockSong Entries", serializeMapEntries(manifest.unlockSong()));
        set("UnlockWorld Entries", serializeMapEntries(manifest.unlockWorld()));
        set("UnlockNether Entries", serializeMapEntries(manifest.unlockNether()));
        set("UnlockVoid Entries", serializeMapEntries(manifest.unlockVoid()));

        MetaData meta = state.level().meta();
        set("UID", Integer.toString(meta.uid()));
        set("Initial Arena", meta.initialArena());
        set("Offset(ms)", Long.toString(meta.offset()));
        set("Level", format(meta.level()));
        set("Charters(csv)", joinCsv(meta.charters()));
        set("Meta Comments(csv)", joinCsv(meta.comments()));
        set("BPM List", serializeBpmList(meta.bpms()));
    }

    private void apply() {
        try {
            SongManifestData manifest = state.project().manifest();
            manifest.setName(get("Name"));
            manifest.setComposer(get("Composer"));
            manifest.setIcon(get("Icon"));
            manifest.setAlias(get("Alias"));
            manifest.setLength(parseInt(get("Length(ms)")));
            manifest.setRespackSha1(get("SHA1"));
            manifest.setKey(get("Key"));
            manifest.setDescription(get("Description"));
            manifest.setSongId(parseInt(get("Song ID")));
            manifest.setVersion(get("Version"));
            manifest.setBeatsPerBar(parseInt(get("Beats Per Bar")));
            replaceStrings(manifest.comments(), splitCsv(get("Comments(csv)")));
            replaceStrings(manifest.playerAlias(), splitCsv(get("PlayerAlias(csv)")));
            replaceStrings(manifest.tags(), splitCsv(get("Tags(csv)")));
            replaceMaps(manifest.unlockSong(), parseMapEntries(get("UnlockSong Entries")));
            replaceMaps(manifest.unlockWorld(), parseMapEntries(get("UnlockWorld Entries")));
            replaceMaps(manifest.unlockNether(), parseMapEntries(get("UnlockNether Entries")));
            replaceMaps(manifest.unlockVoid(), parseMapEntries(get("UnlockVoid Entries")));

            MetaData meta = state.level().meta();
            meta.setUid(parseInt(get("UID")));
            meta.setInitialArena(get("Initial Arena"));
            meta.setOffset(parseLong(get("Offset(ms)")));
            meta.setLevel(parseDouble(get("Level")));
            replaceStrings(meta.charters(), splitCsv(get("Charters(csv)")));
            replaceStrings(meta.comments(), splitCsv(get("Meta Comments(csv)")));
            replaceBpms(meta.bpms(), parseBpmList(get("BPM List")));

            state.sortCurrentLevel();
            state.markDirty();
            status = "Applied meta changes";
        } catch (RuntimeException exception) {
            status = "Apply failed: " + exception.getMessage();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        context.fill(0, 0, width, height, BG);

        drawLeftPanel(context);
        drawRightPanel(context);

        context.drawText(textRenderer, title, PADDING, PADDING + 4, ACCENT, false);
        if (!status.isEmpty()) {
            context.drawText(textRenderer, Text.literal(status), PADDING + 120, PADDING + 4,
                    status.startsWith("Applied") ? GREEN : WARN, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLeftPanel(DrawContext context) {
        context.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelWidth, leftPanelY + leftPanelHeight, PANEL_BG);
        context.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelWidth, leftPanelY + 1, BORDER);
        context.fill(leftPanelX, leftPanelY, leftPanelX + 1, leftPanelY + leftPanelHeight, BORDER);
        context.fill(leftPanelX + leftPanelWidth - 1, leftPanelY, leftPanelX + leftPanelWidth, leftPanelY + leftPanelHeight, BORDER);
        context.fill(leftPanelX, leftPanelY + leftPanelHeight - 1, leftPanelX + leftPanelWidth, leftPanelY + leftPanelHeight, BORDER);

        int contentY = leftPanelY + GAP - scrollOffset;
        context.enableScissor(leftPanelX + 1, leftPanelY + 1, leftPanelX + leftPanelWidth - 1, leftPanelY + leftPanelHeight - 1);
        for (FieldRow row : rows) {
            if (row.section) {
                contentY += 6;
                context.drawText(textRenderer, Text.literal(row.label), leftPanelX + GAP, contentY, ACCENT, false);
                contentY += 16;
            } else {
                context.drawText(textRenderer, Text.literal(row.label), leftPanelX + GAP, contentY, MUTED, false);
                row.widget.setY(contentY + 10);
                contentY += FIELD_HEIGHT;
            }
        }
        context.disableScissor();
    }

    private void drawRightPanel(DrawContext context) {
        context.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + rightPanelHeight, PANEL_BG);
        context.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + 1, BORDER);
        context.fill(rightPanelX, rightPanelY, rightPanelX + 1, rightPanelY + rightPanelHeight, BORDER);
        context.fill(rightPanelX + rightPanelWidth - 1, rightPanelY, rightPanelX + rightPanelWidth, rightPanelY + rightPanelHeight, BORDER);
        context.fill(rightPanelX, rightPanelY + rightPanelHeight - 1, rightPanelX + rightPanelWidth, rightPanelY + rightPanelHeight, BORDER);

        int x = rightPanelX + GAP;
        int y = rightPanelY + GAP;
        context.drawText(textRenderer, Text.literal("In-Game Lore Preview"), x, y, ACCENT, false);
        y += 18;

        List<Text> lore = buildLore();
        context.enableScissor(rightPanelX + 1, rightPanelY + 1, rightPanelX + rightPanelWidth - 1, rightPanelY + rightPanelHeight - 1);
        for (Text line : lore) {
            context.drawText(textRenderer, line, x, y, TEXT, false);
            y += LORE_LINE_HEIGHT;
        }
        context.disableScissor();
    }

    private List<Text> buildLore() {
        List<String> templates = List.of(
                "&7&o{alias}",
                "{comments}",
                "",
                "<#57FF00>* &f难度: &eLv.&a{DIFF}",
                "<#57FF00>* &f谱师: &a{CHARTER}",
                "<#57FF00>* &f曲师: &a{COMPOSER}",
                "<#57FF00>* &f精选集: &a{ALBUM}",
                "<#57FF00>* &fBPM: &a{BPM}",
                " ",
                "&f个人最好成绩",
                "<#00F1FF>{PB}",
                " ",
                "&f游玩次数:",
                "<#00F2FF>{TIMES}",
                " ",
                "&f上次游戏:",
                "<#00F2FF>{LAST}",
                " ",
                "&f热度: ",
                "<#00FFA3>{TOTAL_ALL}",
                "",
                "&6&l排行榜",
                "{RANK_1}",
                "{RANK_2}",
                "{RANK_3}",
                "{RANK_PLAYER}"
        );

        String composer = get("Composer").trim();
        if (composer.isEmpty()) {
            composer = "未知作曲";
        }
        String levelText = get("Level").trim();
        if (levelText.isEmpty()) {
            levelText = "?";
        }
        String charters = get("Charters(csv)").trim();
        if (charters.isEmpty()) {
            charters = "N/A";
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("alias", get("Alias").trim());
        placeholders.put("COMPOSER", composer);
        placeholders.put("DIFF", levelText);
        placeholders.put("CHARTER", charters);
        placeholders.put("ALBUM", "未知精选集");
        placeholders.put("BPM", previewBpmLabel());
        placeholders.put("PB", "-");
        placeholders.put("TIMES", "-");
        placeholders.put("LAST", "-");
        placeholders.put("TOTAL_ALL", "-");
        placeholders.put("RANK_1", "-");
        placeholders.put("RANK_2", "-");
        placeholders.put("RANK_3", "-");
        placeholders.put("RANK_PLAYER", "-");

        Map<String, List<String>> linePlaceholders = new LinkedHashMap<>();
        List<String> comments = splitCsv(get("Comments(csv)"));
        List<String> commentLines = comments.isEmpty() ? List.of() : comments.stream().map(c -> "&7" + c).toList();
        linePlaceholders.put("comments", commentLines);

        List<Text> result = new ArrayList<>();
        for (String template : templates) {
            result.addAll(renderLoreTemplateLine(template, placeholders, linePlaceholders));
        }
        return result;
    }

    private List<Text> renderLoreTemplateLine(String template,
                                                  Map<String, String> placeholders,
                                                  Map<String, List<String>> linePlaceholders) {
        if (template == null) {
            return List.of();
        }

        List<String> expanded = new ArrayList<>();
        expanded.add(template);

        Matcher matcher = BRACED_PLACEHOLDER_PATTERN.matcher(template);
        java.util.LinkedHashSet<String> listKeys = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (linePlaceholders.containsKey(key)) {
                listKeys.add(key);
            }
        }

        for (String key : listKeys) {
            List<String> values = linePlaceholders.get(key);
            if (values == null || values.isEmpty()) {
                return List.of();
            }
            List<String> next = new ArrayList<>();
            for (String current : expanded) {
                for (String value : values) {
                    next.add(replacePlaceholder(current, key, value));
                }
            }
            expanded = next;
        }

        List<Text> rendered = new ArrayList<>();
        for (String current : expanded) {
            if (shouldHideLoreLine(current, placeholders)) {
                continue;
            }
            String resolved = replacePlaceholders(current, placeholders);
            if (BRACED_PLACEHOLDER_PATTERN.matcher(current).find() && resolved.isBlank()) {
                continue;
            }
            rendered.add(formatLine(resolved));
        }
        return rendered;
    }

    private boolean shouldHideLoreLine(String template, Map<String, String> placeholders) {
        Matcher matcher = BRACED_PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (placeholders.containsKey(key) && Objects.toString(placeholders.get(key), "").isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty() || placeholders.isEmpty()) {
            return text;
        }
        Matcher matcher = BRACED_PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!placeholders.containsKey(key)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(Objects.toString(placeholders.get(key), "")));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replacePlaceholder(String text, String key, String value) {
        return text.replace("{" + key + "}", Objects.toString(value, ""));
    }

    private Text formatLine(String line) {
        line = line.replace('&', '§');

        net.minecraft.text.MutableText result = Text.literal("");
        Style currentStyle = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '§' && i + 1 < line.length()) {
                if (!buffer.isEmpty()) {
                    result.append(Text.literal(buffer.toString()).setStyle(currentStyle));
                    buffer.setLength(0);
                }
                char code = line.charAt(i + 1);
                currentStyle = applyCode(currentStyle, code);
                i += 2;
                continue;
            }
            if (c == '<' && i + 8 <= line.length()) {
                String maybe = line.substring(i, i + 8);
                Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(maybe);
                if (hexMatcher.matches()) {
                    if (!buffer.isEmpty()) {
                        result.append(Text.literal(buffer.toString()).setStyle(currentStyle));
                        buffer.setLength(0);
                    }
                    int rgb = Integer.parseInt(hexMatcher.group(1), 16);
                    currentStyle = currentStyle.withColor(rgb);
                    i += 8;
                    continue;
                }
            }
            buffer.append(c);
            i++;
        }
        if (!buffer.isEmpty()) {
            result.append(Text.literal(buffer.toString()).setStyle(currentStyle));
        }
        return result;
    }

    private Style applyCode(Style style, char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> style.withColor(Formatting.BLACK);
            case '1' -> style.withColor(Formatting.DARK_BLUE);
            case '2' -> style.withColor(Formatting.DARK_GREEN);
            case '3' -> style.withColor(Formatting.DARK_AQUA);
            case '4' -> style.withColor(Formatting.DARK_RED);
            case '5' -> style.withColor(Formatting.DARK_PURPLE);
            case '6' -> style.withColor(Formatting.GOLD);
            case '7' -> style.withColor(Formatting.GRAY);
            case '8' -> style.withColor(Formatting.DARK_GRAY);
            case '9' -> style.withColor(Formatting.BLUE);
            case 'a' -> style.withColor(Formatting.GREEN);
            case 'b' -> style.withColor(Formatting.AQUA);
            case 'c' -> style.withColor(Formatting.RED);
            case 'd' -> style.withColor(Formatting.LIGHT_PURPLE);
            case 'e' -> style.withColor(Formatting.YELLOW);
            case 'f' -> style.withColor(Formatting.WHITE);
            case 'k' -> style.withObfuscated(true);
            case 'l' -> style.withBold(true);
            case 'm' -> style.withStrikethrough(true);
            case 'n' -> style.withUnderline(true);
            case 'o' -> style.withItalic(true);
            case 'r' -> Style.EMPTY;
            default -> style;
        };
    }

    private String previewBpmLabel() {
        List<BpmPoint> bpms = parseBpmList(get("BPM List"));
        if (bpms.isEmpty()) {
            return "?";
        }
        if (bpms.size() == 1) {
            return formatBpm(bpms.get(0).bpm());
        }
        double min = bpms.stream().mapToDouble(BpmPoint::bpm).min().orElse(0);
        double max = bpms.stream().mapToDouble(BpmPoint::bpm).max().orElse(0);
        if (Math.abs(min - max) < 0.0001) {
            return formatBpm(min);
        }
        return formatBpm(min) + " / " + formatBpm(max);
    }

    private String formatBpm(double bpm) {
        if (Math.abs(bpm - Math.rint(bpm)) < 0.0001d) {
            return String.valueOf((long) Math.rint(bpm));
        }
        return String.format(Locale.US, "%.2f", bpm).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInside(mouseX, mouseY, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight)) {
            scrollOffset -= (int) verticalAmount * 24;
            int maxScroll = Math.max(0, rows.size() * FIELD_HEIGHT - leftPanelHeight + GAP * 2);
            scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == net.minecraft.client.util.InputUtil.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void close() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(parent);
            if (parent instanceof ChartEditorScreen editor) {
                editor.refreshAfterMetaEdit();
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean isInside(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    private static String joinCsv(List<String> values) {
        return String.join(",", values);
    }

    private static List<String> splitCsv(String text) {
        List<String> values = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return values;
        }
        for (String token : text.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String serializeMapEntries(List<Map<String, Object>> entries) {
        List<String> rows = new ArrayList<>();
        for (Map<String, Object> map : entries) {
            List<String> pairs = new ArrayList<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                pairs.add(entry.getKey() + "=" + entry.getValue());
            }
            rows.add(String.join(",", pairs));
        }
        return String.join(" | ", rows);
    }

    private static List<Map<String, Object>> parseMapEntries(String text) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return list;
        }
        String[] rows = text.split("\\|");
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            for (String pair : trimmed.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        return list;
    }

    private static String serializeBpmList(List<BpmPoint> bpms) {
        List<String> parts = new ArrayList<>();
        for (BpmPoint point : bpms) {
            parts.add(format(point.beat()) + ":" + format(point.bpm()));
        }
        return String.join(",", parts);
    }

    private static List<BpmPoint> parseBpmList(String text) {
        List<BpmPoint> list = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return list;
        }
        for (String token : text.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":", 2);
            if (parts.length == 2) {
                list.add(new BpmPoint(parseDouble(parts[0]), parseDouble(parts[1])));
            }
        }
        return list;
    }

    private static void replaceStrings(List<String> target, List<String> source) {
        target.clear();
        target.addAll(source);
    }

    private static void replaceMaps(List<Map<String, Object>> target, List<Map<String, Object>> source) {
        target.clear();
        target.addAll(source);
    }

    private static void replaceBpms(List<BpmPoint> target, List<BpmPoint> source) {
        target.clear();
        target.addAll(source);
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    private static long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

}
