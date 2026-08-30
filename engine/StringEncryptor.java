package engine;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class StringEncryptor {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java StringEncryptor <input.jar> <output.jar>");
            return;
        }

        File inJar = new File(args[0]);
        File outJar = new File(args[1]);

        int totalEncrypted = 0;
        int totalClasses = 0;

        try (ZipFile zipIn = new ZipFile(inJar);
             ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outJar))) {

            Enumeration<? extends ZipEntry> entries = zipIn.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream is = zipIn.getInputStream(entry);

                if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                    totalClasses++;
                    byte[] classBytes = readAllBytes(is);
                    ClassReader cr = new ClassReader(classBytes);
                    ClassNode cn = new ClassNode();
                    cr.accept(cn, 0);

                    boolean modified = false;
                    int classKey = 0x4B3D ^ (entry.getName().hashCode() & 0x7FFF);

                    // Skip annotation interfaces and records headers if needed
                    if ((cn.access & Opcodes.ACC_ANNOTATION) == 0 && (cn.access & Opcodes.ACC_INTERFACE) == 0) {
                        for (MethodNode mn : cn.methods) {
                            if (mn.instructions == null) continue;
                            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                                if (insn.getOpcode() == Opcodes.LDC) {
                                    LdcInsnNode ldc = (LdcInsnNode) insn;
                                    if (ldc.cst instanceof String) {
                                        String original = (String) ldc.cst;
                                        if (original.length() > 0 && original.length() < 5000) {
                                            String encrypted = encrypt(original, classKey);
                                            InsnList replacement = new InsnList();
                                            replacement.add(new LdcInsnNode(encrypted));
                                            replacement.add(new IntInsnNode(Opcodes.SIPUSH, classKey));
                                            replacement.add(new MethodInsnNode(
                                                    Opcodes.INVOKESTATIC,
                                                    cn.name,
                                                    "I111lI1",
                                                    "(Ljava/lang/String;I)Ljava/lang/String;",
                                                    false
                                            ));
                                            mn.instructions.insertBefore(ldc, replacement);
                                            mn.instructions.remove(ldc);
                                            modified = true;
                                            totalEncrypted++;
                                        }
                                    }
                                }
                            }
                        }

                        if (modified) {
                            addDecryptorMethod(cn);
                        }
                    }

                    if (modified) {
                        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        cn.accept(cw);
                        byte[] modifiedBytes = cw.toByteArray();
                        ZipEntry outEntry = new ZipEntry(entry.getName());
                        zipOut.putNextEntry(outEntry);
                        zipOut.write(modifiedBytes);
                        zipOut.closeEntry();
                    } else {
                        ZipEntry outEntry = new ZipEntry(entry.getName());
                        zipOut.putNextEntry(outEntry);
                        zipOut.write(classBytes);
                        zipOut.closeEntry();
                    }
                } else {
                    ZipEntry outEntry = new ZipEntry(entry.getName());
                    zipOut.putNextEntry(outEntry);
                    zipOut.write(readAllBytes(is));
                    zipOut.closeEntry();
                }
            }
        }

        System.out.println("[v] String Encryption Complete: " + totalEncrypted + " strings encrypted across " + totalClasses + " classes.");
    }

    private static String encrypt(String s, int key) {
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        byte[] enc = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            enc[i] = (byte) (((raw[i] & 0xFF) ^ (key + (i & 0x0F))) & 0xFF);
        }
        return Base64.getEncoder().encodeToString(enc);
    }

    private static void addDecryptorMethod(ClassNode cn) {
        // Synthetic private static String I111lI1(String s, int k)
        MethodNode mn = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                "I111lI1",
                "(Ljava/lang/String;I)Ljava/lang/String;",
                null,
                null
        );

        LabelNode l0 = new LabelNode();
        mn.instructions.add(l0);
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_CHAR));
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, 3));

        mn.instructions.add(new InsnNode(Opcodes.ICONST_0));
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, 4));

        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();

        mn.instructions.add(loopStart);
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));

        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new InsnNode(Opcodes.BALOAD));
        mn.instructions.add(new IntInsnNode(Opcodes.SIPUSH, 255));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        mn.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 15));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        mn.instructions.add(new IntInsnNode(Opcodes.SIPUSH, 255));
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        mn.instructions.add(new InsnNode(Opcodes.I2C));
        mn.instructions.add(new InsnNode(Opcodes.CASTORE));

        mn.instructions.add(new IincInsnNode(4, 1));
        mn.instructions.add(new JumpInsnNode(Opcodes.GOTO, loopStart));

        mn.instructions.add(loopEnd);
        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));

        cn.methods.add(mn);
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
        return baos.toByteArray();
    }
}
