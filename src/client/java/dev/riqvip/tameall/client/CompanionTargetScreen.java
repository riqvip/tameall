package dev.riqvip.tameall.client;

import dev.riqvip.tameall.companion.TargetCatalog;
import dev.riqvip.tameall.companion.TargetSelection;
import dev.riqvip.tameall.network.CompanionTargetCatalogPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Compact, searchable and hierarchical include/exclude editor. */
public final class CompanionTargetScreen extends Screen {
    private static final int MIN_WIDTH = 360;
    private static final int MAX_WIDTH = 560;
    private static final int MIN_HEIGHT = 220;
    private static final int MAX_HEIGHT = 390;
    private static final int ROW_HEIGHT = 22;

    private final CompanionTargetCatalogPayload payload;
    private final LinkedHashSet<String> initialIncludes;
    private final LinkedHashSet<String> initialExcludes;
    private final LinkedHashSet<String> includes;
    private final LinkedHashSet<String> excludes;
    private final Set<String> collapsed = new HashSet<>();
    private EditBox search;
    private String searchValue = "";
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int rowTop;
    private int visibleRows;
    private int scrollOffset;
    private boolean scrollbarDragging;
    private boolean saving;
    private boolean showDiscardPrompt;
    private String message;
    private boolean messageError;

    public CompanionTargetScreen(CompanionTargetCatalogPayload payload) {
        super(Component.translatable("screen.tameall.targets"));
        this.payload = payload;
        this.initialIncludes = new LinkedHashSet<>(payload.selection().includes());
        this.initialExcludes = new LinkedHashSet<>(payload.selection().excludes());
        this.includes = new LinkedHashSet<>(initialIncludes);
        this.excludes = new LinkedHashSet<>(initialExcludes);
        collapsed.add("advanced");
    }

