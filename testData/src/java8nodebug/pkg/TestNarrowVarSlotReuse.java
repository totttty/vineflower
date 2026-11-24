package pkg;

// A byte local and a later int local share one slot (no debug info): merging them widens the byte to int,
// and the byte's uses (stored into a byte[], incremented with implicit narrowing) no longer compile.
public class TestNarrowVarSlotReuse {
  public void test(int n, byte[] a) {
    {
      byte[] pos = new byte[6];
      byte v = 0;
      while (v < n) {
        pos[v] = v;
        v++;
      }
      a[0] = pos[1];
    }
    {
      byte[] q = a;
      int k = 4095;
      while (k > 0) {
        q[k] = 1;
        k--;
      }
      a[1] = (byte) k;
    }
  }

  // merging stays possible when the narrow variable's uses accept int
  public int testMergeable(int n, int[] a) {
    {
      byte v = (byte) n;
      a[0] = v;
    }
    {
      int k = n * 4095;
      a[1] = k;
    }
    return a[0];
  }
}
