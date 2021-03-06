package protobuf.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.GeneratingCompiler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import protobuf.facet.PbFacet;

import java.io.File;

/**
 * A precompile task for the Protobuffer compiler.
 * @author Travis Cripps
 */
public class PbPrecompileTask implements CompileTask {

    private static final Logger LOG = Logger.getInstance("#protobuf.compiler.PbPrecompileTask");

    @Override
    public boolean execute(CompileContext context) {
        boolean result = true;

        doCleanupIfNeeded(context);

        final Project project = context.getProject();

        // Kick off the {@PBCompiler protobuffers generating compiler}.
        PbCompiler compiler = new PbCompiler(project);
        GeneratingCompiler.GenerationItem[] generationItems = compiler.getGenerationItems(context);
        compiler.generate(context, generationItems, null);
        return result;
    }

    private void doCleanupIfNeeded(CompileContext compileContext) {
        if (!compileContext.isRebuild()) {
            return;
        }

        final CompileScope compileScope = compileContext.getCompileScope();
        Module[] affectedModules = compileScope.getAffectedModules();

        for (Module module : affectedModules) {
            final PbFacet facet = PbFacet.getInstance(module);
            if (facet == null || !facet.getConfiguration().isCompilationEnabled())
                continue;

            // Continue cleanup if a Protobuf facet has been created for the module.
            File outputPath = new File(facet.getConfiguration().getCompilerOutputPath());
            if (!outputPath.exists() || !outputPath.isDirectory())
                continue;

            LOG.info("Cleaning up a facet on rebuild: " + module.getName() + ", " + outputPath.getAbsolutePath());
            cleanDirectory(outputPath);
        }
    }

    /**
     * Cleans a directory but does not delete it. It recursively deletes all subdirectories.
     * @param dir a directory to clean.
     */
    private static void cleanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    deleteFileOrLog(f);
                }
            }
        }
    }

    /**
     * Recursively delete the directory.
     * @param dir a directory to delete.
     */
    private static void deleteDirectory(File dir) {
        cleanDirectory(dir);
        deleteFileOrLog(dir);
    }

    private static void deleteFileOrLog(File fileToDelete) {
        if (!fileToDelete.delete()) {
            LOG.error("Unable to delete:", fileToDelete.getAbsolutePath());
        }
    }
}
