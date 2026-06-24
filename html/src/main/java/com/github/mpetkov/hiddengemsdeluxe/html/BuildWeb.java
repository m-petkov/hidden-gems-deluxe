package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;
import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Builds the TeaVM JavaScript output and optionally serves it locally. */
public class BuildWeb {

    public static void main(String[] args) {
        boolean startServer = args.length == 0 || !"--no-server".equals(args[0]);
        File outputDir = new File("build/dist");

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
                .build(outputDir);

        copyFullViewportIndexHtml(outputDir);
    }

    /** Copies static web shell files into the TeaVM output directory. */
    private static void copyFullViewportIndexHtml(File outputDir) {
        File webappDir = new File("webapp");
        File targetDir = new File(outputDir, "webapp");
        copyWebFile(webappDir, targetDir, "index.html");
        copyFavicon(targetDir);
    }

    private static void copyFavicon(File targetDir) {
        File source = new File("../lwjgl3/src/main/resources/libgdx128.png");
        File target = new File(targetDir, "favicon.png");
        if (!source.isFile()) {
            throw new RuntimeException("Missing desktop icon for favicon: " + source.getAbsolutePath());
        }
        try {
            targetDir.mkdirs();
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy favicon.png", e);
        }
    }

    private static void copyWebFile(File sourceDir, File targetDir, String name) {
        File source = new File(sourceDir, name);
        File target = new File(targetDir, name);
        if (!source.isFile()) {
            throw new RuntimeException("Missing web asset: " + source.getAbsolutePath());
        }
        try {
            targetDir.mkdirs();
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy web asset: " + name, e);
        }
    }
}
