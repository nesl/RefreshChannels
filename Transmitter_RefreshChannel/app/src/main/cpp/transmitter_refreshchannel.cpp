#include <jni.h>
#include <string>

#include <android/choreographer.h>
#include <chrono>
#include <android/log.h>
#include <list>

std::string TAG = "Tx_RR_listener";

//jclass clazz;
jmethodID global_setText;
jmethodID global_writeToFile;
jmethodID global_RefreshRateListener;
JNIEnv* global_env;
jobject global_jtextViewObject_Status;
jobject global_Object_MainActivity;

int64_t lastTime = 0;

AChoreographer *choreographer;

int64_t getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
}


static void refreshRateCallback(int64_t vsyncPeriodNanos, void *data) {
    int refreshrate = round( 1e9/vsyncPeriodNanos );
    std::string refreshrate_string = std::to_string(refreshrate);
    std::string log_str ="";

    int64_t curTime = getTimeNsec();
    int64_t duration_ns = curTime - lastTime;

    std::string curTime_string = std::to_string(curTime);
    __android_log_print(ANDROID_LOG_INFO, TAG.c_str(), "Refresh rate listener: %s",  refreshrate_string.c_str());

    // set rr in mainactivity

    global_env->CallVoidMethod(global_Object_MainActivity, global_RefreshRateListener, refreshrate);


    std::string duration_ns_string = std::to_string(duration_ns);
    std::string vsyncPeriodNanos_string = std::to_string(vsyncPeriodNanos);
    log_str = refreshrate_string + " " + duration_ns_string + " " + vsyncPeriodNanos_string;


    // write to file

    lastTime = curTime;


}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_transmitter_1refreshchannel_MainActivity_StartReceiving(
        JNIEnv* env,
        jobject /* this */,
        jobject jtextViewObject_Status,
        jobject jtextViewObject_MainActivity) {

    std::string hello = "Start Receiving";
    // settext from JNI
    // https://stackoverflow.com/questions/33606822/how-to-print-onto-android-screen-from-native-code-in-ndk
    //getting the class
    jclass clazz_TextView = env->FindClass("android/widget/TextView");
    jclass clazz_MainActivity = env->FindClass("com/example/transmitter_refreshchannel/MainActivity");

    //getting the method
//    jmethodID setText = env->GetMethodID(clazz, "setText", "(Ljava/lang/CharSequence;)V");
    global_setText = env->GetMethodID(clazz_TextView, "setText", "(Ljava/lang/CharSequence;)V");
    global_writeToFile = env->GetMethodID(clazz_MainActivity, "writeToFile", "(Ljava/lang/String;)V");

    global_RefreshRateListener = env->GetMethodID(clazz_MainActivity, "RefreshRateListener", "(I)V");


    // set method as global; will be used in callback()
    // must set as global; otherwise error.
    // Not: global_jtextViewObject = jtextViewObject;
    global_env = env;
    global_jtextViewObject_Status = env->NewGlobalRef(jtextViewObject_Status);
    global_Object_MainActivity = env->NewGlobalRef(jtextViewObject_MainActivity);

    // set text to text view
    jstring jstr = global_env->NewStringUTF("Sending:\n"
                                            "Round 0: [1, 1, 0, 0, 1, 1, 0, 0, 0, 1]\n"
                                            "Round 1: [1, 0, 1, 0, 0, 1, 0, 1, 0, 1]\n"
                                            "Round 2: [1, 1, 1, 1, 0, 0, 1, 0, 1, 1]\n"
                                            );
    global_env->CallVoidMethod(global_jtextViewObject_Status, global_setText, jstr);

    // read refresh rate using callback api in NDK
    choreographer = AChoreographer_getInstance();
    AChoreographer_registerRefreshRateCallback(choreographer, refreshRateCallback, nullptr);
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_receiver_1refreshchannel_MainActivity_StopReceiving(
        JNIEnv* env,
        jobject /* this */) {

    std::string hello = "Stop Receiving";

    // read refresh rate using callback api in NDK
    choreographer = AChoreographer_getInstance();
    AChoreographer_unregisterRefreshRateCallback(choreographer, refreshRateCallback, nullptr);

    return env->NewStringUTF(hello.c_str());
}
