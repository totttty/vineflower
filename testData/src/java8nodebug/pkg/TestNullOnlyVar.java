package pkg;

import java.io.File;

// Locals only ever assigned null (no debug info): their type comes from their uses, not Object.
public class TestNullOnlyVar {
  public String testInstance(boolean b) {
    File f = null;
    if (b) {
      System.out.println(f != null ? 1 : 0);
    }
    return f != null ? f.getPath() : "";
  }

  public void testArgument() {
    File f = null;
    take(f);
  }

  public void testObjectUse() {
    String s = null;
    System.out.println(s);
  }

  public int testConflict(boolean b) {
    CharSequence c = null;
    String s = null;
    if (b) {
      return s.length() + c.length();
    }
    return 0;
  }

  private static void take(File f) {
  }
}
