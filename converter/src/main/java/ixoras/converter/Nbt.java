package ixoras.converter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal NBT reader/writer for the converter, with no library behind it (the JDK reads zlib and gzip).
 * Values are plain Java objects: Byte, Short, Integer, Long, Float, Double, byte[], String, int[], long[],
 * {@link NList} and {@link Compound}. A compound keeps its key order so a rewritten tag is the same shape
 * as the one read. Strings are Java's modified UTF-8 (DataInput.readUTF), which is what NBT uses.
 *
 * <p>UNVERIFIED against 26.1.2: that the on-disk NBT format of this Minecraft version is still the classic
 * big-endian format with these thirteen tag ids. The converter fails loudly on an unknown id.
 */
final class Nbt {
    static final int END = 0, BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6, BYTE_ARRAY = 7,
            STRING = 8, LIST = 9, COMPOUND = 10, INT_ARRAY = 11, LONG_ARRAY = 12;

    private Nbt() {
    }

    /** A list keeps its element type, because an empty list still has one and the writer must repeat it. */
    static final class NList {
        final int type;
        final List<Object> items = new ArrayList<>();

        NList(int type) {
            this.type = type;
        }
    }

    static final class Compound extends LinkedHashMap<String, Object> {
        private static final long serialVersionUID = 1L;
    }

    /** A named root tag, as it sits at the start of a chunk or a .dat file. */
    static final class Root {
        final String name;
        final Compound value;

        Root(String name, Compound value) {
            this.name = name;
            this.value = value;
        }
    }

    static Root readRoot(DataInputStream in) throws IOException {
        int type = in.readUnsignedByte();
        if (type != COMPOUND) {
            throw new IOException("NBT root is tag id " + type + ", not a compound");
        }
        String name = in.readUTF();
        return new Root(name, (Compound) read(in, COMPOUND, 0));
    }

    static void writeRoot(DataOutputStream out, Root root) throws IOException {
        out.writeByte(COMPOUND);
        out.writeUTF(root.name);
        write(out, root.value);
    }

    private static Object read(DataInputStream in, int type, int depth) throws IOException {
        if (depth > 512) {
            throw new IOException("NBT nested deeper than 512");
        }
        switch (type) {
            case BYTE:
                return in.readByte();
            case SHORT:
                return in.readShort();
            case INT:
                return in.readInt();
            case LONG:
                return in.readLong();
            case FLOAT:
                return in.readFloat();
            case DOUBLE:
                return in.readDouble();
            case BYTE_ARRAY: {
                byte[] a = new byte[checkLen(in.readInt())];
                in.readFully(a);
                return a;
            }
            case STRING:
                return in.readUTF();
            case LIST: {
                int et = in.readUnsignedByte();
                int n = in.readInt();
                if (n < 0 || (et == END && n > 0)) {
                    throw new IOException("bad NBT list header");
                }
                NList l = new NList(et);
                for (int i = 0; i < n; i++) {
                    l.items.add(read(in, et, depth + 1));
                }
                return l;
            }
            case COMPOUND: {
                Compound c = new Compound();
                while (true) {
                    int t = in.readUnsignedByte();
                    if (t == END) {
                        return c;
                    }
                    String k = in.readUTF();
                    c.put(k, read(in, t, depth + 1));
                }
            }
            case INT_ARRAY: {
                int[] a = new int[checkLen(in.readInt())];
                for (int i = 0; i < a.length; i++) {
                    a[i] = in.readInt();
                }
                return a;
            }
            case LONG_ARRAY: {
                long[] a = new long[checkLen(in.readInt())];
                for (int i = 0; i < a.length; i++) {
                    a[i] = in.readLong();
                }
                return a;
            }
            default:
                throw new IOException("unknown NBT tag id " + type);
        }
    }

    private static int checkLen(int n) throws IOException {
        if (n < 0 || n > 64 * 1024 * 1024) {
            throw new IOException("implausible NBT array length " + n);
        }
        return n;
    }

    static int typeOf(Object v) throws IOException {
        if (v instanceof Byte) return BYTE;
        if (v instanceof Short) return SHORT;
        if (v instanceof Integer) return INT;
        if (v instanceof Long) return LONG;
        if (v instanceof Float) return FLOAT;
        if (v instanceof Double) return DOUBLE;
        if (v instanceof byte[]) return BYTE_ARRAY;
        if (v instanceof String) return STRING;
        if (v instanceof NList) return LIST;
        if (v instanceof Compound) return COMPOUND;
        if (v instanceof int[]) return INT_ARRAY;
        if (v instanceof long[]) return LONG_ARRAY;
        throw new IOException("cannot write " + v.getClass());
    }

    private static void write(DataOutputStream out, Object v) throws IOException {
        if (v instanceof Byte) {
            out.writeByte((Byte) v);
        } else if (v instanceof Short) {
            out.writeShort((Short) v);
        } else if (v instanceof Integer) {
            out.writeInt((Integer) v);
        } else if (v instanceof Long) {
            out.writeLong((Long) v);
        } else if (v instanceof Float) {
            out.writeFloat((Float) v);
        } else if (v instanceof Double) {
            out.writeDouble((Double) v);
        } else if (v instanceof byte[]) {
            byte[] a = (byte[]) v;
            out.writeInt(a.length);
            out.write(a);
        } else if (v instanceof String) {
            out.writeUTF((String) v);
        } else if (v instanceof NList) {
            NList l = (NList) v;
            out.writeByte(l.type);
            out.writeInt(l.items.size());
            for (Object o : l.items) {
                write(out, o);
            }
        } else if (v instanceof Compound) {
            for (Map.Entry<String, Object> e : ((Compound) v).entrySet()) {
                out.writeByte(typeOf(e.getValue()));
                out.writeUTF(e.getKey());
                write(out, e.getValue());
            }
            out.writeByte(END);
        } else if (v instanceof int[]) {
            int[] a = (int[]) v;
            out.writeInt(a.length);
            for (int x : a) {
                out.writeInt(x);
            }
        } else if (v instanceof long[]) {
            long[] a = (long[]) v;
            out.writeInt(a.length);
            for (long x : a) {
                out.writeLong(x);
            }
        } else {
            throw new IOException("cannot write " + v.getClass());
        }
    }
}
