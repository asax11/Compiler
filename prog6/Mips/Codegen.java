package Mips;
import Temp.Temp;
import Temp.TempList;
import Temp.Label;
import Temp.LabelList;
import java.util.Hashtable;

public class Codegen {
  MipsFrame frame;
  public Codegen(MipsFrame f) {frame = f;}

  private Assem.InstrList ilist = null, last = null;

  private void emit(Assem.Instr inst) {
    if (last != null)
      last = last.tail = new Assem.InstrList(inst, null);
    else {
      if (ilist != null)
	throw new Error("Codegen.emit");
      last = ilist = new Assem.InstrList(inst, null);
    }
  }

  Assem.InstrList codegen(Tree.Stm s) {
    munchStm(s);
    Assem.InstrList l = ilist;
    ilist = last = null;
    return l;
  }

  static Assem.Instr OPER(String a, TempList d, TempList s, LabelList j) {
    return new Assem.OPER("\t" + a, d, s, j);
  }
  static Assem.Instr OPER(String a, TempList d, TempList s) {
    return new Assem.OPER("\t" + a, d, s);
  }
  static Assem.Instr MOVE(String a, Temp d, Temp s) {
    return new Assem.MOVE("\t" + a, d, s);
  }

  static TempList L(Temp h) {
    return new TempList(h, null);
  }
  static TempList L(Temp h, TempList t) {
    return new TempList(h, t);
  }

  void munchStm(Tree.Stm s) {
    if (s instanceof Tree.MOVE) 
      munchStm((Tree.MOVE)s);
    else if (s instanceof Tree.UEXP)
      munchStm((Tree.UEXP)s);
    else if (s instanceof Tree.JUMP)
      munchStm((Tree.JUMP)s);
    else if (s instanceof Tree.CJUMP)
      munchStm((Tree.CJUMP)s);
    else if (s instanceof Tree.LABEL)
      munchStm((Tree.LABEL)s);
    else
      throw new Error("Codegen.munchStm");
  }

  void munchStm(Tree.MOVE s) {
    Temp to = munchExp(s.dst);
		Temp from = munchExp(s.src);
		emit(new Assem.MOVE("move `d0,`s0", to, from));
}

  void munchStm(Tree.UEXP s) {
    munchExp(s.exp);
  }

  void munchStm(Tree.JUMP s) {
    LabelList targets = s.targets;
    emit(new Assem.OPER("j " + targets.head, null, null, targets));
}

  private static String[] CJUMP = new String[10];
  static {
    CJUMP[Tree.CJUMP.EQ ] = "beq";
    CJUMP[Tree.CJUMP.NE ] = "bne";
    CJUMP[Tree.CJUMP.LT ] = "blt";
    CJUMP[Tree.CJUMP.GT ] = "bgt";
    CJUMP[Tree.CJUMP.LE ] = "ble";
    CJUMP[Tree.CJUMP.GE ] = "bge";
    CJUMP[Tree.CJUMP.ULT] = "bltu";
    CJUMP[Tree.CJUMP.ULE] = "bleu";
    CJUMP[Tree.CJUMP.UGT] = "bgtu";
    CJUMP[Tree.CJUMP.UGE] = "bgeu";
  }

  void munchStm(Tree.CJUMP s) {
    String operation = CJUMP[s.relop];
    
    Temp left = munchExp(s.left);
    Temp right = munchExp(s.right);
    
    TempList valueList = L(left, L(right));
    LabelList jumpList = new LabelList(s.iftrue, new LabelList(s.iffalse, null));
    
     emit(new Assem.OPER(operation + "`s0, `s1, `j0", null, valueList, jumpList));
}

  void munchStm(Tree.LABEL l) {
    String name = l.label.toString();
    emit(new Assem.LABEL(name + ":", l.label));
}

  Temp munchExp(Tree.Exp s) {
    if (s instanceof Tree.CONST)
      return munchExp((Tree.CONST)s);
    else if (s instanceof Tree.NAME)
      return munchExp((Tree.NAME)s);
    else if (s instanceof Tree.TEMP)
      return munchExp((Tree.TEMP)s);
    else if (s instanceof Tree.BINOP)
      return munchExp((Tree.BINOP)s);
    else if (s instanceof Tree.MEM)
      return munchExp((Tree.MEM)s);
    else if (s instanceof Tree.CALL)
      return munchExp((Tree.CALL)s);
    else
      throw new Error("Codegen.munchExp");
  }

  Temp munchExp(Tree.CONST e) {
    if (e.value != 0) {
      Temp temp = new Temp();
      TempList list = L(temp);
      emit(new Assem.OPER("li `d0," + e.value, list, null));
      return temp;
    } else {
      return frame.ZERO;
    }
}


  Temp munchExp(Tree.NAME e) {
    Temp temp = new Temp();
    emit(OPER("la `d0 " + e.label.toString(), L(temp), null));
    return temp;
}

  Temp munchExp(Tree.TEMP e) {
    if (e.temp == frame.FP) {
      Temp t = new Temp();
      emit(OPER("addu `d0 `s0 " + frame.name + "_framesize",
		L(t), L(frame.SP)));
      return t;
    }
    return e.temp;
  }

