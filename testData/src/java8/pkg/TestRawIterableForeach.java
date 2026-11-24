package pkg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Iterator loops whose element is cast on next(), over iterables whose static element type is Object
// (raw types, as obfuscated code without Signature attributes has them, or List<Object>).
public class TestRawIterableForeach {
  static class Bag implements Iterable {
    final List items = new ArrayList();

    public Iterator iterator() {
      return items.iterator();
    }
  }

  static class Names implements Iterable<String> {
    public Iterator<String> iterator() {
      return null;
    }
  }

  public void testRawClass(Bag bag) {
    Iterator it = bag.iterator();
    while (it.hasNext()) {
      String s = (String) it.next();
      System.out.println(s.length());
    }
  }

  public void testRawList(List list) {
    Iterator it = list.iterator();
    while (it.hasNext()) {
      String s = (String) it.next();
      System.out.println(s.length());
    }
  }

  public int testRawUnboxing(Set set) {
    int sum = 0;
    Iterator it = set.iterator();
    while (it.hasNext()) {
      int i = (Integer) it.next();
      sum += i;
    }
    return sum;
  }

  List<Object> objects = new ArrayList<>();

  public void testObjectList() {
    Iterator<Object> it = objects.iterator();
    while (it.hasNext()) {
      String s = (String) it.next();
      System.out.println(s.length());
    }
  }

  // no cast needed below
  public void testGenericList(List<String> list) {
    for (String s : list) {
      System.out.println(s.length());
    }
  }

  public void testRawObject(List list) {
    for (Object o : list) {
      System.out.println(o);
    }
  }

  public void testParameterizedClass(Names names) {
    for (String s : names) {
      System.out.println(s.length());
    }
  }
}
