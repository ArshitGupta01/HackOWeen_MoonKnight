import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Starts Koin (dependency injection) before any screen is created.
        KoinIosKt.startKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
