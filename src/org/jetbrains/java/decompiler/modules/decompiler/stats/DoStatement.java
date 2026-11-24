// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.stats;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FieldExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Pattern;
import org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.CodeType;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// Loop statement
public class DoStatement extends Statement {
  public enum Type {
    INFINITE, DO_WHILE, WHILE, FOR, FOR_EACH
  }

  private Type looptype;

  private final List<Exprent> initExprent = new ArrayList<>();
  private final List<Exprent> conditionExprent = new ArrayList<>();
  private final List<Exprent> incExprent = new ArrayList<>();

  // *****************************************************************************
  // constructors
  // *****************************************************************************

  protected DoStatement() {
    super(StatementType.DO);
    looptype = Type.INFINITE;

    initExprent.add(null);
    conditionExprent.add(null);
    incExprent.add(null);
  }

  protected DoStatement(Statement head) {

    this();

    first = head;
    stats.addWithKey(first, first.id);

    // post is always null!
  }

  // *****************************************************************************
  // public methods
  // *****************************************************************************

  public static Statement isHead(Statement head) {

    if (head.getLastBasicType() == LastBasicType.GENERAL && !head.isMonitorEnter()) {

      // at most one outgoing edge
      StatEdge edge = null;
      List<StatEdge> lstSuccs = head.getSuccessorEdges(STATEDGE_DIRECT_ALL);
      if (!lstSuccs.isEmpty()) {
        edge = lstSuccs.get(0);
      }

      // regular loop
      if (edge != null && edge.getType() == StatEdge.TYPE_REGULAR && edge.getDestination() == head) {
        return new DoStatement(head);
      }

      // continues
      if (!(head instanceof DoStatement) && (edge == null || edge.getType() != StatEdge.TYPE_REGULAR) &&
          head.getContinueSet().contains(head.getBasichead())) {
        return new DoStatement(head);
      }
    }

    return null;
  }

