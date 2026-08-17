#include <jni.h>
#include <string>

#include <android/choreographer.h>
#include <chrono>
#include <android/log.h>
#include <list>

std::string TAG = "Receiver";

jboolean completed = false;
jboolean sync = false;

//jclass clazz;
jmethodID global_setText;
jmethodID global_writeToFile;
JNIEnv* global_env;
jobject global_jtextViewObject_Status;
jobject global_jtextViewObject_DataID;
jobject global_Object_MainActivity;

int sync_count = 0;
int sync_Num = 5;

// 10, 24, 30, 48, 60, 96, 120
// mode 1
        float refreshrate_0 = (float) 96;
        float refreshrate_1 = (float) 120;
        float refreshrate_sync = (float) 30;
        float refreshrate_end = (float) 48;

// 10, 24, 30, 48, 60, 96, 120
// mode 2
//float refreshrate_0 = (float) 60;
//float refreshrate_1 = (float) 96;
//float refreshrate_sync = (float) 48;
//float refreshrate_end = (float) 10;

// 10, 24, 30, 48, 60, 96, 120
// mode 3
//float refreshrate_0 = (float) 60;
//float refreshrate_1 = (float) 120;
//float refreshrate_sync = (float) 48;
//float refreshrate_end = (float) 10;

// 10, 24, 30, 48, 60, 96, 120
// mode 4
//float refreshrate_0 = (float) 48;
//float refreshrate_1 = (float) 120;
//float refreshrate_sync = (float) 96;
//float refreshrate_end = (float) 10;

// 10, 24, 30, 48, 60, 96, 120
// mode 5: 48, 96
//float refreshrate_0 = (float) 48;
//float refreshrate_1 = (float) 96;
//float refreshrate_sync = (float) 30;
//float refreshrate_end = (float) 10;


// 10, 24, 30, 48, 60, 96, 120
// mode 6: 48, 60
//float refreshrate_0 = (float) 48;
//float refreshrate_1 = (float) 60;
//float refreshrate_sync = (float) 30;
//float refreshrate_end = (float) 10;

std::list<int> data_received;
int last_bit = -1;


int64_t lastTime;
int interval_ms;

AChoreographer *choreographer;

int64_t getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (int64_t) now.tv_sec*1000000000LL + now.tv_nsec;
}


static void refreshRateCallback(int64_t vsyncPeriodNanos, void *data) {
    int refreshrate = round( 1e9/vsyncPeriodNanos );
    std::string refreshrate_string = std::to_string(refreshrate);

    int64_t curTime = getTimeNsec()/ 1000000;
    std::string curTime_string = std::to_string(curTime);
//    __android_log_print(ANDROID_LOG_INFO, curTime_string.c_str(), "Refresh rate: %s",  refreshrate_string.c_str());
    __android_log_print(ANDROID_LOG_INFO, TAG.c_str(), "Refresh rate: %s",  refreshrate_string.c_str());

    int noPreviousBits = round((curTime - lastTime)*1.0/interval_ms);
    std::string noPreviousBits_str = std::to_string(noPreviousBits);




    // new Rx
    if(refreshrate == refreshrate_sync){
        __android_log_write(ANDROID_LOG_ERROR, TAG.c_str(), "......Received sync");
        data_received= {};
        last_bit = -1; // sync
    } else if(refreshrate == refreshrate_0){
//        std::string log_str = "Receiving 0... noPreviousBits:" + noPreviousBits_str;
//        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());

        if(last_bit != -1){ // sync
            std::string log_str = "......Received " + noPreviousBits_str + " bits: " + std::to_string(last_bit) ;
            __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());
            while(noPreviousBits>0){
                data_received.push_back(last_bit);
                noPreviousBits--;
            }
        }
        last_bit = 0;

        std::string log_str = "Now receiving 0... ";
        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());
    }
    else if(refreshrate == refreshrate_1){
//        std::string log_str = "Receiving 1... noPreviousBits:" + noPreviousBits_str;
//        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());



        if(last_bit != -1){
            std::string log_str = "......Received " + noPreviousBits_str + " bits: " + std::to_string(last_bit);
            __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());
            while(noPreviousBits>0){
                data_received.push_back(last_bit);
                noPreviousBits--;
            }
        }
        last_bit = 1;
        std::string log_str = "Now receiving 1... ";
        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());

    } else if( refreshrate == refreshrate_end){
//        std::string log_str = "Receiving end... noPreviousBits:" + noPreviousBits_str;
//        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());

        if(last_bit != -1){
            std::string log_str = "......Received " + noPreviousBits_str + " bits: " + std::to_string(last_bit);
            __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());
            while(noPreviousBits>0){
                data_received.push_back(last_bit);
                noPreviousBits--;
            }
        }
        std::string log_str = "......Received end!";
        __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), log_str.c_str());
        // convert to string
        std::string data_received_str;
        for (int x : data_received) {
            data_received_str.append(std::to_string(x) + ' ');

        }
        log_str = "Received " + std::to_string(data_received.size()) +" bits: " + data_received_str;
        __android_log_write(ANDROID_LOG_ERROR, TAG.c_str(), log_str.c_str());

        // show received data in textview
        jstring jstr = global_env->NewStringUTF(log_str.c_str());
        global_env->CallVoidMethod(global_jtextViewObject_Status, global_setText, jstr);

        // write/store to file
        jstr = global_env->NewStringUTF(data_received_str.c_str());
        global_env->CallVoidMethod(global_Object_MainActivity, global_writeToFile, jstr);
        data_received= {};
        last_bit = -1;
    }

    lastTime = curTime;


}


