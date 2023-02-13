package com.particle_life.app;

import imgui.ImGui;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

public class ImGuiStatsFormatter {

    private final ArrayList<Item> items = new ArrayList<>();
    private int maxNameLength;
    private int maxValueLength;

    public void reset() {
        maxNameLength = 0;
        maxValueLength = 0;
    }

    public void start() {
        items.clear();
    }

    public void put(String name, String value) {
        items.add(new Item(name, value));
    }

    public void end() {

        int currentMaxNameLength = getMaxLength(Item::name);
        int currentMaxValueLength = getMaxLength(Item::value);
        maxNameLength = Math.max(maxNameLength, currentMaxNameLength);
        maxValueLength = Math.max(maxValueLength, currentMaxValueLength);

        String template = "%-" + (maxNameLength + 1) + "s %" + maxValueLength + "s";

        for (Item item : items) {
            ImGui.text(String.format(template, item.name + ":", item.value));
        }
    }

    private int getMaxLength(Function<Item, String> attribute) {
        Optional<Integer> max = items.stream().map(attribute).map(String::length).max(Integer::compareTo);
        return max.orElse(0);
    }

    private static record Item(String name, String value) {
    }
}