    @Override
    protected void init() {
        panelWidth = Math.clamp(width - 8, MIN_WIDTH, MAX_WIDTH);
        panelHeight = Math.clamp(height - 8, MIN_HEIGHT, MAX_HEIGHT);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowTop = top + 75;
        visibleRows = Math.max(4, (panelHeight - 119) / ROW_HEIGHT);
        String oldSearch = searchValue;
        boolean focused = search != null && search.isFocused();
        clearWidgets();
        search = new EditBox(font, left + 12, top + 36, panelWidth - 24, 20,
                Component.translatable("tameall.targets.search"));
        search.setMaxLength(128);
        search.setTextShadow(false);
        search.setTextColor(CompanionUiStyle.TEXT);
        search.setTextColorUneditable(CompanionUiStyle.MUTED_TEXT);
        search.setHint(Component.literal("Search groups, names, or IDs"));
        search.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                "Filter the target catalog by group, mob name, or entity ID.")));
        search.setValue(oldSearch);
        search.setResponder(value -> {
            searchValue = value;
            scrollOffset = 0;
            rebuildRows();
        });
        addRenderableWidget(search);
        if (focused) {
            search.setFocused(true);
            setFocused(search);
        }
        rebuildRows();
    }

    private void rebuildRows() {
        List<Row> rows = rows();
        int max = Math.max(0, rows.size() - visibleRows);
        scrollOffset = Math.clamp(scrollOffset, 0, max);
        boolean focused = search != null && search.isFocused();
        // Keep the existing search box and its value while replacing only row controls.
        if (children().size() > 1) {
            clearWidgets();
            addRenderableWidget(search);
        }
        int end = Math.min(rows.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            Row row = rows.get(index);
            int y = rowTop + (index - scrollOffset) * ROW_HEIGHT;
            if (row.header) {
                int headerWidth = panelWidth - (selectableHeader(row) ? 110 : 24);
                DarkButton collapse = addButton(Component.literal(row.expanded ? "▼ " + row.label : "▶ " + row.label),
                        left + 12, y, headerWidth, 20, () -> toggleCollapsed(row.id));
                collapse.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        row.expanded ? "Collapse this section." : "Expand this section.")));
                if (selectableHeader(row)) {
                    DarkButton state = addButton(Component.literal(state(row.id)), left + panelWidth - 92, y, 78, 20,
                            () -> cycle(row.id));
                    state.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                            "Cycle this group between Off, Include, and Exclude.")));
                }
            } else {
                DarkButton state = addButton(Component.literal(state(row.id)), left + panelWidth - 92, y, 78, 20,
                        () -> cycle(row.id));
                state.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                        "Cycle this mob between Off, Include, and Exclude.")));
            }
        }
        int footerY = top + panelHeight - 28;
        if (showDiscardPrompt) {
            DarkButton promptSave = addButton(Component.translatable(saving ? "tameall.saving" : "tameall.save"),
                    left + 12, footerY, 82, 20, this::save);
            promptSave.active = dirty() && !saving;
            promptSave.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    "Save the target changes and close this editor.")));
            addButton(Component.translatable("tameall.discard"), left + 98, footerY, 82, 20, this::discard)
                    .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                            "Discard the unsaved target changes.")));
            addButton(Component.translatable("tameall.keep_editing"), left + 184, footerY, 104, 20, this::keepEditing)
                    .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                            "Close this prompt and keep editing.")));
        } else {
            DarkButton save = addButton(Component.translatable(saving ? "tameall.saving" : "tameall.save"),
                    left + panelWidth - 176, footerY, 78, 20, this::save);
            save.active = dirty() && !saving;
            save.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                    "Save the selected target groups and mobs.")));
            addButton(Component.translatable("tameall.cancel"), left + panelWidth - 92, footerY, 78, 20, this::cancel)
                    .setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(
                            "Close without saving target changes.")));
        }
        if (focused) {
            search.setFocused(true);
            setFocused(search);
        }
    }

    private List<Row> rows() {
        String query = searchValue.trim().toLowerCase(Locale.ROOT);
        Map<String, TargetCatalog.Entry> byId = new HashMap<>();
        for (TargetCatalog.Entry entry : payload.entries()) byId.putIfAbsent(entry.id(), entry);
        List<Row> result = new ArrayList<>();
        addSelectedSection(result, "excluded", "Excluded", excludes, byId, query);
        addSelectedSection(result, "included", "Included", includes, byId, query);

        List<TargetCatalog.Entry> groups = payload.entries().stream()
                .filter(entry -> "group".equals(entry.kind()))
                .toList();
        for (TargetCatalog.Entry group : groups) {
            List<TargetCatalog.Entry> children = childrenFor(group.id(), query);
            if (!query.isEmpty() && children.isEmpty() && !matches(group, query)) continue;
            boolean expanded = !collapsed.contains(group.id());
            result.add(Row.header(group.id(), group.label(), expanded));
            if (expanded) for (TargetCatalog.Entry child : children) result.add(Row.entry(child));
        }
        addCategory(result, "uncategorized", "Uncategorized", query);
        addCategory(result, "advanced", "Advanced tags", query);
        List<TargetCatalog.Entry> unavailable = payload.entries().stream()
                .filter(entry -> "unavailable".equals(entry.kind()))
                .filter(entry -> query.isEmpty() || matches(entry, query))
                .toList();
        if (!unavailable.isEmpty()) {
            boolean expanded = !collapsed.contains("unavailable");
            result.add(Row.header("unavailable", "Unavailable saved selections", expanded));
            if (expanded) for (TargetCatalog.Entry entry : unavailable) result.add(Row.entry(entry));
        }
        return result;
    }

    private void addSelectedSection(List<Row> rows, String id, String label, Set<String> selected,
                                    Map<String, TargetCatalog.Entry> byId, String query) {
        List<TargetCatalog.Entry> entries = selected.stream().map(byId::get)
                .filter(entry -> entry != null && (query.isEmpty() || matches(entry, query))).toList();
        if (entries.isEmpty()) return;
        boolean expanded = !collapsed.contains(id);
        rows.add(Row.header(id, label, expanded));
        if (expanded) for (TargetCatalog.Entry entry : entries) rows.add(Row.entry(entry));
    }

    private void addCategory(List<Row> rows, String category, String label, String query) {
        List<TargetCatalog.Entry> entries = payload.entries().stream()
                .filter(entry -> ("advanced".equals(category) ? "tag".equals(entry.kind()) : "type".equals(entry.kind()))
                        && entry.categories().contains(category))
                .filter(entry -> query.isEmpty() || matches(entry, query)).toList();
        if (entries.isEmpty()) return;
        boolean expanded = !collapsed.contains(category);
        rows.add(Row.header(category, label, expanded));
        if (expanded) for (TargetCatalog.Entry entry : entries) rows.add(Row.entry(entry));
    }

    private List<TargetCatalog.Entry> childrenFor(String groupId, String query) {
        return payload.entries().stream()
                .filter(entry -> "type".equals(entry.kind()) && entry.categories().contains(groupId))
                .filter(entry -> query.isEmpty() || matches(entry, query))
                .toList();
    }

    private boolean matches(TargetCatalog.Entry entry, String query) {
        return entry.id().toLowerCase(Locale.ROOT).contains(query)
                || entry.label().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean selectableHeader(Row row) {
        return row.header && row.id.startsWith("group:");
    }

    private void toggleCollapsed(String id) {
        if (!collapsed.add(id)) collapsed.remove(id);
        rebuildRows();
    }

    private void cycle(String id) {
        if (includes.remove(id)) excludes.add(id);
        else if (excludes.remove(id)) { }
        else includes.add(id);
        message = null;
        rebuildRows();
    }

    private String state(String id) {
        if (excludes.contains(id)) return "Exclude";
        if (includes.contains(id)) return "Include";
        return "Off";
    }

    private boolean dirty() {
        return !initialIncludes.equals(includes) || !initialExcludes.equals(excludes);
    }

    private void save() {
        if (!dirty() || saving) return;
        showDiscardPrompt = false;
        saving = true;
        message = "Waiting for server confirmation...";
        messageError = false;
        rebuildRows();
        ClientPlayNetworking.send(new CompanionTargetSelectionPayload(payload.bondId(), payload.revision(),
                new TargetSelection(Set.copyOf(includes), Set.copyOf(excludes))));
    }

    private void cancel() {
        if (dirty()) {
            showDiscardPrompt = true;
            message = null;
            messageError = false;
            rebuildRows();
            return;
        }
        CompanionScreens.openLatest();
    }

    private void discard() {
        includes.clear();
        includes.addAll(initialIncludes);
        excludes.clear();
        excludes.addAll(initialExcludes);
        showDiscardPrompt = false;
        message = null;
        messageError = false;
        CompanionScreens.openLatest();
    }

    private void keepEditing() {
        showDiscardPrompt = false;
        message = null;
        messageError = false;
        rebuildRows();
    }

    public void acceptResult(CompanionTargetSelectionResultPayload result) {
        if (!payload.bondId().equals(result.bondId())) return;
        saving = false;
        message = result.message();
        messageError = !result.success();
        if (result.success()) {
            initialIncludes.clear(); initialIncludes.addAll(includes);
            initialExcludes.clear(); initialExcludes.addAll(excludes);
            CompanionScreens.openLatest();
        } else rebuildRows();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (!dirty()) {
                CompanionScreens.openLatest();
            } else {
                showDiscardPrompt = true;
                message = null;
                messageError = false;
                rebuildRows();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left && mouseX <= left + panelWidth && mouseY >= rowTop
                && mouseY <= top + panelHeight - 34 && scrollY != 0.0D) {
            int max = Math.max(0, rows().size() - visibleRows);
            scrollOffset = Math.clamp(scrollOffset + (scrollY < 0.0D ? 1 : -1), 0, max);
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        int trackX = left + panelWidth - 8;
        if (button == 0 && mouseX >= trackX - 3 && mouseX <= trackX + 3
                && mouseY >= rowTop && mouseY <= top + panelHeight - 34) {
            scrollbarDragging = true;
            updateScroll(mouseY);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbarDragging) {
            updateScroll(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        scrollbarDragging = false;
        return super.mouseReleased(event);
    }

    private void updateScroll(double mouseY) {
        int max = Math.max(0, rows().size() - visibleRows);
        int trackTop = rowTop;
        int trackHeight = Math.max(1, top + panelHeight - 34 - trackTop);
        scrollOffset = Math.clamp((int) ((mouseY - trackTop) * max / (double) trackHeight), 0, max);
        rebuildRows();
    }

    private DarkButton addButton(Component label, int x, int y, int buttonWidth, int buttonHeight, Runnable action) {
        DarkButton button = new DarkButton(font, x, y, buttonWidth, buttonHeight, label, ignored -> action.run());
        addRenderableWidget(button);
        return button;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        CompanionUiStyle.panel(g, left, top, panelWidth, panelHeight);
        g.text(font, title, left + 12, top + 12, CompanionUiStyle.TEXT, false);
        g.text(font, Component.literal("Include and exclude groups or individual living mobs."),
                left + 12, top + 24, CompanionUiStyle.MUTED_TEXT, false);
        List<Row> rows = rows();
        int end = Math.min(rows.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            Row row = rows.get(index);
            if (row.header) continue;
            int y = rowTop + (index - scrollOffset) * ROW_HEIGHT + 5;
            int stateLeft = left + panelWidth - 92;
            int idRight = stateLeft - 8;
            int idLeft = left + Math.max(120, panelWidth - 260);
            int idWidth = Math.max(0, idRight - idLeft);
            String id = font.plainSubstrByWidth(row.id, idWidth);
            int labelWidth = Math.max(70, idLeft - (left + 26));
            g.text(font, Component.literal(font.plainSubstrByWidth(row.label, labelWidth)),
                    left + 18, y, CompanionUiStyle.TEXT, false);
            g.text(font, Component.literal(id), idRight - font.width(id), y,
                    CompanionUiStyle.ID_TEXT, false);
        }
        int max = Math.max(0, rows.size() - visibleRows);
        int trackTop = rowTop;
        int trackBottom = top + panelHeight - 34;
        g.fill(left + panelWidth - 8, trackTop, left + panelWidth - 5, trackBottom, 0xFF4A4A4A);
        if (max > 0) {
            int thumbHeight = Math.max(12, (trackBottom - trackTop) * visibleRows / rows.size());
            int thumbTop = trackTop + (trackBottom - trackTop - thumbHeight) * scrollOffset / max;
            g.fill(left + panelWidth - 9, thumbTop, left + panelWidth - 4, thumbTop + thumbHeight, 0xFFAAAAAA);
        }
        if (showDiscardPrompt) {
            g.text(font, Component.literal("Unsaved changes"), left + 12, top + panelHeight - 45,
                    CompanionUiStyle.ERROR, false);
        } else if (message != null) {
            g.text(font, Component.literal(font.plainSubstrByWidth(message, panelWidth - 190)),
                    left + 12, top + panelHeight - 45,
                    messageError ? CompanionUiStyle.ERROR : CompanionUiStyle.MUTED_TEXT, false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private record Row(String id, String label, boolean header, boolean expanded) {
        static Row header(String id, String label, boolean expanded) { return new Row(id, label, true, expanded); }
        static Row entry(TargetCatalog.Entry entry) { return new Row(entry.id(), entry.label(), false, false); }
    }
}
