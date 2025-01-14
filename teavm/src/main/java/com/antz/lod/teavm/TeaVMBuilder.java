package com.antz.lod.teavm;

import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder;
import com.github.xpenatan.gdx.backends.teavm.config.plugins.TeaReflectionSupplier;
import com.github.xpenatan.gdx.backends.teavm.gen.SkipClass;
import java.io.File;
import java.io.IOException;
import org.teavm.tooling.TeaVMTool;
import org.teavm.vm.TeaVMOptimizationLevel;

/** Builds the TeaVM/HTML application. */
@SkipClass
public class TeaVMBuilder {
    public static void main(String[] args) throws IOException {
        TeaBuildConfiguration teaBuildConfiguration = new TeaBuildConfiguration();
        teaBuildConfiguration.assetsPath.add(new File("../assets"));
        teaBuildConfiguration.webappPath = new File("build/dist").getCanonicalPath();

        // Register any extra classpath assets here:
        // teaBuildConfiguration.additionalAssetsClasspathFiles.add("com/antz/lod/asset.extension");

        // Register any classes or packages that require reflection here:
        TeaReflectionSupplier.addReflectionClass("com.antz.lod.dto.LevelOfDetailDTO");
        TeaReflectionSupplier.addReflectionClass("com.antz.lod.dto.LoDDTO");
        TeaReflectionSupplier.addReflectionClass("com.antz.lod.dto.MeshLoDDTO");
        TeaReflectionSupplier.addReflectionClass("com.antz.lod.dto.MeshLoDDTO");
        TeaReflectionSupplier.addReflectionClass("com.badlogic.gdx.utils.Array");

        TeaVMTool tool = TeaBuilder.config(teaBuildConfiguration);
        tool.setMainClass(TeaVMLauncher.class.getName());
        // For many (or most) applications, using the highest optimization won't add much to build time.
        // If your builds take too long, and runtime performance doesn't matter, you can change FULL to SIMPLE .
        tool.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
        tool.setObfuscated(false);
        TeaBuilder.build(tool);
    }
}
