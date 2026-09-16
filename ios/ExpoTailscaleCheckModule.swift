import ExpoModulesCore

public class ExpoTailscaleCheckModule: Module {
  private var detector: TailscaleDetector?
  private var state: TailscaleState?
  private let stateLock = NSLock()

  public func definition() -> ModuleDefinition {
    Name("ExpoTailscaleCheck")

    OnCreate {
      detector = TailscaleDetector()

      detector?.onStateChanged = { [weak self] state in
        guard let self = self else { return }

        self.stateLock.lock()
        let changed = self.state != state
        if changed { self.state = state }
        self.stateLock.unlock()

        if changed {
          self.sendEvent("onStateChange", [
            "state": state.rawValue
          ])
        }
      }
    }

    OnDestroy {
      detector = nil
    }

    Events("onStateChange")

    Function("getState") {
      self.stateLock.lock()
      let current = self.state
      self.stateLock.unlock()
      return current?.rawValue ?? "UNKNOWN"
    }
    
    Function("tailscaleInterface") { () -> [String: String]? in
      guard let interface = detector?.tailscaleInterface() else { return nil }
      return ["name": interface.name, "ip": interface.ip]
    }
  }
}
