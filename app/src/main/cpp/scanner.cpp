#include <iostream>
#include <iomanip>
#include <vector>
#include <set>
#include <string>
#include <cstdlib>
#include <pthread.h>
#include <unistd.h>
#include <chrono>
#include <thread>

#include <tins/ip.h>
#include <tins/tcp.h>
#include <tins/ip_address.h>
#include <tins/ethernetII.h>
#include <tins/network_interface.h>
#include <tins/sniffer.h>
#include <tins/utils.h>
#include <tins/packet_sender.h>
#include <tins/tins.h>

using std::cout;
using std::endl;
using std::vector;
using std::pair;
using std::setw;
using std::string;
using std::set;
using std::map;
using std::runtime_error;


using namespace Tins;

class Scanner {
public:
    Scanner(const NetworkInterface& interface);
    void run();
private:
    NetworkInterface iface;
    Sniffer sniffer;
    map<IPv4Address, HWAddress<6>> ip2mac;
    bool callback(PDU& pdu);
    static void* thread_proc(void* param);
    void launch_sniffer();
    void arp_sweep(const IPv4Range &v4Range);
};

Scanner::Scanner(const NetworkInterface& interface)
        : iface(interface), sniffer(interface.name()) {
    sniffer.set_filter("arp or icmp");
}
void* Scanner::thread_proc(void* param) {
    Scanner* data = (Scanner*)param;
    data->launch_sniffer();
    return 0;
}
void Scanner::launch_sniffer() {
    sniffer.sniff_loop(make_sniffer_handler(this, &Scanner::callback));
}

// Scan packet handler
bool Scanner::callback(PDU& pdu) {
    // Parse it as a DHCP PDU.
    const ARP* arp = pdu.find_pdu<ARP>();
    const ICMP* icmp = pdu.find_pdu<ICMP>();

    if(arp != 0 && arp->opcode() == ARP::REPLY){
        // Check if we have seen this address
        auto iter = ip2mac.find(arp->sender_ip_addr());
        // Continue if we already know about this address
        if(iter != ip2mac.end()) return true;
        // Add address if we do not know about it
        ip2mac.insert({ arp->sender_ip_addr(), arp->sender_hw_addr() });
    }
    // STOP if special packet is received
    return icmp == 0 || !(icmp->type() == ICMP::PARAM_PROBLEM);
}
void Scanner::run() {
    // Get address range TODO: IPv6?
    IPv4Range v4Range = IPv4Range::from_mask(iface.ipv4_address(), iface.ipv4_mask());
    // Launch our sniff thread.
    pthread_t thread;
    pthread_create(&thread, 0, &Scanner::thread_proc, this);
    arp_sweep(v4Range);
    // Wait for our sniffer.
    void* dummy;
    pthread_join(thread, &dummy);
    // Display results
    for(auto val: ip2mac){
        cout << val.first << "=>" << val.second << endl;
    }
}
void Scanner::arp_sweep(const IPv4Range &v4Range) {
    PacketSender sender;
    auto info = iface.info();
    ip2mac.insert({ info.ip_addr, info.hw_addr });
    for(int _ = 0; _ < 2; _++ ){
        for(const auto &addr : v4Range){
            EthernetII eth = ARP::make_arp_request(addr, info.ip_addr, info.hw_addr);
            sender.send(eth, iface);
            std::this_thread::sleep_for(std::chrono::milliseconds(5));
        }
    }
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    // Special packet to indicate that we're done. This will be sniffed
    // by our function, which will in turn return false.
    IP ip = IP(info.ip_addr, info.ip_addr) / ICMP(ICMP::PARAM_PROBLEM);
    // We use an ethernet pdu, otherwise the kernel will drop it.
    EthernetII eth = EthernetII(info.hw_addr, info.hw_addr) / ip;
    sender.send(eth, iface);
}
void scan(int argc, char* argv[]) {
    // Resolve the interface which will be our gateway
    NetworkInterface iface(argv[1]);
    cout << "Sniffing on interface: " << iface.name() << endl;
    Scanner scanner(iface);
    scanner.run();
}
int main(int argc, char* argv[]) {
    if (argc < 2) {
        cout << "Usage: " <<* argv << " <interface> <scan type>" << endl;
        return 1;
    }
    try {
        scan(argc, argv);
    }
    catch(runtime_error& ex) {
        cout << "Error - " << ex.what() << endl;
    }
}