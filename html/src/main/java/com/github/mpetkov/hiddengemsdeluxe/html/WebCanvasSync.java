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

    @JSBody(script = "return Math.max(1, window.devicePixelRatio || 1);")
    public static native float devicePixelRatio();

    /**
     * Sets CSS display size and the high-DPI drawing buffer separately.
     * @return true when the buffer size changed
     */
    @JSBody(params = {"canvasId", "cssW", "cssH", "bufferW", "bufferH"}, script = ""
            + "var canvas = document.getElementById(canvasId);\n"
            + "if (!canvas) { return false; }\n"
            + "var cssW = cssW | 0;\n"
            + "var cssH = cssH | 0;\n"
            + "var bufferW = bufferW | 0;\n"
            + "var bufferH = bufferH | 0;\n"
            + "if (cssW <= 0 || cssH <= 0 || bufferW <= 0 || bufferH <= 0) { return false; }\n"
            + "var changed = canvas.width !== bufferW || canvas.height !== bufferH;\n"
            + "canvas.width = bufferW;\n"
            + "canvas.height = bufferH;\n"
            + "canvas.style.width = cssW + 'px';\n"
            + "canvas.style.height = cssH + 'px';\n"
            + "canvas.style.display = 'block';\n"
            + "canvas.style.margin = '0';\n"
            + "canvas.style.padding = '0';\n"
            + "canvas.style.border = 'none';\n"
            + "return changed;\n")
    public static native boolean resizeCanvasBuffer(String canvasId, int cssW, int cssH, int bufferW, int bufferH);

    public static boolean resizeCanvasBuffer(int cssW, int cssH, int bufferW, int bufferH) {
        return resizeCanvasBuffer(CANVAS_ID, cssW, cssH, bufferW, bufferH);
    }
}
