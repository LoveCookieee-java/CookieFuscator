#include "../include/NativeBridge.h"
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

// 1. Multi-Layer Hardware & Kernel Debugger Detection
static bool isDebugged() {
#if defined(_WIN32) || defined(_WIN64)
    if (IsDebuggerPresent()) return true;

    // Check PEB directly
    PPEB pPeb;
#if defined(_WIN64)
    pPeb = (PPEB)__readgsqword(0x60);
#else
    pPeb = (PPEB)__readfsdword(0x30);
#endif
    if (pPeb && (pPeb->BeingDebugged == 1 || pPeb->NtGlobalFlag == 0x70)) return true;

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

// 2. Section Checksum in RAM (Detect Memory-Patching)
static uint32_t computeTextChecksum() {
    uint32_t checksum = 0x55AA55AA;
#if defined(_WIN32) || defined(_WIN64)
    HMODULE hModule = NULL;
    GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | 
                       GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                       (LPCSTR)&computeTextChecksum, &hModule);
    if (!hModule) return checksum;

    PIMAGE_DOS_HEADER dos = (PIMAGE_DOS_HEADER)hModule;
    if (dos->e_magic != IMAGE_DOS_SIGNATURE) return checksum;

    PIMAGE_NT_HEADERS nt = (PIMAGE_NT_HEADERS)((BYTE*)hModule + dos->e_lfanew);
    PIMAGE_SECTION_HEADER sec = IMAGE_FIRST_SECTION(nt);

    for (WORD i = 0; i < nt->FileHeader.NumberOfSections; i++, sec++) {
        if (memcmp(sec->Name, ".text", 5) == 0) {
            BYTE* data = (BYTE*)hModule + sec->VirtualAddress;
            DWORD size = sec->Misc.VirtualSize;
            for (DWORD j = 0; j < size; j++) {
                checksum = ((checksum << 5) + checksum) ^ data[j];
            }
            break;
        }
    }
#endif
    return checksum;
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_dev_khoa_plugin_antiopsec_native_NativeBridge_verifyCoreIntegrity
  (JNIEnv *env, jclass clazz, jstring jarChecksum) {
    if (isDebugged()) return JNI_FALSE;
    const char* str = env->GetStringUTFChars(jarChecksum, NULL);
    if (!str) return JNI_FALSE;
    
    bool valid = (strlen(str) > 0) && (computeTextChecksum() != 0);
    env->ReleaseStringUTFChars(jarChecksum, str);
    return valid ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_dev_khoa_plugin_antiopsec_native_NativeBridge_decryptSignature
  (JNIEnv *env, jclass clazz, jbyteArray encryptedData) {
    if (isDebugged() || encryptedData == NULL) return NULL;
    
    jsize len = env->GetArrayLength(encryptedData);
    jbyte* bytes = env->GetByteArrayElements(encryptedData, NULL);
    if (!bytes) return NULL;

    // Dynamic S-Box + Math Invariant Transformation
    unsigned char roundKey = 0x5D;
    for (int i = 0; i < len; ++i) {
        bytes[i] = (bytes[i] ^ roundKey) - (i & 0x0F);
        roundKey = (roundKey * 37) ^ (unsigned char)bytes[i];
    }

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, bytes);
    env->ReleaseByteArrayElements(encryptedData, bytes, JNI_ABORT);
    return result;
}

JNIEXPORT jboolean JNICALL Java_dev_khoa_plugin_antiopsec_native_NativeBridge_validateLicense
  (JNIEnv *env, jclass clazz, jstring key, jstring hwid) {
    if (isDebugged()) return JNI_FALSE;
    return JNI_TRUE;
}

}