  private static String[] BINOP = new String[10];
  static {
    BINOP[Tree.BINOP.PLUS   ] = "add";
    BINOP[Tree.BINOP.MINUS  ] = "sub";
    BINOP[Tree.BINOP.MUL    ] = "mulo";
    BINOP[Tree.BINOP.DIV    ] = "div";
    BINOP[Tree.BINOP.AND    ] = "and";
    BINOP[Tree.BINOP.OR     ] = "or";
    BINOP[Tree.BINOP.LSHIFT ] = "sll";
    BINOP[Tree.BINOP.RSHIFT ] = "srl";
    BINOP[Tree.BINOP.ARSHIFT] = "sra";
    BINOP[Tree.BINOP.XOR    ] = "xor";
  }

  private static int shift(int i) {
    int shift = 0;
    if ((i >= 2) && ((i & (i - 1)) == 0)) {
      while (i > 1) {
	shift += 1;
	i >>= 1;
      }
    }
    return shift;
  }

  Temp munchExp(Tree.BINOP e) {
    if (e.left instanceof Tree.CONST && e.right instanceof Tree.CONST) {
      return munchExp(e, (Tree.CONST)e.left, (Tree.CONST)e.right);
    } else if (e.left instanceof Tree.CONST) {
      return munchExp(e, (Tree.CONST)e.left, e.right);
    } else if (e.right instanceof Tree.CONST) {
      return munchExp(e, e.left, (Tree.CONST)e.right);
    }
    
    Temp temp = new Temp();
    TempList tempList = L(temp);
    String operation = BINOP[e.binop];
    
    Temp left = munchExp(e.left);
    Temp right = munchExp(e.right);
    
    TempList operandList = L(left, L(right, null));
    
    emit(new Assem.OPER(operation + " `d0, `s0,`s1", tempList, operandList));
    
    return temp;
}
  Temp munchExp(Tree.BINOP e, Tree.CONST left, Tree.CONST right) {
    Temp temp = new Temp();
    TempList tempList = L(temp);
    String operation = BINOP[e.binop];
            
    emit(new Assem.OPER(operation + " `d0," + left.value +  "," + right.value, tempList, null));
    
    return temp;
  }
  
  //BINOPS
  
  Temp munchExp(Tree.BINOP e, Tree.Exp left, Tree.CONST right) {
    Temp temp = new Temp();
    TempList tempList = L(temp);
    String operation = BINOP[e.binop];
    
    Temp leftTemp = munchExp(left);
    
    TempList operandList = L(leftTemp);
    
    emit(new Assem.OPER(operation + " `d0, `s0," + right.value, tempList, operandList));
    
    return temp;
  }
  
  Temp munchExp(Tree.BINOP e, Tree.CONST left, Tree.Exp right) {
    Temp temp = new Temp();
    TempList tempList = L(temp);
    String operation = BINOP[e.binop];
    
    Temp rightTemp = munchExp(right);
    
    TempList operandList = L(rightTemp);
    
    emit(new Assem.OPER(operation + " `d0," + left.value + ", `s0", tempList, operandList));
    
    return temp;
}

  Temp munchExp(Tree.MEM e) {
    if (e.exp instanceof Tree.CONST) {
      return munchExp(e, (Tree.CONST)e.exp);
    }
    
    Temp t = new Temp();
    emit(OPER("lw `d0 (`s0)", L(t), L(munchExp(e.exp))));
    return t;
  }
  
  Temp munchExp(Tree.MEM e, Tree.CONST c) {
    Temp t = new Temp();
    emit(OPER("lw `d0 " + c.value, L(t), null));
    return t;
}

   Temp munchExp(Tree.CALL s) {
    if (s.func instanceof Tree.NAME) {
      return munchExp(s, (Tree.NAME) s.func);
    }
    emit(OPER("jal `d0 `s0", frame.calldefs, L(munchExp(s.func), munchArgs(0, s.args))));
    return frame.V0;
  }
  
  Temp munchExp(Tree.CALL s, Tree.NAME name) {
    String label = name.label.toString();
    emit(OPER("jal " + name,  frame.calldefs, munchArgs(0, s.args)));
    return frame.V0;
}

  private TempList munchArgs(int i, Tree.ExpList args) {
    if (args == null)
      return null;
    Temp src = munchExp(args.head);
    if (i > frame.maxArgs)
      frame.maxArgs = i;
    switch (i) {
    case 0:
      emit(MOVE("move `d0 `s0", frame.A0, src));
      break;
    case 1:
      emit(MOVE("move `d0 `s0", frame.A1, src));
      break;
    case 2:
      emit(MOVE("move `d0 `s0", frame.A2, src));
      break;
    case 3:
      emit(MOVE("move `d0 `s0", frame.A3, src));
      break;
    default:
      emit(OPER("sw `s0 " + (i-1)*frame.wordSize() + "(`s1)",
		null, L(src, L(frame.SP))));
      break;
    }
    return L(src, munchArgs(i+1, args.tail));
  }
}
