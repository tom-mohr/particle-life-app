package com.particle_life.app;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;

public class ImGuiUtils {

    /**
     * Helper to display a little (?) mark which shows a tooltip when hovered.
     *
     * @param text will be displayed as tooltip when hovered
     */
    static void helpMarker(String text) {
        helpMarker("(?)", text);
    }

    /**
     * Helper to display disabled text which shows a tooltip when hovered.
     *
     * @param title will always be displayed
     * @param text  will be displayed as tooltip when hovered
     */
    static void helpMarker(String title, String text) {
        ImGui.textDisabled(title);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0f);
            ImGui.textUnformatted(text);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
        }
    }

    static void advancedGuiHint() {
        helpMarker("[Advanced GUI]", "Some options are only visible if \"Advanced GUI\" is activated. The \"Advanced GUI\" can be enabled via Menu > View > Advanced GUI.");
    }

    static void setImGuiStyle() {
        // Deep Dark stylejanekb04 from ImThemes
        ImGuiStyle style = ImGui.getStyle();

        style.setAlpha(1.0f);
        style.setDisabledAlpha(0.6000000238418579f);
        style.setWindowPadding(8.0f, 8.0f);
        style.setWindowRounding(7.0f);
        style.setWindowBorderSize(1.0f);
        style.setWindowMinSize(32.0f, 32.0f);
        style.setWindowTitleAlign(0.0f, 0.5f);
        style.setWindowMenuButtonPosition(ImGuiDir.Left);
        style.setChildRounding(4.0f);
        style.setChildBorderSize(1.0f);
        style.setPopupRounding(4.0f);
        style.setPopupBorderSize(1.0f);
        style.setFramePadding(5.0f, 2.0f);
        style.setFrameRounding(3.0f);
        style.setFrameBorderSize(1.0f);
        style.setItemSpacing(6.0f, 6.0f);
        style.setItemInnerSpacing(6.0f, 6.0f);
        style.setCellPadding(6.0f, 6.0f);
        style.setIndentSpacing(25.0f);
        style.setColumnsMinSpacing(6.0f);
        style.setScrollbarSize(15.0f);
        style.setScrollbarRounding(9.0f);
        style.setGrabMinSize(10.0f);
        style.setGrabRounding(3.0f);
        style.setTabRounding(4.0f);
        style.setTabBorderSize(1.0f);
        style.setTabMinWidthForCloseButton(0.0f);
        style.setColorButtonPosition(ImGuiDir.Right);
        style.setButtonTextAlign(0.5f, 0.5f);
        style.setSelectableTextAlign(0.0f, 0.0f);

        float[][] colors = style.getColors();

        colors[ImGuiCol.Text] = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        colors[ImGuiCol.TextDisabled] = new float[]{0.4980392158031464f, 0.4980392158031464f, 0.4980392158031464f, 1.0f};
        colors[ImGuiCol.WindowBg] = new float[]{0.09803921729326248f, 0.09803921729326248f, 0.09803921729326248f, 1.0f};
        colors[ImGuiCol.ChildBg] = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        colors[ImGuiCol.PopupBg] = new float[]{0.1882352977991104f, 0.1882352977991104f, 0.1882352977991104f, 0.9200000166893005f};
        colors[ImGuiCol.Border] = new float[]{0.1882352977991104f, 0.1882352977991104f, 0.1882352977991104f, 0.2899999916553497f};
        colors[ImGuiCol.BorderShadow] = new float[]{0.0f, 0.0f, 0.0f, 0.239999994635582f};
        colors[ImGuiCol.FrameBg] = new float[]{0.0470588244497776f, 0.0470588244497776f, 0.0470588244497776f, 0.5400000214576721f};
        colors[ImGuiCol.FrameBgHovered] = new float[]{0.1882352977991104f, 0.1882352977991104f, 0.1882352977991104f, 0.5400000214576721f};
        colors[ImGuiCol.FrameBgActive] = new float[]{0.2000000029802322f, 0.2196078449487686f, 0.2274509817361832f, 1.0f};
        colors[ImGuiCol.TitleBg] = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.TitleBgActive] = new float[]{0.05882352963089943f, 0.05882352963089943f, 0.05882352963089943f, 1.0f};
        colors[ImGuiCol.TitleBgCollapsed] = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.MenuBarBg] = new float[]{0.1372549086809158f, 0.1372549086809158f, 0.1372549086809158f, 1.0f};
        colors[ImGuiCol.ScrollbarBg] = new float[]{0.0470588244497776f, 0.0470588244497776f, 0.0470588244497776f, 0.5400000214576721f};
        colors[ImGuiCol.ScrollbarGrab] = new float[]{0.3372549116611481f, 0.3372549116611481f, 0.3372549116611481f, 0.5400000214576721f};
        colors[ImGuiCol.ScrollbarGrabHovered] = new float[]{0.4000000059604645f, 0.4000000059604645f, 0.4000000059604645f, 0.5400000214576721f};
        colors[ImGuiCol.ScrollbarGrabActive] = new float[]{0.5568627715110779f, 0.5568627715110779f, 0.5568627715110779f, 0.5400000214576721f};
        colors[ImGuiCol.CheckMark] = new float[]{0.3294117748737335f, 0.6666666865348816f, 0.8588235378265381f, 1.0f};
        colors[ImGuiCol.SliderGrab] = new float[]{0.3372549116611481f, 0.3372549116611481f, 0.3372549116611481f, 0.5400000214576721f};
        colors[ImGuiCol.SliderGrabActive] = new float[]{0.5568627715110779f, 0.5568627715110779f, 0.5568627715110779f, 0.5400000214576721f};
        colors[ImGuiCol.Button] = new float[]{0.0470588244497776f, 0.0470588244497776f, 0.0470588244497776f, 0.5400000214576721f};
        colors[ImGuiCol.ButtonHovered] = new float[]{0.1882352977991104f, 0.1882352977991104f, 0.1882352977991104f, 0.5400000214576721f};
        colors[ImGuiCol.ButtonActive] = new float[]{0.2000000029802322f, 0.2196078449487686f, 0.2274509817361832f, 1.0f};
        colors[ImGuiCol.Header] = new float[]{0.0f, 0.0f, 0.0f, 0.5199999809265137f};
        colors[ImGuiCol.HeaderHovered] = new float[]{0.0f, 0.0f, 0.0f, 0.3600000143051147f};
        colors[ImGuiCol.HeaderActive] = new float[]{0.2000000029802322f, 0.2196078449487686f, 0.2274509817361832f, 0.3300000131130219f};
        colors[ImGuiCol.Separator] = new float[]{0.2784313857555389f, 0.2784313857555389f, 0.2784313857555389f, 0.2899999916553497f};
        colors[ImGuiCol.SeparatorHovered] = new float[]{0.4392156898975372f, 0.4392156898975372f, 0.4392156898975372f, 0.2899999916553497f};
        colors[ImGuiCol.SeparatorActive] = new float[]{0.4000000059604645f, 0.4392156898975372f, 0.4666666686534882f, 1.0f};
        colors[ImGuiCol.ResizeGrip] = new float[]{0.2784313857555389f, 0.2784313857555389f, 0.2784313857555389f, 0.2899999916553497f};
        colors[ImGuiCol.ResizeGripHovered] = new float[]{0.4392156898975372f, 0.4392156898975372f, 0.4392156898975372f, 0.2899999916553497f};
        colors[ImGuiCol.ResizeGripActive] = new float[]{0.4000000059604645f, 0.4392156898975372f, 0.4666666686534882f, 1.0f};
        colors[ImGuiCol.Tab] = new float[]{0.0f, 0.0f, 0.0f, 0.5199999809265137f};
        colors[ImGuiCol.TabHovered] = new float[]{0.1372549086809158f, 0.1372549086809158f, 0.1372549086809158f, 1.0f};
        colors[ImGuiCol.TabActive] = new float[]{0.2000000029802322f, 0.2000000029802322f, 0.2000000029802322f, 0.3600000143051147f};
        colors[ImGuiCol.TabUnfocused] = new float[]{0.0f, 0.0f, 0.0f, 0.5199999809265137f};
        colors[ImGuiCol.TabUnfocusedActive] = new float[]{0.1372549086809158f, 0.1372549086809158f, 0.1372549086809158f, 1.0f};
        colors[ImGuiCol.PlotLines] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.PlotLinesHovered] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.PlotHistogram] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.PlotHistogramHovered] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.TableHeaderBg] = new float[]{0.0f, 0.0f, 0.0f, 0.5199999809265137f};
        colors[ImGuiCol.TableBorderStrong] = new float[]{0.0f, 0.0f, 0.0f, 0.5199999809265137f};
        colors[ImGuiCol.TableBorderLight] = new float[]{0.2784313857555389f, 0.2784313857555389f, 0.2784313857555389f, 0.2899999916553497f};
        colors[ImGuiCol.TableRowBg] = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
        colors[ImGuiCol.TableRowBgAlt] = new float[]{1.0f, 1.0f, 1.0f, 0.05999999865889549f};
        colors[ImGuiCol.TextSelectedBg] = new float[]{0.2000000029802322f, 0.2196078449487686f, 0.2274509817361832f, 1.0f};
        colors[ImGuiCol.DragDropTarget] = new float[]{0.3294117748737335f, 0.6666666865348816f, 0.8588235378265381f, 1.0f};
        colors[ImGuiCol.NavHighlight] = new float[]{1.0f, 0.0f, 0.0f, 1.0f};
        colors[ImGuiCol.NavWindowingHighlight] = new float[]{1.0f, 0.0f, 0.0f, 0.699999988079071f};
        colors[ImGuiCol.NavWindowingDimBg] = new float[]{1.0f, 0.0f, 0.0f, 0.2000000029802322f};
        colors[ImGuiCol.ModalWindowDimBg] = new float[]{1.0f, 0.0f, 0.0f, 0.3499999940395355f};
    }
}