  @Override
  public HashSet<Statement> buildContinueSet() {
    super.buildContinueSet();

    continueSet.remove(first.getBasichead());

    return continueSet;
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();

    buf.append(ExprProcessor.listToJava(varDefinitions, indent));

    if (isLabeled()) {
      buf.appendIndent(indent).append("label").append(this.id).append(":").appendLineSeparator();
    }

    switch (looptype) {
      case INFINITE:
        buf.appendIndent(indent).append("while (true) {").appendLineSeparator();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        break;
      case DO_WHILE:
        buf.appendIndent(indent).append("do {").appendLineSeparator();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false));
        buf.appendIndent(indent).append("} while (");
        buf.pushNewlineGroup(indent, 1);
        buf.appendPossibleNewline();
        buf.append(conditionExprent.get(0).toJava(indent));
        buf.appendPossibleNewline("", true);
        buf.popNewlineGroup();
        buf.append(");").appendLineSeparator();
        break;
      case WHILE:
        buf.appendIndent(indent).append("while (");
        buf.pushNewlineGroup(indent, 1);
        buf.appendPossibleNewline();
        buf.append(conditionExprent.get(0).toJava(indent));
        buf.appendPossibleNewline("", true);
        buf.popNewlineGroup();
        buf.append(") {").appendLineSeparator();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        break;
      case FOR:
        buf.appendIndent(indent);
        buf.pushNewlineGroup(indent, 1);
        buf.append("for (");
        if (initExprent.get(0) != null) {
          buf.append(initExprent.get(0).toJava(indent));
        }
        buf.append(";").appendPossibleNewline(" ")
          .append(conditionExprent.get(0).toJava(indent)).append(";").appendPossibleNewline(" ")
          .append(incExprent.get(0).toJava(indent))
          .appendPossibleNewline("", true);
        buf.popNewlineGroup();
        buf.append(") {").appendLineSeparator();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, false));
        buf.appendIndent(indent).append("}").appendLineSeparator();
        break;
      case FOR_EACH:
        buf.appendIndent(indent).append("for (").append(initExprent.get(0).toJava(indent));
        VarType iterType = incExprent.get(0).getInferredExprType(null); //TODO: Find a better then null? For now just calls it to clear casts if needed
        buf.append(" : ");
        VarType[] iterableCast = getIterableElementCast(initExprent.get(0), incExprent.get(0), iterType);
        if (iterableCast != null) {
          buf.append('(').appendCastTypeName(iterableCast[0]).append(')');
          if (iterableCast.length > 1) {
            buf.append('(').appendCastTypeName(iterableCast[1]).append(')');
          }
          boolean quote = incExprent.get(0).getPrecedence() >= FunctionType.CAST.precedence;
          if (quote) {
            buf.append('(');
          }
          buf.append(incExprent.get(0).toJava(indent));
          if (quote) {
            buf.append(')');
          }
        } else {
          buf.append(incExprent.get(0).toJava(indent));
        }
        buf.append(") {").appendLineSeparator();
        buf.append(ExprProcessor.jmpWrapper(first, indent + 1, true));
        buf.appendIndent(indent).append("}").appendLineSeparator();
    }

    return buf;
  }

  /**
   * The loop variable of a for-each over an iterator takes its type from the checkcast (or unboxing) on
   * {@code next()}. When the iterable's static element type is not assignable to it, javac rejects the loop:
   * a raw expression type yields {@code Object} elements, and so does e.g. a {@code List<Object>}. Returns the casts
   * to print before the iterable ({@code (Iterable<T>)}, or {@code (Iterable<T>)(Iterable)} when the expression's own
   * element type is a different concrete type), or null when none is needed or the element type is unknown.
   */
  private static VarType[] getIterableElementCast(Exprent var, Exprent iterable, VarType iterType) {
    if (!(var instanceof VarExprent)) {
      return null;
    }
    VarType varType = ((VarExprent) var).getDefinitionVarType();
    if (varType == null || iterType == null || iterType.type != CodeType.OBJECT || iterType.arrayDim != 0 ||
        iterType.value == null || VarType.VARTYPE_OBJECT.equals(varType)) {
      return null;
    }
    if (varType.type != CodeType.OBJECT) {
      varType = varType.arrayDim == 0 ? getBoxedType(varType) : null;
      if (varType == null) {
        return null;
      }
    }

    StructContext ctx = DecompilerContext.getStructContext();
    if (ctx.getClass(ITERABLE.value) == null || ctx.getClass(iterType.value) == null) {
      return null;
    }
    VarType iterableType = GenericType.getGenericSuperType(iterType, ITERABLE);
    if (!(iterableType instanceof GenericType) || !ITERABLE.value.equals(iterableType.value)) {
      return null; // not known to be an Iterable
    }
    List<VarType> args = ((GenericType) iterableType).getArguments();
    if (args.size() != 1 || args.get(0) == null) {
      return null;
    }
    VarType element = args.get(0);
    VarType cast = new GenericType(CodeType.OBJECT, 0, ITERABLE.value, null, Collections.singletonList(varType), GenericType.WILDCARD_NO);

    if (!isDeclaredType(iterable, iterType)) {
      return null; // the printed expression may have a more precise type than the one inferred here
    }

    if (element.type == CodeType.GENVAR) {
      // A type variable of the class's own hierarchy is only unbound (erased to Object) for a raw expression type
      return iterType.isGeneric() ? null : new VarType[] {cast};
    }

    int wildcard = element instanceof GenericType ? ((GenericType) element).getWildcard() : GenericType.WILDCARD_NO;
    if (element.type != CodeType.OBJECT || element.value == null || wildcard == GenericType.WILDCARD_SUPER) {
      return null;
    }
    if (wildcard == GenericType.WILDCARD_UNBOUND) {
      element = VarType.VARTYPE_OBJECT;
    }
    if (ctx.getClass(element.value) == null ||
        (element.arrayDim == varType.arrayDim && ctx.instanceOf(element.value, varType.value))) {
      return null; // already assignable
    }
    return new VarType[] {cast, ITERABLE};
  }

  /**
   * Whether {@code type}, inferred for {@code exprent}, is the static type javac will see for the printed expression:
   * a class without type parameters; a field (its declared type, or a type that cannot depend on the instance); a
   * local or parameter of a method without a generic signature; a printed cast; an invocation of a method without a
   * generic signature, or of a member of a raw type (erased), or whose inferred type is parameterized (raw is also
   * what a failed inference returns).
   */
  private static boolean isDeclaredType(Exprent exprent, VarType type) {
    StructContext ctx = DecompilerContext.getStructContext();
    StructClass cls = ctx.getClass(type.value);
    if (cls != null && (cls.getSignature() == null || cls.getSignature().fparameters.isEmpty())) {
      return true;
    }
    if (exprent instanceof FieldExprent) {
      FieldExprent field = (FieldExprent) exprent;
      StructClass owner = ctx.getClass(field.getClassname());
      while (owner != null) {
        StructField fd = owner.getField(field.getName(), field.getDescriptor().descriptorString);
        if (fd != null) {
          return fd.getSignature() == null || fd.hasModifier(CodeConstants.ACC_STATIC) || owner.getSignature() == null;
        }
        owner = owner.superClass == null ? null : ctx.getClass((String) owner.superClass.value);
      }
      return false;
    }
    if (exprent instanceof VarExprent) {
      MethodWrapper method = (MethodWrapper) DecompilerContext.getContextProperty(DecompilerContext.CURRENT_METHOD_WRAPPER);
      return method != null && method.methodStruct.getSignature() == null;
    }
    if (exprent instanceof FunctionExprent) {
      // a cast that is printed (an elided one leaves its operand's possibly more precise type)
      return ((FunctionExprent) exprent).getFuncType() == FunctionType.CAST && ((FunctionExprent) exprent).doesCast();
    }
    if (exprent instanceof InvocationExprent) {
      InvocationExprent inv = (InvocationExprent) exprent;
      StructClass owner = ctx.getClass(inv.getClassname());
      StructMethod mt = owner == null ? null : owner.getMethodRecursive(inv.getName(), inv.getStringDescriptor());
      if (mt != null && mt.getSignature() == null) {
        return true; // the declared return type
      }
      if (mt != null && !type.isGeneric() && inv.getInstance() != null) {
        // a member of a raw type is erased
        VarType instType = inv.getInstance().getInferredExprType(null);
        StructClass instCls = instType.type == CodeType.OBJECT && instType.arrayDim == 0 ? ctx.getClass(instType.value) : null;
        if (instCls != null && !instType.isGeneric() && instCls.getSignature() != null &&
            !instCls.getSignature().fparameters.isEmpty() && isDeclaredType(inv.getInstance(), instType)) {
          return true;
        }
      }
      return type.isGeneric();
    }
    return false;
  }

  private static final VarType ITERABLE = new VarType(CodeType.OBJECT, 0, "java/lang/Iterable");

  private static VarType getBoxedType(VarType primitive) {
    for (Map.Entry<VarType, VarType> e : VarType.UNBOXING_TYPES.entrySet()) {
      if (e.getValue().equals(primitive)) {
        return e.getKey();
      }
    }
    return null;
  }

  @Override
  public List<Object> getSequentialObjects() {

    List<Object> lst = new ArrayList<>();

    switch (looptype) {
      case FOR:
        if (getInitExprent() != null) {
          lst.add(getInitExprent());
        }
      case WHILE:
        lst.add(getConditionExprent());
        break;
      case FOR_EACH:
        lst.add(getInitExprent());
        lst.add(getIncExprent());
    }

    lst.add(first);

    switch (looptype) {
      case DO_WHILE:
        lst.add(getConditionExprent());
        break;
      case FOR:
        lst.add(getIncExprent());
    }

    return lst;
  }

  @Override
  public void replaceExprent(Exprent oldexpr, Exprent newexpr) {
    if (initExprent.get(0) == oldexpr) {
      initExprent.set(0, newexpr);
    }
    if (conditionExprent.get(0) == oldexpr) {
      conditionExprent.set(0, newexpr);
    }
    if (incExprent.get(0) == oldexpr) {
      incExprent.set(0, newexpr);
    }
  }

  @Override
  public List<VarExprent> getImplicitlyDefinedVars() {
    List<VarExprent> vars = new ArrayList<>();

    // Impossible in foreach loops, and quit if condition doesn't exist
    if (looptype == Type.FOR_EACH || getConditionExprent() == null) {
      return null;
    }

    List<Exprent> conditionList = getConditionExprent().getAllExprents(true, true);

    for (Exprent condition : conditionList) {
      if (condition instanceof FunctionExprent func) {

        // Pattern match variable is implicitly defined
        if (func.getFuncType() == FunctionType.INSTANCEOF && func.getLstOperands().size() > 2) {
          vars.addAll(((Pattern) func.getLstOperands().get(2)).getPatternVars());
        }
      }
    }

    return vars;
  }

  @Override
  public boolean hasBasicSuccEdge() {
    return looptype != Type.INFINITE;
  }

  @Override
  public Statement getSimpleCopy() {
    return new DoStatement();
  }

  // *****************************************************************************
  // getter and setter methods
  // *****************************************************************************

  public List<Exprent> getInitExprentList() {
    return initExprent;
  }

  public List<Exprent> getConditionExprentList() {
    return conditionExprent;
  }

  public List<Exprent> getIncExprentList() {
    return incExprent;
  }

  public Exprent getConditionExprent() {
    return conditionExprent.get(0);
  }

  public void setConditionExprent(Exprent conditionExprent) {
    this.conditionExprent.set(0, conditionExprent);
  }

  public Exprent getIncExprent() {
    return incExprent.get(0);
  }

  public void setIncExprent(Exprent incExprent) {
    this.incExprent.set(0, incExprent);
  }

  public Exprent getInitExprent() {
    return initExprent.get(0);
  }

  public void setInitExprent(Exprent initExprent) {
    this.initExprent.set(0, initExprent);
  }

  public Type getLooptype() {
    return looptype;
  }

  public void setLooptype(Type looptype) {
    this.looptype = looptype;
  }
}