extern "C" JNIEXPORT jstring JNICALL
Java_com_example_receiver_1refreshchannel_MainActivity_StartReceiving(
        JNIEnv* env,
        jobject /* this */,
        int interval,
        jobject jtextViewObject_Status,
//        jobject jtextViewObject_DataID,
        jobject jtextViewObject_MainActivity) {

    std::string hello = "Start Receiving: " + std::to_string(interval);
    __android_log_write(ANDROID_LOG_INFO, TAG.c_str(), "Waiting for receiving...");

    // settext from JNI
    // https://stackoverflow.com/questions/33606822/how-to-print-onto-android-screen-from-native-code-in-ndk
    //getting the class
    jclass clazz_TextView = env->FindClass("android/widget/TextView");
    jclass clazz_MainActivity = env->FindClass("com/example/receiver_refreshchannel/MainActivity");

    //getting the method
//    jmethodID setText = env->GetMethodID(clazz, "setText", "(Ljava/lang/CharSequence;)V");
    global_setText = env->GetMethodID(clazz_TextView, "setText", "(Ljava/lang/CharSequence;)V");
    global_writeToFile = env->GetMethodID(clazz_MainActivity, "writeToFile", "(Ljava/lang/String;)V");

    // set method as global; will be used in callback()
    // must set as global; otherwise error.
    // Not: global_jtextViewObject = jtextViewObject;
    global_env = env;
    global_jtextViewObject_Status = env->NewGlobalRef(jtextViewObject_Status);
//    global_jtextViewObject_DataID = env->NewGlobalRef(jtextViewObject_DataID);
    global_Object_MainActivity = env->NewGlobalRef(jtextViewObject_MainActivity);

    // set text to text view
    jstring jstr = global_env->NewStringUTF("Status: receiving...");
    global_env->CallVoidMethod(global_jtextViewObject_Status, global_setText, jstr);

//    global_env->CallVoidMethod(global_Object_MainActivity, global_writeToFile, jstr);


    completed = false;
    sync = false;
    interval_ms = interval;
    last_bit = -1;
    data_received = {};
    sync_count = 0;


    // read refresh rate using callback api in NDK
    choreographer = AChoreographer_getInstance();
//    void *data;
    AChoreographer_registerRefreshRateCallback(choreographer, refreshRateCallback, nullptr);

    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_receiver_1refreshchannel_MainActivity_StopReceiving(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Stop Receiving";


    completed = false;
    sync = false;
    last_bit = -1;
    data_received = {};
    sync_count = 0;

    // read refresh rate using callback api in NDK
    choreographer = AChoreographer_getInstance();
//    void *data;
    AChoreographer_unregisterRefreshRateCallback(choreographer, refreshRateCallback, nullptr);

    return env->NewStringUTF(hello.c_str());
}