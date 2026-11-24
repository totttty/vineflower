package pkg;

// A boolean & with a constant true operand (decompiled with boolean-as-int off): the constant must stay boolean
// when the other operand is a method result or field.
public class TestBooleanAndConstant {
  boolean flag;

  boolean check(int i) {
    return i > 2;
  }

  public void testInvocation(int a, int b) {
    boolean ok = true & check(a);
    int c = a << 8 | b;
    ok = ok & check(c);
    if (!ok) {
      return;
    }
    System.out.println(c);
  }

  public boolean testField() {
    return true & flag;
  }

  public int testInt(int a) {
    return 1 & a;
  }
}
