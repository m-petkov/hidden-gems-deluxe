package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;

/** Builds the TeaVM JavaScript output and optionally serves it locally. */
public class BuildWeb {

    public static void main(String[] args) {
        boolean startServer = args.length == 0 || !"--no-server".equals(args[0]);

        WebBackend backend = new WebBackend()
                .setHtmlTitle("Hidden Gems Deluxe")
                .setHtmlWidth(0)
                .setHtmlHeight(0)
                .setStartJettyAfterBuild(startServer);

        new TeaCompiler(backend)
                .addAssets(new AssetFileHandle("../assets"))
                .setMainClass(HtmlLauncher.class.getName())
                .setOutputName("app")
                .setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
                .setObfuscated(false)
                .addReflectionClass("com.github.mpetkov.hiddengemsdeluxe.**")
                .addReflectionClass("com.badlogic.gdx.scenes.scene2d.ui.**")
                .addReflectionClass("com.badlogic.gdx.graphics.g2d.freetype.**")
                .addReflectionClass("com.badlogic.gdx.graphics.g2d.PixmapPacker**")
                .build(new File("build/dist"));
    }
}
