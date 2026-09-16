//
//  TailscaleDetector.swift
//  Pods
//
//  Created by Dashnyam Batbayar on 2026.09.16.
//

import Foundation
import Network

enum TailscaleState: String {
  case disconnected = "NONE"
  case splitTunnel = "SPLIT_TUNNEL"
  case exitNodeActive = "EXIT_NODE"
}

final class TailscaleDetector {
  private var monitor: NWPathMonitor = NWPathMonitor()
  private let monitorQueue = DispatchQueue(label: "TailscaleDetectorQueue")

  var onStateChanged: ((TailscaleState) -> Void)?

  init() {
    monitor.pathUpdateHandler = { [weak self] path in
      guard let self = self else { return }
      let state = self.evaluateState(currentPath: path)
      DispatchQueue.main.async {
        self.onStateChanged?(state)
      }
    }
    monitor.start(queue: monitorQueue)
  }

  deinit {
    monitor.cancel()
  }

  private func evaluateState(currentPath: NWPath) -> TailscaleState {
    guard currentPath.status == .satisfied else { return .disconnected }

    guard let tailscale = tailscaleInterface() else { return .disconnected }
    
    if interfacesServingDefaultRoute().contains(tailscale.name) {
      return .exitNodeActive
    } else {
      return .splitTunnel
    }
  }

  func tailscaleInterface() -> (name: String, ip: String)? {
    var ifaddr: UnsafeMutablePointer<ifaddrs>?
    guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else { return nil }
    defer { freeifaddrs(ifaddr) }

    for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
      let flags = Int32(ptr.pointee.ifa_flags)
      let name = String(cString: ptr.pointee.ifa_name)

      guard (flags & (IFF_UP | IFF_RUNNING)) == (IFF_UP | IFF_RUNNING), name.starts(with: "utun") else { continue }

      guard let ifaAddr = ptr.pointee.ifa_addr else { continue }

      let addrFamily = ifaAddr.pointee.sa_family
      if addrFamily == UInt8(AF_INET) {
        var addr = ifaAddr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee.sin_addr }
        let ipVal = CFSwapInt32BigToHost(addr.s_addr)

        if ipVal >= 0x64400000 && ipVal <= 0x647FFFFF {
          var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
          inet_ntop(AF_INET, &addr, &hostname, socklen_t(hostname.count))
          return (name, String(cString: hostname))
        }
      }
    }
    return nil
  }
  
  private func interfacesServingDefaultRoute() -> Set<String> {
    var result = Set<String>()
    
    var mib: [Int32] = [CTL_NET, PF_ROUTE, 0, AF_INET, NET_RT_DUMP, 0]
    var len = 0
    guard sysctl(&mib, u_int(mib.count), nil, &len, nil, 0) == 0, len > 0 else { return result }
    
    var buffer = [UInt8](repeating: 0, count: len)
    guard sysctl(&mib, u_int(mib.count), &buffer, &len, nil, 0) == 0 else { return result }
    
    let rtmHeaderSize = 92
    let rtaDst: Int32 = 0x1
    let rtfIfscope: Int32 = 0x1000000

    buffer.withUnsafeBytes { raw in
      var offset = 0

      while offset + rtmHeaderSize <= raw.count {
        let msglen = Int(raw.loadUnaligned(fromByteOffset: offset, as: UInt16.self))
        if msglen == 0 { break }

        let rtmIndex = raw.loadUnaligned(fromByteOffset: offset + 4, as: UInt16.self)
        let rtmFlags = raw.loadUnaligned(fromByteOffset: offset + 8, as: Int32.self)
        let rtmAddrs = raw.loadUnaligned(fromByteOffset: offset + 12, as: Int32.self)

        let dstOffset = offset + rtmHeaderSize
        if (rtmAddrs & rtaDst) != 0, (rtmFlags & rtfIfscope) == 0,
           dstOffset + MemoryLayout<sockaddr_in>.size <= raw.count {
          let dst = raw.loadUnaligned(fromByteOffset: dstOffset, as: sockaddr_in.self)
          if dst.sin_family == UInt8(AF_INET), dst.sin_addr.s_addr == 0 {
            var name = [CChar](repeating: 0, count: Int(IFNAMSIZ))
            if if_indextoname(UInt32(rtmIndex), &name) != nil {
              result.insert(String(cString: name))
            }
          }
        }
        
        offset += msglen
      }
    }
    
    return result
  }
}
