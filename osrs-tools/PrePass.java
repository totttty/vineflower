import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Builds the exact jar oracle-deob's decompile stage hands Vineflower: runs DecompileStage's two pre-decompile
 * bytecode workarounds (neutralizeInvalidLambdaInvokedynamics, then splitDisjointIntLocalSlots) by reflection.
 * Reads the input jar as data (ASM); never loads or runs its classes.
 *   java -cp 'oracle-deob/kotlin/daemon/build/install/daemon/lib/*' PrePass.java IN.jar OUT.jar WORKDIR
 */
public class PrePass {
    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);
        Path work = Path.of(args[2]);
        Files.createDirectories(work);
        Class<?> optsClass = Class.forName("oracledeob.decompile.DecompileOptions");
        Object opts = optsClass.getConstructor(int.class).newInstance(1);
        Class<?> stageClass = Class.forName("oracledeob.decompile.DecompileStage");
        Object stage = stageClass.getConstructor(optsClass).newInstance(opts);
        Method lambda = stageClass.getDeclaredMethod("neutralizeInvalidLambdaInvokedynamics", Path.class, Path.class);
        Method split = stageClass.getDeclaredMethod("splitDisjointIntLocalSlots", Path.class, Path.class);
        lambda.setAccessible(true);
        split.setAccessible(true);
        Path p1 = (Path) lambda.invoke(stage, in, work);
        Path j1 = p1 != null ? p1 : in;
        Path p2 = (Path) split.invoke(stage, j1, work);
        Path j2 = p2 != null ? p2 : j1;
        Files.copy(j2, out, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("lambdaNeutralized=" + (p1 != null) + " localSlotsSplit=" + (p2 != null) + " -> " + out);
    }
}
