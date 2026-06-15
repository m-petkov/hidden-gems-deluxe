package com.github.mpetkov.hiddengemsdeluxe.html;

import org.teavm.jso.JSBody;

/** Low-level browser canvas/window access (bypasses flaky TeaVM resize events). */
public final class WebCanvasSync {

    private static final String CANVAS_ID = "canvas";

    private WebCanvasSync() {
    }

    @JSBody(script = "return Math.max(1, window.innerWidth || document.documentElement.clientWidth || 1);")
    public static native int viewportWidth();

    @JSBody(script = "return Math.max(1, window.innerHeight || document.documentElement.clientHeight || 1);")
    public static native int viewportHeight();

    /**
     * Resizes the canvas drawing buffer to match the viewport.
     * @return true when the buffer size changed
     */
    @JSBody(params = {"canvasId", "width", "height"}, script = ""
            + "var canvas = document.getElementById(canvasId);\n"
            + "if (!canvas) { return false; }\n"
            + "var w = width | 0;\n"
            + "var h = height | 0;\n"
            + "if (w <= 0 || h <= 0) { return false; }\n"
            + "var changed = canvas.width !== w || canvas.height !== h;\n"
            + "if (changed) {\n"
            + "  canvas.width = w;\n"
            + "  canvas.height = h;\n"
            + "}\n"
            + "canvas.style.width = '';\n"
            + "canvas.style.height = '';\n"
            + "canvas.style.display = 'block';\n"
            + "canvas.style.margin = '0';\n"
            + "canvas.style.padding = '0';\n"
            + "canvas.style.border = 'none';\n"
            + "return changed;\n")
    public static native boolean resizeCanvasBuffer(String canvasId, int width, int height);

    public static boolean resizeCanvasBuffer(int width, int height) {
        return resizeCanvasBuffer(CANVAS_ID, width, height);
    }
}
