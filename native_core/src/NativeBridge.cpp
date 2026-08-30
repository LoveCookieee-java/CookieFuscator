#include <jni.h>
#include <vector>
#include <string>
#include <cstring>
#include <chrono>

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <intrin.h>
#include <psapi.h>
#else
#include <sys/ptrace.h>
#include <unistd.h>
#include <sys/mman.h>
#endif

// 1. Hardware & Kernel Debugger Detection
static bool isDebugged() {
#if defined(_WIN32) || defined(_WIN64)
    if (IsDebuggerPresent()) return true;

    // Check PEB directly (BeingDebugged at 0x02, NtGlobalFlag at 0xBC for x64 / 0x68 for x86)
#if defined(_WIN64)
    unsigned char* pPeb = (unsigned char*)__readgsqword(0x60);
    if (pPeb) {
        if (pPeb[2] == 1) return true;
        if (*(unsigned int*)(pPeb + 0xBC) == 0x70) return true;
    }
#else
    unsigned char* pPeb = (unsigned char*)__readfsdword(0x30);
    if (pPeb) {
        if (pPeb[2] == 1) return true;
        if (*(unsigned int*)(pPeb + 0x68) == 0x70) return true;
    }
#endif

    // Hardware breakpoints (Dr0-Dr3)
    CONTEXT ctx;
    ZeroMemory(&ctx, sizeof(CONTEXT));
    ctx.ContextFlags = CONTEXT_DEBUG_REGISTERS;
    HANDLE hThread = GetCurrentThread();
    if (GetThreadContext(hThread, &ctx)) {
        if (ctx.Dr0 || ctx.Dr1 || ctx.Dr2 || ctx.Dr3) return true;
    }
#else
    if (ptrace(PTRACE_TRACEME, 0, 1, 0) < 0) return true;
#endif
    return false;
}

// 2. Native Payload Decryptor
static jbyteArray JNICALL native_decrypt(JNIEnv *env, jclass clazz, jbyteArray encryptedData) {
    if (isDebugged() || encryptedData == NULL) return NULL;

    jsize len = env->GetArrayLength(encryptedData);
    jbyte* bytes = env->GetByteArrayElements(encryptedData, NULL);
    if (!bytes) return NULL;

    std::vector<unsigned char> dec(len);
    unsigned char roundKey = 0x5D;
    for (int i = 0; i < len; ++i) {
        unsigned char e = (unsigned char)bytes[i];
        unsigned char val = (e ^ roundKey) & 0xFF;
        unsigned char p = (val - (i & 0x0F)) & 0xFF;
        dec[i] = p;
        roundKey = ((roundKey * 37) ^ p) & 0xFF;
    }

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, (jbyte*)dec.data());
    
    // Wipe memory
    env->ReleaseByteArrayElements(encryptedData, bytes, JNI_ABORT);
#if defined(_WIN32) || defined(_WIN64)
    SecureZeroMemory(dec.data(), dec.size());
#else
    memset(dec.data(), 0, dec.size());
#endif
    return result;
}

extern "C" {

// Static export fallback
JNIEXPORT jbyteArray JNICALL Java_cookie_fack_please_d111_Bootstrap_decryptNative
  (JNIEnv *env, jclass clazz, jbyteArray encryptedData) {
    return native_decrypt(env, clazz, encryptedData);
}

// JNI_OnLoad - Dynamic RegisterNatives (0 IDA Exports)
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }

    jclass cls = env->FindClass("cookie/fack/please/d111/Bootstrap");
    if (cls != NULL) {
        JNINativeMethod methods[] = {
            {(char*)"decryptNative", (char*)"([B)[B", (void*)&native_decrypt}
        };
        env->RegisterNatives(cls, methods, 1);
        env->DeleteLocalRef(cls);
    }
    return JNI_VERSION_1_8;
}

}
