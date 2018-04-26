#include <iostream>
#include <iomanip>
#include <vector>
#include <set>
#include <string>
#include <cstdlib>
#include <pthread.h>
#include <unistd.h>
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
using std::runtime_error;


using namespace Tins;

class Scanner {
public:
    Scanner(const NetworkInterface& interface,
            const IPv4Address& address);

    void run();
private:
    void ping_sweep(const NetworkInterface& iface, IPv4Address dest_ip);
    bool callback(PDU& pdu);
    static void* thread_proc(void* param);
    void launch_sniffer();

    NetworkInterface iface;
    IPv4Address host_to_scan;
    Sniffer sniffer;
};

Scanner::Scanner(const NetworkInterface& interface,
                 const IPv4Address& address)
        : iface(interface), host_to_scan(address), sniffer(interface.name()) {
    sniffer.set_filter("icmp");
}

void* Scanner::thread_proc(void* param) {
    Scanner* data = (Scanner*)param;
    data->launch_sniffer();
    return 0;
}

void Scanner::launch_sniffer() {
    sniffer.sniff_loop(make_sniffer_handler(this, &Scanner::callback));
}

/* Our scan handler. This will receive SYNs and RSTs and inform us
 * the scanned port's status.
 */
bool Scanner::callback(PDU& pdu) {
    // Find the layers we want.
    const IP& ip = pdu.rfind_pdu<IP>();
    const ICMP& icmp = pdu.rfind_pdu<ICMP>();
    cout << "IP: " << ip.src_addr() << " ICMP:" << icmp.type() ;
    return !(icmp.type() == ICMP::PARAM_PROBLEM);
}

void Scanner::run() {
    pthread_t thread;
    // Launch our sniff thread.
    pthread_create(&thread, 0, &Scanner::thread_proc, this);
    // Start sending SYNs to port.
    ping_sweep(iface, host_to_scan);

    // Wait for our sniffer.
    void* dummy;
    pthread_join(thread, &dummy);
}

void Scanner::ping_sweep(const NetworkInterface& iface, IPv4Address dest_ip) {
    // Retrieve the addresses.
    NetworkInterface::Info info = iface.addresses();
    PacketSender sender;
    // Allocate the IP PDU
    IP ip = IP(dest_ip, info.ip_addr) / ICMP();
    // Get the reference to the ICMP PDU
    ICMP& icmp = ip.rfind_pdu<ICMP>();
    // Set the SYN flag on.
    icmp.type(ICMP::ECHO_REQUEST);
    // Just some random port.
    cout << "Sending PINGs..." << endl;
    for (int i : {0, 1, 2, 3}) {
        // Set the new port and send the packet!
        sender.send(ip);
        // Wait 1 second.
        sleep(1);
    }
    /* Special packet to indicate that we're done. This will be sniffed
     * by our function, which will in turn return false.
     */
    icmp.type(ICMP::PARAM_PROBLEM);
    // Pretend we're the scanned host...
    ip.src_addr(dest_ip);
    // We use an ethernet pdu, otherwise the kernel will drop it.
    EthernetII eth = EthernetII(info.hw_addr, info.hw_addr) / ip;
    sender.send(eth, iface);
}

void scan(int argc, char* argv[]) {
    IPv4Address ip(argv[1]);
    // Resolve the interface which will be our gateway
    NetworkInterface iface(ip);
    cout << "Sniffing on interface: " << iface.name() << endl;
    Scanner scanner(iface, ip);
    scanner.run();
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        cout << "Usage: " <<* argv << " <IPADDR>" << endl;
        return 1;
    }
    try {
        scan(argc, argv);
    }
    catch(runtime_error& ex) {
        cout << "Error - " << ex.what() << endl;
    }
}